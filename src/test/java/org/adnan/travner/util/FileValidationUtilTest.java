package org.adnan.travner.util;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class FileValidationUtilTest {

    private static byte[] png(int width, int height) throws Exception {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        assertTrue(ImageIO.write(image, "png", out));
        return out.toByteArray();
    }

    @Test
    void validPng_isAcceptedAndCanonicalTypeReturned() throws Exception {
        byte[] bytes = png(10, 10);

        FileValidationUtil.ValidatedMedia result = FileValidationUtil.validateFile(
                new MockMultipartFile("file", "photo.png", "image/png", bytes));

        assertArrayEquals(bytes, result.bytes());
        assertEquals("image/png", result.contentType());
    }

    @Test
    void emptyAndOversizeFilesAreRejected() throws Exception {
        assertThrows(IllegalArgumentException.class, () -> FileValidationUtil.validateFile(
                new MockMultipartFile("file", "empty.png", "image/png", new byte[0])));

        byte[] tooLarge = new byte[(int) FileValidationUtil.MAX_FILE_SIZE + 1];
        Arrays.fill(tooLarge, (byte) 1);
        assertThrows(IllegalArgumentException.class, () -> FileValidationUtil.validateFile(
                new MockMultipartFile("file", "big.mp4", "video/mp4", tooLarge)));
    }

    @Test
    void dangerousExtensionIsRejected() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "payload.exe", "application/octet-stream", png(2, 2));
        assertThrows(IllegalArgumentException.class, () -> FileValidationUtil.validateFile(file));
    }

    @Test
    void extensionMustMatchMagicBytes() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "photo.pdf", "application/pdf", png(2, 2));
        assertThrows(IllegalArgumentException.class, () -> FileValidationUtil.validateFile(file));
    }

    @Test
    void declaredContentTypeMustMatchMagicBytes() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "photo.png", "text/plain", png(2, 2));
        assertThrows(IllegalArgumentException.class, () -> FileValidationUtil.validateFile(file));
    }

    @Test
    void octetStreamDeclareIsAcceptedAndTypeIsDetected() throws Exception {
        FileValidationUtil.ValidatedMedia result = FileValidationUtil.validateFile(
                new MockMultipartFile("file", "photo.png", "application/octet-stream", png(4, 4)));
        assertEquals("image/png", result.contentType());
    }

    @Test
    void oversizedImageDimensionsAreRejectedBeforeDecoding() throws Exception {
        byte[] bytes = png(10, 10);
        // Patch PNG IHDR width (big-endian, bytes 16..19) to 5000 px.
        bytes[16] = 0; bytes[17] = 0; bytes[18] = 0x13; bytes[19] = (byte) 0x88;

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> FileValidationUtil.validateFile(
                        new MockMultipartFile("file", "huge.png", "image/png", bytes)));
        assertTrue(error.getMessage().contains("dimensions"));
    }

    @Test
    void mp4IsDetectedFromMagicBytes() {
        byte[] mp4 = new byte[16];
        byte[] brand = {'f', 't', 'y', 'p', 'i', 's', 'o', 'm'};
        System.arraycopy(brand, 0, mp4, 4, brand.length);

        FileValidationUtil.ValidatedMedia result = FileValidationUtil.validateFile(
                new MockMultipartFile("file", "clip.mp4", "video/mp4", mp4));
        assertEquals("video/mp4", result.contentType());
    }

    @Test
    void textContentIsNotAcceptedAsMedia() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "notes.txt", "text/plain", "hello".getBytes(StandardCharsets.UTF_8));
        assertThrows(IllegalArgumentException.class, () -> FileValidationUtil.validateFile(file));
    }

    @Test
    void sanitizeFilename_stripsPathsAndHandlesDotlessLongNames() {
        assertEquals("passwd", FileValidationUtil.sanitizeFilename("../../etc/passwd"));
        assertEquals("README", FileValidationUtil.sanitizeFilename("README"));
        assertEquals("file", FileValidationUtil.sanitizeFilename(null));
        assertEquals(80, FileValidationUtil.sanitizeFilename("a".repeat(200)).length());
    }
}
