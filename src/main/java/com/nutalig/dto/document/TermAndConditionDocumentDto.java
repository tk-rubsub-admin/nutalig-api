package com.nutalig.dto.document;

import lombok.Data;

import java.io.FileInputStream;

@Data
public class TermAndConditionDocumentDto {
    private FileInputStream stamp;
    private FileInputStream signature;
    private String salesName;
    private String accountName;
    private String bankName;
    private String branchName;
    private String accountNo;
}
