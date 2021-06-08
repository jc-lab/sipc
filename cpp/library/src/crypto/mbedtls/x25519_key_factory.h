/**
 * @file	x25519_key_factory.h
 * @author	Joseph Lee <joseph@jc-lab.net>
 * @date	2021-06-06
 * @copyright Copyright (C) 2021 jc-lab.
 *            This software may be modified and distributed under the terms
 *            of the Apache License 2.0.  See the LICENSE file for details.
 */


#ifndef JCU_SIPC_CPP_LIBRARY_SRC_CRYPTO_MBEDTLS_X_25519_KEY_FACTORY_H_
#define JCU_SIPC_CPP_LIBRARY_SRC_CRYPTO_MBEDTLS_X_25519_KEY_FACTORY_H_

#include <mbedtls/ecp.h>

#include <jcu-sipc/crypto/ephemeral_key_factory.h>

namespace jcu {
namespace sipc {
namespace crypto {
namespace mbedtls {

class CryptoImpl;

class X25519KeyFactory : public EphemeralKeyFactory {
 private:
  std::shared_ptr<CryptoImpl> crypto_impl_;
  mbedtls_ecp_group group_;
  int init_rc_;

 public:
  X25519KeyFactory(std::shared_ptr<CryptoImpl> crypto_impl);
  ~X25519KeyFactory();

  bool isReady() const override;
  int getLibraryErrno() const override;
  std::string getAlgorithm() const override;
  std::unique_ptr<EphemeralKeyPair> generateKeyPair() const override;

  std::shared_ptr<CryptoImpl> getCryptoImpl() const;
  void ecpGroupCopyTo(mbedtls_ecp_group *group) const;
};

} // namespace mbedtls
} // namespace crypto
} // namespace sipc
} // namespace jcu

#endif //JCU_SIPC_CPP_LIBRARY_SRC_CRYPTO_MBEDTLS_X_25519_KEY_FACTORY_H_
