//
// Created by jichan on 2021-06-06.
//

#include <jcu-sipc/proto/sipc_frames.pb.h>
#include <jcu-sipc/protocol/frame_converter.h>

namespace jcu {
namespace sipc {
namespace protocol {

void FrameConverter::add(ProtoHolder&& holder) {
  type_map_.emplace(holder.frame_type, std::move(holder));
}

FrameConverter::FrameConverter() {
  add(ProtoHolder {
      0xf1,
      std::make_unique<proto::AlertFrame>(),
      [](FrameHandlers* handlers, ::google::protobuf::Message* message) -> void {
        handlers->onAlertFrame(dynamic_cast<proto::AlertFrame*>(message));
      }});
  add(ProtoHolder {
      0x11,
      std::make_unique<proto::ClientHelloFrame>(),
      [](FrameHandlers* handlers, ::google::protobuf::Message* message) -> void {
        handlers->onClientHelloFrame(dynamic_cast<proto::ClientHelloFrame*>(message));
      }});
  add(ProtoHolder {
      0x12,
      std::make_unique<proto::ServerHelloFrame>(),
      [](FrameHandlers* handlers, ::google::protobuf::Message* message) -> void {
        handlers->onServerHelloFrame(dynamic_cast<proto::ServerHelloFrame*>(message));
      }});
  add(ProtoHolder {
      0x1a,
      std::make_unique<proto::EncryptedWrappedData>(),
      [](FrameHandlers* handlers, ::google::protobuf::Message* message) -> void {
        handlers->onWrappedDataFrame(dynamic_cast<proto::EncryptedWrappedData*>(message));
      }
  });
}

const FrameConverter *FrameConverter::getInstance() {
  static FrameConverter instance;
  return &instance;
}

bool FrameConverter::parse(FrameHandlers *frame_handlers, uint8_t frame_type, const unsigned char *frame_payload, int payload_length) const {
  auto holder_it = type_map_.find(frame_type);
  if (holder_it == type_map_.cend()) {
    return false;
  }
  const auto& holder = holder_it->second;
  std::unique_ptr<::google::protobuf::Message> message(holder.default_instance->New());
  if (!message->ParseFromArray(frame_payload, payload_length)) {
    return false;
  }
  holder.router(frame_handlers, message.get());
  return true;
}

uint8_t FrameConverter::getFrameType(const ::google::protobuf::Message *message) const {
  for (auto it = type_map_.cbegin(); it != type_map_.cend(); it++) {
    auto const& holder = it->second;
    if (holder.default_instance->GetTypeName() == message->GetTypeName()) {
      return holder.frame_type;
    }
  }
  return 0;
}

} // namespace protocol
} // namespace sipc
} // namespace jcu
