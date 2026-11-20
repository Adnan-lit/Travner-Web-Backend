package org.adnan.travner.util;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class ChatFileValidatorTest {

    private static final byte[] PNG = new byte[]{
            (byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0};

    @Test
    void validPng_isAcceptedAndCanonicalTypeReturned() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "photo.png", "image/png", PNG);

        byte[] bytes = ChatFileValidator.validate(file);

        assertArrayEquals(PNG, bytes);
        assertEquals("image/png", ChatFileValidator.contentTypeOf(bytes));
    }

    @Test
    void dotlessTextFile_isAcceptedInsteadOfThrowing() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "README", "text/plain", "hello world\n".getBytes(StandardCharsets.UTF_8));

        assertDoesNotThrow(() -> ChatFileValidator.validate(file));
        assertEquals("README", ChatFileValidator.sanitizeFilename("README"));
    }

    @Test
    void emptyAndOversizeFilesAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> ChatFileValidator.validate(
                new MockMultipartFile("file", "empty.png", "image/png", new byte[0])));

        byte[] tooLarge = new byte[(int) ChatFileValidator.MAX_ATTACHMENT_SIZE + 1];
        Arrays.fill(tooLarge, (byte) 1);
        assertThrows(IllegalArgumentException.class, () -> ChatFileValidator.validate(
                new MockMultipartFile("file", "big.txt", "text/plain", tooLarge)));
    }

    @Test
    void dangerousExtensionIsRejected() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "payload.exe", "application/octet-stream", PNG);

        assertThrows(IllegalArgumentException.class, () -> ChatFileValidator.validate(file));
    }

    @Test
    void extensionMustMatchMagicBytes() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "photo.pdf", "application/pdf", PNG);

        assertThrows(IllegalArgumentException.class, () -> ChatFileValidator.validate(file));
    }

    @Test
    void declaredContentTypeMustMatchMagicBytes() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "photo.png", "text/plain", PNG);

        assertThrows(IllegalArgumentException.class, () -> ChatFileValidator.validate(file));
    }

    @Test
    void sanitizeFilename_stripsPathsAndHandlesLongDotlessNames() {
        assertEquals("passwd", ChatFileValidator.sanitizeFilename("../../etc/passwd"));
        assertEquals("file", ChatFileValidator.sanitizeFilename(".."));
        assertEquals("file", ChatFileValidator.sanitizeFilename(null));
        String longName = "a".repeat(200);
        assertEquals(80, ChatFileValidator.sanitizeFilename(longName).length());
    }
}
