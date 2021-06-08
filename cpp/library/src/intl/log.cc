//
// Created by jichan on 2021-03-08.
//

#include <cstring>
#include <stdio.h>
#include <time.h>
#include <jcu-sipc/log.h>

#ifdef _MSC_VER
#define SYSTEM_GMTIME_S(a, b) gmtime_s(a, b)
#else
#define SYSTEM_GMTIME_S(a, b) gmtime_r(b, a)
#endif

#define LOGF_BUFFER_REMAINING (buf.size() - buf_position)
#define LOGF_BOUNDARY_CHECK if ((buf.size() - buf_position) <= 0) { break; }

namespace jcu {
namespace sipc {
namespace intl {

class DefaultLoggerImpl : public Logger {
 private:
  LogWriter_t log_writer_;

 public:
  DefaultLoggerImpl(const LogWriter_t &writer) :
      log_writer_(writer) {

  }

  void logf(LogLevel level, const char *format, ...) override {
    static char* LEVEL_TEXT[] = {
        "TRACE", // kLogTrace
        "DEBUG", // kLogDebug
        "INFO ", // kLogInfo
        "WARN ", // kLogWarn
        "ERROR" // kLogError
    };

    std::va_list arg_list;
    time_t now_ts = time(nullptr);
    tm now_tm;
    std::vector<char> buf(std::strlen(format) + 4096);
    char* buf_ptr = buf.data();
    size_t buf_position = 0;

    if (level < kLogTrace || level > kLogError) {
      return ;
    }

    do {
      SYSTEM_GMTIME_S(&now_tm, &now_ts);

      buf_position += strftime(&buf_ptr[buf_position], LOGF_BUFFER_REMAINING, "%Y-%m-%dT%H:%M:%S%z ", &now_tm);
      LOGF_BOUNDARY_CHECK;

      buf_position += snprintf(&buf_ptr[buf_position], LOGF_BUFFER_REMAINING, "[%s] ", LEVEL_TEXT[level]);

      va_start(arg_list, format);
      vsnprintf(&buf_ptr[buf_position], LOGF_BUFFER_REMAINING, format, arg_list);
      va_end(arg_list);
    } while (0);

    if (log_writer_) {
      log_writer_(buf.data());
    }
  }
};

} // namespace intl

std::shared_ptr<Logger> createDefaultLogger(const LogWriter_t &writer) {
  return std::make_shared<intl::DefaultLoggerImpl>(writer);
}

} // namespace sipc
} // namespace jcu
