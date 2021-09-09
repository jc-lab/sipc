if (NOT TARGET uvw-static)
    if (NOT UVW_GIT_TAG)
        set(UVW_GIT_TAG 58b299ee60d62386a2339dab3f99d30570b33085) # v2.9.0_libuv_v1.41
    endif ()

    set(FETCH_LIBUV OFF)
    set(BUILD_UVW_LIBS ON)
    set(BUILD_TESTING OFF)

    FetchContent_Declare(
            uvw
            GIT_REPOSITORY https://github.com/skypjack/uvw.git
            GIT_TAG ${UVW_GIT_TAG}
    )
    FetchContent_GetProperties(uvw)
    if (NOT uvw_POPULATED)
        FetchContent_Populate(uvw)
    endif ()
    add_subdirectory(${uvw_SOURCE_DIR} third_party/uvw)
endif()
