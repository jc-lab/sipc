/**
 * @file	x25519_key_factory.cc
 * @author	Joseph Lee <joseph@jc-lab.net>
 * @date	2021-06-06
 * @copyright Copyright (C) 2021 jc-lab.
 *            This software may be modified and distributed under the terms
 *            of the Apache License 2.0.  See the LICENSE file for details.
 */


#include "x25519_key_factory.h"
#include "x25519_key_pair.h"

namespace jcu {
namespace sipc {
namespace crypto {
namespace mbedtls {

X25519KeyFactory::X25519KeyFactory(std::shared_ptr<CryptoImpl> crypto_impl) :
    crypto_impl_(crypto_impl) {
  mbedtls_ecp_group_init(&group_);
  init_rc_ = mbedtls_ecp_group_load(&group_, MBEDTLS_ECP_DP_CURVE25519);
}

X25519KeyFactory::~X25519KeyFactory() {
  mbedtls_ecp_group_free(&group_);
}

bool X25519KeyFactory::isReady() const {
  return init_rc_ == 0;
}

int X25519KeyFactory::getLibraryErrno() const {
  return init_rc_;
}

std::string X25519KeyFactory::getAlgorithm() const {
  return "x25519";
}

std::unique_ptr<EphemeralKeyPair> X25519KeyFactory::generateKeyPair() const {
  return std::make_unique<X25519KeyPair>(this);
}

std::shared_ptr<CryptoImpl> X25519KeyFactory::getCryptoImpl() const {
  return crypto_impl_;
}

void X25519KeyFactory::ecpGroupCopyTo(mbedtls_ecp_group *group) const {
  mbedtls_ecp_group_copy(group, &group_);
}

} // namespace mbedtls
} // namespace crypto
} // namespace sipc
} // namespace jcu
