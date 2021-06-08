//
// Created by jichan on 2021-06-06.
//

#ifndef JCU_SIPC_CPP_LIBRARY_SRC_PROTOCOL_FRAME_CONVERTER_H_
#define JCU_SIPC_CPP_LIBRARY_SRC_PROTOCOL_FRAME_CONVERTER_H_

#include <stdint.h>

#include <memory>
#include <map>
#include <functional>

#include <jcu-sipc/proto/sipc_frames.pb.h>

namespace jcu {
namespace sipc {
namespace protocol {

class FrameHandlers {
 public:
  virtual void onAlertFrame(proto::AlertFrame* frame) = 0;
  virtual void onClientHelloFrame(proto::ClientHelloFrame* frame) = 0;
  virtual void onServerHelloFrame(proto::ServerHelloFrame* frame) = 0;
  virtual void onWrappedDataFrame(proto::EncryptedWrappedData* frame) = 0;
};

class FrameConverter {
 private:
  struct ProtoHolder {
    uint8_t frame_type;
    std::unique_ptr<::google::protobuf::Message> default_instance;
    std::function<void(FrameHandlers* handlers, ::google::protobuf::Message* message)> router;
  };

  std::map<uint8_t, ProtoHolder> type_map_;
  void add(ProtoHolder&& holder);

  FrameConverter();

 public:
  static const FrameConverter* getInstance();

  bool parse(FrameHandlers* frame_handlers, uint8_t frame_type, const unsigned char* frame_payload, int payload_length) const;
  uint8_t getFrameType(const ::google::protobuf::Message* message) const;
};

} // namespace protocol
} // namespace sipc
} // namespace jcu

#endif //JCU_SIPC_CPP_LIBRARY_SRC_PROTOCOL_FRAME_CONVERTER_H_
