package com.nutalig.service;

import com.nutalig.config.AppProperties;
import com.nutalig.controller.file.response.UploadFileResponse;
import lombok.RequiredArgsConstructor;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FileStorageService {

    private final AppProperties appProperties;

    public UploadFileResponse uploadFile(MultipartFile file) throws Exception {
        return uploadFile(file, null);
    }

    public UploadFileResponse uploadFile(MultipartFile file, String relativeDir) throws Exception {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is empty.");
        }

        String contentType = StringUtils.trimToNull(file.getContentType());
        String extension = resolveExtension(file.getOriginalFilename(), contentType);

        String generatedFileName = UUID.randomUUID() +
                ((extension == null || extension.isBlank()) ? "" : "." + extension);
        String normalizedRelativeDir = normalizeRelativeDir(relativeDir);
        String storedFileName = StringUtils.isBlank(normalizedRelativeDir)
                ? generatedFileName
                : normalizedRelativeDir + "/" + generatedFileName;

        Path uploadPath = Paths.get(appProperties.getUpload().getDir()).toAbsolutePath().normalize();
        Path targetDirectory = StringUtils.isBlank(normalizedRelativeDir)
                ? uploadPath
                : uploadPath.resolve(normalizedRelativeDir).normalize();
        if (!targetDirectory.startsWith(uploadPath)) {
            throw new IllegalArgumentException("Invalid upload directory.");
        }
        Files.createDirectories(targetDirectory);

        Path targetPath = targetDirectory.resolve(generatedFileName).normalize();
        try (InputStream inputStream = file.getInputStream()) {
            Files.copy(inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING);
        }

        String url = buildPublicFileUrl(storedFileName);

        return new UploadFileResponse(storedFileName, url, contentType);
    }

    public UploadFileResponse uploadGeneratedFile(byte[] content, String fileBaseName, String contentType) throws IOException {
        if (content == null || content.length == 0) {
            throw new IllegalArgumentException("File content is empty.");
        }

        String extension = resolveExtension(fileBaseName, contentType);
        String safeBaseName = sanitizeFileBaseName(fileBaseName);
        String fileName = safeBaseName +
                ((extension == null || extension.isBlank()) ? "" : "." + extension);

        Path uploadPath = Paths.get(appProperties.getUpload().getDir()).toAbsolutePath().normalize();
        Files.createDirectories(uploadPath);

        Path targetPath = uploadPath.resolve(fileName);
        Files.write(targetPath, content);

        String url = buildPublicFileUrl(fileName);

        return new UploadFileResponse(fileName, url, contentType);
    }

    public boolean fileExists(String fileName) {
        if (StringUtils.isBlank(fileName)) {
            return false;
        }

        Path uploadPath = Paths.get(appProperties.getUpload().getDir()).toAbsolutePath().normalize();
        return Files.exists(uploadPath.resolve(fileName));
    }

    public String getPublicFileUrl(String fileName) {
        return buildPublicFileUrl(fileName);
    }

    public InputStream openUploadedFile(String fileName) throws IOException {
        Path uploadPath = Paths.get(appProperties.getUpload().getDir()).toAbsolutePath().normalize();
        Path filePath = uploadPath.resolve(fileName).normalize();
        if (!filePath.startsWith(uploadPath) || !Files.exists(filePath)) {
            return null;
        }
        return new FileInputStream(filePath.toFile());
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

    private String sanitizeFileBaseName(String fileBaseName) {
        String sanitized = StringUtils.defaultIfBlank(fileBaseName, UUID.randomUUID().toString());
        sanitized = sanitized.replaceAll("[\\\\/]+", "_");
        sanitized = sanitized.replaceAll("[^A-Za-z0-9._-]", "_");
        sanitized = sanitized.replaceAll("_+", "_");
        sanitized = StringUtils.removeStart(sanitized, ".");
        sanitized = StringUtils.removeEnd(sanitized, ".");
        return StringUtils.defaultIfBlank(sanitized, UUID.randomUUID().toString());
    }

    private String normalizeRelativeDir(String relativeDir) {
        if (StringUtils.isBlank(relativeDir)) {
            return null;
        }

        String normalized = relativeDir.replace("\\", "/").trim();
        normalized = StringUtils.strip(normalized, "/");
        if (StringUtils.isBlank(normalized)) {
            return null;
        }

        String sanitized = Arrays.stream(normalized.split("/"))
                .map(this::sanitizePathSegment)
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.joining("/"));

        return StringUtils.defaultIfBlank(sanitized, null);
    }

    private String sanitizePathSegment(String pathSegment) {
        String sanitized = StringUtils.defaultString(pathSegment).trim();
        sanitized = sanitized.replaceAll("[^A-Za-z0-9._-]", "_");
        sanitized = sanitized.replaceAll("_+", "_");
        sanitized = StringUtils.removeStart(sanitized, ".");
        sanitized = StringUtils.removeEnd(sanitized, ".");
        return sanitized;
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

    private String buildPublicFileUrl(String fileName) {
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        if (requestAttributes instanceof ServletRequestAttributes) {
            return ServletUriComponentsBuilder.fromCurrentContextPath()
                    .path("/uploads/")
                    .path(fileName)
                    .build()
                    .toUriString();
        }

        String publicBaseUrl = StringUtils.removeEnd(appProperties.getUpload().getPublicBaseUrl(), "/");
        return publicBaseUrl + "/" + fileName;
    }
}
