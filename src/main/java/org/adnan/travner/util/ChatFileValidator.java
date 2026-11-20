package org.adnan.travner.util;

import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Validation for chat attachments. The file's magic bytes are the source of
 * truth: the client-declared MIME type and extension must agree with them.
 * This prevents a renamed executable or script from being served as a document.
 */
public final class ChatFileValidator {

    public static final long MAX_ATTACHMENT_SIZE = 10L * 1024 * 1024;

    private static final Map<Kind, String> CANONICAL_TYPES = Map.of(
            Kind.JPEG, "image/jpeg",
            Kind.PNG, "image/png",
            Kind.GIF, "image/gif",
            Kind.WEBP, "image/webp",
            Kind.BMP, "image/bmp",
            Kind.PDF, "application/pdf",
            Kind.DOC, "application/msword",
            Kind.DOCX, "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            Kind.TXT, "text/plain");

    private static final Map<Kind, Set<String>> ALLOWED_EXTENSIONS = Map.of(
            Kind.JPEG, Set.of(".jpg", ".jpeg"),
            Kind.PNG, Set.of(".png"),
            Kind.GIF, Set.of(".gif"),
            Kind.WEBP, Set.of(".webp"),
            Kind.BMP, Set.of(".bmp"),
            Kind.PDF, Set.of(".pdf"),
            Kind.DOC, Set.of(".doc"),
            Kind.DOCX, Set.of(".docx"),
            Kind.TXT, Set.of(".txt", ".text", ".md", ".csv", ""));

    private static final List<String> DANGEROUS_EXTENSIONS = List.of(
            ".exe", ".bat", ".cmd", ".com", ".pif", ".scr", ".vbs", ".js", ".jar",
            ".zip", ".rar", ".7z", ".tar", ".php", ".asp", ".jsp", ".sh", ".svg", ".html", ".htm");

    private ChatFileValidator() {
    }

    /** Validates the file and returns its bytes, which the caller stores in GridFS. */
    public static byte[] validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File cannot be empty");
        }
        if (file.getSize() > MAX_ATTACHMENT_SIZE) {
            throw new IllegalArgumentException("File size must be less than 10MB");
        }

        byte[] bytes;
        try (InputStream stream = file.getInputStream()) {
            bytes = readUpTo(stream, MAX_ATTACHMENT_SIZE + 1);
        } catch (IOException e) {
            throw new IllegalArgumentException("Unable to read uploaded file", e);
        }
        if (bytes.length == 0 || bytes.length > MAX_ATTACHMENT_SIZE) {
            throw new IllegalArgumentException("File size must be less than 10MB");
        }

        String originalFilename = file.getOriginalFilename();
        String extension = extensionOf(originalFilename);
        if (DANGEROUS_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException("File type not allowed: " + extension);
        }

        Kind kind = detectKind(bytes);
        if (kind == null) {
            throw new IllegalArgumentException("Unsupported or unrecognised file content");
        }
        if (!ALLOWED_EXTENSIONS.get(kind).contains(extension)) {
            throw new IllegalArgumentException("File extension does not match its content");
        }

        String declaredType = file.getContentType();
        if (declaredType != null && !declaredType.isBlank()
                && !"application/octet-stream".equalsIgnoreCase(declaredType)
                && !declaredType.equalsIgnoreCase(CANONICAL_TYPES.get(kind))) {
            throw new IllegalArgumentException("File content type does not match its content");
        }
        return bytes;
    }

    /** Canonical MIME type for already-validated content. */
    public static String contentTypeOf(byte[] bytes) {
        return CANONICAL_TYPES.get(detectKind(bytes));
    }

    /**
     * Strips any path components and unsafe characters. Dotless and very long
     * names are handled without substring(-1) failures.
     */
    public static String sanitizeFilename(String name) {
        if (name == null || name.isBlank()) {
            return "file";
        }
        String base = name.replace('\\', '/');
        base = base.substring(base.lastIndexOf('/') + 1);
        base = base.replaceAll("[^a-zA-Z0-9._-]", "_");
        if (base.isEmpty() || ".".equals(base) || "..".equals(base)) {
            return "file";
        }
        int dot = base.lastIndexOf('.');
        String extension = dot > 0 ? base.substring(dot) : "";
        String stem = dot > 0 ? base.substring(0, dot) : base;
        if (stem.length() > 80) {
            stem = stem.substring(0, 80);
        }
        return stem + extension;
    }

    private static String extensionOf(String filename) {
        if (filename == null) {
            return "";
        }
        String base = filename.replace('\\', '/');
        base = base.substring(base.lastIndexOf('/') + 1);
        int dot = base.lastIndexOf('.');
        return dot > 0 ? base.substring(dot).toLowerCase() : "";
    }

    private static byte[] readUpTo(InputStream stream, long limit) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int read;
        while (out.size() < limit && (read = stream.read(buffer)) != -1) {
            out.write(buffer, 0, (int) Math.min(read, limit - out.size()));
        }
        return out.toByteArray();
    }

    private enum Kind { JPEG, PNG, GIF, WEBP, BMP, PDF, DOC, DOCX, TXT }

    private static Kind detectKind(byte[] b) {
        if (startsWith(b, 0xFF, 0xD8, 0xFF)) {
            return Kind.JPEG;
        }
        if (startsWith(b, 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)) {
            return Kind.PNG;
        }
        if (asciiAt(b, 0, "GIF87a") || asciiAt(b, 0, "GIF89a")) {
            return Kind.GIF;
        }
        if (asciiAt(b, 0, "RIFF") && asciiAt(b, 8, "WEBP")) {
            return Kind.WEBP;
        }
        if (b.length >= 2 && b[0] == 'B' && b[1] == 'M') {
            return Kind.BMP;
        }
        if (asciiAt(b, 0, "%PDF-")) {
            return Kind.PDF;
        }
        if (startsWith(b, 0xD0, 0xCF, 0x11, 0xE0, 0xA1, 0xB1, 0x1A, 0xE1)) {
            return Kind.DOC;
        }
        if (startsWith(b, 0x50, 0x4B, 0x03, 0x04)) {
            return Kind.DOCX;
        }
        if (isPlainText(b)) {
            return Kind.TXT;
        }
        return null;
    }

    private static boolean isPlainText(byte[] b) {
        int sample = Math.min(b.length, 512);
        for (int i = 0; i < sample; i++) {
            int value = b[i] & 0xFF;
            if (value == 0 || (value < 0x09) || (value > 0x0D && value < 0x20 && value != 0x1B)) {
                return false;
            }
        }
        String text = new String(Arrays.copyOf(b, sample), StandardCharsets.UTF_8);
        return !text.contains("\uFFFD");
    }

    private static boolean startsWith(byte[] b, int... signature) {
        if (b.length < signature.length) {
            return false;
        }
        for (int i = 0; i < signature.length; i++) {
            if ((b[i] & 0xFF) != signature[i]) {
                return false;
            }
        }
        return true;
    }

    private static boolean asciiAt(byte[] b, int offset, String value) {
        if (b.length < offset + value.length()) {
            return false;
        }
        for (int i = 0; i < value.length(); i++) {
            if (b[offset + i] != (byte) value.charAt(i)) {
                return false;
            }
        }
        return true;
    }
}
