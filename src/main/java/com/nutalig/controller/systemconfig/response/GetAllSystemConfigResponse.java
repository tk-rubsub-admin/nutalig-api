package com.nutalig.controller.systemconfig.response;

import com.nutalig.controller.response.Pagination;
import com.nutalig.dto.SystemConfigDto;
import lombok.Data;

import java.util.List;

@Data
public class GetAllSystemConfigResponse {

    private List<SystemConfigDto> systemConfigList;
    private Pagination pagination;
}
