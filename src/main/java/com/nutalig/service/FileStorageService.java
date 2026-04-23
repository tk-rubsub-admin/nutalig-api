package com.nutalig.service;

import com.nutalig.config.AppProperties;
import com.nutalig.controller.file.response.UploadImageResponse;
import org.apache.commons.io.FilenameUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import lombok.RequiredArgsConstructor;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FileStorageService {

    private final AppProperties appProperties;

    public UploadImageResponse uploadImage(MultipartFile file) throws Exception {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is empty.");
        }

        String extension = FilenameUtils.getExtension(file.getOriginalFilename());
        if (extension == null || extension.isBlank()) {
            extension = resolveExtensionFromContentType(file.getContentType());
        }

        String fileName = UUID.randomUUID() +
                ((extension == null || extension.isBlank()) ? "" : "." + extension);

        Path uploadPath = Paths.get(appProperties.getUpload().getDir()).toAbsolutePath().normalize();
        Files.createDirectories(uploadPath);

        Path targetPath = uploadPath.resolve(fileName);
        Files.copy(file.getInputStream(), targetPath);

        String url = appProperties.getUpload().getPublicBaseUrl() + "/" + fileName;

        System.out.println("Saved file to: " + targetPath);
        System.out.println("Public URL: " + url);

        return new UploadImageResponse(fileName, url);
    }

    private String resolveExtensionFromContentType(String contentType) {
        if (contentType == null) {
            return "";
        }

        return switch (contentType.toLowerCase()) {
            case "image/jpeg", "image/jpg" -> "jpg";
            case "image/png" -> "png";
            case "image/webp" -> "webp";
            case "image/gif" -> "gif";
            default -> "";
        };
    }
}
