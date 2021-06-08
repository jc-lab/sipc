/**
 * @file	base.h
 * @author	Joseph Lee <joseph@jc-lab.net>
 * @date	2021-06-04
 * @copyright Copyright (C) 2021 jc-lab.
 *            This software may be modified and distributed under the terms
 *            of the Apache License 2.0.  See the LICENSE file for details.
 */


#ifndef JCU_SIPC_TRANSPORT_BASE_H_
#define JCU_SIPC_TRANSPORT_BASE_H_

#include <memory>
#include <functional>
#include <vector>
#include <deque>
#include <mutex>
#include <condition_variable>
#include <atomic>

#include <uvw/loop.h>
#include <uvw/emitter.h>
#include <uvw/stream.h>

#include "../connect_info.h"

#include "../protocol/frame_reader.h"

namespace jcu {
namespace sipc {
namespace transport {

typedef std::function<void(bool is_error, const uvw::ErrorEvent &error_event)> WriteHandler_t;
typedef std::function<void()> CloseHandler_t;

class TransportHandler {
 public:
  virtual void onTransportConnect() = 0;
  virtual void onTransportClose() = 0;
  virtual void onTransportError(const uvw::ErrorEvent &evt) = 0;
};

class Base {
 protected:
  std::shared_ptr<uvw::Loop> loop_;

  /**
   * 0 = not closing
   * 1 = closing
   * 2 = not
   */
  int closing_;
  CloseHandler_t current_close_handler_;

  WriteHandler_t current_write_handler_;
  protocol::FrameReader frame_reader_;
  std::weak_ptr<protocol::FrameHandlers> frame_handlers_;
  std::weak_ptr<TransportHandler> transport_handler_;

 private:
  struct SendContext;

  std::mutex send_mtx_;
  std::deque<std::shared_ptr<SendContext>> send_queue_;
  std::atomic<int32_t> send_running_;
  void sendQueued();

 public:
  Base(std::shared_ptr<uvw::Loop> loop);

  virtual std::list<std::string> getChannelTypes() const = 0;

  virtual int start(const ConnectInfo *connect_info) = 0;

  void close(bool immediately, const CloseHandler_t& close_handler);

  void onError(const uvw::ErrorEvent &evt);
  void onConnectEvent(const uvw::ConnectEvent &evt);
  void onDataEvent(const uvw::DataEvent &evt);
  void onEndEvent(const uvw::EndEvent &evt);
  void onCloseEvent(const uvw::CloseEvent &evt);

  virtual void write(std::unique_ptr<char[]> data, unsigned int length, const WriteHandler_t &write_handler) = 0;

  void sendFrame(::google::protobuf::Message* message, const WriteHandler_t &write_handler);
  void setTransportHandler(std::weak_ptr<TransportHandler> transport_handler);
  void setFrameHandlers(std::weak_ptr<protocol::FrameHandlers> frame_handlers);

  bool isClosing() const;

 protected:
  virtual std::shared_ptr<Base> getSelf() = 0;
  virtual void closeImpl() = 0;
};

} // namespace transport
} // namespace sipc
} // namespace jcu

#endif //JCU_SIPC_TRANSPORT_BASE_H_
