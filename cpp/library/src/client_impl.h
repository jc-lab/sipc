/**
 * @file	client_impl.h
 * @author	Joseph Lee <joseph@jc-lab.net>
 * @date	2021-03-08
 * @copyright Copyright (C) 2021 jc-lab.
 *            This software may be modified and distributed under the terms
 *            of the Apache License 2.0.  See the LICENSE file for details.
 */


#ifndef JCU_SIPC_CPP_LIBRARY_SRC_CLIENT_IMPL_H_
#define JCU_SIPC_CPP_LIBRARY_SRC_CLIENT_IMPL_H_

#include <memory>
#include <map>
#include <mutex>

#include <jcu-sipc/transport/base.h>
#include <jcu-sipc/protocol/frame_converter.h>

#include <jcu-sipc/client.h>
#include <jcu-sipc/connect_info.h>

#include <jcu-sipc/crypto/ephemeral_key_pair.h>

#include "event/request_context.h"

namespace jcu {
namespace sipc {

//namespace grpc {
//class MiddleGrpcChannel;
//}

namespace intl {

class WrappedDataReceiverAdapterBase {
 public:
  virtual ~WrappedDataReceiverAdapterBase() {}
  virtual int invoke(const unsigned char* data, int length) = 0;
  virtual const ::google::protobuf::Message* getDefaultInstance() const = 0;
};

template<class T>
class WrappedDataReceiverAdapter : public WrappedDataReceiverAdapterBase {
 private:
  T default_instance_;
  std::function<void(const T& payload)> handler_;

 public:
  WrappedDataReceiverAdapter(std::function<void(const T&)> handler) :
  handler_(std::move(handler)) {}

  const ::google::protobuf::Message* getDefaultInstance() const override {
    return &default_instance_;
  }

  int invoke(const unsigned char* data, int length) override {
    bool parse_result;
    std::unique_ptr<T> instance(default_instance_.New());
    parse_result = instance->ParseFromArray(data, length);
    if (!parse_result) return 1;
    handler_(*instance);
    return 0;
  }
};


struct OnRequestMethod {
  std::string request_name;
  CalleeRequestContextBaseFactory_t request_context_factory;
  std::function<void(std::shared_ptr<CalleeRequestContextBase>)> handler;
};

class ClientImpl : public Client, public protocol::FrameHandlers, public transport::TransportHandler {
 private:
  std::weak_ptr<ClientImpl> self_;
  std::shared_ptr<uvw::Loop> loop_;
  std::shared_ptr<Logger> logger_;
  std::shared_ptr<crypto::CryptoBase> crypto_;

  std::unique_ptr<crypto::SecureRandom> default_random_;

  std::map<std::string, std::shared_ptr<transport::Base>> available_transports_;
  std::shared_ptr<transport::Base> transport_;

  std::unique_ptr<ConnectInfo> connect_info_;

  ConnectionStatus connection_status_;

  std::vector<unsigned char> client_nonce_;
  std::vector<unsigned char> server_nonce_;
  std::unique_ptr<crypto::EphemeralKeyPair> ephemeral_key_pair_;
  std::vector<unsigned char> ecdh_derived_key_;
  std::vector<unsigned char> shared_master_secret_;
  int64_t my_counter_;
  int64_t peer_counter_;

  OnHandshakeHandler_t handshake_handler_;

  std::map<std::string, std::unique_ptr<WrappedDataReceiverAdapterBase>> wrapped_data_receivers_;
  std::map<std::string, OnRequestMethod> request_methods_;

  std::mutex mutex_;
  std::map<std::string, std::shared_ptr<RequestContext>> running_calls_;

  void earlyInit();
  void closeImpl();

 public:
  static std::shared_ptr<ClientImpl> create(std::shared_ptr<uvw::Loop> loop,
                                            std::shared_ptr<Logger> logger,
                                            std::shared_ptr<crypto::CryptoBase> crypto);

  ClientImpl(std::shared_ptr<uvw::Loop> loop,
             std::shared_ptr<Logger> logger,
             std::shared_ptr<crypto::CryptoBase> crypto);
  virtual ~ClientImpl();
  void registerTransport(std::shared_ptr<transport::Base> transport) override;
  int connect(const std::string &connect_info) override;
  ConnectionStatus getConnectionStatus() const override;

  Client &onHandshake(const OnHandshakeHandler_t &handler) override;
//  std::shared_ptr<::grpc::ChannelInterface> getGrpcChannel() override;

  void close() override;

  void onRequestImpl(
      const std::string &request_name,
      const CalleeRequestContextBaseFactory_t &request_context_factory,
      std::function<void(std::shared_ptr<CalleeRequestContextBase>)> handler
  ) override;
  std::shared_ptr<CallerRequestContext> createCallerRequestContext(
      const std::string &request_name,
      const ::google::protobuf::Message &request,
      const RawProtobufHandler_t &progress_handler,
      const RawProtobufHandler_t &response_handler
  ) override;
  void requestImpl(std::shared_ptr<CallerRequestContext> ctx) override;

 public:
  void onAlertFrame(proto::AlertFrame *frame) override;
  void onClientHelloFrame(proto::ClientHelloFrame *frame) override;
  void onServerHelloFrame(proto::ServerHelloFrame *frame) override;
  void onWrappedDataFrame(proto::EncryptedWrappedData *frame) override;
  void onTransportConnect() override;
  void onTransportClose() override;
  void onTransportError(const uvw::ErrorEvent &evt) override;

  int generateSecret(std::vector<unsigned char> &output, const char *type, int64_t counter, int size);
  int wrapDataToSend(proto::EncryptedWrappedData *frame, const unsigned char *data, int length);
  int unwrapDataToRecv(std::vector<unsigned char> &data, const proto::EncryptedWrappedData *frame);

  int sendWrappedData(const proto::WrappedData& frame, const transport::WriteHandler_t& write_handler);
  std::string getTypeUrl(const ::google::protobuf::Message* message);
  int sendApplicationData(const ::google::protobuf::Message* message);

  void eventStreamDone(RequestContext* ctx);
  void registerWrappedDataImpl(std::unique_ptr<WrappedDataReceiverAdapterBase> adapter);

  template<class T>
  void registerWrappedData(std::function<void(const T&)> handler) {
    std::unique_ptr<WrappedDataReceiverAdapterBase> adapter(new WrappedDataReceiverAdapter<T>(handler));
    registerWrappedDataImpl(std::move(adapter));
  }

  std::string generateUUID();

 private:
  void onDataEventRequest(const proto::EventRequest& payload);
  void onDataEventProgress(const proto::EventProgress& payload);
  void onDataEventComplete(const proto::EventComplete& payload);
};

} // namespace intl
} // namespace sipc
} // namespace jcu

#endif //JCU_SIPC_CPP_LIBRARY_SRC_CLIENT_IMPL_H_
