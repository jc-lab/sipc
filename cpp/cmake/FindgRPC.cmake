include(${CMAKE_CURRENT_LIST_DIR}/common.cmake)

set(gRPC_FOUND TRUE)

if (JCU_SIPC_SUB_PROJECT)
    include(${THIRDPARTIES_OUTPUT_DIR}/lib/cmake/grpc/gRPCConfig.cmake)
endif()
