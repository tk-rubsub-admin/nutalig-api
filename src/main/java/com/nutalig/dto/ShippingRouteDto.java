package com.nutalig.dto;

import lombok.Data;

@Data
public class ShippingRouteDto {
    private Long id;
    private String routeCode;
    private Long originLocationId;
    private String originLocationName;
    private Long destinationLocationId;
    private String destinationLocationName;
    private Boolean active;
}
