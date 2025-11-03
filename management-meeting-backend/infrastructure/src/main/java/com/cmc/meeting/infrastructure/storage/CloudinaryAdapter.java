package com.cmc.meeting.infrastructure.storage;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.cmc.meeting.application.port.storage.FileStoragePort;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.HashMap;

@Component
public class CloudinaryAdapter implements FileStoragePort {

    private final Cloudinary cloudinary;

    public CloudinaryAdapter(Cloudinary cloudinary) {
        this.cloudinary = cloudinary;
    }

    @Override
    public Map<String, String> uploadFile(MultipartFile multipartFile) throws IOException {
        File file = convertMultiPartToFile(multipartFile);

        // Upload lên Cloudinary
        Map uploadResult = cloudinary.uploader().upload(file, ObjectUtils.emptyMap());

        // Xóa file tạm
        file.delete();

        // Lấy URL và Public ID
        Map<String, String> result = new HashMap<>();
        result.put("url", uploadResult.get("url").toString());
        result.put("public_id", uploadResult.get("public_id").toString());

        return result;
    }

    @Override
    public void deleteFile(String publicId) throws IOException {
        cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
    }

    // Helper chuyển đổi MultipartFile sang File
    private File convertMultiPartToFile(MultipartFile file) throws IOException {
        File convFile = new File(System.getProperty("java.io.tmpdir") + "/" + file.getOriginalFilename());
        try (FileOutputStream fos = new FileOutputStream(convFile)) {
            fos.write(file.getBytes());
        }
        return convFile;
    }
}