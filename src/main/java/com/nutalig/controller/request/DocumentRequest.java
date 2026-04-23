package com.nutalig.controller.request;

import com.nutalig.constant.ExportFileFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DocumentRequest {

    private ExportFileFormat format;
    private Boolean isOriginal;
    private Boolean isCopy;

}
