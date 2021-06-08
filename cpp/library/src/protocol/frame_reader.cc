//
// Created by jichan on 2021-06-06.
//

#include <jcu-sipc/protocol/frame_reader.h>

#define MAX_BUFFER_SIZE 16777216

namespace jcu {
namespace sipc {
namespace protocol {

FrameReader::FrameReader() :
    read_position_(0) {
}

int32_t FrameReader::readInt24(const void *ptr) {
  const auto* ucptr = (const unsigned char*)ptr;
  int32_t value = 0;
  value |= (((int32_t)ucptr[0]) & 0xff) << 0;
  value |= (((int32_t)ucptr[1]) & 0xff) << 8;
  value |= (((int32_t)ucptr[2]) & 0xff) << 16;
  return value;
}

void FrameReader::writeInt24(void *ptr, int32_t value) {
  auto* ucptr = (unsigned char*)ptr;
  ucptr[0] = (unsigned char)(value & 0xff);
  ucptr[1] = (unsigned char)((value >> 8) & 0xff);
  ucptr[2] = (unsigned char)((value >> 16) & 0xff);
}

bool FrameReader::process(FrameHandlers *handlers, const unsigned char *data, int length) {
  int post_buffer_size = read_position_ + length;
  if (post_buffer_size > MAX_BUFFER_SIZE) {
    post_buffer_size = MAX_BUFFER_SIZE;
  }
  if (post_buffer_size > buffer_.size()) {
    buffer_.resize(alignedSize(post_buffer_size));
  }

  bool has_next_packet;
  do {
    int32_t frame_size = 0;
    uint8_t frame_type = 0;
    unsigned char *buffer_ptr = buffer_.data();

    if (length > 0) {
      int remaining_buffer = buffer_.size() - read_position_;
      int available_length = (remaining_buffer > length) ? length : remaining_buffer;
      memcpy(buffer_.data() + read_position_, data, available_length);
      read_position_ += available_length;
      data += available_length;
      length -= available_length;
    }

    has_next_packet = false;

    if (read_position_ < 4) {
      break;
    }

    frame_size = readInt24(buffer_ptr + 0);
    frame_type = buffer_ptr[3];

    if (frame_size <= read_position_) {
      // Has One or more Packet

      if (!FrameConverter::getInstance()->parse(handlers, frame_type, buffer_ptr + 4, frame_size - 4)) {
        // Failed
        return false;
      }

      int next_frame_length = read_position_ - frame_size;
      if (next_frame_length > 0) {
        has_next_packet = true;
        memmove(buffer_ptr, buffer_ptr + frame_size, next_frame_length);
      }
      read_position_ = next_frame_length;
    } else {
      if (read_position_ == MAX_BUFFER_SIZE) {
        // Buffer Overflow
        return false;
      }
    }
  } while(length > 0 || has_next_packet);

  return true;
}

int FrameReader::alignedSize(int size) {
  int mod = size % 4096;
  if (mod) {
    size += 4096 - mod;
  }
  return size;
}

} // namespace protocol
} // namespace sipc
} // namespace jcu
