package com.nutalig.entity.id;

import lombok.Data;

import java.io.Serializable;

@Data
public class SearchFieldId implements Serializable {
    private String screenCode;
    private String fieldCode;
}
