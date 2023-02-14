#!/bin/bash

mkdir -p ./sipc_proto
protoc --proto_path=../java/core/src/main/proto --go_out=./sipc_proto --go_opt=paths=source_relative sipc.proto
