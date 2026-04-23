package com.nutalig.dto.document;

import lombok.Data;

import java.io.FileInputStream;

@Data
public class DefaultDocumentDto {
    private String docNo;
    private String docDate;
    private String refDocNo;
    private FileInputStream logo;
    private String accountName;
    private String bankName;
    private String accountNo;
    private Boolean isCopy = false;
}
