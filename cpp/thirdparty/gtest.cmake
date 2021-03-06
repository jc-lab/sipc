if (NOT TARGET gtest OR NOT TARGET gmock OR NOT TARGET gmock_main)
    message("Can't find googletest. Add as an external project.")
    ExternalProject_Add(
            googletest_project

            GIT_REPOSITORY https://github.com/google/googletest.git
            GIT_TAG 703bd9caab50b139428cea1aaff9974ebee5742e # release-1.10.0

            CMAKE_ARGS
            ${SUBPROJECTS_COMMON_CMAKE_ARGS}

            UPDATE_COMMAND ""
            PATCH_COMMAND ""
            TEST_COMMAND ""
    )
    function(add_gtest_library name)
        add_library(${name} STATIC IMPORTED GLOBAL)
        set_target_properties(
                ${name} PROPERTIES
                IMPORTED_LOCATION_DEBUG ${THIRDPARTIES_OUTPUT_DIR}/lib/${CMAKE_STATIC_LIBRARY_PREFIX}${name}d${CMAKE_STATIC_LIBRARY_SUFFIX}
                IMPORTED_LOCATION_RELEASE ${THIRDPARTIES_OUTPUT_DIR}/lib/${CMAKE_STATIC_LIBRARY_PREFIX}${name}${CMAKE_STATIC_LIBRARY_SUFFIX}
                IMPORTED_LOCATION_RELWITHDEBINFO ${THIRDPARTIES_OUTPUT_DIR}/lib/${CMAKE_STATIC_LIBRARY_PREFIX}${name}${CMAKE_STATIC_LIBRARY_SUFFIX}
                INTERFACE_INCLUDE_DIRECTORIES ${THIRDPARTIES_OUTPUT_DIR}/include
        )
        add_dependencies(${name} googletest_project)
    endfunction()

    if (NOT TARGET gtest)
        add_gtest_library(gtest)
    endif()
    if (NOT TARGET gmock)
        add_gtest_library(gmock)
    endif()
    if (NOT TARGET gtest_main)
        add_gtest_library(gtest_main)
    endif()
endif()
