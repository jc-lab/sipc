/**
 * @file	stdio.h
 * @author	Joseph Lee <joseph@jc-lab.net>
 * @date	2021-06-04
 * @copyright Copyright (C) 2021 jc-lab.
 *            This software may be modified and distributed under the terms
 *            of the Apache License 2.0.  See the LICENSE file for details.
 */


#ifndef JCU_SIPC_TRANSPORT_STDIO_H_
#define JCU_SIPC_TRANSPORT_STDIO_H_

#include <uvw/pipe.h>
#include <uvw/tty.h>

#include "base.h"

namespace jcu {
namespace sipc {
namespace transport {

class Stdio : public Base {
 public:
  enum HandleType {
    kStdin,
    kStdout
  };

 private:
  std::weak_ptr<Stdio> self_;

  std::shared_ptr<uvw::TTYHandle> tty_stdout_;
  std::shared_ptr<uvw::PipeHandle> pipe_stdout_;
  std::shared_ptr<uvw::TTYHandle> tty_stdin_;
  std::shared_ptr<uvw::PipeHandle> pipe_stdin_;

  std::atomic_int32_t out_error_ignore_;

  Stdio(std::shared_ptr<uvw::Loop> loop);

  std::list<std::string> getChannelTypes() const override;

  int start(const ConnectInfo *connect_info) override;

  void closeImpl() override;

 protected:
  std::shared_ptr<Base> getSelf() override;

 public:
  static std::shared_ptr<Stdio> create(std::shared_ptr<uvw::Loop> loop);

  void write(std::unique_ptr<char[]> data, unsigned int length, const WriteHandler_t &write_handler) override;
};

} // namespace transport
} // namespace sipc
} // namespace jcu

#endif //JCU_SIPC_TRANSPORT_STDIO_H_
