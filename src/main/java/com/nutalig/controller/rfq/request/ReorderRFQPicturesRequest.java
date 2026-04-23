package com.nutalig.controller.rfq.request;

import lombok.Data;

import java.util.List;

@Data
public class ReorderRFQPicturesRequest {

    private List<Long> pictureIds;
}
