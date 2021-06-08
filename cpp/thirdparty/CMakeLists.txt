if (MSVC)
    add_compile_options(/utf-8)
endif ()

include(gtest.cmake)
include(mbedtls.cmake)
include(uv.cmake)
include(uvw.cmake)
include(nlohmann_json.cmake)
include(protobuf.cmake)

#TODO: USE GRPC INTERFACE
# include(grpc.cmake)
