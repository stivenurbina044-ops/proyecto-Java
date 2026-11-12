#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
invoice_check_dir=$(mktemp -d /tmp/little-horizons-invoices-XXXXXX)
trap 'rm -rf "$invoice_check_dir"' EXIT
mkdir "$invoice_check_dir/classes"
find src/main/java -name '*.java' -print0 | xargs -0 javac --release 17 -d "$invoice_check_dir/classes"
cp src/main/resources/school.properties "$invoice_check_dir/classes/"
javac --release 17 -cp "$invoice_check_dir/classes" -d "$invoice_check_dir/classes" \
    src/test/java/com/littlehorizons/erp/InvoiceGenerationCheck.java
printf 'invoice.outputDirectory=%s/invoices\n' "$invoice_check_dir" > "$invoice_check_dir/test.properties"
DB_URL=jdbc:invoice-check DB_USER=test DB_PASSWORD=test INVOICE_CONFIG_PATH="$invoice_check_dir/test.properties" \
    java -cp "$invoice_check_dir/classes" com.littlehorizons.erp.InvoiceGenerationCheck "$invoice_check_dir"
if command -v pdftotext >/dev/null 2>&1; then
    pdftotext "$invoice_check_dir/preview.pdf" "$invoice_check_dir/preview.txt"
    python3 - "$invoice_check_dir/preview.txt" <<'PY'
import sys
from pathlib import Path
text = Path(sys.argv[1]).read_text()
for expected in ['LH-000123', '350000.50', 'Mariana López Torres', 'Lucía Torres Gómez', 'CUFE simulado']:
    assert expected in text, expected
print('PDF text and Spanish accents verified.')
PY
fi
