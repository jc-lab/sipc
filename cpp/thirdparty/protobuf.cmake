find_package(Protobuf REQUIRED)

ExternalProject_Add(
        protobuf_project

        GIT_REPOSITORY https://github.com/protocolbuffers/protobuf.git
        GIT_TAG 70db61a91bae270dca5db2f9837deea11118b148 # v3.17.2

        SOURCE_SUBDIR "cmake"

        CONFIGURE_COMMAND ""
        BUILD_COMMAND ${CMAKE_COMMAND} -E echo "Starting $<CONFIG> build"
        COMMAND ${CMAKE_COMMAND}
        <SOURCE_DIR>
        -DBUILD_TYPE=$<CONFIG>
        -DCMAKE_BUILD_TYPE=$<CONFIG>
        -DCMAKE_INSTALL_CONFIG_NAME=$<CONFIG>
        ${SUBPROJECTS_COMMON_CMAKE_ARGS}
        "-Dprotobuf_BUILD_SHARED_LIBS=OFF"
        "-Dprotobuf_WITH_ZLIB=OFF"
        "-Dprotobuf_BUILD_TESTS=OFF"
        "-Dprotobuf_MSVC_STATIC_RUNTIME=${JCU_SIPC_MSVC_STATIC_RUNTIME}"
        WORKING_DIRECTORY <BINARY_DIR>
        COMMAND       ${CMAKE_COMMAND} --build <BINARY_DIR> --config $<CONFIG>
        COMMAND       ${CMAKE_COMMAND} -E echo "$<CONFIG> build complete"

        UPDATE_COMMAND ""
        PATCH_COMMAND ""
        TEST_COMMAND ""
)

