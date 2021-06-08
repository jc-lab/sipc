//
// Created by jichan on 2021-06-04.
//

#ifndef JCU_SIPC_CPP_LIBRARY_INCLUDE_JCU_SIPC_TRANSPORT_TCP_H_
#define JCU_SIPC_CPP_LIBRARY_INCLUDE_JCU_SIPC_TRANSPORT_TCP_H_

#include <atomic>

#include <uvw/tcp.h>

#include "base.h"

namespace jcu {
namespace sipc {
namespace transport {

class Tcp : public Base {
 private:
  std::shared_ptr<uvw::TCPHandle> handle_;
  std::weak_ptr<Tcp> self_;

  std::atomic_int32_t error_ignore_;

  Tcp(std::shared_ptr<uvw::Loop> loop);

  std::list<std::string> getChannelTypes() const override;

  int start(const ConnectInfo *connect_info) override;

  void closeImpl() override;

 protected:
  std::shared_ptr<Base> getSelf() override;

 public:
  static std::shared_ptr<Tcp> create(std::shared_ptr<uvw::Loop> loop);

  void write(std::unique_ptr<char[]> data, unsigned int length, const WriteHandler_t &write_handler) override;
};

} // namespace transport
} // namespace sipc
} // namespace jcu

#endif //JCU_SIPC_CPP_LIBRARY_INCLUDE_JCU_SIPC_TRANSPORT_TCP_H_
