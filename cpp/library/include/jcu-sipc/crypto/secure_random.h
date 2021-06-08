/**
 * @file	secure_random.h
 * @author	Joseph Lee <joseph@jc-lab.net>
 * @date	2021-03-08
 * @copyright Copyright (C) 2021 jc-lab.
 *            This software may be modified and distributed under the terms
 *            of the Apache License 2.0.  See the LICENSE file for details.
 */


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
