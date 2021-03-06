/**
 * @file	client.h
 * @author	Joseph Lee <joseph@jc-lab.net>
 * @date	2021-03-08
 * @copyright Copyright (C) 2021 jc-lab.
 *            This software may be modified and distributed under the terms
 *            of the Apache License 2.0.  See the LICENSE file for details.
 */


#ifndef JCU_SIPC_CLIENT_H_
#define JCU_SIPC_CLIENT_H_

#include <memory>
#include <string>
#include <functional>
#include <future>
#include <utility>

#include <uvw/loop.h>

//#include <grpcpp/impl/codegen/channel_interface.h>

#include "log.h"
#include "crypto/crypto_base.h"
#include "transport/base.h"

namespace jcu {
namespace sipc {

enum ConnectionStatus {
  kConnectionIdle = 0,
  kConnectionConnecting,
  kConnectionEstablished,
  kConnectionError,
  kConnectionDone
};

class CalleeRequestContextHandler {
 public:
  virtual void progress(const ::google::protobuf::Message* progress) = 0;
  virtual void complete(const ::google::protobuf::Message* response) = 0;
};

class CalleeRequestContextBase {
 private:
  std::string stream_id_;
  std::shared_ptr<CalleeRequestContextHandler> handler_;

 public:
  CalleeRequestContextBase(std::string stream_id, std::shared_ptr<CalleeRequestContextHandler> handler) :
  stream_id_(std::move(stream_id)),
  handler_(std::move(handler))
  {}

  const std::string& getStreamId() const {
    return stream_id_;
  }
  void progress(const ::google::protobuf::Message* progress) {
    handler_->progress(progress);
  }
  void complete(const ::google::protobuf::Message* response) {
    handler_->complete(response);
  }

  virtual void setRequest(const char* data, int len) = 0;
};

template<class T>
class CalleeRequestContext : public CalleeRequestContextBase {
 private:
  T request_;

 public:
  CalleeRequestContext(const std::string &stream_id, const std::shared_ptr<CalleeRequestContextHandler> &handler)
      : CalleeRequestContextBase(stream_id, handler) {}

  const T& getRequest() {
    return request_;
  }

  void setRequest(const char* data, int len) override {
    request_.ParseFromArray(data, len);
  }
};

typedef std::function<std::shared_ptr<CalleeRequestContextBase>(const std::string& stream_id, std::shared_ptr<CalleeRequestContextHandler> handler)> CalleeRequestContextBaseFactory_t;
typedef std::function<void(const unsigned char* data, int length)> RawProtobufHandler_t;

class CallerRequestContext {
 public:
  virtual const std::vector<unsigned char> &getRequest() const = 0;
  virtual const std::string& getRequestName() const = 0;
  virtual void feedProgress(const unsigned char* data, int length) = 0;
  virtual void feedComplete(const unsigned char* data, int length) = 0;
};

class Client {
 public:
  typedef std::function<void(Client* client)> OnHandshakeHandler_t;

  /**
   * Create Client instance
   *
   * @return Client Instance
   */
  static std::shared_ptr<Client> create(std::shared_ptr<uvw::Loop> loop, std::shared_ptr<Logger> logger = nullptr, std::shared_ptr<crypto::CryptoBase> crypto = nullptr);

  virtual void registerTransport(std::shared_ptr<transport::Base> transport) = 0;

  /**
   * connect
   *
   * @param connectInfo base64-encoded connect info (generated by server)
   * @return result (0 if succeed)
   */
  virtual int connect(const std::string &connect_info) = 0;

  virtual void close() = 0;

  virtual ConnectionStatus getConnectionStatus() const = 0;

  virtual Client& onHandshake(const OnHandshakeHandler_t& handler) = 0;

  virtual void onRequestImpl(
      const std::string& request_name,
      const CalleeRequestContextBaseFactory_t& request_context_factory,
      std::function<void(std::shared_ptr<CalleeRequestContextBase> request_context)> handler
      ) = 0;

  virtual std::shared_ptr<CallerRequestContext> createCallerRequestContext(
      const std::string& request_name,
      const ::google::protobuf::Message& request,
      const RawProtobufHandler_t& progress_handler,
      const RawProtobufHandler_t& response_handler
  ) = 0;
  virtual void requestImpl(std::shared_ptr<CallerRequestContext> ctx) = 0;

  template<class TREQ>
  void onRequest(const std::string& request_name, std::function<void(std::shared_ptr<CalleeRequestContext<TREQ>> request_context)> handler) {
    onRequestImpl(
        request_name,
        [](const std::string& stream_id, std::shared_ptr<CalleeRequestContextHandler> handler) -> std::shared_ptr<CalleeRequestContextBase> {
          return std::make_shared<CalleeRequestContext<TREQ>>(stream_id, handler);
        }, [handler](std::shared_ptr<CalleeRequestContextBase> request_context) -> void {
          handler(std::dynamic_pointer_cast<CalleeRequestContext<TREQ>>(request_context));
        });
  }

  template<class TREQ, class TPROG, class TRES>
  std::future<TRES> request(
      const std::string& request_name,
      const TREQ& request,
      std::function<void(const TPROG& progress)> progress_handler,
      std::function<void(const TRES& response)> response_handler
  ) {
    std::shared_ptr<std::promise<TRES>> promise(std::make_shared<std::promise<TRES>>());
    auto ctx = createCallerRequestContext(
        request_name,
        request,
        [progress_handler](const unsigned char* data, int length) -> void {
          TPROG msg;
          msg.ParseFromArray(data, length);
          progress_handler(msg);
        },
        [response_handler, promise](const unsigned char* data, int length) -> void {
          TRES msg;
          msg.ParseFromArray(data, length);
          response_handler(msg);
          promise->set_value(std::move(msg));
        });
    requestImpl(ctx);
    return std::move(promise->get_future());
  }

  template<class TREQ, class TRES>
  std::future<TRES> request(
      const std::string& request_name,
      const TREQ& request,
      std::function<void(const TRES& response)> response_handler
  ) {
    std::shared_ptr<std::promise<TRES>> promise(std::make_shared<std::promise<TRES>>());
    auto ctx = createCallerRequestContext(
        request_name,
        request,
        [](const unsigned char* data, int length) -> void {
        },
        [response_handler, promise](const unsigned char* data, int length) -> void {
          TRES msg;
          msg.ParseFromArray(data, length);
          response_handler(msg);
          promise->set_value(std::move(msg));
        });
    requestImpl(ctx);
    return std::move(promise->get_future());
  }

//  virtual std::shared_ptr<::grpc::ChannelInterface> getGrpcChannel() = 0;
};

} // namespace sipc
} // namespace jcu

#endif //JCU_SIPC_CLIENT_H_
