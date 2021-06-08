/**
 * @file	server_impl.cc
 * @author	Joseph Lee <joseph@jc-lab.net>
 * @date	2021-03-08
 * @copyright Copyright (C) 2021 jc-lab.
 *            This software may be modified and distributed under the terms
 *            of the Apache License 2.0.  See the LICENSE file for details.
 */


#include "server_impl.h"

namespace jcu {
namespace sipc {
namespace intl {

} // namespace intl

Server *Server::newInstance() {
  return new Server();
}

} // namespace sipc
} // namespace jcu
