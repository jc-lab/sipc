#!/bin/sh
export SOURCE_DIR="$PWD"
export NODE_PATH="$(npm root -g)"
(cd ~ && npm install -g @yarn-tool/yarnlock-parse)
node util/get-protobuf-version.js

