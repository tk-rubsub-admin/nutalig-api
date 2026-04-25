package com.nutalig.utils;

import org.apache.commons.io.FilenameUtils;

import java.util.Locale;

public final class RfqAttachmentUtil {

    private RfqAttachmentUtil() {
    }

    public static String extractFileName(String pictureUrl) {
        if (pictureUrl == null || pictureUrl.isBlank()) {
            return null;
        }

        int lastSlashIndex = pictureUrl.lastIndexOf('/');
        return lastSlashIndex >= 0 ? pictureUrl.substring(lastSlashIndex + 1) : pictureUrl;
    }

    public static String resolveContentType(String pictureUrl) {
        String extension = FilenameUtils.getExtension(extractFileName(pictureUrl));
        if (extension == null || extension.isBlank()) {
            return "application/octet-stream";
        }

        return switch (extension.toLowerCase(Locale.ROOT)) {
            case "jpg", "jpeg" -> "image/jpeg";
            case "png" -> "image/png";
            case "gif" -> "image/gif";
            case "webp" -> "image/webp";
            case "pdf" -> "application/pdf";
            case "ai" -> "application/illustrator";
            case "txt" -> "text/plain";
            case "csv" -> "text/csv";
            case "doc" -> "application/msword";
            case "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            case "xls" -> "application/vnd.ms-excel";
            case "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            case "ppt" -> "application/vnd.ms-powerpoint";
            case "pptx" -> "application/vnd.openxmlformats-officedocument.presentationml.presentation";
            case "zip" -> "application/zip";
            default -> "application/octet-stream";
        };
    }

    public static Boolean isImage(String pictureUrl) {
        return resolveContentType(pictureUrl).startsWith("image/");
    }
}
