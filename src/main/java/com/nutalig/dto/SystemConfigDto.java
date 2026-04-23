package com.nutalig.dto;

import com.nutalig.constant.SystemConstant;
import lombok.Data;

@Data
public class SystemConfigDto {

    private SystemConstant groupCode;
    private String code;
    private String nameTh;
    private String nameEn;
    private Integer sort;
    
}
