package com.itmanagement.servicedesk.modules.requests.controller;

import com.itmanagement.servicedesk.modules.requests.model.EquipmentRequestAllocationRequest;
import com.itmanagement.servicedesk.modules.requests.model.EquipmentRequestCreateRequest;
import com.itmanagement.servicedesk.modules.requests.model.EquipmentRequestNote;
import com.itmanagement.servicedesk.modules.requests.model.EquipmentRequestProcurementRequest;
import com.itmanagement.servicedesk.modules.requests.model.EquipmentRequestResponse;
import com.itmanagement.servicedesk.modules.requests.routes.EquipmentRequestRoutes;
import com.itmanagement.servicedesk.modules.requests.service.EquipmentRequestService;
import com.itmanagement.servicedesk.modules.users.model.UserAccount;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(EquipmentRequestRoutes.BASE_PATH)
public class EquipmentRequestController {
    private final EquipmentRequestService requests;

    public EquipmentRequestController(EquipmentRequestService requests) {
        this.requests = requests;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('requests:read_all') or hasAuthority('requests:read_own') or hasAuthority('requests:approve_department')")
    public List<EquipmentRequestResponse> list(@AuthenticationPrincipal UserAccount actor) {
        return requests.list(actor);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('requests:create')")
    public EquipmentRequestResponse create(@Valid @RequestBody EquipmentRequestCreateRequest input,
                                           @AuthenticationPrincipal UserAccount actor) {
        return requests.create(input, actor);
    }

    @GetMapping(EquipmentRequestRoutes.BY_ID)
    @PreAuthorize("hasAuthority('requests:read_all') or hasAuthority('requests:read_own') or hasAuthority('requests:approve_department')")
    public EquipmentRequestResponse get(@PathVariable Long requestId,
                                        @AuthenticationPrincipal UserAccount actor) {
        return requests.get(requestId, actor);
    }

    @PostMapping(EquipmentRequestRoutes.APPROVE)
    @PreAuthorize("hasAuthority('requests:approve_department') or hasAuthority('requests:approve_all')")
    public EquipmentRequestResponse approve(@PathVariable Long requestId,
            @RequestBody(required = false) EquipmentRequestNote input,
            @AuthenticationPrincipal UserAccount actor) {
        return requests.approve(requestId, input, actor);
    }

    @PostMapping(EquipmentRequestRoutes.REJECT)
    @PreAuthorize("hasAuthority('requests:approve_department') or hasAuthority('requests:approve_all')")
    public EquipmentRequestResponse reject(@PathVariable Long requestId, @Valid @RequestBody EquipmentRequestNote input,
            @AuthenticationPrincipal UserAccount actor) {
        return requests.reject(requestId, input, actor);
    }

    @PostMapping(EquipmentRequestRoutes.PROCUREMENT_NEEDED)
    @PreAuthorize("hasAuthority('requests:allocate')")
    public EquipmentRequestResponse sendToProcurement(@PathVariable Long requestId,
            @Valid @RequestBody EquipmentRequestNote input, @AuthenticationPrincipal UserAccount actor) {
        return requests.sendToProcurement(requestId, input, actor);
    }

    @PostMapping(EquipmentRequestRoutes.PURCHASE)
    @PreAuthorize("hasAuthority('requests:procure')")
    public EquipmentRequestResponse recordPurchase(@PathVariable Long requestId,
            @Valid @RequestBody EquipmentRequestProcurementRequest input,
            @AuthenticationPrincipal UserAccount actor) {
        return requests.recordPurchase(requestId, input, actor);
    }

    @PostMapping(EquipmentRequestRoutes.RECEIVED)
    @PreAuthorize("hasAuthority('requests:procure')")
    public EquipmentRequestResponse markReceived(@PathVariable Long requestId,
            @RequestBody(required = false) EquipmentRequestNote input,
            @AuthenticationPrincipal UserAccount actor) {
        return requests.markReceived(requestId, input, actor);
    }

    @PostMapping(EquipmentRequestRoutes.ALLOCATE)
    @PreAuthorize("hasAuthority('requests:allocate')")
    public EquipmentRequestResponse allocate(@PathVariable Long requestId,
            @Valid @RequestBody EquipmentRequestAllocationRequest input,
            @AuthenticationPrincipal UserAccount actor) {
        return requests.allocate(requestId, input.assetId(), input.note(), actor);
    }

    @PostMapping(EquipmentRequestRoutes.CANCEL)
    @PreAuthorize("hasAuthority('requests:cancel')")
    public EquipmentRequestResponse cancel(@PathVariable Long requestId,
                                           @AuthenticationPrincipal UserAccount actor) {
        return requests.cancel(requestId, actor);
    }
}
