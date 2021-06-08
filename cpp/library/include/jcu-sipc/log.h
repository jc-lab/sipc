/**
 * @file	log.h
 * @author	Joseph Lee <joseph@jc-lab.net>
 * @date	2021-03-08
 * @copyright Copyright (C) 2021 jc-lab.
 *            This software may be modified and distributed under the terms
 *            of the Apache License 2.0.  See the LICENSE file for details.
 */


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
