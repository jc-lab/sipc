ExternalProject_Add(
        grpc_project

        DEPENDS libprotobuf

        GIT_REPOSITORY https://github.com/grpc/grpc.git
        GIT_TAG 54dc182082db941aa67c7c3f93ad858c99a16d7d # v1.38.0

        SOURCE_DIR "${GRPC_SRC_DIR}"
        BUILD_COMMAND ${CMAKE_COMMAND} --build <BINARY_DIR> --config $<CONFIG> --target grpc++_unsecure

        CMAKE_ARGS
        ${SUBPROJECTS_COMMON_CMAKE_ARGS}
        "-DBUILD_SHARED_LIBS=OFF"
        "-DgRPC_PROTOBUF_PROVIDER=package"
        "-DOPENSSL_NO_ASM=ON"
        "-DgRPC_MSVC_STATIC_RUNTIME=${JCU_SIPC_MSVC_STATIC_RUNTIME}"
        "-DABSL_ENABLE_INSTALL=ON"

        UPDATE_COMMAND ""
        PATCH_COMMAND ""
        TEST_COMMAND ""
)

function(add_grpc_library name)
    if (NOT TARGET ${name})
        add_library(${name} STATIC IMPORTED GLOBAL)
        set_target_properties(
                ${name} PROPERTIES
                IMPORTED_LOCATION ${THIRDPARTIES_OUTPUT_DIR}/lib/${CMAKE_STATIC_LIBRARY_PREFIX}${name}${CMAKE_STATIC_LIBRARY_SUFFIX}
                INTERFACE_INCLUDE_DIRECTORIES ${THIRDPARTIES_OUTPUT_DIR}/include
        )
        add_dependencies(${name} grpc_project)
    endif ()
endfunction()

function(add_grpc_executable name)
    if (NOT TARGET ${name})
        add_executable(${name} IMPORTED GLOBAL)
        set_target_properties(
                ${name} PROPERTIES
                IMPORTED_LOCATION ${THIRDPARTIES_OUTPUT_DIR}/bin/${name}${CMAKE_EXECUTABLE_SUFFIX}
        )
        add_dependencies(${name} grpc_project)
    endif ()
endfunction()

#add_grpc_library(absl_bad_optional_access)
#add_grpc_library(absl_bad_variant_access)
#add_grpc_library(absl_base)
#add_grpc_library(absl_city)
#add_grpc_library(absl_civil_time)
#add_grpc_library(absl_cord)
#add_grpc_library(absl_debugging_internal)
#add_grpc_library(absl_demangle_internal)
#add_grpc_library(absl_exponential_biased)
#add_grpc_library(absl_graphcycles_internal)
#add_grpc_library(absl_hash)
#add_grpc_library(absl_hashtablez_sampler)
#add_grpc_library(absl_int128)
#add_grpc_library(absl_log_severity)
#add_grpc_library(absl_malloc_internal)
#add_grpc_library(absl_raw_hash_set)
#add_grpc_library(absl_raw_logging_internal)
#add_grpc_library(absl_spinlock_wait)
#add_grpc_library(absl_stacktrace)
#add_grpc_library(absl_status)
#add_grpc_library(absl_statusor)
#add_grpc_library(absl_strings)
#add_grpc_library(absl_strings_internal)
#add_grpc_library(absl_str_format_internal)
#add_grpc_library(absl_symbolize)
#add_grpc_library(absl_synchronization)
#add_grpc_library(absl_throw_delegate)
#add_grpc_library(absl_time)
#add_grpc_library(absl_time_zone)
#add_grpc_library(absl_wyhash)
#add_grpc_library(address_sorting)
#add_grpc_library(cares)
#add_grpc_library(gpr)
#add_grpc_library(grpc++)
#add_grpc_library(grpc++_alts)
#add_grpc_library(grpc++_error_details)
#add_grpc_library(grpc++_reflection)
#add_grpc_library(grpc++_unsecure)
#add_grpc_library(grpc)
#add_grpc_library(grpcpp_channelz)
#add_grpc_library(grpc_plugin_support)
#add_grpc_library(grpc_unsecure)
#add_grpc_library(re2)
##add_grpc_library(ssl)
#add_grpc_library(upb)
##add_grpc_library(zlibd)
##add_grpc_library(zlibstaticd)
#
#add_grpc_executable(grpc_cpp_plugin)
