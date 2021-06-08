cmake_minimum_required(VERSION 3.17)
project(jcu_sipc_cpp)

set(CMAKE_CXX_STANDARD 17)

set(JCU_SIPC_MSVC_STATIC_RUNTIME OFF CACHE BOOL "jcu-sipc msvc static runtime")

list(APPEND CMAKE_MODULE_PATH ${CMAKE_CURRENT_SOURCE_DIR}/cmake)
include(cmake/common.cmake)

set(PROJECT_COMMON_FLAGS_DEBUG "")
set(PROJECT_COMMON_FLAGS_RELEASE "")
if (MSVC)
    if (JCU_SIPC_MSVC_STATIC_RUNTIME)
        set(PROJECT_COMMON_FLAGS_DEBUG "${PROJECT_COMMON_FLAGS_DEBUG} /MTd")
        set(PROJECT_COMMON_FLAGS_RELEASE "${PROJECT_COMMON_FLAGS_RELEASE} /MT")
    else ()
        set(PROJECT_COMMON_FLAGS_DEBUG "${PROJECT_COMMON_FLAGS_DEBUG} /MDd")
        set(PROJECT_COMMON_FLAGS_RELEASE "${PROJECT_COMMON_FLAGS_RELEASE} /MD")
    endif ()
endif ()

enable_testing()

include(ExternalProject)
include(FetchContent)

set(GRPC_SRC_DIR ${CMAKE_BINARY_DIR}/thirdparty/grpc-src)
make_directory(${GRPC_SRC_DIR})
add_subdirectory(thirdparty)

# ================================================================================

if (MSVC AND JCU_SIPC_MSVC_STATIC_RUNTIME)
    foreach (flag_var
            CMAKE_C_FLAGS
            CMAKE_C_FLAGS_DEBUG
            CMAKE_C_FLAGS_RELEASE
            CMAKE_C_FLAGS_MINSIZEREL
            CMAKE_C_FLAGS_RELWITHDEBINFO
            CMAKE_CXX_FLAGS
            CMAKE_CXX_FLAGS_DEBUG
            CMAKE_CXX_FLAGS_RELEASE
            CMAKE_CXX_FLAGS_MINSIZEREL
            CMAKE_CXX_FLAGS_RELWITHDEBINFO)
        if (${flag_var} MATCHES "/MD")
            string(REGEX REPLACE "/MD" "/MT" ${flag_var} "${${flag_var}}")
        endif (${flag_var} MATCHES "/MD")
    endforeach ()
endif ()

add_subdirectory(library)
#add_subdirectory(sample)
