package com.stockland.app.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.Uploader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CloudinaryServiceTests {
    @Mock
    private Cloudinary cloudinary;

    @Mock
    private Uploader uploader;

    @InjectMocks
    private CloudinaryServiceImpl imageService;

    @Test
    void uploadImage_ShouldReturnUrl_WhenSuccessful() throws IOException {
        MultipartFile mockFile = new MockMultipartFile("file", "test.jpg", "image/jpeg", "test data".getBytes());

        Map<String, Object> fakeResponse = new HashMap<>();
        fakeResponse.put("url", "http://res.cloudinary.com/demo/image/upload/sample.jpg");
        fakeResponse.put("public_id", "sample_id");

        when(cloudinary.uploader()).thenReturn(uploader);
        when(uploader.upload(any(byte[].class), anyMap())).thenReturn(fakeResponse);

        Map obj = imageService.uploadFile(mockFile, "properties");

        assertEquals("http://res.cloudinary.com/demo/image/upload/sample.jpg", obj.get("url"));
        verify(uploader, times(1)).upload(any(byte[].class), anyMap());
    }

    @Test
    void ShouldDeleteFile_WhenPublicIdProvided() throws IOException {
        String publicId = "sample_id";

        when(cloudinary.uploader()).thenReturn(uploader);

        Map<String, Object> destroyResponse = new HashMap<>();
        destroyResponse.put("result", "ok");

        when(uploader.destroy(eq(publicId), anyMap())).thenReturn(destroyResponse);

        imageService.deleteFile(publicId);

        verify(uploader, times(1)).destroy(eq(publicId), anyMap());
    }
}
