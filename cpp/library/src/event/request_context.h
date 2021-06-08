/**
 * @file	request_context.h
 * @author	Joseph Lee <joseph@jc-lab.net>
 * @date	2021-06-07
 * @copyright Copyright (C) 2021 jc-lab.
 *            This software may be modified and distributed under the terms
 *            of the Apache License 2.0.  See the LICENSE file for details.
 */


#ifndef JCU_SIPC_CPP_LIBRARY_SRC_EVENT_REQUEST_CONTEXT_H_
#define JCU_SIPC_CPP_LIBRARY_SRC_EVENT_REQUEST_CONTEXT_H_

#include <jcu-sipc/client.h>
#include <jcu-sipc/proto/sipc_frames.pb.h>

#include <utility>

namespace jcu {
namespace sipc {
namespace intl {

class ClientImpl;

class RequestContext {
 public:
  virtual ~RequestContext() {}
  virtual const std::string &getStreamId() const = 0;
  virtual void onProgress(const proto::EventProgress &payload) {}
  virtual void onComplete(const proto::EventComplete &payload) {}
};

class CalleeRequestContextImpl :
    public RequestContext,
    public CalleeRequestContextHandler {
 private:
  std::shared_ptr<ClientImpl> client_;
  std::string stream_id_;

 public:
  CalleeRequestContextImpl(std::shared_ptr<ClientImpl> client, std::string stream_id);
  const std::string &getStreamId() const override;
  void progress(const ::google::protobuf::Message *progress) override;
  void complete(const ::google::protobuf::Message *response) override;
};

class CallerRequestContextImpl : public CallerRequestContext, public RequestContext {
 private:
  std::string stream_id_;

 private:
  std::string request_name_;
  std::vector<unsigned char> request_;
  RawProtobufHandler_t progress_handler_;
  RawProtobufHandler_t response_handler_;

 public:
  CallerRequestContextImpl(
      std::string stream_id,
      std::string request_name,
      const ::google::protobuf::Message &request,
      RawProtobufHandler_t progress_handler,
      RawProtobufHandler_t response_handler
  );

  const std::string &getStreamId() const override;

  const std::vector<unsigned char> &getRequest() const {
    return request_;
  }

  const std::string &getRequestName() const override {
    return request_name_;
  }

  void feedProgress(const unsigned char *data, int length) override {
    if (progress_handler_) {
      progress_handler_(data, length);
    }
  }

  void feedComplete(const unsigned char *data, int length) override {
    if (response_handler_) {
      response_handler_(data, length);
    }
  }

  void onProgress(const proto::EventProgress &payload) override;
  void onComplete(const proto::EventComplete &payload) override;
};

} // namespace intl
} // namespace sipc
} // namespace jcu

#endif //JCU_SIPC_CPP_LIBRARY_SRC_EVENT_REQUEST_CONTEXT_H_
