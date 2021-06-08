ExternalProject_Add(
        protobuf_project

        GIT_REPOSITORY https://github.com/protocolbuffers/protobuf.git
        GIT_TAG 70db61a91bae270dca5db2f9837deea11118b148 # v3.17.2

        SOURCE_SUBDIR "cmake"

        CMAKE_ARGS
        "-DCMAKE_MODULE_PATH=${CMAKE_MODULE_PATH}"
        "-DTHIRDPARTIES_OUTPUT_DIR=${THIRDPARTIES_OUTPUT_DIR}"
        "-DCMAKE_INSTALL_PREFIX=${THIRDPARTIES_OUTPUT_DIR}"
        "-Dprotobuf_BUILD_SHARED_LIBS=OFF"
        "-Dprotobuf_WITH_ZLIB=OFF"
        "-Dprotobuf_BUILD_TESTS=OFF"
        "-Dprotobuf_MSVC_STATIC_RUNTIME=${JCU_SIPC_MSVC_STATIC_RUNTIME}"

        UPDATE_COMMAND ""
        PATCH_COMMAND ""
        TEST_COMMAND ""
)

function(add_protobuf_library name)
    add_library(${name} STATIC IMPORTED GLOBAL)
    set_target_properties(
            ${name} PROPERTIES
            IMPORTED_LOCATION_DEBUG ${THIRDPARTIES_OUTPUT_DIR}/lib/${CMAKE_STATIC_LIBRARY_PREFIX}${name}d${CMAKE_STATIC_LIBRARY_SUFFIX}
            IMPORTED_LOCATION_RELEASE ${THIRDPARTIES_OUTPUT_DIR}/lib/${CMAKE_STATIC_LIBRARY_PREFIX}${name}${CMAKE_STATIC_LIBRARY_SUFFIX}
            IMPORTED_LOCATION_RELWITHDEBINFO ${THIRDPARTIES_OUTPUT_DIR}/lib/${CMAKE_STATIC_LIBRARY_PREFIX}${name}${CMAKE_STATIC_LIBRARY_SUFFIX}
            INTERFACE_INCLUDE_DIRECTORIES ${THIRDPARTIES_OUTPUT_DIR}/include
    )
    add_dependencies(${name} protobuf_project)
endfunction()

add_protobuf_library(libprotobuf)
