package org.adnan.travner.util;

import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Validation for uploaded media. The file's magic bytes are the source of
 * truth: extension and declared MIME type must agree with the detected kind.
 * Image dimensions are bounded before decoding to avoid decompression bombs.
 */
public final class FileValidationUtil {

    public static final long MAX_FILE_SIZE = 20L * 1024 * 1024;
    public static final int MAX_IMAGE_DIMENSION = 4096;

    private enum Kind { JPEG, PNG, GIF, WEBP, BMP, MP4, MOV, AVI, WMV }

    private static final Set<Kind> IMAGE_KINDS = Set.of(
            Kind.JPEG, Kind.PNG, Kind.GIF, Kind.WEBP, Kind.BMP);

    private static final Map<Kind, String> CANONICAL_TYPES = Map.of(
            Kind.JPEG, "image/jpeg",
            Kind.PNG, "image/png",
            Kind.GIF, "image/gif",
            Kind.WEBP, "image/webp",
            Kind.BMP, "image/bmp",
            Kind.MP4, "video/mp4",
            Kind.MOV, "video/quicktime",
            Kind.AVI, "video/x-msvideo",
            Kind.WMV, "video/x-ms-wmv");

    private static final Map<Kind, Set<String>> ALLOWED_EXTENSIONS = Map.of(
            Kind.JPEG, Set.of(".jpg", ".jpeg"),
            Kind.PNG, Set.of(".png"),
            Kind.GIF, Set.of(".gif"),
            Kind.WEBP, Set.of(".webp"),
            Kind.BMP, Set.of(".bmp"),
            Kind.MP4, Set.of(".mp4", ".m4v"),
            Kind.MOV, Set.of(".mov"),
            Kind.AVI, Set.of(".avi"),
            Kind.WMV, Set.of(".wmv"));

    private static final List<String> DANGEROUS_EXTENSIONS = List.of(
            ".exe", ".bat", ".cmd", ".com", ".pif", ".scr", ".vbs", ".js", ".jar",
            ".zip", ".rar", ".7z", ".tar", ".php", ".asp", ".jsp", ".sh");

    private FileValidationUtil() {
    }

    /** Validated bytes plus the canonical, detected content type. */
    public record ValidatedMedia(byte[] bytes, String contentType) {
    }

    public static ValidatedMedia validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File cannot be null or empty");
        }

        byte[] bytes;
        try (InputStream stream = file.getInputStream()) {
            bytes = readUpTo(stream, MAX_FILE_SIZE + 1);
        } catch (IOException e) {
            throw new IllegalArgumentException("Unable to read uploaded file", e);
        }
        if (bytes.length == 0 || bytes.length > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("File size exceeds maximum limit of 20MB");
        }

        String extension = extensionOf(file.getOriginalFilename());
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

        validateDimensions(bytes, kind);
        return new ValidatedMedia(bytes, CANONICAL_TYPES.get(kind));
    }

    public static boolean isImage(String mimeType) {
        return mimeType != null && CANONICAL_TYPES.containsValue(mimeType)
                && mimeType.startsWith("image/");
    }

    public static boolean isVideo(String mimeType) {
        return mimeType != null && mimeType.startsWith("video/")
                && CANONICAL_TYPES.containsValue(mimeType);
    }

    /**
     * Strips path components and unsafe characters, handling dotless and long
     * names without substring(-1) failures.
     */
    public static String sanitizeFilename(String filename) {
        if (filename == null || filename.isBlank()) {
            return "file";
        }
        String base = filename.replace('\\', '/');
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
        if (asciiAt(b, 4, "ftyp")) {
            return asciiAt(b, 8, "qt  ") ? Kind.MOV : Kind.MP4;
        }
        if (asciiAt(b, 0, "RIFF") && asciiAt(b, 8, "AVI ")) {
            return Kind.AVI;
        }
        if (startsWith(b, 0x30, 0x26, 0xB2, 0x75, 0x8E, 0x66, 0xCF, 0x11)) {
            return Kind.WMV;
        }
        return null;
    }

    private static void validateDimensions(byte[] bytes, Kind kind) {
        if (!IMAGE_KINDS.contains(kind)) {
            return;
        }
        checkDeclaredDimensions(bytes, kind);
        try {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(bytes));
            if (image == null || image.getWidth() <= 0 || image.getHeight() <= 0) {
                throw new IllegalArgumentException("Unable to read image content");
            }
            rejectIfTooLarge(image.getWidth(), image.getHeight());
        } catch (IOException e) {
            throw new IllegalArgumentException("Unable to read image content", e);
        }
    }

    /** Cheap header checks reject huge canvases before ImageIO allocates a bitmap. */
    private static void checkDeclaredDimensions(byte[] b, Kind kind) {
        if (kind == Kind.PNG && b.length >= 24) {
            rejectIfTooLarge(int32BE(b, 16), int32BE(b, 20));
        } else if (kind == Kind.GIF && b.length >= 10) {
            rejectIfTooLarge((b[6] & 0xFF) | ((b[7] & 0xFF) << 8),
                    (b[8] & 0xFF) | ((b[9] & 0xFF) << 8));
        } else if (kind == Kind.BMP && b.length >= 26) {
            rejectIfTooLarge(int32(b, 18), Math.abs(int32(b, 22)));
        }
    }

    private static void rejectIfTooLarge(int width, int height) {
        if (width > MAX_IMAGE_DIMENSION || height > MAX_IMAGE_DIMENSION) {
            throw new IllegalArgumentException("Image dimensions exceed the maximum of "
                    + MAX_IMAGE_DIMENSION + "px");
        }
    }

    private static int int32(byte[] b, int offset) {
        return (b[offset] & 0xFF) | ((b[offset + 1] & 0xFF) << 8)
                | ((b[offset + 2] & 0xFF) << 16) | ((b[offset + 3] & 0xFF) << 24);
    }

    /** PNG IHDR stores width/height big-endian; BMP uses little-endian. */
    private static int int32BE(byte[] b, int offset) {
        return ((b[offset] & 0xFF) << 24) | ((b[offset + 1] & 0xFF) << 16)
                | ((b[offset + 2] & 0xFF) << 8) | (b[offset + 3] & 0xFF);
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
