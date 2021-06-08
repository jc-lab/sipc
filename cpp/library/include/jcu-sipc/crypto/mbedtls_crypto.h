//
// Created by jichan on 2021-03-08.
//

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
