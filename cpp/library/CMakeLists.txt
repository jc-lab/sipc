#find_package(absl REQUIRED)
#find_package(gRPC REQUIRED)

set(mbedcrypto_target "${MBEDTLS_TARGET_PREFIX}mbedcrypto")

set(SRC_DIR ${CMAKE_CURRENT_SOURCE_DIR}/src)
set(INC_DIR ${CMAKE_CURRENT_SOURCE_DIR}/include)

set(ALL_SRC_FILES
        ${INC_DIR}/jcu-sipc/server.h
        ${INC_DIR}/jcu-sipc/client.h
        ${INC_DIR}/jcu-sipc/log.h
        ${INC_DIR}/jcu-sipc/connect_info.h
        ${INC_DIR}/jcu-sipc/transport/base.h
        ${INC_DIR}/jcu-sipc/transport/stdio.h
        ${INC_DIR}/jcu-sipc/transport/tcp.h
        ${INC_DIR}/jcu-sipc/protocol/frame_reader.h
        ${INC_DIR}/jcu-sipc/protocol/frame_converter.h
        ${INC_DIR}/jcu-sipc/crypto/crypto_base.h
        ${INC_DIR}/jcu-sipc/crypto/secure_random.h
        ${INC_DIR}/jcu-sipc/crypto/mbedtls_crypto.h
        ${INC_DIR}/jcu-sipc/crypto/ephemeral_key_factory.h
        ${INC_DIR}/jcu-sipc/crypto/ephemeral_key_pair.h
        ${SRC_DIR}/intl/log.cc
        ${SRC_DIR}/server.cc
        ${SRC_DIR}/client.cc
        ${SRC_DIR}/server_impl.h
        ${SRC_DIR}/server_impl.cc
        ${SRC_DIR}/client_impl.h
        ${SRC_DIR}/client_impl.cc
        ${SRC_DIR}/base64/base64.h
        ${SRC_DIR}/base64/base64.cc
        ${SRC_DIR}/connect_info.cc
        ${SRC_DIR}/transport/base.cc
        ${SRC_DIR}/transport/stdio.cc
        ${SRC_DIR}/transport/tcp.cc
        ${SRC_DIR}/protocol/frame_reader.cc
        ${SRC_DIR}/protocol/frame_converter.cc
        ${SRC_DIR}/crypto/mbedtls/crypto_impl.h
        ${SRC_DIR}/crypto/mbedtls/crypto_impl.cc
        ${SRC_DIR}/crypto/mbedtls/secure_random_impl.h
        ${SRC_DIR}/crypto/mbedtls/secure_random_impl.cc
        ${SRC_DIR}/crypto/mbedtls/x25519_key_factory.h
        ${SRC_DIR}/crypto/mbedtls/x25519_key_factory.cc
        ${SRC_DIR}/crypto/mbedtls/x25519_key_pair.h
        ${SRC_DIR}/crypto/mbedtls/x25519_key_pair.cc
#        ${SRC_DIR}/grpc/middle_grpc_channel.h
#        ${SRC_DIR}/grpc/middle_grpc_channel.cc
        ${SRC_DIR}/event/request_context.h
        ${SRC_DIR}/event/request_context.cc
        )

set(jcu_sipc_target jcu_sipc)

set(GENERATED_INC_DIR ${CMAKE_CURRENT_BINARY_DIR}/include)
make_directory(${GENERATED_INC_DIR})
set(GENERATED_PROTOC_DIR ${CMAKE_CURRENT_BINARY_DIR}/include/jcu-sipc/proto/)
make_directory(${GENERATED_PROTOC_DIR})

add_library(${jcu_sipc_target} STATIC ${ALL_SRC_FILES})
if (TARGET googletest_project)
    add_dependencies(${jcu_sipc_target} googletest_project)
endif()
if (TARGET mbedtls_project)
    add_dependencies(${jcu_sipc_target} mbedtls_project)
endif()
target_link_libraries(
        ${jcu_sipc_target}
        PUBLIC
        ${mbedcrypto_target}
        nlohmann_json
        uv_a
        uvw-static
        ${_JCU_SIPC_PROTOBUF_LIBRARIES}
#        gRPC::grpc++_unsecure
)
target_include_directories(
        ${jcu_sipc_target}
        PUBLIC
        ${_JCU_SIPC_PROTOBUF_WELLKNOWN_INCLUDE_DIR}
)
if (WIN32)
    target_link_libraries(
            ${jcu_sipc_target}
            PUBLIC
            ws2_32
            iphlpapi
            userenv
    )
else()
    target_link_libraries(
            ${jcu_sipc_target}
            PUBLIC
            pthread dl
    )
endif ()
target_include_directories(
        ${jcu_sipc_target}
        PRIVATE
        ${CMAKE_CURRENT_BINARY_DIR}
#        ${GRPC_SRC_DIR}
        PUBLIC
        $<BUILD_INTERFACE:${INC_DIR}>
        $<BUILD_INTERFACE:${CMAKE_CURRENT_BINARY_DIR}/include>
        $<INSTALL_INTERFACE:include>
)

protobuf_generate_ex(
        LANGUAGE cpp
        TARGET ${jcu_sipc_target}
        IMPORT_DIRS ${CMAKE_CURRENT_SOURCE_DIR}/../../proto
        PROTOC_OUT_DIR ${GENERATED_PROTOC_DIR}
        PROTOS sipc_frames.proto
)
