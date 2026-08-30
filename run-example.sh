#!/usr/bin/env sh
set -eu

OUT="${TMPDIR:-/tmp}/creator-digest-classes"
mkdir -p "$OUT"
javac -d "$OUT" $(find src/main/java -name '*.java')
java -cp "$OUT" education.digest.DigestServiceExample
