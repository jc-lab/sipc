/**
 * @file	request_context.cc
 * @author	Joseph Lee <joseph@jc-lab.net>
 * @date	2021-06-07
 * @copyright Copyright (C) 2021 jc-lab.
 *            This software may be modified and distributed under the terms
 *            of the Apache License 2.0.  See the LICENSE file for details.
 */


#include "request_context.h"

#include "../client_impl.h"

namespace jcu {
namespace sipc {
namespace intl {

CalleeRequestContextImpl::CalleeRequestContextImpl(std::shared_ptr<ClientImpl> client, std::string stream_id) :
    client_(client),
    stream_id_(std::move(stream_id)) {
}

const std::string &CalleeRequestContextImpl::getStreamId() const {
  return stream_id_;
}

void CalleeRequestContextImpl::progress(const ::google::protobuf::Message *progress) {
  std::shared_ptr<ClientImpl> client = client_;
  std::shared_ptr<proto::EventProgress> message(std::make_shared<proto::EventProgress>());
  message->set_stream_id(stream_id_);
  message->set_data(progress->SerializeAsString());
  client_->asyncRun(std::make_shared<std::function<void()>>([client, message]() -> void {
    client->sendApplicationData(message.get());
  }));
}

void CalleeRequestContextImpl::complete(const ::google::protobuf::Message *response) {
  std::shared_ptr<ClientImpl> client = client_;
  std::string stream_id = getStreamId();
  std::shared_ptr<proto::EventComplete> message(std::make_unique<proto::EventComplete>());
  message->set_stream_id(stream_id_);
  message->set_data(response->SerializeAsString());
  client_->asyncRun(std::make_shared<std::function<void()>>([client, stream_id, message]() -> void {
    client->sendApplicationData(message.get());
    client->eventStreamDone(stream_id);
  }));
}

CallerRequestContextImpl::CallerRequestContextImpl(
    std::string stream_id,
    std::string request_name,
    const ::google::protobuf::Message &request,
    RawProtobufHandler_t progress_handler,
    RawProtobufHandler_t response_handler
) :
    stream_id_(std::move(stream_id)),
    request_name_(std::move(request_name)),
    progress_handler_(std::move(progress_handler)),
    response_handler_(std::move(response_handler)) {
  request_.resize(request.ByteSizeLong());
  request.SerializeToArray(request_.data(), request_.size());
}

const std::string & CallerRequestContextImpl::getStreamId() const {
  return stream_id_;
}

void CallerRequestContextImpl::onProgress(const proto::EventProgress &payload) {
  this->feedProgress((const unsigned char*)payload.data().c_str(), payload.data().length());
}

void CallerRequestContextImpl::onComplete(const proto::EventComplete &payload) {
  this->feedComplete((const unsigned char*)payload.data().c_str(), payload.data().length());
}

} // namespace intl
} // namespace sipc
} // namespace jcu
