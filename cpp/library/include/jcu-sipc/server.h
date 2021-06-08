/**
 * @file	server.h
 * @author	Joseph Lee <joseph@jc-lab.net>
 * @date	2021-03-08
 * @copyright Copyright (C) 2021 jc-lab.
 *            This software may be modified and distributed under the terms
 *            of the Apache License 2.0.  See the LICENSE file for details.
 */


#ifndef JCU_SIPC_SERVER_H_
#define JCU_SIPC_SERVER_H_

#include <memory>

namespace jcu {
namespace sipc {

class Server {
 public:
  static Server *newInstance();

  static std::unique_ptr<Server> newUniqueInstance() {
    return std::unique_ptr<Server>(newInstance());
  }

  static std::shared_ptr<Server> newServerInstance() {
    return std::shared_ptr<Server>(newInstance());
  }
};

} // namespace sipc
} // namespace jcu

#endif //JCU_SIPC_SERVER_H_
