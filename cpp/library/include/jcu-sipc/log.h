//
// Created by jichan on 2021-03-08.
//

#ifndef JCU_SIPC_LOG_H_
#define JCU_SIPC_LOG_H_

#include <memory>
#include <string>
#include <cstdarg>
#include <functional>

namespace jcu {
namespace sipc {

typedef std::function<void(const std::string &log)> LogWriter_t;

class Logger {
 public:
  enum LogLevel {
    kLogTrace,
    kLogDebug,
    kLogInfo,
    kLogWarn,
    kLogError
  };

  virtual void logf(LogLevel level, const char *format, ...) = 0;
};

std::shared_ptr<Logger> createDefaultLogger(const LogWriter_t& writer);

} // namespace sipc
} // namespace jcu

#endif //JCU_SIPC_LOG_H_
