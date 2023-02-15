FROM debian:bullseye

ARG DEBIAN_FRONTEND=noninteractive

RUN apt-get update -y && \
    apt-get install -y \
    bash curl wget gnupg2 git python3 \
    openjdk-11-jdk-headless

RUN mkdir -p /work/src
ADD [ ".", "/work/src" ]

WORKDIR /work/src/java
RUN chmod +x ./gradlew && \
    ./gradlew build