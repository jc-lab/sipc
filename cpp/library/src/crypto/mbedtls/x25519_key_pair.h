//
// Created by jichan on 2021-06-06.
//

#ifndef JCU_SIPC_CPP_LIBRARY_SRC_CRYPTO_X_25519_KEY_PAIR_H_
#define JCU_SIPC_CPP_LIBRARY_SRC_CRYPTO_X_25519_KEY_PAIR_H_

#include <mbedtls/ecdh.h>

#include <jcu-sipc/crypto/ephemeral_key_pair.h>

namespace jcu {
namespace sipc {
namespace crypto {
namespace mbedtls {

class X25519KeyFactory;

class X25519KeyPair : public EphemeralKeyPair {
 private:
  std::shared_ptr<CryptoImpl> crypto_impl_;

  mbedtls_ecdh_context ctx_;
  int init_rc_;

 public:
  X25519KeyPair(const X25519KeyFactory* key_factory);
  ~X25519KeyPair() override;
  bool isReady() const override;
  int getLibraryErrno() const override;
  std::string getAlgorithm() const override;

  int derive(std::vector<unsigned char> &output,
             const unsigned char *public_key_bytes,
             int public_key_length) const override;
  int getPublicKey(std::vector<unsigned char> &output) const override;
};

} // namespace mbedtls
} // namespace crypto
} // namespace sipc
} // namespace jcu

#endif //JCU_SIPC_CPP_LIBRARY_SRC_CRYPTO_X_25519_KEY_PAIR_H_
