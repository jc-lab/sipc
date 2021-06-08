find_package(Protobuf REQUIRED)
#find_package(gRPC REQUIRED)

set(project_prefix jcu_sipc_test_)

add_executable("${project_prefix}sample-app" sample.cc)

if (MSVC)
    target_link_options("${project_prefix}sample-app" PRIVATE "/DEBUG:FASTLINK")
endif()

target_link_libraries("${project_prefix}sample-app"
        PRIVATE
        jcu_sipc
        )
target_include_directories("${project_prefix}sample-app"
        PRIVATE
        ${CMAKE_CURRENT_BINARY_DIR}
        )
protobuf_generate_ex(
        LANGUAGE cpp
        TARGET "${project_prefix}sample-app"
        IMPORT_DIRS ${CMAKE_CURRENT_SOURCE_DIR}/../../sample-proto
        PROTOS sample1.proto
#        PLUGIN protoc-gen-grpc=$<TARGET_FILE:gRPC::grpc_cpp_plugin>
#        PROTOC_OPTIONS --grpc_out=${CMAKE_CURRENT_BINARY_DIR}
)

