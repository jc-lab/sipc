//
// Created by jichan on 2021-06-06.
//

#ifndef JCU_SIPC_CPP_LIBRARY_SRC_CRYPTO_MBEDTLS_CRYPTO_IMPL_H_
#define JCU_SIPC_CPP_LIBRARY_SRC_CRYPTO_MBEDTLS_CRYPTO_IMPL_H_

#include <mbedtls/entropy.h>

#include <jcu-sipc/crypto/mbedtls_crypto.h>
#include <vector>

namespace jcu {
namespace sipc {
namespace crypto {
namespace mbedtls {

typedef int (*entropy_func_t)(void *data, unsigned char *output, size_t len);

class CryptoImpl : public MbedtlsCrypto {
 private:
  std::weak_ptr<CryptoImpl> self_;
  mbedtls_entropy_context entropy_;
  std::unique_ptr<SecureRandom> default_secure_random_;

 public:
  static std::shared_ptr<CryptoImpl> create();

  CryptoImpl();
  ~CryptoImpl();
  void earlyInit();
  entropy_func_t getEntropyFunc() const;
  mbedtls_entropy_context *getEntropyContext() const;

  SecureRandom *getDefaultSecureRandom() const;

  std::unique_ptr<SecureRandom> createSecureRandom() const override;
  std::shared_ptr<EphemeralKeyFactory> getECKeyFactory(const std::string &algorithm) const override;

  int hmacSha256(unsigned char *output,
                 const unsigned char *key_bytes,
                 int key_length,
                 const unsigned char *message_bytes,
                 int message_length) const override;

  int aesGcmEncrypt(std::vector<unsigned char> &cipher_text,
                    std::vector<unsigned char> &auth_tag,
                    const unsigned char *key_bytes,
                    int key_length,
                    const unsigned char *iv_bytes,
                    int iv_length,
                    int auth_tag_length,
                    const unsigned char *message_bytes,
                    int message_length) const override;
  int aesGcmDecrypt(std::vector<unsigned char> &plain_text,
                    const unsigned char *key_bytes,
                    int key_length,
                    const unsigned char *iv_bytes,
                    int iv_length,
                    const unsigned char *auth_tag,
                    int auth_tag_length,
                    const unsigned char *message_bytes,
                    int message_length) const override;
};

} // namespace mbedtls
} // namespace crypto
} // namespace sipc
} // namespace jcu

#endif //JCU_SIPC_CPP_LIBRARY_SRC_CRYPTO_MBEDTLS_CRYPTO_IMPL_H_
