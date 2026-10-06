package com.nutalig.service;

import com.nutalig.constant.QuotationStatus;
import com.nutalig.constant.ShippingMethod;
import com.nutalig.dto.QuotationRequestDto;
import com.nutalig.entity.QuotationEntity;
import com.nutalig.mapper.CustomerMapper;
import com.nutalig.mapper.EmployeeMapper;
import com.nutalig.mapper.RequestPriceHeaderMapper;
import com.nutalig.repository.QuotationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QuotationShippingServiceTest {
    @Mock private QuotationRepository repository;
    @Mock private UserProfileService userProfileService;
    @Mock private CustomerMapper customerMapper;
    @Mock private EmployeeMapper employeeMapper;
    @Mock private RequestPriceHeaderMapper requestPriceHeaderMapper;
    @Mock private ActivityHistoryService activityHistoryService;
    @InjectMocks private QuotationService service;
    private QuotationEntity quotation;

    @BeforeEach
    void setUp() {
        quotation = new QuotationEntity();
        quotation.setQuotationNo("QT-TEST");
        quotation.setStatus(QuotationStatus.DRAFT);
        quotation.setDocDate(LocalDate.of(2026, 10, 6));
        quotation.setExpireDate(LocalDate.of(2026, 10, 20));
        when(repository.findById("QT-TEST")).thenReturn(Optional.of(quotation));
    }

    @ParameterizedTest
    @EnumSource(ShippingMethod.class)
    void editingQuotationPreservesDetailedShippingMethod(ShippingMethod method) throws Exception {
        when(repository.save(quotation)).thenReturn(quotation);
        QuotationRequestDto request = new QuotationRequestDto();
        request.setShipping(" " + method.name().toLowerCase(java.util.Locale.ROOT) + " ");

        assertEquals(method.name(), service.updateQuotation("QT-TEST", request, "user").getShipping());
        assertEquals(method.name(), quotation.getShipping());
    }

    @Test
    void allStillSupported() throws Exception {
        when(repository.save(quotation)).thenReturn(quotation);
        QuotationRequestDto request = new QuotationRequestDto();
        request.setShipping("ALL");
        assertEquals("ALL", service.updateQuotation("QT-TEST", request, "user").getShipping());
    }

    @Test
    void unknownShippingIsRejectedBeforeSave() {
        QuotationRequestDto request = new QuotationRequestDto();
        request.setShipping("SEA_FCL_UNKNOWN");
        assertThrows(IllegalArgumentException.class, () -> service.updateQuotation("QT-TEST", request, "user"));
        verify(repository, never()).save(any());
    }
}
