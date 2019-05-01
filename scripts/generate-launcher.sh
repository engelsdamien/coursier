#!/bin/bash

# grepped and updated during releases
VERSION=1.1.0-SNAPSHOT
ACTUAL_VERSION="${ACTUAL_VERSION:-"$VERSION"}"
CACHE_VERSION=v1

OUTPUT="${OUTPUT:-"coursier.jar"}"

SBTPACK_LAUNCHER="$(dirname "$0")/../cli/target/pack/bin/coursier"

if [ ! -f "$SBTPACK_LAUNCHER" ]; then
  sbt ++2.12.4 "project cli" pack
fi

"$SBTPACK_LAUNCHER" bootstrap \
  "io.get-coursier::coursier-cli:$ACTUAL_VERSION" \
  "io.get-coursier::coursier-okhttp:$ACTUAL_VERSION" \
  --assembly --preamble=false \
  -f -o "$OUTPUT" \
  "$@"
