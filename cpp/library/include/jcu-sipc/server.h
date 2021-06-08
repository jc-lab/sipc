//
// Created by jichan on 2021-03-08.
//

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
