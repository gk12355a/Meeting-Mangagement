package com.cmc.meeting.application.port.storage;

import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.Map;

public interface FileStoragePort {
    // Trả về [fileUrl, publicId]
    Map<String, String> uploadFile(MultipartFile file) throws IOException;
    void deleteFile(String publicId) throws IOException;
}