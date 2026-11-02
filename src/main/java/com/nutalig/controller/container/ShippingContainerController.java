package com.nutalig.controller.container;
import com.nutalig.constant.ShippingContainerStatus;
import com.nutalig.controller.request.PageableRequest;
import com.nutalig.controller.response.*;
import com.nutalig.dto.ShippingContainerDto;
import com.nutalig.exception.*;
import com.nutalig.service.ShippingContainerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import static com.nutalig.constant.ResponseStatus.SUCCESS;

@RestController @RequiredArgsConstructor @RequestMapping("/v1/shipping-containers")
public class ShippingContainerController {
    private final ShippingContainerService service;
    @GetMapping @PreAuthorize("hasAuthority('PERM_CONTAINER_VIEW')")
    public GeneralResponse<Pageable<ShippingContainerDto>> search(@RequestParam(required=false)String keyword,
            @RequestParam(required=false)ShippingContainerStatus status,@RequestParam(required=false)String containerSize,PageableRequest pageable) throws InvalidRequestException {
        return new GeneralResponse<>(SUCCESS,service.search(keyword,status,containerSize,pageable));
    }
    @GetMapping("/{id}") @PreAuthorize("hasAuthority('PERM_CONTAINER_VIEW')")
    public GeneralResponse<ShippingContainerDto> get(@PathVariable Long id) throws DataNotFoundException { return new GeneralResponse<>(SUCCESS,service.get(id)); }
    @PostMapping @PreAuthorize("hasAuthority('PERM_CONTAINER_CREATE')")
    public GeneralResponse<ShippingContainerDto> create(@Valid @RequestBody ShippingContainerRequest request,@RequestHeader("userId")String user) throws InvalidRequestException {
        return new GeneralResponse<>(SUCCESS,service.create(request,user));
    }
    @PutMapping("/{id}") @PreAuthorize("hasAuthority('PERM_CONTAINER_EDIT')")
    public GeneralResponse<ShippingContainerDto> update(@PathVariable Long id,@Valid @RequestBody ShippingContainerRequest request,@RequestHeader("userId")String user) throws DataNotFoundException,InvalidRequestException {
        return new GeneralResponse<>(SUCCESS,service.update(id,request,user));
    }
    @GetMapping("/sources") @PreAuthorize("hasAnyAuthority('PERM_CONTAINER_CREATE','PERM_CONTAINER_VIEW','PERM_CONTAINER_EDIT')")
    public GeneralResponse<List<ShippingContainerDto.Source>> previewSources(@RequestParam String containerSize,
            @RequestParam Long supplierShippingId,@RequestParam Long destinationId,@RequestParam Long deliveryId)
            throws DataNotFoundException,InvalidRequestException {
        return new GeneralResponse<>(SUCCESS,service.previewSources(containerSize,supplierShippingId,destinationId,deliveryId));
    }
    @GetMapping("/{id}/sources") @PreAuthorize("hasAuthority('PERM_CONTAINER_VIEW')")
    public GeneralResponse<List<ShippingContainerDto.Source>> sources(@PathVariable Long id,@RequestParam Long deliveryId) throws DataNotFoundException,InvalidRequestException {
        return new GeneralResponse<>(SUCCESS,service.sources(id,deliveryId));
    }
    @PostMapping("/{id}/loads") @PreAuthorize("hasAuthority('PERM_CONTAINER_EDIT')")
    public GeneralResponse<ShippingContainerDto> add(@PathVariable Long id,@Valid @RequestBody ShippingContainerRequest.Load request,@RequestHeader("userId")String user) throws DataNotFoundException,InvalidRequestException {
        return new GeneralResponse<>(SUCCESS,service.addLoad(id,request,user));
    }
    @DeleteMapping("/{id}/loads/{loadId}") @PreAuthorize("hasAuthority('PERM_CONTAINER_EDIT')")
    public GeneralResponse<ShippingContainerDto> remove(@PathVariable Long id,@PathVariable Long loadId,@RequestParam Long version,@RequestHeader("userId")String user) throws DataNotFoundException,InvalidRequestException {
        return new GeneralResponse<>(SUCCESS,service.removeLoad(id,loadId,version,user));
    }
    @PostMapping("/{id}/actions/{action}")
    @PreAuthorize("(#action == 'CANCEL' and hasAuthority('PERM_CONTAINER_DELETE')) or (#action != 'CANCEL' and hasAuthority('PERM_CONTAINER_EDIT'))")
    public GeneralResponse<ShippingContainerDto> action(@PathVariable Long id,@PathVariable String action,
            @Valid @RequestBody ShippingContainerRequest.Action request,@RequestHeader("userId")String user) throws DataNotFoundException,InvalidRequestException {
        return new GeneralResponse<>(SUCCESS,service.action(id,action,request,user));
    }
}
