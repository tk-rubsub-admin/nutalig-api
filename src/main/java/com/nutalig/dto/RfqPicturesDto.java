package com.nutalig.dto;

import lombok.Data;

import java.time.ZonedDateTime;

@Data
public class RfqPicturesDto {

    private Long id;
    private String pictureUrl;
    private String fileName;
    private String fileType;
    private Integer sort;
    private ZonedDateTime updatedDate;
    private String updatedBy;
}
