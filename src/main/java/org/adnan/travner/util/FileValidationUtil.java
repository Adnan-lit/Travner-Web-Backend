package org.adnan.travner.util;

import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.List;

/**
 * Utility class for file validation and security checks
 */
public class FileValidationUtil {

    // Allowed image MIME types
    private static final List<String> ALLOWED_IMAGE_TYPES = Arrays.asList(
            "image/jpeg",
            "image/png",
            "image/gif",
            "image/webp",
            "image/bmp");

    // Allowed video MIME types
    private static final List<String> ALLOWED_VIDEO_TYPES = Arrays.asList(
            "video/mp4",
            "video/quicktime",
            "video/x-msvideo", // .avi
            "video/x-ms-wmv" // .wmv
    );

    // Maximum file size: 20MB
    private static final long MAX_FILE_SIZE = 20 * 1024 * 1024;

    // Dangerous file extensions that should never be allowed
    private static final List<String> DANGEROUS_EXTENSIONS = Arrays.asList(
            ".exe", ".bat", ".cmd", ".com", ".pif", ".scr", ".vbs", ".js", ".jar",
            ".zip", ".rar", ".7z", ".tar", ".php", ".asp", ".jsp", ".sh");

    /**
     * Validates if the uploaded file is safe and allowed
     */
    public static void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File cannot be null or empty");
        }

        // Check file size
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("File size exceeds maximum limit of 20MB");
        }

        // Get file info
        String originalFilename = file.getOriginalFilename();
        String contentType = file.getContentType();

        // Check for dangerous file extensions
        if (originalFilename != null) {
            String lowerFilename = originalFilename.toLowerCase();
            for (String dangerousExt : DANGEROUS_EXTENSIONS) {
                if (lowerFilename.endsWith(dangerousExt)) {
                    throw new IllegalArgumentException("File type not allowed: " + dangerousExt);
                }
            }
        }

        // Validate MIME type
        if (contentType == null ||
                (!ALLOWED_IMAGE_TYPES.contains(contentType) && !ALLOWED_VIDEO_TYPES.contains(contentType))) {
            throw new IllegalArgumentException("Invalid file type. Only images and videos are allowed.");
        }

        // Additional security check: verify file extension matches MIME type
        if (originalFilename != null && contentType != null) {
            validateFileExtensionMatchesMimeType(originalFilename, contentType);
        }
    }

    /**
     * Validates that file extension matches the MIME type to prevent spoofing
     */
    private static void validateFileExtensionMatchesMimeType(String filename, String mimeType) {
        String extension = getFileExtension(filename).toLowerCase();

        switch (mimeType) {
            case "image/jpeg":
                if (!extension.equals(".jpg") && !extension.equals(".jpeg")) {
                    throw new IllegalArgumentException("File extension does not match MIME type");
                }
                break;
            case "image/png":
                if (!extension.equals(".png")) {
                    throw new IllegalArgumentException("File extension does not match MIME type");
                }
                break;
            case "image/gif":
                if (!extension.equals(".gif")) {
                    throw new IllegalArgumentException("File extension does not match MIME type");
                }
                break;
            case "image/webp":
                if (!extension.equals(".webp")) {
                    throw new IllegalArgumentException("File extension does not match MIME type");
                }
                break;
            case "video/mp4":
                if (!extension.equals(".mp4")) {
                    throw new IllegalArgumentException("File extension does not match MIME type");
                }
                break;
            case "video/quicktime":
                if (!extension.equals(".mov")) {
                    throw new IllegalArgumentException("File extension does not match MIME type");
                }
                break;
            case "video/x-msvideo":
                if (!extension.equals(".avi")) {
                    throw new IllegalArgumentException("File extension does not match MIME type");
                }
                break;
        }
    }

    /**
     * Extracts file extension from filename
     */
    private static String getFileExtension(String filename) {
        if (filename == null || filename.lastIndexOf('.') == -1) {
            return "";
        }
        return filename.substring(filename.lastIndexOf('.'));
    }

    /**
     * Generates a safe filename by removing potentially dangerous characters
     */
    public static String sanitizeFilename(String filename) {
        if (filename == null) {
            return "file";
        }

        // Remove potentially dangerous characters and keep only alphanumeric, dots,
        // hyphens, and underscores
        String sanitized = filename.replaceAll("[^a-zA-Z0-9._-]", "_");

        // Ensure filename is not too long
        if (sanitized.length() > 100) {
            String extension = getFileExtension(sanitized);
            String nameWithoutExt = sanitized.substring(0, sanitized.lastIndexOf('.'));
            sanitized = nameWithoutExt.substring(0, 100 - extension.length()) + extension;
        }

        return sanitized;
    }

    /**
     * Checks if the file is an image
     */
    public static boolean isImage(String mimeType) {
        return mimeType != null && ALLOWED_IMAGE_TYPES.contains(mimeType);
    }

    /**
     * Checks if the file is a video
     */
    public static boolean isVideo(String mimeType) {
        return mimeType != null && ALLOWED_VIDEO_TYPES.contains(mimeType);
    }
}