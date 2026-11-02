package com.nutalig.controller.container;
import com.nutalig.service.ShippingContainerService;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.*;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
class ShippingContainerControllerTest {
    void permission(String code){SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("USER",null,List.of(new SimpleGrantedAuthority(code))));}
    @Test void viewCreateEditAndCancelAreIndependentlyAuthorized()throws Exception {
        try(var context=new AnnotationConfigApplicationContext(Config.class)) {
            var controller=context.getBean(ShippingContainerController.class);var service=context.getBean(ShippingContainerService.class);
            permission("PERM_CONTAINER_VIEW");assertNotNull(controller.get(1L));
            assertThrows(AccessDeniedException.class,()->controller.create(new ShippingContainerRequest(),"USER"));
            assertThrows(AccessDeniedException.class,()->controller.add(1L,new ShippingContainerRequest.Load(),"USER"));
            permission("PERM_CONTAINER_CREATE");assertNotNull(controller.previewSources("20GP",1L,2L,3L));assertNotNull(controller.create(new ShippingContainerRequest(),"USER"));
            assertThrows(AccessDeniedException.class,()->controller.update(1L,new ShippingContainerRequest(),"USER"));
            permission("PERM_CONTAINER_EDIT");assertNotNull(controller.action(1L,"SEAL",new ShippingContainerRequest.Action(),"USER"));
            assertThrows(AccessDeniedException.class,()->controller.action(1L,"CANCEL",new ShippingContainerRequest.Action(),"USER"));
            permission("PERM_CONTAINER_DELETE");assertThrows(AccessDeniedException.class,()->controller.previewSources("20GP",1L,2L,3L));assertNotNull(controller.action(1L,"CANCEL",new ShippingContainerRequest.Action(),"USER"));
            assertThrows(AccessDeniedException.class,()->controller.action(1L,"REOPEN",new ShippingContainerRequest.Action(),"USER"));
            verify(service).action(eq(1L),eq("CANCEL"),any(),eq("USER"));
        }finally{SecurityContextHolder.clearContext();}
    }
    @Configuration @EnableMethodSecurity static class Config {
        @Bean ShippingContainerService service(){return mock(ShippingContainerService.class);}
        @Bean ShippingContainerController controller(ShippingContainerService service){return new ShippingContainerController(service);}
    }
}
