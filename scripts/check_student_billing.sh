#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
billing_check_dir=$(mktemp -d /tmp/little-horizons-billing-XXXXXX)
trap 'rm -rf "$billing_check_dir"' EXIT
mkdir "$billing_check_dir/classes"
find src/main/java -name '*.java' -print0 | xargs -0 javac --release 17 -d "$billing_check_dir/classes"
javac --release 17 -cp "$billing_check_dir/classes" -d "$billing_check_dir/classes" \
    src/test/java/com/littlehorizons/erp/StudentBillingCheck.java
DB_URL=jdbc:billing-check DB_USER=test DB_PASSWORD=test \
    java -cp "$billing_check_dir/classes" com.littlehorizons.erp.StudentBillingCheck
