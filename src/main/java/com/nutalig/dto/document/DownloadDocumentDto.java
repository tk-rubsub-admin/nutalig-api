package com.nutalig.dto.document;

import com.nutalig.constant.ExportFileFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DownloadDocumentDto {

    private String baseFileName;
    private ExportFileFormat format;
    private List<FileItem> files;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class FileItem {
        private String fileName;
        private String base64;
        private String contentType;
    }
}