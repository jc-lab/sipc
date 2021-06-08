/**
 * @file	secure_random_impl.cc
 * @author	Joseph Lee <joseph@jc-lab.net>
 * @date	2021-06-06
 * @copyright Copyright (C) 2021 jc-lab.
 *            This software may be modified and distributed under the terms
 *            of the Apache License 2.0.  See the LICENSE file for details.
 */


#include <string.h>
#include <mbedtls/entropy.h>

#include <utility>

#include "crypto_impl.h"
#include "secure_random_impl.h"

namespace jcu {
namespace sipc {
namespace crypto {
namespace mbedtls {

SecureRandomImpl::SecureRandomImpl(std::shared_ptr<CryptoImpl> crypto_impl) :
    crypto_impl_(std::move(crypto_impl)),
    inited_(false) {
  mbedtls_ctr_drbg_init(&ctr_drbg_);
}

SecureRandomImpl::~SecureRandomImpl() {
  mbedtls_ctr_drbg_free(&ctr_drbg_);
}

int SecureRandomImpl::init(const unsigned char *seed_bytes, int seed_length) {
  int ret;

  ret = mbedtls_ctr_drbg_seed(
      &ctr_drbg_,
      crypto_impl_->getEntropyFunc(),
      crypto_impl_->getEntropyContext(),
      (const unsigned char *) seed_bytes,
      seed_length
      );

  if(ret) return ret;

  inited_ = true;

  return 0;
}

int SecureRandomImpl::random(unsigned char *buffer, size_t size) const {
  int rc;
  if (!inited_) {
    rc = ((SecureRandomImpl*)this)->init(nullptr, 0);
    if (rc) return rc;
  }
  return mbedtls_ctr_drbg_random((void*)&ctr_drbg_, buffer, size);
}

} // namespace mbedtls
} // namespace crypto
} // namespace sipc
} // namespace jcu
