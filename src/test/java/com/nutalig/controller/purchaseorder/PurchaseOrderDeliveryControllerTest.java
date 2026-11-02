package com.nutalig.controller.purchaseorder;

import com.nutalig.constant.PurchaseOrderDeliveryStatus;
import com.nutalig.constant.DeliveryContainerStatus;
import com.nutalig.controller.purchaseorder.request.SearchPurchaseOrderDeliveryRequest;
import com.nutalig.controller.request.PageableRequest;
import com.nutalig.controller.response.Pageable;
import com.nutalig.service.PurchaseOrderDeliverySearchService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.context.annotation.*;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.time.LocalDate;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class PurchaseOrderDeliveryControllerTest {
    @Test void bindsDatesStatusAndPaginationToDeliverySearch() throws Exception {
        var service=mock(PurchaseOrderDeliverySearchService.class);
        when(service.search(any(),any())).thenReturn(new Pageable<>());
        var mvc=MockMvcBuilders.standaloneSetup(new PurchaseOrderDeliveryController(service)).build();
        mvc.perform(post("/v1/purchase-orders/deliveries/search").param("page","2").param("size","20")
                .contentType(MediaType.APPLICATION_JSON).content("""
                {"purchaseOrderNo":"PO-1","deliveryNo":"D001","supplierName":"Factory",
                 "containerStatus":"PARTIALLY_ASSIGNED","status":"IN_TRANSIT","shippingMethod":"SEA","shippedDateStart":"2026-10-24",
                 "shippedDateEnd":"2026-10-25","expectedWarehouseDateEnd":"2026-10-30"}
                """)).andExpect(status().isOk());
        var request=ArgumentCaptor.forClass(SearchPurchaseOrderDeliveryRequest.class);
        var page=ArgumentCaptor.forClass(PageableRequest.class);
        verify(service).search(request.capture(),page.capture());
        assertEquals("PO-1",request.getValue().getPurchaseOrderNo());
        assertEquals(PurchaseOrderDeliveryStatus.IN_TRANSIT,request.getValue().getStatus());
        assertEquals(DeliveryContainerStatus.PARTIALLY_ASSIGNED,request.getValue().getContainerStatus());
        assertEquals(LocalDate.of(2026,10,24),request.getValue().getShippedDateStart());
        assertEquals(2,page.getValue().getPage()); assertEquals(20,page.getValue().getSize());
    }
    @ParameterizedTest
    @ValueSource(strings={"{\"containerStatus\":\"INVALID\"}","{\"status\":\"INVALID\"}","{\"shippedDateStart\":\"invalid-date\"}"})
    void rejectsMalformedCriteria(String json) throws Exception {
        var service=mock(PurchaseOrderDeliverySearchService.class);
        var mvc=MockMvcBuilders.standaloneSetup(new PurchaseOrderDeliveryController(service)).build();
        mvc.perform(post("/v1/purchase-orders/deliveries/search").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }
    @Test void enforcesTheSameViewPermissionAsTheMenu() throws Exception {
        try(var context=new AnnotationConfigApplicationContext(SecurityConfig.class)) {
            var controller=context.getBean(PurchaseOrderDeliveryController.class);
            var service=context.getBean(PurchaseOrderDeliverySearchService.class);
            SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("USER",null,List.of(new SimpleGrantedAuthority("PERM_PURCHASE_ORDER_CREATE"))));
            assertThrows(AccessDeniedException.class,()->controller.search(null,new PageableRequest()));
            verifyNoInteractions(service);
            SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("USER",null,List.of(new SimpleGrantedAuthority("PERM_PURCHASE_ORDER_VIEW"))));
            assertNotNull(controller.search(null,new PageableRequest()));
            verify(service).search(isNull(),any());
        } finally { SecurityContextHolder.clearContext(); }
    }
    @Configuration
    @EnableMethodSecurity
    static class SecurityConfig {
        @Bean PurchaseOrderDeliverySearchService service() { return mock(PurchaseOrderDeliverySearchService.class); }
        @Bean PurchaseOrderDeliveryController controller(PurchaseOrderDeliverySearchService service) { return new PurchaseOrderDeliveryController(service); }
    }
}
