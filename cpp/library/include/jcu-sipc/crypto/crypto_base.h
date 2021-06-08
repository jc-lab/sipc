//
// Created by jichan on 2021-03-08.
//

#ifndef JCU_SIPC_CRYPTO_CRYPTO_BASE_H_
#define JCU_SIPC_CRYPTO_CRYPTO_BASE_H_

#include <memory>
#include <string>
#include <vector>

namespace jcu {
namespace sipc {
namespace crypto {

class SecureRandom;
class EphemeralKeyFactory;

class CryptoBase {
 public:
  virtual ~CryptoBase() = default;

  virtual std::unique_ptr<SecureRandom> createSecureRandom() const = 0;
  virtual std::shared_ptr<EphemeralKeyFactory> getECKeyFactory(const std::string& algorithm) const = 0;

  virtual int hmacSha256(
      unsigned char* output,
      const unsigned char* key_bytes,
      int key_length,
      const unsigned char* message_bytes,
      int message_length
      ) const = 0;

  virtual int aesGcmEncrypt(
      std::vector<unsigned char>& cipher_text,
      std::vector<unsigned char>& auth_tag,
      const unsigned char* key_bytes,
      int key_length,
      const unsigned char* iv_bytes,
      int iv_length,
      int auth_tag_length,
      const unsigned char* message_bytes,
      int message_length
  ) const = 0;

  virtual int aesGcmDecrypt(
      std::vector<unsigned char>& plain_text,
      const unsigned char* key_bytes,
      int key_length,
      const unsigned char* iv_bytes,
      int iv_length,
      const unsigned char* auth_tag,
      int auth_tag_length,
      const unsigned char* message_bytes,
      int message_length
  ) const = 0;
};

} // namespace crypto
} // namespace sipc
} // namespace jcu

#endif //JCU_SIPC_CPP_LIBRARY_SRC_INTL_CRYPTO_BASE_H_
