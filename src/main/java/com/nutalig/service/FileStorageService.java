package com.nutalig.service;

import com.nutalig.config.AppProperties;
import com.nutalig.controller.file.response.UploadFileResponse;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import lombok.RequiredArgsConstructor;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FileStorageService {

    private final AppProperties appProperties;

    public UploadFileResponse uploadFile(MultipartFile file) throws Exception {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is empty.");
        }

        String contentType = StringUtils.trimToNull(file.getContentType());
        String extension = resolveExtension(file.getOriginalFilename(), contentType);

        String fileName = UUID.randomUUID() +
                ((extension == null || extension.isBlank()) ? "" : "." + extension);

        Path uploadPath = Paths.get(appProperties.getUpload().getDir()).toAbsolutePath().normalize();
        Files.createDirectories(uploadPath);

        Path targetPath = uploadPath.resolve(fileName);
        try (InputStream inputStream = file.getInputStream()) {
            Files.copy(inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING);
        }

        String url = appProperties.getUpload().getPublicBaseUrl() + "/" + fileName;

        return new UploadFileResponse(fileName, url, contentType);
    }

    public UploadFileResponse uploadImage(MultipartFile file) throws Exception {
        return uploadFile(file);
    }

    private String resolveExtension(String originalFilename, String contentType) {
        String extension = FilenameUtils.getExtension(originalFilename);
        if (StringUtils.isNotBlank(extension)) {
            return extension;
        }
        return resolveExtensionFromContentType(contentType);
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
            case "application/pdf" -> "pdf";
            case "application/illustrator", "application/postscript" -> "ai";
            case "text/plain" -> "txt";
            case "text/csv" -> "csv";
            case "application/msword" -> "doc";
            case "application/vnd.openxmlformats-officedocument.wordprocessingml.document" -> "docx";
            case "application/vnd.ms-excel" -> "xls";
            case "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" -> "xlsx";
            case "application/vnd.ms-powerpoint" -> "ppt";
            case "application/vnd.openxmlformats-officedocument.presentationml.presentation" -> "pptx";
            case "application/zip", "application/x-zip-compressed" -> "zip";
            default -> "";
        };
    }
}
