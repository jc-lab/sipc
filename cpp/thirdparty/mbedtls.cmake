set(MBEDTLS_COMMON_FLAGS)

if(MSVC)
    set(MBEDTLS_COMMON_FLAGS "${MBEDTLS_COMMON_FLAGS} /utf-8")
endif ()

if (NOT TARGET mbedcrypto OR NOT TARGET mbedtls OR NOT TARGET mbedx509)
    set(TEMP_MBEDTLS_PATCH_COMMAND "")
    if (NOT WIN32)
        set(TEMP_MBEDTLS_PATCH_COMMAND patch -p1 < ${CMAKE_CURRENT_LIST_DIR}/mbedtls-fix-test-suite-for-alpine.patch)
    endif()

    message("Can't find mbedtls. Add as an external project.")
    ExternalProject_Add(
            mbedtls_project

            GIT_REPOSITORY https://github.com/ARMmbed/mbedtls.git
            GIT_TAG e483a77c85e1f9c1dd2eb1c5a8f552d2617fe400 # v2.26.0

            CMAKE_ARGS
            ${SUBPROJECTS_COMMON_CMAKE_ARGS}
            "-DCMAKE_C_FLAGS=${MBEDTLS_COMMON_FLAGS}"
            "-DCMAKE_C_FLAGS_DEBUG=${PROJECT_COMMON_FLAGS_DEBUG} ${MBEDTLS_COMMON_FLAGS}"
            "-DCMAKE_C_FLAGS_RELEASE=${PROJECT_COMMON_FLAGS_RELEASE} ${MBEDTLS_COMMON_FLAGS}"
            "-DCMAKE_C_FLAGS_RELWITHDEBINFO=${PROJECT_COMMON_FLAGS_RELEASE} ${MBEDTLS_COMMON_FLAGS}"
            "-DCMAKE_CXX_FLAGS=${MBEDTLS_COMMON_FLAGS}"
            "-DCMAKE_CXX_FLAGS_DEBUG=${PROJECT_COMMON_FLAGS_DEBUG} ${MBEDTLS_COMMON_FLAGS}"
            "-DCMAKE_CXX_FLAGS_RELEASE=${PROJECT_COMMON_FLAGS_RELEASE} ${MBEDTLS_COMMON_FLAGS}"
            "-DCMAKE_CXX_FLAGS_RELWITHDEBINFO=${PROJECT_COMMON_FLAGS_RELEASE} ${MBEDTLS_COMMON_FLAGS}"
            "-DENABLE_PROGRAMS=OFF"

            UPDATE_COMMAND ""
            PATCH_COMMAND ${TEMP_MBEDTLS_PATCH_COMMAND}
            TEST_COMMAND ""
    )

    function(add_mbedtls_library name)
        add_library(${name} STATIC IMPORTED GLOBAL)
        target_include_directories(
                ${name}
                INTERFACE
                ${THIRDPARTIES_OUTPUT_DIR}/include
        )
        set_target_properties(
                ${name} PROPERTIES
                IMPORTED_LOCATION ${THIRDPARTIES_OUTPUT_DIR}/lib/${CMAKE_STATIC_LIBRARY_PREFIX}${name}${CMAKE_STATIC_LIBRARY_SUFFIX}
        )
        add_dependencies(${name} mbedtls_project)
    endfunction()

    add_mbedtls_library(mbedcrypto)
    add_mbedtls_library(mbedx509)
    add_mbedtls_library(mbedtls)
endif()
