package com.nutalig.dto;

import com.nutalig.constant.SearchFieldInputType;
import lombok.Data;

@Data
public class SearchFieldDto {
    private String screenCode;
    private String fieldCode;
    private String labelTh;
    private String labelEn;
    private SearchFieldInputType inputType;
    private Integer sortOrder;
    private Boolean visible;
}
