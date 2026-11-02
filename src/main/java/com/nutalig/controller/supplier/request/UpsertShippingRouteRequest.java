package com.nutalig.controller.supplier.request;

import lombok.Data;

@Data
public class UpsertShippingRouteRequest {
    private String routeCode;
    private Long originLocationId;
    private Long destinationLocationId;
}
