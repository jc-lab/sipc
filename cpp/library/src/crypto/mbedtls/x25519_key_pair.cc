/**
 * @file	x25519_key_pair.cc
 * @author	Joseph Lee <joseph@jc-lab.net>
 * @date	2021-06-06
 * @copyright Copyright (C) 2021 jc-lab.
 *            This software may be modified and distributed under the terms
 *            of the Apache License 2.0.  See the LICENSE file for details.
 */


#include <string.h>

#include <mbedtls/ecp.h>
#include <mbedtls/ecdh.h>
#include <mbedtls/pk.h>
#include <mbedtls/asn1.h>
#include <mbedtls/asn1write.h>
#include <mbedtls/oid.h>
#include <mbedtls/error.h>

#ifndef MBEDTLS_ERROR_ADD
#define MBEDTLS_ERROR_ADD( high, low ) \
        mbedtls_error_add( high, low, __FILE__, __LINE__ )
#endif

#include <jcu-sipc/crypto/secure_random.h>

#include "crypto_impl.h"
#include "x25519_key_factory.h"
#include "x25519_key_pair.h"

namespace jcu {
namespace sipc {
namespace crypto {
namespace mbedtls {

static int secure_random_rng_cb(void *ctx, unsigned char * buf, size_t size) {
  auto* secure_random = (SecureRandom*)ctx;
  return secure_random->random(buf, size);
}

X25519KeyPair::X25519KeyPair(const X25519KeyFactory *key_factory) {
  crypto_impl_ = key_factory->getCryptoImpl();
  auto secure_random = crypto_impl_->getDefaultSecureRandom();

  mbedtls_ecdh_init(&ctx_);
  key_factory->ecpGroupCopyTo(&ctx_.grp);

  init_rc_ = mbedtls_ecdh_gen_public(
      &ctx_.grp,
      &ctx_.d,
      &ctx_.Q,
      secure_random_rng_cb,
      secure_random
      );
}

X25519KeyPair::~X25519KeyPair() {
  mbedtls_ecdh_free(&ctx_);
}

bool X25519KeyPair::isReady() const {
  return init_rc_ == 0;
}

int X25519KeyPair::getLibraryErrno() const {
  return init_rc_;
}

std::string X25519KeyPair::getAlgorithm() const {
  return "x25519";
}

/* Get a PK algorithm identifier
 *
 *  AlgorithmIdentifier  ::=  SEQUENCE  {
 *       algorithm               OBJECT IDENTIFIER,
 *       parameters              ANY DEFINED BY algorithm OPTIONAL  }
 */
static int pk_get_pk_alg( unsigned char **p,
                          const unsigned char *end,
                          mbedtls_asn1_buf *alg_oid, mbedtls_asn1_buf *params)
{
  int ret = MBEDTLS_ERR_ERROR_CORRUPTION_DETECTED;

  memset( params, 0, sizeof(mbedtls_asn1_buf) );

  if( ( ret = mbedtls_asn1_get_alg( p, end, alg_oid, params ) ) != 0 )
    return( MBEDTLS_ERR_PK_INVALID_ALG + ret );

  return( 0 );
}

/*
 * EC public key is an EC point
 *
 * The caller is responsible for clearing the structure upon failure if
 * desired. Take care to pass along the possible ECP_FEATURE_UNAVAILABLE
 * return code of mbedtls_ecp_point_read_binary() and leave p in a usable state.
 */
static int pk_get_ecpubkey( unsigned char **p, const unsigned char *end,
                            mbedtls_ecp_keypair *key )
{
  int ret = MBEDTLS_ERR_ERROR_CORRUPTION_DETECTED;

  if( ( ret = mbedtls_ecp_point_read_binary( &key->grp, &key->Q,
                                             (const unsigned char *) *p, end - *p ) ) == 0 )
  {
    ret = mbedtls_ecp_check_pubkey( &key->grp, &key->Q );
  }

  /*
   * We know mbedtls_ecp_point_read_binary consumed all bytes or failed
   */
  *p = (unsigned char *) end;

  return( ret );
}

static int my_mbedtls_pk_parse_public_key( mbedtls_pk_context *ctx, const mbedtls_ecp_group* inherit_grp,
                                 const unsigned char *key, size_t keylen )
{
  int ret = MBEDTLS_ERR_ERROR_CORRUPTION_DETECTED;
  size_t len;

  mbedtls_asn1_buf alg_oid = {0};
  mbedtls_asn1_buf alg_params = {0};

  unsigned char *p = (unsigned char *) key;
  const unsigned char* end = key + keylen;

  if( ( ret = mbedtls_asn1_get_tag(&p, end, &len,
                                    MBEDTLS_ASN1_CONSTRUCTED | MBEDTLS_ASN1_SEQUENCE ) ) != 0 )
  {
    return (MBEDTLS_ERR_PK_KEY_INVALID_FORMAT + ret);
  }

  end = p + len;

  if( ( ret = pk_get_pk_alg( &p, end, &alg_oid, &alg_params ) ) != 0 )
    return( ret );

  if( ( ret = mbedtls_asn1_get_bitstring_null( &p, end, &len ) ) != 0 )
    return( MBEDTLS_ERR_PK_INVALID_PUBKEY + ret );

  if( p + len != end )
    return( MBEDTLS_ERR_PK_INVALID_PUBKEY + MBEDTLS_ERR_ASN1_LENGTH_MISMATCH );

  if( ( ret = mbedtls_pk_setup( ctx, mbedtls_pk_info_from_type(MBEDTLS_PK_ECKEY) ) ) != 0 )
    return( ret );

  {
    auto kp = mbedtls_pk_ec(*ctx);
    mbedtls_ecp_group_copy(&kp->grp, inherit_grp);
    ret = pk_get_ecpubkey( &p, end, kp );
  }

  if( ret == 0 && p != end )
    ret = ( MBEDTLS_ERR_PK_INVALID_PUBKEY + MBEDTLS_ERR_ASN1_LENGTH_MISMATCH );

  if( ret != 0 )
    mbedtls_pk_free( ctx );

  return( ret );
}

int X25519KeyPair::derive(
    std::vector<unsigned char> &output,
    const unsigned char *public_key_bytes,
    int public_key_length
) const {
  int rc;

  auto secure_random = crypto_impl_->getDefaultSecureRandom();

  mbedtls_pk_context public_key_ctx = { 0 };
  mbedtls_mpi shared_key = {0};

  do {
    mbedtls_mpi_init(&shared_key);

    rc = my_mbedtls_pk_parse_public_key(&public_key_ctx, &ctx_.grp, public_key_bytes, public_key_length);
    if (rc) break;
    auto ecp_public_key = mbedtls_pk_ec(public_key_ctx);

    mbedtls_mpi_lset(&ecp_public_key->Q.Z, 1);
    rc = mbedtls_ecdh_compute_shared(
        (mbedtls_ecp_group *) &ctx_.grp,
        &shared_key,
        &ecp_public_key->Q,
        &ctx_.d,
        secure_random_rng_cb,
        secure_random
        );
    if (rc) break;

    size_t shared_size = mbedtls_mpi_size(&shared_key);
    output.resize(shared_size);
    rc = mbedtls_mpi_write_binary_le(&shared_key, output.data(), output.size());
  } while (false);

  mbedtls_mpi_free(&shared_key);
  mbedtls_pk_free(&public_key_ctx);

  return rc;
}

static int my_mbedtls_pk_write_pubkey_der( const mbedtls_pk_context *key, unsigned char *buf, size_t size)
{
  int ret = MBEDTLS_ERR_ERROR_CORRUPTION_DETECTED;
  unsigned char *c;
  size_t len = 0, par_len = 0, oid_len;
  mbedtls_pk_type_t pk_type;
  const char *oid;

  c = buf + size;

  MBEDTLS_ASN1_CHK_ADD( len, mbedtls_pk_write_pubkey( &c, buf, key ) );

  if( c - buf < 1 )
    return( MBEDTLS_ERR_ASN1_BUF_TOO_SMALL );

  /*
   *  SubjectPublicKeyInfo  ::=  SEQUENCE  {
   *       algorithm            AlgorithmIdentifier,
   *       subjectPublicKey     BIT STRING }
   */
  *--c = 0;
  len += 1;

  unsigned char ec_param_oid[] = { 0x2B, 0x65, 0x6E };

  MBEDTLS_ASN1_CHK_ADD( len, mbedtls_asn1_write_len( &c, buf, len ) );
  MBEDTLS_ASN1_CHK_ADD( len, mbedtls_asn1_write_tag( &c, buf, MBEDTLS_ASN1_BIT_STRING ) );

  MBEDTLS_ASN1_CHK_ADD( len, mbedtls_asn1_write_algorithm_identifier( &c, buf, (const char*) ec_param_oid, sizeof(ec_param_oid), 0 ) );

  MBEDTLS_ASN1_CHK_ADD( len, mbedtls_asn1_write_len( &c, buf, len ) );
  MBEDTLS_ASN1_CHK_ADD( len, mbedtls_asn1_write_tag( &c, buf, MBEDTLS_ASN1_CONSTRUCTED |
      MBEDTLS_ASN1_SEQUENCE ) );

  return( (int) len );
}

int X25519KeyPair::getPublicKey(std::vector<unsigned char> &output) const {
  int rc;
  mbedtls_pk_context pk_ctx;
  mbedtls_ecp_keypair* kp;

  mbedtls_pk_init(&pk_ctx);

  do {
    rc = mbedtls_pk_setup(&pk_ctx, mbedtls_pk_info_from_type(MBEDTLS_PK_ECKEY));
    if (rc) break;

    kp = mbedtls_pk_ec(pk_ctx);
    mbedtls_ecp_group_copy(&kp->grp, &ctx_.grp);
    mbedtls_ecp_copy(&kp->Q, &ctx_.Q);

    const char *oid;
    size_t olen;
    rc = mbedtls_oid_get_oid_by_ec_grp(kp->grp.id, &oid, &olen);

    output.resize(mbedtls_pk_get_len(&pk_ctx) + 1024);
    int output_len = my_mbedtls_pk_write_pubkey_der(&pk_ctx, output.data(), output.size());
    if (output_len < 0) {
      rc = output_len;
      break;
    }
    memmove(output.data(), output.data() + output.size() - output_len, output_len);

    output.resize(output_len);
  } while(0);

  mbedtls_pk_free(&pk_ctx);

  return 0;
}

} // namespace mbedtls
} // namespace crypto
} // namespace sipc
} // namespace jcu
