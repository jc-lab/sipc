/**
 * @file	base.cc
 * @author	Joseph Lee <joseph@jc-lab.net>
 * @date	2021-06-04
 * @copyright Copyright (C) 2021 jc-lab.
 *            This software may be modified and distributed under the terms
 *            of the Apache License 2.0.  See the LICENSE file for details.
 */


#include <jcu-sipc/transport/base.h>

namespace jcu {
namespace sipc {
namespace transport {

struct Base::SendContext {
  std::shared_ptr<Base> base;
  std::vector<unsigned char> frame;
  int written;
  std::function<void()> next;
  WriteHandler_t done_handler;

  void done() {
    next = nullptr;
    base->sendQueued();
  }

  std::unique_ptr<char[]> sliceNext(int* output_length, int slice_size) {
    int remaining = frame.size() - written;
    int available = (remaining > slice_size) ? slice_size : remaining;
    std::unique_ptr<char[]> new_buffer(new char[available]);
    memcpy(new_buffer.get(), frame.data() + written, available);
    *output_length = available;
    written += available;
    return std::move(new_buffer);
  }

  bool isRemaining() const {
    return written < frame.size();
  }
};

Base::Base(std::shared_ptr<uvw::Loop> loop) :
    loop_(loop),
    send_running_(0),
    closing_(0) {
}

bool Base::isClosing() const {
  return closing_ > 0;
}

void Base::close(bool immediately, const CloseHandler_t& close_handler) {
  if (closing_) return ;
  closing_ = 1;
  if (immediately) {
    closeImpl();
    if (close_handler) {
      close_handler();
    }
  } else {
    std::unique_lock<std::mutex> lock(send_mtx_);
    if (send_queue_.empty()) {
      closeImpl();
      if (close_handler) {
        close_handler();
      }
    } else {
      current_close_handler_ = close_handler;
    }
  }
}

void Base::onError(const uvw::ErrorEvent &evt) {
  if (current_write_handler_) {
    current_write_handler_(true, evt);
    current_write_handler_ = nullptr;
  }

  closeImpl();

  std::shared_ptr<TransportHandler> transport_handler(transport_handler_.lock());
  if (transport_handler) {
    transport_handler->onTransportError(evt);
  }
}

void Base::onConnectEvent(const uvw::ConnectEvent &evt) {
  std::shared_ptr<TransportHandler> transport_handler(transport_handler_.lock());
  if (!transport_handler) return;

  transport_handler->onTransportConnect();
}

void Base::onDataEvent(const uvw::DataEvent &evt) {
  std::shared_ptr<protocol::FrameHandlers> frame_handlers(frame_handlers_.lock());
  if (!frame_handlers) {
    return ;
  }
  if (!frame_reader_.process(frame_handlers.get(), (const unsigned char*)evt.data.get(), evt.length)) {
    return ;
  }
}

void Base::onEndEvent(const uvw::EndEvent &evt) {
  std::shared_ptr<TransportHandler> transport_handler(transport_handler_.lock());
  if (!transport_handler) return;
}

void Base::onCloseEvent(const uvw::CloseEvent &evt) {
  std::shared_ptr<TransportHandler> transport_handler(transport_handler_.lock());
  if (!transport_handler) return;

  transport_handler->onTransportClose();
}

void Base::setTransportHandler(std::weak_ptr<TransportHandler> transport_handler) {
  transport_handler_ = std::move(transport_handler);
}

void Base::setFrameHandlers(std::weak_ptr<protocol::FrameHandlers> frame_handlers) {
  frame_handlers_ = std::move(frame_handlers);
}

void Base::sendFrame(::google::protobuf::Message *message, const WriteHandler_t &done_handler) {
  std::shared_ptr<Base> self(getSelf());

  int message_size = message->ByteSizeLong();
  uint8_t frame_type = protocol::FrameConverter::getInstance()->getFrameType(message);
  if (!frame_type) {
    return ;
  }

  std::shared_ptr<SendContext> send_context(std::make_shared<SendContext>());
  send_context->base = self;
  send_context->frame.resize(4 + message_size);
  send_context->written = 0;
  send_context->done_handler = done_handler;

  protocol::FrameReader::writeInt24(send_context->frame.data(), 4 + message_size);
  send_context->frame[3] = frame_type;
  message->SerializeToArray(send_context->frame.data() + 4, message_size);

  send_context->next = [send_context, self]() -> void {
    int sliced_size = 0;
    auto sliced_data = send_context->sliceNext(&sliced_size, 4096);
    self->write(std::move(sliced_data), sliced_size, [send_context, sliced_size](bool is_error, const uvw::ErrorEvent &error_event) -> void {
      if (is_error) {
        send_context->done_handler(is_error, error_event);
        return;
      }
      if (send_context->isRemaining()) {
        send_context->next();
      } else {
        send_context->done_handler(false, error_event);
        send_context->done();
      }
    });
  };

  bool run_send_queue = false;
  do {
    std::unique_lock<std::mutex> lock(send_mtx_);
    send_queue_.emplace_back(send_context);
    if (send_running_.fetch_or(1) == 0) {
      run_send_queue = true;
    }
  } while(0);
  if (run_send_queue) {
    sendQueued();
  }
}

void Base::sendQueued() {
  std::shared_ptr<SendContext> send_context;
  do {
    std::unique_lock<std::mutex> lock(send_mtx_);
    if (!send_queue_.empty()) {
      send_context = send_queue_.front();
      send_queue_.pop_front();
    } else {
      send_running_.store(0);
    }
  } while(0);
  if (send_context) {
    send_context->next();
  } else {
    if (closing_) {
      closing_ = 2;
      closeImpl();
      if (current_close_handler_) {
        current_close_handler_();
      }
    }
  }
}

} // namespace transport
} // namespace sipc
} // namespace jcu
