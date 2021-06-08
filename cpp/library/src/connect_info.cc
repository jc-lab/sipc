//
// Created by jichan on 2021-06-03.
//

#include <memory>
#include <string>
#include <vector>

#include <nlohmann/json.hpp>

#include <jcu-sipc/connect_info.h>

#include "base64/base64.h"

namespace jcu {
namespace sipc {

ConnectInfo::ConnectInfo() {}

void ConnectInfo::updateToBson(nlohmann::json &document) const {
  document["channel_type"] = channel_type_;
  document["channel_id"] = channel_id_;
  document["algo"] = algo_;
  document["public_key"] = public_key_;
}

void ConnectInfo::updateFromBson(const nlohmann::json &document) {
  channel_type_ = document["channel_type"].get<std::string>();
  channel_id_ = document["channel_id"].get<std::string>();
  algo_ = document["algo"].get<std::string>();
  public_key_ = document["public_key"].get_binary();
}

std::string ConnectInfo::toBase64() const {
  ::nlohmann::json bson;
  updateToBson(bson);
  std::vector<uint8_t> raw = ::nlohmann::json::to_bson(bson);
  return intl::base64_encode(raw.data(), raw.size(), true);
}

TcpConnectInfo::TcpConnectInfo() :
    port_(0) {

}

void TcpConnectInfo::updateToBson(::nlohmann::json &document) const {
  ConnectInfo::updateToBson(document);
  document["address"] = address_;
  document["port"] = port_;
}

void TcpConnectInfo::updateFromBson(const ::nlohmann::json &document) {
  ConnectInfo::updateFromBson(document);
  address_ = document["address"].get<std::string>();
  port_ = document["port"].get<int>();
}

std::unique_ptr<ConnectInfo> parseConnectInfo(const std::string &text) {
  std::string raw = intl::base64_decode(text);
  auto bson_data = ::nlohmann::json::from_bson(raw.cbegin(), raw.cend());
  auto channel_type = bson_data["channel_type"].get<std::string>();
  std::unique_ptr<ConnectInfo> instance;
  if (channel_type == "tcp4") {
    instance = std::make_unique<TcpConnectInfo>();
  } else if (channel_type == "tcp6") {
    instance = std::make_unique<TcpConnectInfo>();
  } else if (channel_type == "stdio") {
    instance = std::make_unique<ConnectInfo>();
  } else {
    return nullptr;
  }
  instance->updateFromBson(bson_data);
  return std::move(instance);
}

} // namespace sipc
} // namespace jcu

