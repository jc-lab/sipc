/**
 * @file	mbedtls_crypto.h
 * @author	Joseph Lee <joseph@jc-lab.net>
 * @date	2021-03-08
 * @copyright Copyright (C) 2021 jc-lab.
 *            This software may be modified and distributed under the terms
 *            of the Apache License 2.0.  See the LICENSE file for details.
 */


#ifndef JCU_SIPC_CRYPTO_MBEDTLS_CRYPTO_H_
#define JCU_SIPC_CRYPTO_MBEDTLS_CRYPTO_H_

#include "crypto_base.h"

namespace jcu {
namespace sipc {
namespace crypto {

class MbedtlsCrypto : public CryptoBase {
 public:
  static std::shared_ptr<MbedtlsCrypto> create();
};

} // namespace crypto
} // namespace sipc
} // namespace jcu

#endif //JCU_SIPC_CRYPTO_MBEDTLS_CRYPTO_H_
