//
// Created by jichan on 2021-06-04.
//

#include <jcu-sipc/transport/tcp.h>

namespace jcu {
namespace sipc {
namespace transport {

Tcp::Tcp(std::shared_ptr<uvw::Loop> loop) :
    Base(std::move(loop)),
    error_ignore_(0) {
}

std::list<std::string> Tcp::getChannelTypes() const {
  std::list<std::string> list;
  list.emplace_back("tcp4");
  list.emplace_back("tcp6");
  return std::move(list);
}

int Tcp::start(const ConnectInfo *connect_info) {
  const auto *tcp_connect_info = dynamic_cast<const TcpConnectInfo *>(connect_info);
  std::shared_ptr<Tcp> self(self_.lock());
  handle_ = loop_->resource<uvw::TCPHandle>();
  handle_->on<uvw::ErrorEvent>([self](const uvw::ErrorEvent &evt, auto &handle) -> void {
    if (self->error_ignore_.fetch_and(0) == 0) {
      self->onError(evt);
    }
  });
  handle_->once<uvw::ConnectEvent>([self](const uvw::ConnectEvent &evt, auto &handle) -> void {
    handle.read();
    self->onConnectEvent(evt);
  });
  handle_->on<uvw::DataEvent>([self](const uvw::DataEvent &evt, auto &handle) -> void {
    self->onDataEvent(evt);
  });
  handle_->once<uvw::EndEvent>([self](const uvw::EndEvent &evt, auto &handle) -> void {
    self->onEndEvent(evt);
  });
  handle_->once<uvw::CloseEvent>([self](const uvw::CloseEvent &evt, auto &handle) -> void {
    self->onCloseEvent(evt);
  });
  handle_->connect(tcp_connect_info->getAddress(), tcp_connect_info->getPort());
  return 0;
}

std::shared_ptr<Tcp> Tcp::create(std::shared_ptr<uvw::Loop> loop) {
  auto instance = std::shared_ptr<Tcp>(new Tcp(loop));
  instance->self_ = instance;
  return instance;
}

void Tcp::write(std::unique_ptr<char[]> data, unsigned int length, const WriteHandler_t &write_handler) {
  std::shared_ptr<Tcp> self(self_.lock());

  // If immediately closed
  if (!handle_) {
    return ;
  }

  self->error_ignore_.store(1);
  int rc = handle_->tryWrite(data.get(), length);
  self->error_ignore_.store(0);

//  fprintf(stderr, "WRITE OK %d\n", length);

  if (rc > 0) {
    uvw::ErrorEvent error_event{0};
    write_handler(false, error_event);
  }else if (rc == UV_EAGAIN) {
    self->current_write_handler_ = write_handler;
    handle_->once<uvw::WriteEvent>([self, write_handler, length](uvw::WriteEvent &evt, auto &handle) -> void {
      uvw::ErrorEvent error_event{0};
      self->current_write_handler_ = nullptr;
      write_handler(false, error_event);
    });
    handle_->write(std::move(data), length);
  } else {
    uvw::ErrorEvent error_event{rc};
    write_handler(true, error_event);
  }
}


void Tcp::closeImpl() {
  if (handle_) {
    handle_->close();
  }
  handle_.reset();
}

std::shared_ptr<Base> Tcp::getSelf() {
  std::shared_ptr<Tcp> self(self_.lock());
  return std::static_pointer_cast<Base>(self);
}

} // namespace transport
} // namespace sipc
} // namespace jcu
