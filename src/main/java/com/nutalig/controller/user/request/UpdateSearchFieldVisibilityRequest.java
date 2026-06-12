package com.nutalig.controller.user.request;

import lombok.Data;

import java.util.Map;

@Data
public class UpdateSearchFieldVisibilityRequest {
    private String roleCode;
    private String screenCode;
    private Map<String, Boolean> fields;
}
