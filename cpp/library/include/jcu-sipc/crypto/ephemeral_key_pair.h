/**
 * @file	ephemeral_key_pair.h
 * @author	Joseph Lee <joseph@jc-lab.net>
 * @date	2021-03-08
 * @copyright Copyright (C) 2021 jc-lab.
 *            This software may be modified and distributed under the terms
 *            of the Apache License 2.0.  See the LICENSE file for details.
 */


#ifndef JCU_SIPC_CRYPTO_EPHEMERAL_KEY_PAIR_H_
#define JCU_SIPC_CRYPTO_EPHEMERAL_KEY_PAIR_H_

#include <memory>
#include <string>
#include <vector>

namespace jcu {
namespace sipc {
namespace crypto {

class EphemeralKeyPair {
 public:
  virtual ~EphemeralKeyPair() = default;
  virtual bool isReady() const = 0;
  virtual int getLibraryErrno() const = 0;
  virtual std::string getAlgorithm() const = 0;
  virtual int derive(std::vector<unsigned char>& output, const unsigned char* public_key_bytes, int public_key_length) const = 0;
  virtual int getPublicKey(std::vector<unsigned char>& output) const = 0;
};

} // namespace crypto
} // namespace sipc
} // namespace jcu

#endif //JCU_SIPC_CRYPTO_EPHEMERAL_KEY_PAIR_H_
