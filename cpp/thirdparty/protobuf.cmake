set(_JCU_SIPC_PROTOBUF_LIBRARY_NAME "libprotobuf")

if (protobuf_BUILD_SHARED_LIBS)
    set(protobuf_BUILD_SHARED_LIBS OFF CACHE BOOL "Build protobuf shared libs")
endif()
if (protobuf_MSVC_STATIC_RUNTIME)
    set(protobuf_MSVC_STATIC_RUNTIME ${JCU_SIPC_MSVC_STATIC_RUNTIME} CACHE BOOL STRING "Build protobuf msvc static runtime")
endif()
if (NOT JCU_SIPC_PROTOBUF_PROVIDER)
    set(JCU_SIPC_PROTOBUF_PROVIDER "module")
endif()

# Copyright 2017 gRPC authors.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

if(JCU_SIPC_PROTOBUF_PROVIDER STREQUAL "inherit")
    if(TARGET protobuf::${_JCU_SIPC_PROTOBUF_LIBRARY_NAME})
        set(_JCU_SIPC_PROTOBUF_LIBRARIES protobuf::${_JCU_SIPC_PROTOBUF_LIBRARY_NAME})
    elseif(TARGET ${_JCU_SIPC_PROTOBUF_LIBRARY_NAME})
        set(_JCU_SIPC_PROTOBUF_LIBRARIES ${_JCU_SIPC_PROTOBUF_LIBRARY_NAME})
    endif()
    if(TARGET protobuf::libprotoc)
        set(_JCU_SIPC_PROTOBUF_PROTOC_LIBRARIES protobuf::libprotoc)
    elseif(TARGET libprotoc)
        set(_JCU_SIPC_PROTOBUF_PROTOC_LIBRARIES libprotoc)
    endif()
    if(TARGET protobuf::protoc)
        set(_JCU_SIPC_PROTOBUF_PROTOC protobuf::protoc)
        if(CMAKE_CROSSCOMPILING)
            find_program(_JCU_SIPC_PROTOBUF_PROTOC_EXECUTABLE protobuf::protoc)
        else()
            set(_JCU_SIPC_PROTOBUF_PROTOC_EXECUTABLE $<TARGET_FILE:protobuf::protoc>)
        endif()
    elseif(TARGET protoc)
        set(_JCU_SIPC_PROTOBUF_PROTOC protoc)
        if(CMAKE_CROSSCOMPILING)
            find_program(_JCU_SIPC_PROTOBUF_PROTOC_EXECUTABLE protoc)
        else()
            set(_JCU_SIPC_PROTOBUF_PROTOC_EXECUTABLE $<TARGET_FILE:protoc>)
        endif()
    endif()
    # For well-known .proto files distributed with protobuf
    if (NOT _JCU_SIPC_PROTOBUF_IMPORT_DIRS)
        set(_JCU_SIPC_PROTOBUF_IMPORT_DIRS "${PROTOBUF_ROOT_DIR}/src")
    endif()
elseif(JCU_SIPC_PROTOBUF_PROVIDER STREQUAL "module")
    # Building the protobuf tests require gmock what is not part of a standard protobuf checkout.
    # Disable them unless they are explicitly requested from the cmake command line (when we assume
    # gmock is downloaded to the right location inside protobuf).
    if(NOT protobuf_BUILD_TESTS)
        set(protobuf_BUILD_TESTS OFF CACHE BOOL "Build protobuf tests")
    endif()
    # Disable building protobuf with zlib. Building protobuf with zlib breaks
    # the build if zlib is not installed on the system.
    if(NOT protobuf_WITH_ZLIB)
        set(protobuf_WITH_ZLIB OFF CACHE BOOL "Build protobuf with zlib.")
    endif()
    if(NOT PROTOBUF_ROOT_DIR)
        FetchContent_Declare(
                protobuf
                GIT_REPOSITORY https://github.com/protocolbuffers/protobuf.git
                GIT_TAG 70db61a91bae270dca5db2f9837deea11118b148 # v3.17.2
        )
        FetchContent_GetProperties(protobuf)
        if (NOT protobuf_POPULATED)
            FetchContent_Populate(protobuf)
        endif ()
        set(PROTOBUF_ROOT_DIR ${protobuf_SOURCE_DIR})
    endif()

    if(EXISTS "${PROTOBUF_ROOT_DIR}/cmake/CMakeLists.txt")
        set(protobuf_MSVC_STATIC_RUNTIME OFF CACHE BOOL "Link static runtime libraries")
        add_subdirectory(${PROTOBUF_ROOT_DIR}/cmake third_party/protobuf)
        if(TARGET ${_JCU_SIPC_PROTOBUF_LIBRARY_NAME})
            set(_JCU_SIPC_PROTOBUF_LIBRARIES ${_JCU_SIPC_PROTOBUF_LIBRARY_NAME})
        endif()
        if(TARGET libprotoc)
            set(_JCU_SIPC_PROTOBUF_PROTOC_LIBRARIES libprotoc)
        endif()
        if(TARGET protoc)
            set(_JCU_SIPC_PROTOBUF_PROTOC protoc)
            if(CMAKE_CROSSCOMPILING)
                find_program(_JCU_SIPC_PROTOBUF_PROTOC_EXECUTABLE protoc)
            else()
                set(_JCU_SIPC_PROTOBUF_PROTOC_EXECUTABLE $<TARGET_FILE:protoc>)
            endif()
        endif()
        # For well-known .proto files distributed with protobuf
        set(_JCU_SIPC_PROTOBUF_WELLKNOWN_INCLUDE_DIR "${PROTOBUF_ROOT_DIR}/src")
        set(_JCU_SIPC_PROTOBUF_IMPORT_DIRS "${PROTOBUF_ROOT_DIR}/src")
    else()
        message(WARNING "JCU_SIPC_PROTOBUF_PROVIDER is \"module\" but PROTOBUF_ROOT_DIR is wrong")
    endif()
    if(JCU_SIPC_INSTALL AND NOT _JCU_SIPC_INSTALL_SUPPORTED_FROM_MODULE)
        message(WARNING "JCU_SIPC_INSTALL will be forced to FALSE because JCU_SIPC_PROTOBUF_PROVIDER is \"module\" and CMake version (${CMAKE_VERSION}) is less than 3.13.")
        set(JCU_SIPC_INSTALL FALSE)
    endif()
