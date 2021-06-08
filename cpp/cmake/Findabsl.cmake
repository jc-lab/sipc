include(${CMAKE_CURRENT_LIST_DIR}/common.cmake)

set(absl_FOUND TRUE)

if (JCU_SIPC_SUB_PROJECT)
    include(${THIRDPARTIES_OUTPUT_DIR}/lib/cmake/absl/abslConfig.cmake)
else()
    include(${CMAKE_CURRENT_LIST_DIR}/thirdparty/abslConfig.cmake)
endif()
