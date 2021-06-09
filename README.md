# sipc

Secure IPC

* Encrypted communication through ECDH by passing Host's PublicKey through Environment Variable or Cmdline.

* It works with asynchronous IO through libuv, and uvw is used for C++.

# Transport methods

## STDIO

**Working in progress**

* stdin / stdout

## TCP

* By default, automatically used a port that not in use.
* default hostname is 127.0.0.2.

# Usage

## Java

**Gradle:**

```
implementation 'kr.jclab.sipc:sipc-java:<LATEST_VERSION>'
```

## CMake (C++)

```
include(FetchContent)

FetchContent_Declare(
        jcu_sipc
        GIT_REPOSITORY https://github.com/jc-lab/sipc.git
        GIT_TAG <LATEST_VERSION>
)
FetchContent_GetProperties(jcu_sipc)
if (NOT jcu_sipc_POPULATED)
    FetchContent_Populate(jcu_sipc)
    add_subdirectory(${jcu_sipc_SOURCE_DIR}/cpp ${jcu_sipc_BINARY_DIR})
endif ()

add_executable(YOUR_EXECUTABLE)
target_link_libraries(YOUR_EXECUTABLE PRIVATE jcu_sipc)
```

## SAMPLE

See [docs/SAMPLE.md](docs/SAMPLE.md)

# License

Apache License 2.0
