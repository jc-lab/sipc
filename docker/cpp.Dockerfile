FROM ubuntu:20.04

ARG DEBIAN_FRONTEND=noninteractive

RUN apt-get update -y && \
    apt-get install -y \
    bash curl wget gnupg2 \
    build-essential git cmake python3

RUN mkdir -p /work/src
ADD [ ".", "/work/src" ]

RUN ls -al /work/src && \
    mkdir -p /work/build-cpp && \
    cd /work/build-cpp && \
    cmake /work/src/cpp

WORKDIR /work/build-cpp
RUN cmake --build . --config RelWithDebInfo

