package com.stockland.app.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.Uploader;
import com.cloudinary.utils.ObjectUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CloudinaryServiceImplTest {

    @Mock
    private Cloudinary cloudinary;

    @Mock
    private Uploader uploader;

    @InjectMocks
    private CloudinaryServiceImpl cloudinaryService;

    @BeforeEach
    void setUp() {
        when(cloudinary.uploader()).thenReturn(uploader);
    }

    // ── uploadFile ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("uploadFile returns result map from Cloudinary on success")
    void uploadFile_ReturnsResultMap_OnSuccess() throws IOException {
        byte[] content = "image-bytes".getBytes();
        MockMultipartFile file = new MockMultipartFile("file", "photo.jpg", "image/jpeg", content);

        Map<String, Object> expected = Map.of(
                "secure_url", "https://res.cloudinary.com/demo/image/upload/photo.jpg",
                "public_id", "properties/photo"
        );

        when(uploader.upload(eq(content), any(Map.class))).thenReturn(expected);

        Map result = cloudinaryService.uploadFile(file, "properties");

        assertNotNull(result);
        assertEquals("https://res.cloudinary.com/demo/image/upload/photo.jpg", result.get("secure_url"));
        assertEquals("properties/photo", result.get("public_id"));
        verify(uploader).upload(eq(content), any(Map.class));
    }

    @Test
    @DisplayName("uploadFile passes folder name in upload options")
    void uploadFile_PassesFolderName_InOptions() throws IOException {
        byte[] content = "data".getBytes();
        MockMultipartFile file = new MockMultipartFile("file", "img.png", "image/png", content);

        when(uploader.upload(any(byte[].class), any(Map.class))).thenReturn(Map.of());

        cloudinaryService.uploadFile(file, "avatars");

        verify(uploader).upload(
                eq(content),
                argThat((Map opts) -> "avatars".equals(opts.get("folder")))
        );
    }

    @Test
    @DisplayName("uploadFile throws RuntimeException when Cloudinary throws IOException")
    void uploadFile_ThrowsRuntimeException_OnIOException() throws IOException {
        byte[] content = "bytes".getBytes();
        MockMultipartFile file = new MockMultipartFile("file", "photo.jpg", "image/jpeg", content);

        when(uploader.upload(any(byte[].class), any(Map.class)))
                .thenThrow(new IOException("network error"));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> cloudinaryService.uploadFile(file, "properties"));

        assertTrue(ex.getMessage().contains("Cloudinary upload failed"));
        assertTrue(ex.getMessage().contains("network error"));
    }

    @Test
    @DisplayName("uploadFile uses raw file bytes as upload payload")
    void uploadFile_UsesFileBytes_AsPayload() throws IOException {
        byte[] content = "raw-image-data".getBytes();
        MultipartFile file = new MockMultipartFile("file", "raw.jpg", "image/jpeg", content);

        when(uploader.upload(any(byte[].class), any(Map.class))).thenReturn(Map.of());

        cloudinaryService.uploadFile(file, "properties");

        verify(uploader).upload(eq(content), any(Map.class));
    }

    @Test
    @DisplayName("uploadFile throws RuntimeException when file.getBytes() throws IOException")
    void uploadFile_ThrowsRuntimeException_WhenGetBytesThrowsIOException() throws IOException {
        MultipartFile file = mock(MultipartFile.class);
        when(file.getBytes()).thenThrow(new IOException("disk read error"));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> cloudinaryService.uploadFile(file, "properties"));

        assertTrue(ex.getMessage().contains("Cloudinary upload failed"));
        assertTrue(ex.getMessage().contains("disk read error"));
        verify(uploader, never()).upload(any(), any());
    }

    // ── deleteFile ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("deleteFile calls Cloudinary destroy with the correct publicId")
    void deleteFile_CallsDestroy_WithCorrectPublicId() throws IOException {
        String publicId = "properties/photo123";

        cloudinaryService.deleteFile(publicId);

        verify(uploader).destroy(eq(publicId), any(Map.class));
    }

    @Test
    @DisplayName("deleteFile completes without error when Cloudinary responds successfully")
    void deleteFile_CompletesWithoutError_OnSuccess() throws IOException {
        when(uploader.destroy(any(), any())).thenReturn(Map.of("result", "ok"));

        assertDoesNotThrow(() -> cloudinaryService.deleteFile("properties/abc"));
        verify(uploader).destroy(eq("properties/abc"), any(Map.class));
    }

    @Test
    @DisplayName("deleteFile throws RuntimeException when Cloudinary throws IOException")
    void deleteFile_ThrowsRuntimeException_OnIOException() throws IOException {
        when(uploader.destroy(any(), any()))
                .thenThrow(new IOException("timeout"));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> cloudinaryService.deleteFile("properties/abc"));

        assertEquals("Cloudinary deletion failed", ex.getMessage());
    }

    @Test
    @DisplayName("deleteFile passes an empty options map to Cloudinary destroy")
    void deleteFile_PassesEmptyOptionsMap() throws IOException {
        cloudinaryService.deleteFile("properties/xyz");

        verify(uploader).destroy(
                eq("properties/xyz"),
                argThat(opts -> opts != null && ((Map<?, ?>) opts).isEmpty())
        );
    }

    @Test
    @DisplayName("deleteFile invokes destroy and discards the result without error")
    void deleteFile_InvokesDestroy_AndDiscardsResult() throws IOException {
        when(uploader.destroy(eq("properties/discard"), any(Map.class)))
                .thenReturn(Map.of("result", "ok", "resource_type", "image"));

        assertDoesNotThrow(() -> cloudinaryService.deleteFile("properties/discard"));
        verify(uploader).destroy(eq("properties/discard"), any(Map.class));
    }
}

