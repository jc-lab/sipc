find_package(Protobuf REQUIRED)
#find_package(gRPC REQUIRED)

set(project_prefix jcu_sipc_test_)

add_executable("${project_prefix}connect_info" connect_info_tests.cc)
target_link_libraries("${project_prefix}connect_info" PUBLIC jcu_sipc gtest gtest_main)

get_target_property(
        temp gtest INTERFACE_INCLUDE_DIRECTORIES
)
message("temp = ${temp}")
add_test(NAME jcu_sipc-tests-all
        COMMAND "${project_prefix}connect_info"
        )

add_executable(sample-app sample.cc test_res.cc)

if (MSVC)
    target_link_options(sample-app PRIVATE "/DEBUG:FASTLINK")
endif()

target_link_libraries(sample-app
        PRIVATE
        jcu_sipc
        )
target_include_directories(sample-app
        PRIVATE
        ${CMAKE_CURRENT_BINARY_DIR}
        )
protobuf_generate_ex(
        LANGUAGE cpp
        TARGET sample-app
        IMPORT_DIRS ${CMAKE_CURRENT_SOURCE_DIR}/../../sample-proto
        PROTOS sample1.proto
#        PLUGIN protoc-gen-grpc=$<TARGET_FILE:gRPC::grpc_cpp_plugin>
#        PROTOC_OPTIONS --grpc_out=${CMAKE_CURRENT_BINARY_DIR}
)

