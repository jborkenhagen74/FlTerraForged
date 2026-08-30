#!/usr/bin/env sh
set -eu
ROOT=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
OUT="$ROOT/build/manual-compile"
rm -rf "$OUT"
mkdir -p "$OUT/api" "$OUT/common"
find "$ROOT/engine-api/src/main/java" -name '*.java' -print > "$OUT/api-sources.txt"
javac --release 17 -d "$OUT/api" @"$OUT/api-sources.txt"
find "$ROOT/common/src/main/java" -name '*.java' -print > "$OUT/common-sources.txt"
javac --release 17 -cp "$OUT/api" -d "$OUT/common" @"$OUT/common-sources.txt"
echo "Manual Java 17 compile OK"
