/**
 * @file	crypto_impl.cc
 * @author	Joseph Lee <joseph@jc-lab.net>
 * @date	2021-06-06
 * @copyright Copyright (C) 2021 jc-lab.
 *            This software may be modified and distributed under the terms
 *            of the Apache License 2.0.  See the LICENSE file for details.
 */


#include <mbedtls/md.h>
#include <mbedtls/sha256.h>
#include <mbedtls/gcm.h>

#include "crypto_impl.h"

#include <memory>

#include "secure_random_impl.h"
#include "x25519_key_factory.h"

namespace jcu {
namespace sipc {
namespace crypto {

std::shared_ptr<MbedtlsCrypto> MbedtlsCrypto::create() {
  return mbedtls::CryptoImpl::create();
}

namespace mbedtls {

CryptoImpl::CryptoImpl() {
  mbedtls_entropy_init(&entropy_);
}

CryptoImpl::~CryptoImpl() {
  mbedtls_entropy_free(&entropy_);
}

void CryptoImpl::earlyInit() {
  default_secure_random_ = createSecureRandom();
}

std::shared_ptr<CryptoImpl> CryptoImpl::create() {
  auto instance = std::make_shared<CryptoImpl>();
  instance->self_ = instance;
  instance->earlyInit();
  return instance;
}

entropy_func_t CryptoImpl::getEntropyFunc() const {
  return mbedtls_entropy_func;
}

mbedtls_entropy_context *CryptoImpl::getEntropyContext() const {
  return (mbedtls_entropy_context *) &entropy_;
}

std::unique_ptr<SecureRandom> CryptoImpl::createSecureRandom() const {
  std::shared_ptr<CryptoImpl> self(self_.lock());
  return std::unique_ptr<SecureRandom>(new SecureRandomImpl(self));
}

std::shared_ptr<EphemeralKeyFactory> CryptoImpl::getECKeyFactory(const std::string &algorithm) const {
  std::shared_ptr<CryptoImpl> self(self_.lock());
  if (algorithm == "x25519") {
    return std::make_shared<X25519KeyFactory>(self);
  }
  return nullptr;
}

SecureRandom *CryptoImpl::getDefaultSecureRandom() const {
  return default_secure_random_.get();
}
int CryptoImpl::hmacSha256(
    unsigned char *output,
    const unsigned char *key_bytes,
    int key_length,
    const unsigned char *message_bytes,
    int message_length
) const {
  mbedtls_md_context_t ctx;
  mbedtls_md_type_t md_type = MBEDTLS_MD_SHA256;
  mbedtls_md_init(&ctx);
  mbedtls_md_setup(&ctx, mbedtls_md_info_from_type(MBEDTLS_MD_SHA256), 1); //use hmac
  mbedtls_md_hmac_starts(&ctx, key_bytes, key_length);
  mbedtls_md_hmac_update(&ctx, message_bytes, message_length);
  mbedtls_md_hmac_finish(&ctx, output);
  return 0;
}

int CryptoImpl::aesGcmEncrypt(
    std::vector<unsigned char> &cipher_text,
    std::vector<unsigned char> &auth_tag,
    const unsigned char *key_bytes,
    int key_length,
    const unsigned char *iv_bytes,
    int iv_length,
    int auth_tag_length,
    const unsigned char *message_bytes,
    int message_length
) const {
  mbedtls_gcm_context aes;
  mbedtls_gcm_init(&aes);
  mbedtls_gcm_setkey(&aes, MBEDTLS_CIPHER_ID_AES, key_bytes, key_length * 8);
  cipher_text.resize(message_length);
  auth_tag.resize(auth_tag_length / 8);
  mbedtls_gcm_crypt_and_tag(
      &aes,
      MBEDTLS_GCM_ENCRYPT,
      message_length,
      iv_bytes,
      iv_length,
      nullptr,
      0,
      message_bytes,
      cipher_text.data(),
      auth_tag_length / 8,
      auth_tag.data()
  );
  mbedtls_gcm_free(&aes);

  return 0;
}

int CryptoImpl::aesGcmDecrypt(
    std::vector<unsigned char> &plain_text,
    const unsigned char *key_bytes,
    int key_length,
    const unsigned char *iv_bytes,
    int iv_length,
    const unsigned char *auth_tag,
    int auth_tag_length,
    const unsigned char *ciphertext_bytes,
    int ciphertext_length
) const {
  int rc;

  mbedtls_gcm_context aes;
  mbedtls_gcm_init(&aes);
  mbedtls_gcm_setkey(&aes,MBEDTLS_CIPHER_ID_AES, key_bytes, key_length * 8);
  plain_text.resize(ciphertext_length);
  rc = mbedtls_gcm_auth_decrypt(
      &aes,
      ciphertext_length,
      iv_bytes,
      iv_length,
      nullptr,
      0,
      auth_tag,
      auth_tag_length / 8,
      ciphertext_bytes,
      plain_text.data()
      );
  mbedtls_gcm_free(&aes);

  return rc;
}

} // namespace mbedtls
} // namespace crypto
} // namespace sipc
} // namespace jcu