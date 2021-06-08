/**
 * @file	stdio.cc
 * @author	Joseph Lee <joseph@jc-lab.net>
 * @date	2021-06-04
 * @copyright Copyright (C) 2021 jc-lab.
 *            This software may be modified and distributed under the terms
 *            of the Apache License 2.0.  See the LICENSE file for details.
 */


#include <jcu-sipc/transport/stdio.h>

namespace jcu {
namespace sipc {
namespace transport {

Stdio::Stdio(std::shared_ptr<uvw::Loop> loop) :
    Base(std::move(loop)),
    out_error_ignore_(0) {
}

std::list<std::string> Stdio::getChannelTypes() const {
  std::list<std::string> list;
  list.emplace_back("stdio");
  return std::move(list);
}

int Stdio::start(const ConnectInfo *connect_info) {
  std::shared_ptr<Stdio> self(self_.lock());

  tty_stdin_ = loop_->resource<uvw::TTYHandle>(uvw::StdIN, true);
  pipe_stdin_ = (!tty_stdin_) ? loop_->resource<uvw::PipeHandle>() : nullptr;

  tty_stdout_ = loop_->resource<uvw::TTYHandle>(uvw::StdOUT, false);
  pipe_stdout_ = (!tty_stdout_) ? loop_->resource<uvw::PipeHandle>() : nullptr;

  if (pipe_stdout_) {
    pipe_stdout_->on<uvw::ErrorEvent>([self](const uvw::ErrorEvent &evt, auto &handle) -> void {
      self->onError(evt);
    });
    pipe_stdout_->open(uvw::StdOUT);
  } else if (tty_stdout_) {
    tty_stdout_->on<uvw::ErrorEvent>([self](const uvw::ErrorEvent &evt, auto &handle) -> void {
      self->onError(evt);
    });
  }

  if (pipe_stdin_) {
    pipe_stdin_->on<uvw::ErrorEvent>([self](uvw::ErrorEvent &evt, auto &handle) -> void {
      self->onError(evt);
    });
    pipe_stdin_->on<uvw::DataEvent>([self](uvw::DataEvent &evt, auto &handle) -> void {
      self->onDataEvent(evt);
    });
    pipe_stdin_->open(uvw::StdIN);
    pipe_stdin_->read();
  } else if (tty_stdin_) {
    tty_stdin_->on<uvw::ErrorEvent>([self](uvw::ErrorEvent &evt, auto &handle) -> void {
      self->onError(evt);
    });
    tty_stdin_->on<uvw::DataEvent>([self](uvw::DataEvent &evt, auto &handle) -> void {
      self->onDataEvent(evt);
    });
    tty_stdin_->read();
  } else {
    return -1;
  }

  this->onConnectEvent(uvw::ConnectEvent{});

  return 0;
}

std::shared_ptr<Stdio> Stdio::create(std::shared_ptr<uvw::Loop> loop) {
  auto instance = std::shared_ptr<Stdio>(new Stdio(loop));
  instance->self_ = instance;
  return instance;
}

void Stdio::write(std::unique_ptr<char[]> data, unsigned int length, const WriteHandler_t &write_handler) {
  int rc;
  std::shared_ptr<Stdio> self(self_.lock());
  if (pipe_stdout_) {
    out_error_ignore_.store(1);
    rc = pipe_stdout_->tryWrite(data.get(), length);
    out_error_ignore_.store(0);
    if (rc > 0) {
      uvw::ErrorEvent error_event{0};
      write_handler(false, error_event);
    } else if (rc == UV_EAGAIN) {
      current_write_handler_ = write_handler;
      pipe_stdout_->once<uvw::WriteEvent>([self, write_handler](uvw::WriteEvent &evt, auto &handle) -> void {
        uvw::ErrorEvent error_event{0};
        self->current_write_handler_ = nullptr;
        write_handler(false, error_event);
      });
      pipe_stdout_->write(std::move(data), length);
    } else {
      uvw::ErrorEvent error_event{rc};
      write_handler(false, error_event);
    }
  } else if (tty_stdout_) {
    out_error_ignore_.store(1);
    rc = tty_stdout_->tryWrite(data.get(), length);
    out_error_ignore_.store(0);
    if (rc > 0) {
      uvw::ErrorEvent error_event{0};
      write_handler(true, error_event);
    } else if (rc == UV_EAGAIN) {
      current_write_handler_ = write_handler;
      tty_stdout_->once<uvw::WriteEvent>([self, write_handler](uvw::WriteEvent &evt, auto &handle) -> void {
        uvw::ErrorEvent error_event{0};
        self->current_write_handler_ = nullptr;
        write_handler(false, error_event);
      });
      tty_stdout_->write(std::move(data), length);
    } else {
      uvw::ErrorEvent error_event{rc};
      write_handler(true, error_event);
    }
  }
}

void Stdio::closeImpl() {
  if (tty_stdout_) {
    tty_stdout_->close();
  }
  if (pipe_stdout_) {
    pipe_stdout_->close();
  }
  if (tty_stdin_) {
    tty_stdin_->close();
  }
  if (pipe_stdin_) {
    pipe_stdin_->close();
  }
  tty_stdin_.reset();
  pipe_stdin_.reset();
  tty_stdout_.reset();
  pipe_stdout_.reset();
}

std::shared_ptr<Base> Stdio::getSelf() {
  std::shared_ptr<Stdio> self(self_.lock());
  return std::static_pointer_cast<Base>(self);
}

} // namespace transport
} // namespace sipc
} // namespace jcu
