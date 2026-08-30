#!/usr/bin/env sh
set -eu

OUT="${TMPDIR:-/tmp}/creator-digest-test-classes"
mkdir -p "$OUT"
javac -d "$OUT" $(find src/main/java src/test/java -name '*.java')
java -cp "$OUT" education.digest.WeeklyDigestPlanTest
