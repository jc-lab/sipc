add_executable(sample01 temp01.cc)
target_link_libraries(sample01 PUBLIC jcu_sipc)


add_test(NAME ${PROJECT_NAME}-all COMMAND ${PROJECT_NAME})
