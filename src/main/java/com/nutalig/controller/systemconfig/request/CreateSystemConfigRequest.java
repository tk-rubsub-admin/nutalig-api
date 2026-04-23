package com.nutalig.controller.systemconfig.request;

import com.nutalig.constant.SystemConstant;
import lombok.Data;

@Data
public class CreateSystemConfigRequest {

    private SystemConstant groupCode;
    private String code;
    private String nameTh;
    private String nameEn;
    private Integer sort;
}
