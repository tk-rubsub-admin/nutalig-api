package com.nutalig.controller.file.response;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UploadImageResponse {
    private String fileName;
    private String url;
}