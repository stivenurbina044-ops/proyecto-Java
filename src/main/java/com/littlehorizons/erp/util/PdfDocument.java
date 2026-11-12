package com.littlehorizons.erp.util;

import java.io.ByteArrayOutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Minimal paginated PDF writer using standard Courier fonts and WinAnsi encoding. */
public final class PdfDocument {
    private static final Charset TEXT_ENCODING = Charset.forName("windows-1252");
    private final List<StringBuilder> pages = new ArrayList<>();
    private float y;

    public PdfDocument() {
        newPage();
    }

    private void newPage() {
        pages.add(new StringBuilder());
        y = 792;
    }

    public void ensureSpace(float height) {
        if (y - height < 48) {
            newPage();
        }
    }

    public float cursor() { return y; }

    public void gap(float height) {
        ensureSpace(height);
        y -= height;
    }

    public void line(String text, int size, boolean bold) {
        String clean = cleanText(text);
        int width = Math.max(1, (int) (499 / (size * 0.6f)));
        while (clean.length() > width) {
            int cut = clean.lastIndexOf(' ', width);
            if (cut < width / 3) {
                cut = width;
            }
            writeLine(clean.substring(0, cut), size, bold);
            clean = clean.substring(cut).stripLeading();
        }
        writeLine(clean, size, bold);
    }

    private void writeLine(String text, int size, boolean bold) {
        ensureSpace(size + 5);
        page().append(String.format(Locale.ROOT,
                "0.08 0.15 0.25 rg BT /%s %d Tf 1 0 0 1 48 %.2f Tm (%s) Tj ET\n",
                bold ? "F2" : "F1", size, y, escape(text)));
        y -= size + 5;
    }

    public void rule() {
        ensureSpace(12);
        page().append(String.format(Locale.ROOT, "0.1 0.45 0.55 RG 1 w 48 %.2f m 547 %.2f l S\n", y, y));
        y -= 12;
    }

    public void blackSquare(float x, float top, float side) {
        page().append(String.format(Locale.ROOT, "0 0 0 rg %.3f %.3f %.3f %.3f re f\n", x, top - side, side, side));
    }

    private StringBuilder page() { return pages.get(pages.size() - 1); }

    public byte[] toBytes() {
        List<byte[]> objects = new ArrayList<>();
        objects.add(ascii("<< /Type /Catalog /Pages 2 0 R >>"));
        StringBuilder kids = new StringBuilder();
        for (int index = 0; index < pages.size(); index++) {
            kids.append(5 + index * 2).append(" 0 R ");
        }
        objects.add(ascii("<< /Type /Pages /Count " + pages.size() + " /Kids [" + kids + "] >>"));
        objects.add(ascii("<< /Type /Font /Subtype /Type1 /BaseFont /Courier /Encoding /WinAnsiEncoding >>"));
        objects.add(ascii("<< /Type /Font /Subtype /Type1 /BaseFont /Courier-Bold /Encoding /WinAnsiEncoding >>"));
        for (int index = 0; index < pages.size(); index++) {
            int contentId = 6 + index * 2;
            objects.add(ascii("<< /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] "
                    + "/Resources << /Font << /F1 3 0 R /F2 4 0 R >> >> /Contents " + contentId + " 0 R >>"));
            byte[] content = pages.get(index).toString().getBytes(TEXT_ENCODING);
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            stream.writeBytes(ascii("<< /Length " + content.length + " >>\nstream\n"));
            stream.writeBytes(content);
            stream.writeBytes(ascii("\nendstream"));
            objects.add(stream.toByteArray());
        }
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        output.writeBytes(ascii("%PDF-1.4\n% Little Horizons academic simulation\n"));
        List<Integer> offsets = new ArrayList<>();
        for (int index = 0; index < objects.size(); index++) {
            offsets.add(output.size());
            output.writeBytes(ascii((index + 1) + " 0 obj\n"));
            output.writeBytes(objects.get(index));
            output.writeBytes(ascii("\nendobj\n"));
        }
        int xref = output.size();
        output.writeBytes(ascii("xref\n0 " + (objects.size() + 1) + "\n0000000000 65535 f \n"));
        for (int offset : offsets) {
            output.writeBytes(ascii(String.format(Locale.ROOT, "%010d 00000 n \n", offset)));
        }
        output.writeBytes(ascii("trailer\n<< /Size " + (objects.size() + 1)
                + " /Root 1 0 R >>\nstartxref\n" + xref + "\n%%EOF\n"));
        return output.toByteArray();
    }

    private static byte[] ascii(String value) { return value.getBytes(StandardCharsets.US_ASCII); }

    private static String cleanText(String value) {
        if (value == null || value.isBlank()) {
            return "Sin registrar";
        }
        String clean = value.replaceAll("[\\p{Cntrl}]", " ").strip();
        return new String(clean.getBytes(TEXT_ENCODING), TEXT_ENCODING);
    }

    private static String escape(String value) {
        return value.replace("\\", "\\\\").replace("(", "\\(").replace(")", "\\)");
    }
}
