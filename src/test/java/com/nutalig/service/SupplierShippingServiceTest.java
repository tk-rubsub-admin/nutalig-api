package com.nutalig.service;

import com.nutalig.constant.ShippingMethod;
import com.nutalig.constant.ShippingMode;
import com.nutalig.controller.supplier.request.UpsertSupplierShippingDestinationRequest;
import com.nutalig.controller.supplier.request.UpsertSupplierShippingRequest;
import com.nutalig.entity.SupplierShippingEntity;
import com.nutalig.exception.InvalidRequestException;
import com.nutalig.mapper.SupplierMapper;
import com.nutalig.repository.SupplierShippingRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SupplierShippingServiceTest {
    @Mock private SupplierShippingRepository repository;
    @InjectMocks private SupplierService service;

    @Test
    void createsFclMasterAndExposesModeInDto() throws Exception {
        service.createSupplierShipping(request(ShippingMethod.SEA, ShippingMode.FCL, " tz001 "));

        ArgumentCaptor<SupplierShippingEntity> saved = ArgumentCaptor.forClass(SupplierShippingEntity.class);
        verify(repository).save(saved.capture());
        assertEquals(ShippingMode.FCL, saved.getValue().getShippingMode());
        assertEquals("TZ001", saved.getValue().getCarCode());
        assertEquals(ShippingMode.FCL, Mappers.getMapper(SupplierMapper.class).toDto(saved.getValue()).getShippingMode());
    }

    @Test
    void omittedModeKeepsLegacyStandardBehavior() throws Exception {
        service.createSupplierShipping(request(ShippingMethod.SEA, null, "TB001"));
        ArgumentCaptor<SupplierShippingEntity> saved = ArgumentCaptor.forClass(SupplierShippingEntity.class);
        verify(repository).save(saved.capture());
        assertEquals(ShippingMode.STANDARD, saved.getValue().getShippingMode());
    }

    @Test
    void updatesModeAndCodeTogether() throws Exception {
        SupplierShippingEntity existing = new SupplierShippingEntity();
        existing.setId(1L);
        when(repository.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(existing));

        service.updateSupplierShipping(1L, request(ShippingMethod.SEA, ShippingMode.FCL, "TZ002"));

        assertEquals(ShippingMode.FCL, existing.getShippingMode());
        assertEquals("TZ002", existing.getCarCode());
        verify(repository).save(existing);
    }

    @ParameterizedTest
    @MethodSource("invalidMasters")
    void rejectsInconsistentMasterRecords(ShippingMethod method, ShippingMode mode, String code) {
        assertThrows(InvalidRequestException.class, () -> service.createSupplierShipping(request(method, mode, code)));
        verifyNoInteractions(repository);
    }

    private static Stream<Arguments> invalidMasters() {
        return Stream.of(
                Arguments.of(ShippingMethod.SEA, ShippingMode.FCL, "TB001"),
                Arguments.of(ShippingMethod.SEA, ShippingMode.STANDARD, "TZ001"),
                Arguments.of(ShippingMethod.SEA, ShippingMode.FCL, null),
                Arguments.of(ShippingMethod.SEA, ShippingMode.FCL, "TZ"),
                Arguments.of(ShippingMethod.LAND, ShippingMode.FCL, "TZ001"),
                Arguments.of(ShippingMethod.SEA_FCL_20GP, ShippingMode.FCL, "TZ001"),
                Arguments.of(ShippingMethod.AIR, ShippingMode.STANDARD, "AIR001")
        );
    }

    private static UpsertSupplierShippingRequest request(ShippingMethod method, ShippingMode mode, String code) {
        UpsertSupplierShippingRequest request = new UpsertSupplierShippingRequest();
        request.setShippingMethod(method);
        request.setShippingMode(mode);
        request.setCarCode(code);
        UpsertSupplierShippingDestinationRequest destination = new UpsertSupplierShippingDestinationRequest();
        destination.setDestinationName("Bangkok");
        request.setDestinations(List.of(destination));
        return request;
    }
}
