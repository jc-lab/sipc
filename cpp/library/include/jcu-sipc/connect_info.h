/**
 * @file	connect_info.h
 * @author	Joseph Lee <joseph@jc-lab.net>
 * @date	2021-06-03
 * @copyright Copyright (C) 2021 jc-lab.
 *            This software may be modified and distributed under the terms
 *            of the Apache License 2.0.  See the LICENSE file for details.
 */


#ifndef JCU_SIPC_CONNECT_INFO_H_
#define JCU_SIPC_CONNECT_INFO_H_

#include <string>
#include <vector>

#include <nlohmann/json.hpp>

namespace jcu {
namespace sipc {

class ConnectInfo {
 protected:
  std::string channel_id_;
  std::string channel_type_;
  std::string algo_;
  std::vector<unsigned char> public_key_;

 public:
  ConnectInfo();

  std::string toBase64() const;

  virtual void updateToBson(::nlohmann::json &document) const;
  virtual void updateFromBson(const ::nlohmann::json &document);

  const std::string &getChannelType() const {
    return channel_type_;
  }
  const std::string &getChannelId() const {
    return channel_id_;
  }
  const std::string &getAlgo() const {
    return algo_;
  }
  const std::vector<unsigned char> &getPublicKey() const {
    return public_key_;
  }
};

class TcpConnectInfo : public ConnectInfo {
 protected:
  std::string address_;
  int port_;

 public:
  TcpConnectInfo();

  virtual void updateToBson(::nlohmann::json &document) const;
  virtual void updateFromBson(const ::nlohmann::json &document);

  const std::string &getAddress() const {
    return address_;
  }

  int getPort() const {
    return port_;
  }
};

/**
 * parse connect info
 *
 * @param text base64 encoded connect_info
 * @return ConnectInfo instance
 * @throw std::exception
 */
std::unique_ptr<ConnectInfo> parseConnectInfo(const std::string &text);

} // namespace sipc
} // namespace jcu

#endif //JCU_SIPC_CONNECT_INFO_H_
