//
// Created by jichan on 2021-03-08.
//

#ifndef JCU_SIPC_CRYPTO_SECURE_RANDOM_H_
#define JCU_SIPC_CRYPTO_SECURE_RANDOM_H_

#include "crypto_base.h"

namespace jcu {
namespace sipc {
namespace crypto {

class SecureRandom {
 public:
  virtual int init(const unsigned char *seed_bytes, int seed_length) = 0;
  virtual int random(unsigned char* buffer, size_t size) const = 0;
};

} // namespace crypto
} // namespace sipc
} // namespace jcu

#endif //JCU_SIPC_CRYPTO_SECURE_RANDOM_H_
