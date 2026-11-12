# Third-party QR generator

The Java QR encoder in `src/main/java/io/nayuki/qrcodegen/` is from
[Project Nayuki's QR Code generator library](https://www.nayuki.io/page/qr-code-generator-library),
under the MIT License. Copyright and the complete license notice are preserved
in each source file. These files are third-party implementation code, separate
from the ERP's MVC packages. No Maven download or Python process is needed at runtime.

Sources retrieved on 2026-11-11:

- [QrCode.java](https://github.com/nayuki/QR-Code-generator/blob/master/java/src/main/java/io/nayuki/qrcodegen/QrCode.java): Git blob `f0bafbbca19f265ce3572c33b59b5d6862a76bb4`.
- [QrSegment.java](https://github.com/nayuki/QR-Code-generator/blob/master/java/src/main/java/io/nayuki/qrcodegen/QrSegment.java): Git blob `3a8b5345647086f2f1365cdcfa2f4c1d2e8df36b`.
- [BitBuffer.java](https://github.com/nayuki/QR-Code-generator/blob/master/java/src/main/java/io/nayuki/qrcodegen/BitBuffer.java): Git blob `624d129e73d5efa37194d21e0c420c75b96dba8b`.
- [DataTooLongException.java](https://github.com/nayuki/QR-Code-generator/blob/master/java/src/main/java/io/nayuki/qrcodegen/DataTooLongException.java): Git blob `f1453effb23f412d1aad81f3cf241ddb0ec3ab03`.

Only trailing whitespace was normalized; the QR implementation was not modified.
The application uses `QrCode.encodeText` and draws the QR modules directly in the PDF.
The QR encodes academic invoice data; it does not represent validation by the DIAN.
