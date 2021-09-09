if (MSVC)
    add_compile_options(/utf-8)
endif ()

set(SUBPROJECTS_COMMON_CMAKE_ARGS
        "-DJCU_SIPC_SUB_PROJECT=TRUE"
        "-DCMAKE_MODULE_PATH=${CMAKE_MODULE_PATH}"
        "-DTHIRDPARTIES_OUTPUT_DIR=${THIRDPARTIES_OUTPUT_DIR}"
        "-DCMAKE_INSTALL_PREFIX=${THIRDPARTIES_OUTPUT_DIR}"
        )

include(gtest.cmake)
include(mbedtls.cmake)

if (NOT TARGET uv_a)
    include(uv.cmake)
endif()

if (NOT TARGET uvw-static)
    include(uvw.cmake)
endif()

if (NOT TARGET nlohmann_json)
    include(nlohmann_json.cmake)
endif()

#TODO: USE GRPC INTERFACE
# include(grpc.cmake)
