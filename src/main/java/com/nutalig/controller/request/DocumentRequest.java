package com.nutalig.controller.request;

import com.nutalig.constant.ExportFileFormat;
import com.nutalig.constant.TemplateLanguage;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DocumentRequest {

    private ExportFileFormat format;
    private TemplateLanguage lang;
    private Boolean isOriginal;
    private Boolean isCopy;

    public DocumentRequest(ExportFileFormat format, Boolean isOriginal, Boolean isCopy) {
        this.format = format;
        this.lang = TemplateLanguage.EN;
        this.isOriginal = isOriginal;
        this.isCopy = isCopy;
    }
}
