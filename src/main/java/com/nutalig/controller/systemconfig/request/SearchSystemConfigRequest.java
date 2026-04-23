package com.nutalig.controller.systemconfig.request;

import com.nutalig.constant.SystemConstant;
import lombok.Data;

@Data
public class SearchSystemConfigRequest {

    private SystemConstant groupCode;
    private String code;
    private String keyword;
}
