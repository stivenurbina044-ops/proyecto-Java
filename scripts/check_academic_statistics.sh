#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
statistics_check_dir=$(mktemp -d /tmp/little-horizons-statistics-XXXXXX)
trap 'rm -rf "$statistics_check_dir"' EXIT
mkdir "$statistics_check_dir/classes"
find src/main/java -name '*.java' -print0 | xargs -0 javac --release 17 -d "$statistics_check_dir/classes"
javac --release 17 -cp "$statistics_check_dir/classes" -d "$statistics_check_dir/classes" \
    src/test/java/com/littlehorizons/erp/AcademicStatisticsCheck.java
java -cp "$statistics_check_dir/classes" com.littlehorizons.erp.AcademicStatisticsCheck
