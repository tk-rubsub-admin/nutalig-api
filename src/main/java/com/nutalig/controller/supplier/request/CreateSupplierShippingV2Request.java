package com.nutalig.controller.supplier.request;

import com.nutalig.constant.ShippingMethod;
import com.nutalig.constant.ShippingMode;
import lombok.Data;

import java.util.List;

@Data
public class CreateSupplierShippingV2Request {
    private ShippingMethod shippingMethod;
    private ShippingMode shippingMode;
    private String shippingName;
    private String carCode;
    private String remark;
    private List<Long> shippingRouteIds;
}
