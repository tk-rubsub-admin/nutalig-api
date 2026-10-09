package com.nutalig.service;

import com.nutalig.entity.*;
import com.nutalig.exception.InvalidRequestException;
import com.nutalig.repository.RequestPriceTierSplitRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SalesOrderItemShippingServiceTest {
    @Mock RequestPriceTierSplitRepository repository;
    @InjectMocks SalesOrderItemShippingService service;
    private SalesOrderDetailEntity land;
    private SalesOrderDetailEntity sea;
    private RfqTierSplitEntity landSplit;
    private RfqTierSplitEntity seaSplit;

    @BeforeEach
    void setup() {
        land = item(182L, 9L, "LAND");
        sea = item(183L, 10L, "SEA");
        landSplit = split(9L, "LAND");
        seaSplit = split(10L, "SEA_SHARE_FCL_40HQ");
    }

    @Test
    void resolvesCompleteSplitMethodsInOneQueryWithoutChangingSalesOrderSnapshots() {
        when(repository.findAllById(Set.of(9L, 10L))).thenReturn(List.of(landSplit, seaSplit));
        var methods = service.resolveShippingMethods(List.of(land, sea));
        assertEquals("LAND", methods.get(182L));
        assertEquals("SEA_SHARE_FCL_40HQ", methods.get(183L));
        assertEquals("SEA", sea.getShippingMethod());
        verify(repository).findAllById(Set.of(9L, 10L));
    }

    @Test
    void selectsOnlyRequestedRowsEvenWhenAnotherRowHasTheSameSupplierAndShippingCategory() throws Exception {
        SalesOrderDetailEntity otherSea = item(184L, null, "SEA");
        when(repository.findAllById(Set.of(10L))).thenReturn(List.of(seaSplit));
        var selected = service.selectItems(List.of(land, sea, otherSea), List.of(183L), "supplier", "SEA_SHARE_FCL_40HQ");
        assertEquals(List.of(sea), selected.items());
    }

    @Test
    void rejectsUnknownCrossSupplierDuplicateAndEmptySelections() {
        assertThrows(InvalidRequestException.class, () -> service.selectItems(List.of(land), List.of(999L), "supplier", "LAND"));
        assertThrows(InvalidRequestException.class, () -> service.selectItems(List.of(land), List.of(182L), "other", "LAND"));
        assertThrows(InvalidRequestException.class, () -> service.selectItems(List.of(land), List.of(182L, 182L), "supplier", "LAND"));
        assertThrows(InvalidRequestException.class, () -> service.selectItems(List.of(land), List.of(), "supplier", "LAND"));
        verifyNoInteractions(repository);
    }

    @Test
    void rejectsGenericSeaForClosedContainerSplit() {
        when(repository.findAllById(Set.of(10L))).thenReturn(List.of(seaSplit));
        assertThrows(InvalidRequestException.class, () -> service.selectItems(List.of(sea), List.of(183L), "supplier", "SEA"));
    }

    @Test
    void missingOrMismatchedSplitCannotFallBackToGenericShipping() {
        when(repository.findAllById(Set.of(10L))).thenReturn(List.of());
        assertFalse(service.resolveShippingMethods(List.of(sea)).containsKey(183L));
        assertThrows(InvalidRequestException.class, () -> service.selectItems(List.of(sea), List.of(183L), "supplier", "SEA"));
        seaSplit.getRequestPriceDetail().setId(999L);
        when(repository.findAllById(Set.of(10L))).thenReturn(List.of(seaSplit));
        assertFalse(service.resolveShippingMethods(List.of(sea)).containsKey(183L));
    }

    @Test
    void normalItemsUseTheSelectedTransportWithoutChangingTheSalesOrderSnapshots() throws Exception {
        land.setRfqTierSplitId(null);
        sea.setRfqTierSplitId(null);
        sea.setShippingMethod("SEA_FCL_20GP");
        var selection = service.selectItems(List.of(land, sea), List.of(182L, 183L), "supplier", "SEA_FCL_40HQ");
        assertEquals(List.of(land, sea), selection.items());
        assertEquals("SEA_FCL_40HQ", selection.shippingMethods().get(182L));
        assertEquals("SEA_FCL_40HQ", selection.shippingMethods().get(183L));
        assertEquals("LAND", land.getShippingMethod());
        assertEquals("SEA_FCL_20GP", sea.getShippingMethod());
        verifyNoInteractions(repository);
    }

    @Test
    void aSplitUnderAnotherSupplierDoesNotLockNormalSupplierItems() throws Exception {
        land.setRfqTierSplitId(null);
        SupplierEntity other = new SupplierEntity();
        other.setId("other");
        sea.setSupplier(other);
        var selection = service.selectItems(List.of(land, sea), List.of(182L), "supplier", "SEA");
        assertEquals("SEA", selection.shippingMethods().get(182L));
        verifyNoInteractions(repository);
    }

    @Test
    void normalItemsRemainLockedWhenTheirSupplierAlsoHasASplitItem() {
        sea.setRfqTierSplitId(null);
        assertThrows(InvalidRequestException.class,
                () -> service.selectItems(List.of(land, sea), List.of(183L), "supplier", "LAND"));
        verifyNoInteractions(repository);
    }

    @Test
    void normalItemsStillRejectUnknownShippingMethods() {
        land.setRfqTierSplitId(null);
        assertThrows(InvalidRequestException.class,
                () -> service.selectItems(List.of(land), List.of(182L), "supplier", "UNKNOWN"));
        verifyNoInteractions(repository);
    }

    private SalesOrderDetailEntity item(Long id, Long splitId, String method) {
        SalesOrderDetailEntity item = new SalesOrderDetailEntity();
        item.setId(id);
        item.setLineNo(id.intValue());
        item.setRfqDetailId(689L);
        item.setRfqTierSplitId(splitId);
        item.setShippingMethod(method);
        SupplierEntity supplier = new SupplierEntity();
        supplier.setId("supplier");
        item.setSupplier(supplier);
        return item;
    }

    private RfqTierSplitEntity split(Long id, String method) {
        RfqTierSplitEntity split = new RfqTierSplitEntity();
        split.setId(id);
        split.setShippingMethod(method);
        RfqDetailEntity detail = new RfqDetailEntity();
        detail.setId(689L);
        split.setRequestPriceDetail(detail);
        split.setSupplier(land.getSupplier());
        return split;
    }
}
