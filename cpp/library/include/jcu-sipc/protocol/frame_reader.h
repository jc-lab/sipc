/**
 * @file	frame_reader.h
 * @author	Joseph Lee <joseph@jc-lab.net>
 * @date	2021-06-06
 * @copyright Copyright (C) 2021 jc-lab.
 *            This software may be modified and distributed under the terms
 *            of the Apache License 2.0.  See the LICENSE file for details.
 */


#ifndef JCU_SIPC_TRANSPORT_PROTOCOL_FRAME_READER_H_
#define JCU_SIPC_TRANSPORT_PROTOCOL_FRAME_READER_H_

#include <stdint.h>
#include <vector>

#include "frame_converter.h"

namespace jcu {
namespace sipc {
namespace protocol {

class FrameReader {
 private:
  std::vector<unsigned char> buffer_;
  int read_position_;

 public:
  FrameReader();
  bool process(FrameHandlers* handlers, const unsigned char *data, int length);

  static int32_t readInt24(const void* ptr);
  static void writeInt24(void* ptr, int32_t value);
  static int alignedSize(int size);
};

} // namespace protocol
} // namespace sipc
} // namespace jcu

#endif //JCU_SIPC_CPP_LIBRARY_INCLUDE_JCU_SIPC_PROTOCOL_PROTOCOL_HEADER_H_
