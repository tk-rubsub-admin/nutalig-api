package com.nutalig.utils;

import com.nutalig.constant.ShippingMethod;
import com.nutalig.constant.ShippingMode;
import com.nutalig.entity.SupplierShippingEntity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ShippingMethodUtilTest {
    private final SupplierShippingEntity standard = shipping(ShippingMethod.SEA, ShippingMode.STANDARD, "TB001");
    private final SupplierShippingEntity fcl = shipping(ShippingMethod.SEA, ShippingMode.FCL, "TZ001");
    private final SupplierShippingEntity land = shipping(ShippingMethod.LAND, ShippingMode.STANDARD, "TR001");

    @ParameterizedTest
    @ValueSource(strings = {"SEA_FCL_20GP", "SEA_FCL_40HQ", "SEA_SHARE_FCL_20GP", "SEA_SHARE_FCL_40HQ", " sea_fcl_20gp "})
    void closedContainersSelectTzEvenWhenTbAppearsFirst(String method) {
        assertEquals("TZ001", ShippingMethodUtil.getShippingCode(method, List.of(standard, land, fcl)));
        assertTrue(ShippingMethodUtil.matchesSupplierShipping(method, fcl));
        assertFalse(ShippingMethodUtil.matchesSupplierShipping(method, standard));
    }

    @Test
    void standardSeaAndAllNeverSelectAnFclCode() {
        List<SupplierShippingEntity> shippings = List.of(fcl, standard, land);
        assertEquals("TB001", ShippingMethodUtil.getShippingCode("SEA", shippings));
        assertEquals("TR001 & TB001", ShippingMethodUtil.getShippingCode("ALL", shippings));
        assertFalse(ShippingMethodUtil.matchesSupplierShipping("SEA", fcl));
    }

    @Test
    void missingFclDoesNotFallBackToStandardSea() {
        assertEquals("", ShippingMethodUtil.getShippingCode("SEA_FCL_20GP", List.of(standard)));
    }

    @Test
    void legacyNullModeStillMeansStandard() {
        standard.setShippingMode(null);
        assertTrue(ShippingMethodUtil.matchesSupplierShipping("SEA", standard));
        assertFalse(ShippingMethodUtil.matchesSupplierShipping("SEA_FCL_20GP", standard));
    }

    @Test
    void invalidOrMissingMethodsCannotMatch() {
        assertFalse(ShippingMethodUtil.matchesSupplierShipping("SEA_FCL_UNKNOWN", fcl));
        assertFalse(ShippingMethodUtil.matchesSupplierShipping(null, standard));
        assertFalse(ShippingMethodUtil.matchesSupplierShipping("SEA", null));
        assertEquals("", ShippingMethodUtil.getShippingCode("SEA", null));
        assertEquals("ขนส่งทางเครื่องบิน", ShippingMethodUtil.getShippingCode("AIR", List.of()));
    }

    private static SupplierShippingEntity shipping(ShippingMethod method, ShippingMode mode, String code) {
        SupplierShippingEntity shipping = new SupplierShippingEntity();
        shipping.setShippingMethod(method);
        shipping.setShippingMode(mode);
        shipping.setCarCode(code);
        return shipping;
    }
}
