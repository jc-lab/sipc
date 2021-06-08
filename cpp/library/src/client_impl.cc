//
// Created by jichan on 2021-03-08.
//

#include <utility>

#include <jcu-sipc/crypto/secure_random.h>
#include <jcu-sipc/crypto/ephemeral_key_factory.h>
#include <jcu-sipc/crypto/ephemeral_key_pair.h>
#include <jcu-sipc/crypto/mbedtls_crypto.h>

#include "client_impl.h"

namespace jcu {
namespace sipc {
namespace intl {

ClientImpl::ClientImpl(
    std::shared_ptr<uvw::Loop> loop,
    std::shared_ptr<Logger> logger,
    std::shared_ptr<crypto::CryptoBase> crypto
) :
    loop_(std::move(loop)),
    logger_(std::move(logger)),
    crypto_(std::move(crypto)),
    connection_status_(kConnectionIdle),
    my_counter_(0),
    peer_counter_(0) {
  client_nonce_.resize(32);
  auto secure_random = crypto_->createSecureRandom();
  secure_random->random(client_nonce_.data(), client_nonce_.size());
  default_random_ = std::move(secure_random);
}

void ClientImpl::close() {
  std::shared_ptr<ClientImpl> self(self_.lock());

  logger_->logf(Logger::kLogInfo, "[ClientImpl] closing");

  self->available_transports_.clear();
  if (transport_) {
    transport_->close(false, [self]() -> void {
      self->logger_->logf(Logger::kLogInfo, "[ClientImpl] close");

      self->transport_.reset();
      self->loop_.reset();
    });
  }
}

void ClientImpl::registerTransport(std::shared_ptr<transport::Base> transport) {
  std::list<std::string> channel_types = transport->getChannelTypes();
  for (auto it = channel_types.cbegin(); it != channel_types.cend(); it++) {
    available_transports_.emplace(*it, transport);
  }
}

int ClientImpl::connect(const std::string &connect_info_text) {
  std::shared_ptr<ClientImpl> self(self_.lock());

  try {
    auto connect_info = parseConnectInfo(connect_info_text);
    auto key_factory = crypto_->getECKeyFactory(connect_info->getAlgo());
    if (!key_factory) {
      return 3;
    }

    auto transport_it = available_transports_.find(connect_info->getChannelType());
    if (transport_it == available_transports_.cend()) {
      return 2;
    }
    transport_ = transport_it->second;
    connect_info_ = std::move(connect_info);
    transport_->setTransportHandler(std::static_pointer_cast<transport::TransportHandler>(self));
    transport_->setFrameHandlers(std::static_pointer_cast<protocol::FrameHandlers>(self));

    ephemeral_key_pair_ = key_factory->generateKeyPair();

    connection_status_ = kConnectionConnecting;
    return transport_->start(connect_info_.get());
  } catch (std::exception &ex) {
    return 1;
  }
}

void ClientImpl::onTransportConnect() {
  logger_->logf(Logger::kLogInfo, "[ClientImpl] onTransportConnect");

  proto::ClientHelloFrame frame;
  std::vector<unsigned char> client_public_key;
  const auto &server_public_key = connect_info_->getPublicKey();

  ephemeral_key_pair_->getPublicKey(client_public_key);
  ephemeral_key_pair_->derive(ecdh_derived_key_, server_public_key.data(), server_public_key.size());

  frame.set_version(1);
  frame.set_channel_id(connect_info_->getChannelId());
  frame.set_ephemeral_public_key(std::string(client_public_key.cbegin(), client_public_key.cend()));
  frame.set_client_nonce(std::string(client_nonce_.cbegin(), client_nonce_.cend()));

  transport_->sendFrame(&frame, [](bool is_error, const uvw::ErrorEvent &error_event) -> void {});
}

void ClientImpl::onTransportClose() {
  logger_->logf(Logger::kLogInfo, "[ClientImpl] onTransportClose");
  if (connection_status_ != kConnectionError) {
    connection_status_ = kConnectionDone;
  }
}

void ClientImpl::onTransportError(const uvw::ErrorEvent &evt) {
  logger_->logf(Logger::kLogInfo, "[ClientImpl] Transport Error: code=%d, %s, %s", evt.code(), evt.name(), evt.what());
  connection_status_ = kConnectionError;
}

void ClientImpl::onAlertFrame(proto::AlertFrame *frame) {
  logger_->logf(Logger::kLogInfo, "[ClientImpl] ALERT FRAME: %s", frame->message().c_str());
  connection_status_ = kConnectionError;
}

void ClientImpl::onClientHelloFrame(proto::ClientHelloFrame *frame) {
  // Server only
}

void ClientImpl::onServerHelloFrame(proto::ServerHelloFrame *frame) {
  int rc;

  server_nonce_.clear();
  server_nonce_.insert(server_nonce_.end(), frame->server_nonce().cbegin(), frame->server_nonce().cend());
  std::vector<unsigned char> message;
  message.reserve(server_nonce_.size() + client_nonce_.size());
  message.insert(message.end(), server_nonce_.cbegin(), server_nonce_.cend());
  message.insert(message.end(), client_nonce_.cbegin(), client_nonce_.cend());
  shared_master_secret_.resize(32);
  crypto_->hmacSha256(
      shared_master_secret_.data(),
      ecdh_derived_key_.data(),
      ecdh_derived_key_.size(),
      message.data(),
      message.size()
  );

  std::vector<unsigned char> unwrapped_secure_header;
  rc = unwrapDataToRecv(unwrapped_secure_header, &frame->encrypted_data());
  if (rc == 0) {
    connection_status_ = kConnectionEstablished;
    logger_->logf(Logger::kLogInfo, "[ClientImpl] established");

    if (handshake_handler_) {
      handshake_handler_(this);
      handshake_handler_ = nullptr;
    }
  } else {
    connection_status_ = kConnectionError;
    logger_->logf(Logger::kLogInfo, "[ClientImpl] handshake failed: unwrap failed: %d", rc);
  }
}

void ClientImpl::onWrappedDataFrame(proto::EncryptedWrappedData *frame) {
  int rc;
  std::vector<unsigned char> plain_text;
  rc = unwrapDataToRecv(plain_text, frame);
  if (rc) {
    logger_->logf(Logger::kLogInfo, "[ClientImpl] onWrappedDataFrame failed: unwrap failed: %d", rc);
    return;
  }

  proto::WrappedData wrapped_data;
  wrapped_data.ParseFromArray(plain_text.data(), plain_text.size());

  auto receiver_it = wrapped_data_receivers_.find(wrapped_data.message().type_url());
  if (receiver_it == wrapped_data_receivers_.cend()) {
    logger_->logf(Logger::kLogWarn, "Not exists message: %s", wrapped_data.message().type_url().c_str());
  } else {
    auto const& value_ref = wrapped_data.message().value();
    receiver_it->second->invoke((const unsigned char*) value_ref.data(), value_ref.length());
  }
}

std::shared_ptr<ClientImpl> ClientImpl::create(
    std::shared_ptr<uvw::Loop> loop,
    std::shared_ptr<Logger> logger,
    std::shared_ptr<crypto::CryptoBase> crypto
) {
  std::shared_ptr<ClientImpl> instance(new ClientImpl(std::move(loop), std::move(logger), std::move(crypto)));
  instance->self_ = instance;
  instance->earlyInit();
  return instance;
}

void ClientImpl::onDataEventRequest(const proto::EventRequest& payload) {
  std::shared_ptr<ClientImpl> self(self_.lock());

  //TODO: Use thread pool
  std::thread worker_thread([self](const proto::EventRequest& payload) -> void {
    auto method_it = self->request_methods_.find(payload.method_name());
    if (method_it == self->request_methods_.cend()) {
      proto::EventComplete complete_message;
      complete_message.set_stream_id(payload.stream_id());
      complete_message.set_status(proto::EventStatus::METHOD_NOT_EXIST);
      self->sendApplicationData(&complete_message);
      return ;
    }
    auto const& method_def = method_it->second;

    auto ctx = std::make_shared<CalleeRequestContextImpl>(self, payload.stream_id());
    std::shared_ptr<CalleeRequestContextBase> request_context(method_def.request_context_factory(payload.stream_id(), ctx));
    request_context->setRequest(payload.data().c_str(), payload.data().length());
    self->running_calls_.emplace(payload.stream_id(), ctx);
    method_def.handler(request_context);
  }, payload);
  worker_thread.detach();
}

void ClientImpl::onDataEventProgress(const proto::EventProgress& payload) {
  auto call_it = running_calls_.find(payload.stream_id());
  if (call_it == running_calls_.cend()) {
    return ;
  }
  call_it->second->onProgress(payload);
}

void ClientImpl::onDataEventComplete(const proto::EventComplete& payload) {
  auto call_it = running_calls_.find(payload.stream_id());
  if (call_it == running_calls_.cend()) {
    return ;
  }
  call_it->second->onComplete(payload);
  this->eventStreamDone(call_it->second.get());
}

void ClientImpl::earlyInit() {
  registerWrappedData<proto::EventRequest>(std::bind(&ClientImpl::onDataEventRequest, this, std::placeholders::_1));
  registerWrappedData<proto::EventProgress>(std::bind(&ClientImpl::onDataEventProgress, this, std::placeholders::_1));
  registerWrappedData<proto::EventComplete>(std::bind(&ClientImpl::onDataEventComplete, this, std::placeholders::_1));
}

ClientImpl::~ClientImpl() {
  logger_->logf(Logger::kLogDebug, "[ClientImpl] destroying");
}

int ClientImpl::wrapDataToSend(proto::EncryptedWrappedData *frame, const unsigned char *data, int length) {
  int64_t counter = my_counter_++;
  int rc;
  std::vector<unsigned char> key;
  std::vector<unsigned char> iv;
  std::vector<unsigned char> cipher_text;
  std::vector<unsigned char> auth_tag;

  generateSecret(iv, "civ", counter, 16);
  generateSecret(key, "cky", counter, 32);

  rc = crypto_->aesGcmEncrypt(
      cipher_text,
      auth_tag,
      key.data(),
      key.size(),
      iv.data(),
      iv.size(),
      128,
      data,
      length
  );
  if (rc) return rc;

  frame->set_version(1);
  frame->set_cipher_text(std::string(cipher_text.cbegin(), cipher_text.cend()));
  frame->set_auth_tag(std::string(auth_tag.cbegin(), auth_tag.cend()));

  return 0;
}

int ClientImpl::unwrapDataToRecv(std::vector<unsigned char> &data, const proto::EncryptedWrappedData *frame) {
  int64_t counter = peer_counter_++;
  std::vector<unsigned char> key;
  std::vector<unsigned char> iv;

  generateSecret(iv, "siv", counter, 16);
  generateSecret(key, "sky", counter, 32);

  return crypto_->aesGcmDecrypt(
      data,
      key.data(),
      key.size(),
      iv.data(),
      iv.size(),
      (const unsigned char *) frame->auth_tag().data(),
      128,
      (const unsigned char *) frame->cipher_text().data(),
      frame->cipher_text().length()
  );
}

int ClientImpl::generateSecret(std::vector<unsigned char> &output, const char *type, int64_t counter, int size) {
  size_t type_len = strlen(type);
  std::vector<unsigned char> message(type_len + 8);
  unsigned char *p = message.data();
  memcpy(p, type, type_len);
  p += type_len;

  p[0] = (unsigned char) ((counter >> 0) & 0xff);
  p[1] = (unsigned char) ((counter >> 8) & 0xff);
  p[2] = (unsigned char) ((counter >> 16) & 0xff);
  p[3] = (unsigned char) ((counter >> 24) & 0xff);
  p[4] = (unsigned char) ((counter >> 32) & 0xff);
  p[5] = (unsigned char) ((counter >> 40) & 0xff);
  p[6] = (unsigned char) ((counter >> 48) & 0xff);
  p[7] = (unsigned char) ((counter >> 56) & 0xff);

  output.resize(32);
  crypto_->hmacSha256(
      output.data(),
      shared_master_secret_.data(),
      shared_master_secret_.size(),
      message.data(),
      message.size()
  );
  if (output.size() > size) {
    output.resize(size);
  }

  return 0;
}

ConnectionStatus ClientImpl::getConnectionStatus() const {
  return kConnectionIdle;
}

Client &ClientImpl::onHandshake(const OnHandshakeHandler_t &handler) {
  handshake_handler_ = handler;
  return *this;
}

void ClientImpl::onRequestImpl(
    const std::string &request_name,
    const CalleeRequestContextBaseFactory_t &request_context_factory,
    std::function<void(std::shared_ptr<CalleeRequestContextBase>)> handler
) {
  OnRequestMethod item = {
      request_name,
      request_context_factory,
      handler
  };
  request_methods_.emplace(request_name, std::move(item));
}

void ClientImpl::requestImpl(std::shared_ptr<CallerRequestContext> ctx) {
  std::shared_ptr<CallerRequestContextImpl> ctx_impl = std::dynamic_pointer_cast<CallerRequestContextImpl>(ctx);
  proto::EventRequest message;
  proto::WrappedData wrapped_data;

  auto const& request_message = ctx->getRequest();

  message.set_stream_id(ctx_impl->getStreamId());
  message.set_method_name(ctx->getRequestName());
  message.set_data(std::string(request_message.cbegin(), request_message.cend()));

  wrapped_data.set_version(1);
  wrapped_data.mutable_message()->set_value(message.SerializeAsString());
  wrapped_data.mutable_message()->set_type_url(getTypeUrl(&message));
  running_calls_.emplace(ctx_impl->getStreamId(), ctx_impl);
  sendWrappedData(wrapped_data, [](bool is_error, const ::uvw::ErrorEvent& evt) -> void {});
}

int ClientImpl::sendWrappedData(const proto::WrappedData &data, const transport::WriteHandler_t& write_handler) {
  int rc;
  proto::EncryptedWrappedData encrypted_wrapped_data;
  std::vector<unsigned char> buffer;
  buffer.resize(data.ByteSizeLong());
  data.SerializeToArray(buffer.data(), buffer.size());
  rc = wrapDataToSend(&encrypted_wrapped_data, buffer.data(), buffer.size());
  if (rc) return rc;
  this->transport_->sendFrame(&encrypted_wrapped_data, write_handler);
  return 0;
}

void ClientImpl::registerWrappedDataImpl(std::unique_ptr<WrappedDataReceiverAdapterBase> adapter) {
  auto default_instance = adapter->getDefaultInstance();
  std::string type_url = getTypeUrl(default_instance);
  wrapped_data_receivers_.emplace(type_url, std::move(adapter));
}

void ClientImpl::eventStreamDone(RequestContext* ctx) {
  logger_->logf(Logger::kLogInfo, "event:stream[%s] done", ctx->getStreamId().c_str());
  running_calls_.erase(ctx->getStreamId());
}

std::string ClientImpl::generateUUID() {
  char uuid_buf[38] = {0};
  unsigned char buf[16] = { 0 };
  int i = 0;
  default_random_->random(buf, sizeof(buf));

  snprintf(
      uuid_buf, sizeof(uuid_buf),
      "%02x%02x%02x%02x-%02x%02x-%02x%02x-%02x%02x-%02x%02x%02x%02x%02x%02x",
      buf[i++], buf[i++], buf[i++], buf[i++], buf[i++], buf[i++], buf[i++], buf[i++],
      buf[i++], buf[i++], buf[i++], buf[i++], buf[i++], buf[i++], buf[i++], buf[i++]
      );

  return std::string(uuid_buf);
}

std::shared_ptr<CallerRequestContext> ClientImpl::createCallerRequestContext(
    const std::string &request_name,
    const ::google::protobuf::Message& request,
    const RawProtobufHandler_t &progress_handler,
    const RawProtobufHandler_t &response_handler
) {
  return std::make_shared<CallerRequestContextImpl>(
      generateUUID(), request_name, request, progress_handler, response_handler
  );
}

std::string ClientImpl::getTypeUrl(const ::google::protobuf::Message* message) {
  return "pb-type.jc-lab.net/sipc/" + message->GetTypeName();
}

int ClientImpl::sendApplicationData(const ::google::protobuf::Message* message) {
  proto::WrappedData wrapped_data;
  wrapped_data.set_version(1);
  wrapped_data.mutable_message()->set_value(message->SerializeAsString());
  wrapped_data.mutable_message()->set_type_url(getTypeUrl(message));
  return sendWrappedData(wrapped_data, [](bool is_error, const ::uvw::ErrorEvent& evt) -> void {});
}

//std::shared_ptr<::grpc::ChannelInterface> ClientImpl::getGrpcChannel() {
//  return grpc_channel_;
//}

} // namespace intl

std::shared_ptr<Client> Client::create(std::shared_ptr<uvw::Loop> loop,
                                       std::shared_ptr<Logger> logger,
                                       std::shared_ptr<crypto::CryptoBase> crypto) {
  if (!crypto) {
    crypto = jcu::sipc::crypto::MbedtlsCrypto::create();
  }
  if (!logger) {
    logger = createDefaultLogger([](const std::string &line) -> void {}); // Null Logger
  }
  return intl::ClientImpl::create(std::move(loop), logger, crypto);
}

} // namespace sipc
} // namespace jcu
