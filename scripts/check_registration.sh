#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
registration_check_dir=$(mktemp -d /tmp/little-horizons-registration-XXXXXX)
trap 'rm -rf "$registration_check_dir"' EXIT
find src/main/java -name '*.java' -print0 | xargs -0 javac --release 17 -d "$registration_check_dir"
javac --release 17 -cp "$registration_check_dir" -d "$registration_check_dir" src/test/java/com/littlehorizons/erp/RegistrationValidationCheck.java
java -cp "$registration_check_dir" com.littlehorizons.erp.RegistrationValidationCheck
