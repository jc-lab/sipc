/**
 * @file	ephemeral_key_factory.h
 * @author	Joseph Lee <joseph@jc-lab.net>
 * @date	2021-03-08
 * @copyright Copyright (C) 2021 jc-lab.
 *            This software may be modified and distributed under the terms
 *            of the Apache License 2.0.  See the LICENSE file for details.
 */


#ifndef JCU_SIPC_CRYPTO_EPHEMERAL_KEY_FACTORY_H_
#define JCU_SIPC_CRYPTO_EPHEMERAL_KEY_FACTORY_H_

#include <memory>
#include <string>

namespace jcu {
namespace sipc {
namespace crypto {

class EphemeralKeyPair;

class EphemeralKeyFactory {
 public:
  virtual ~EphemeralKeyFactory() = default;
  virtual bool isReady() const = 0;
  virtual int getLibraryErrno() const = 0;
  virtual std::string getAlgorithm() const = 0;
  virtual std::unique_ptr<EphemeralKeyPair> generateKeyPair() const = 0;
};

} // namespace crypto
} // namespace sipc
} // namespace jcu

#endif //JCU_SIPC_CRYPTO_EPHEMERAL_KEY_FACTORY_H_
