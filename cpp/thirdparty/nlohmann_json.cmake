if (NOT TARGET nlohmann_json)
    message("Can't find nlohmann_json. Fetch content.")

    set(JSON_BuildTests OFF CACHE BOOL "JSON_BuildTests" FORCE)

    FetchContent_Declare(
            nlohmann_json
            GIT_REPOSITORY https://github.com/nlohmann/json.git
            GIT_TAG db78ac1d7716f56fc9f1b030b715f872f93964e4 # v3.9.1
    )
    FetchContent_GetProperties(nlohmann_json)
    if (NOT nlohmann_json_POPULATED)
        FetchContent_Populate(nlohmann_json)
        add_subdirectory(${nlohmann_json_SOURCE_DIR} ${nlohmann_json_BINARY_DIR})
    endif ()
endif()
