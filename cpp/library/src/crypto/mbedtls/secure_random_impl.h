/**
 * @file	secure_random_impl.h
 * @author	Joseph Lee <joseph@jc-lab.net>
 * @date	2021-06-06
 * @copyright Copyright (C) 2021 jc-lab.
 *            This software may be modified and distributed under the terms
 *            of the Apache License 2.0.  See the LICENSE file for details.
 */


#ifndef JCU_SIPC_CPP_LIBRARY_SRC_CRYPTO_MBEDTLS_SECURE_RANDOM_IMPL_H_
#define JCU_SIPC_CPP_LIBRARY_SRC_CRYPTO_MBEDTLS_SECURE_RANDOM_IMPL_H_

#include <mbedtls/ctr_drbg.h>

#include <jcu-sipc/crypto/secure_random.h>

namespace jcu {
namespace sipc {
namespace crypto {
namespace mbedtls {

class CryptoImpl;

/**
 * Non thread-safety
 */
class SecureRandomImpl : public SecureRandom {
 private:
  std::shared_ptr<CryptoImpl> crypto_impl_;
  mbedtls_ctr_drbg_context ctr_drbg_;
  bool inited_;

 public:
  SecureRandomImpl(std::shared_ptr<CryptoImpl> crypto_impl);
  ~SecureRandomImpl();
  int init(const unsigned char *seed_bytes, int seed_length) override;
  int random(unsigned char *buffer, size_t size) const override;
};

} // namespace mbedtls
} // namespace crypto
} // namespace sipc
} // namespace jcu

#endif //JCU_SIPC_CPP_LIBRARY_SRC_CRYPTO_MBEDTLS_SECURE_RANDOM_IMPL_H_
