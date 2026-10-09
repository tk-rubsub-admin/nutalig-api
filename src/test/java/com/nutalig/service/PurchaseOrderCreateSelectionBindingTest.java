package com.nutalig.service;

import com.nutalig.controller.purchaseorder.request.CreatePurchaseOrderRequest;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.bind.ServletRequestDataBinder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PurchaseOrderCreateSelectionBindingTest {
    @Test
    void bindsIndexedMultipartSelectionWithoutChangingLegacyAbsence() {
        MockHttpServletRequest form = new MockHttpServletRequest();
        form.addParameter("salesOrderDetailIds[0]", "183");
        form.addParameter("shippingMethodSnapshot", "SEA_SHARE_FCL_40HQ");
        form.addParameter("items[0].salesOrderDetailId", "183");
        CreatePurchaseOrderRequest request = new CreatePurchaseOrderRequest();
        ServletRequestDataBinder binder = new ServletRequestDataBinder(request);
        binder.bind(form);
        assertFalse(binder.getBindingResult().hasErrors());
        assertEquals(List.of(183L), request.getSalesOrderDetailIds());
        assertEquals(183L, request.getItems().getFirst().getSalesOrderDetailId());
        assertEquals("SEA_SHARE_FCL_40HQ", request.getShippingMethodSnapshot());
        CreatePurchaseOrderRequest legacy = new CreatePurchaseOrderRequest();
        new ServletRequestDataBinder(legacy).bind(new MockHttpServletRequest());
        assertNull(legacy.getSalesOrderDetailIds());
    }
}