elseif(JCU_SIPC_PROTOBUF_PROVIDER STREQUAL "package")
    if (NOT Protobuf_FOUND AND NOT PROTOBUF_FOUND)
        find_package(Protobuf REQUIRED ${JCU_SIPC_PROTOBUF_PACKAGE_TYPE})
    endif()

    # {Protobuf,PROTOBUF}_FOUND is defined based on find_package type ("MODULE" vs "CONFIG").
    # For "MODULE", the case has also changed between cmake 3.5 and 3.6.
    # We use the legacy uppercase version for *_LIBRARIES AND *_INCLUDE_DIRS variables
    # as newer cmake versions provide them too for backward compatibility.
    if(Protobuf_FOUND OR PROTOBUF_FOUND)
        set(_JCU_SIPC_PROTOBUF_IMPORT_DIRS ${Protobuf_IMPORT_DIRS})

        if(TARGET protobuf::${_JCU_SIPC_PROTOBUF_LIBRARY_NAME})
            set(_JCU_SIPC_PROTOBUF_LIBRARIES protobuf::${_JCU_SIPC_PROTOBUF_LIBRARY_NAME})
        else()
            set(_JCU_SIPC_PROTOBUF_LIBRARIES ${PROTOBUF_LIBRARIES})
        endif()
        if(TARGET protobuf::libprotoc)
            set(_JCU_SIPC_PROTOBUF_PROTOC_LIBRARIES protobuf::libprotoc)
            # extract the include dir from target's properties
            get_target_property(_JCU_SIPC_PROTOBUF_WELLKNOWN_INCLUDE_DIR protobuf::libprotoc INTERFACE_INCLUDE_DIRECTORIES)
        else()
            set(_JCU_SIPC_PROTOBUF_PROTOC_LIBRARIES ${PROTOBUF_PROTOC_LIBRARIES})
            set(_JCU_SIPC_PROTOBUF_WELLKNOWN_INCLUDE_DIR ${PROTOBUF_INCLUDE_DIRS})
        endif()
        if(TARGET protobuf::protoc)
            set(_JCU_SIPC_PROTOBUF_PROTOC protobuf::protoc)
            if(CMAKE_CROSSCOMPILING)
                find_program(_JCU_SIPC_PROTOBUF_PROTOC_EXECUTABLE protoc)
            else()
                set(_JCU_SIPC_PROTOBUF_PROTOC_EXECUTABLE $<TARGET_FILE:protobuf::protoc>)
            endif()
        else()
            set(_JCU_SIPC_PROTOBUF_PROTOC ${PROTOBUF_PROTOC_EXECUTABLE})
            if(CMAKE_CROSSCOMPILING)
                find_program(_JCU_SIPC_PROTOBUF_PROTOC_EXECUTABLE protoc)
            else()
                set(_JCU_SIPC_PROTOBUF_PROTOC_EXECUTABLE ${PROTOBUF_PROTOC_EXECUTABLE})
            endif()
        endif()
        set(_JCU_SIPC_FIND_PROTOBUF "if(NOT Protobuf_FOUND AND NOT PROTOBUF_FOUND)\n  find_package(Protobuf ${JCU_SIPC_PROTOBUF_PACKAGE_TYPE})\nendif()")
    endif()
endif()
