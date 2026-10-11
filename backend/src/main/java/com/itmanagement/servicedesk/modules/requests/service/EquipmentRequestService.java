package com.itmanagement.servicedesk.modules.requests.service;

import com.itmanagement.servicedesk.modules.assets.model.AssetDetailResponse;
import com.itmanagement.servicedesk.modules.assets.service.AssetService;
import com.itmanagement.servicedesk.modules.requests.model.EquipmentRequest;
import com.itmanagement.servicedesk.modules.requests.model.EquipmentRequestCreateRequest;
import com.itmanagement.servicedesk.modules.requests.model.EquipmentRequestNote;
import com.itmanagement.servicedesk.modules.requests.model.EquipmentRequestProcurementRequest;
import com.itmanagement.servicedesk.modules.requests.model.EquipmentRequestResponse;
import com.itmanagement.servicedesk.modules.requests.model.EquipmentRequestStatus;
import com.itmanagement.servicedesk.modules.requests.model.RequestUserSummary;
import com.itmanagement.servicedesk.modules.requests.repository.EquipmentRequestRepository;
import com.itmanagement.servicedesk.modules.users.model.UserAccount;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class EquipmentRequestService {
    private final EquipmentRequestRepository requests;
    private final AssetService assets;

    public EquipmentRequestService(EquipmentRequestRepository requests, AssetService assets) {
        this.requests = requests;
        this.assets = assets;
    }

    @Transactional(readOnly = true)
    public List<EquipmentRequestResponse> list(UserAccount actor) {
        if (hasAuthority(actor, "requests:read_all")) {
            return requests.findAllDetailed().stream().map(this::toResponse).toList();
        }
        if (hasAuthority(actor, "requests:approve_department") && actor.getDepartment() != null) {
            return requests.findAllForDepartment(actor.getDepartment().getId()).stream().map(this::toResponse).toList();
        }
        if (hasAuthority(actor, "requests:read_own")) {
            return requests.findAllForRequester(actor.getId()).stream().map(this::toResponse).toList();
        }
        throw forbidden("You cannot view equipment requests");
    }

    @Transactional(readOnly = true)
    public EquipmentRequestResponse get(Long requestId, UserAccount actor) {
        return toResponse(findReadable(requestId, actor));
    }

    @Transactional
    public EquipmentRequestResponse create(EquipmentRequestCreateRequest input, UserAccount actor) {
        requireAuthority(actor, "requests:create");
        EquipmentRequest request = new EquipmentRequest(actor, actor.getDepartment(), input.category(),
                input.itemName().trim(), input.justification().trim());
        return toResponse(requests.save(request));
    }

    @Transactional
    public EquipmentRequestResponse approve(Long requestId, EquipmentRequestNote input, UserAccount actor) {
        EquipmentRequest request = findLocked(requestId);
        requireReviewAccess(request, actor);
        ensureNotOwnRequest(request, actor);
        requireStatus(request, EquipmentRequestStatus.PENDING_APPROVAL);
        request.approve(actor, clean(input == null ? null : input.note()));
        return toResponse(request);
    }

    @Transactional
    public EquipmentRequestResponse reject(Long requestId, EquipmentRequestNote input, UserAccount actor) {
        EquipmentRequest request = findLocked(requestId);
        requireReviewAccess(request, actor);
        ensureNotOwnRequest(request, actor);
        requireStatus(request, EquipmentRequestStatus.PENDING_APPROVAL);
        request.reject(actor, clean(input.note()));
        return toResponse(request);
    }

    @Transactional
    public EquipmentRequestResponse sendToProcurement(Long requestId, EquipmentRequestNote input,
                                                       UserAccount actor) {
        requireAuthority(actor, "requests:allocate");
        EquipmentRequest request = findLocked(requestId);
        requireStatus(request, EquipmentRequestStatus.APPROVED);
        request.sendToProcurement(actor, input.note().trim());
        return toResponse(request);
    }

    @Transactional
    public EquipmentRequestResponse recordPurchase(Long requestId, EquipmentRequestProcurementRequest input,
                                                    UserAccount actor) {
        requireAuthority(actor, "requests:procure");
        EquipmentRequest request = findLocked(requestId);
        requireStatus(request, EquipmentRequestStatus.PENDING_PROCUREMENT);
        request.recordPurchase(actor, input.vendor().trim(), clean(input.purchaseReference()), input.cost(),
                input.expectedDeliveryDate(), clean(input.notes()));
        return toResponse(request);
    }

    @Transactional
    public EquipmentRequestResponse markReceived(Long requestId, EquipmentRequestNote input, UserAccount actor) {
        requireAuthority(actor, "requests:procure");
        EquipmentRequest request = findLocked(requestId);
        requireStatus(request, EquipmentRequestStatus.PURCHASING);
        request.markReceived(actor, input == null ? null : clean(input.note()));
        return toResponse(request);
    }

    @Transactional
    public EquipmentRequestResponse allocate(Long requestId, Long assetId, String note, UserAccount actor) {
        requireAuthority(actor, "requests:allocate");
        EquipmentRequest request = findLocked(requestId);
        if (request.getStatus() != EquipmentRequestStatus.APPROVED
                && request.getStatus() != EquipmentRequestStatus.READY_FOR_ALLOCATION) {
            throw conflict("Only approved requests with inventory ready can be allocated");
        }
        AssetDetailResponse allocation = assets.assignAssetForRequest(assetId, request.getRequester().getId(),
                request.getId(), request.getCategory(), clean(note), actor);
        request.fulfill(allocation.id());
        return toResponse(request);
    }

    @Transactional
    public EquipmentRequestResponse cancel(Long requestId, UserAccount actor) {
        requireAuthority(actor, "requests:cancel");
        EquipmentRequest request = findLocked(requestId);
        if (!request.getRequester().getId().equals(actor.getId())) {
            throw notFound("Equipment request not found");
        }
        requireStatus(request, EquipmentRequestStatus.PENDING_APPROVAL);
        request.cancel();
        return toResponse(request);
    }

    private EquipmentRequest findReadable(Long requestId, UserAccount actor) {
        EquipmentRequest request = requests.findDetailedById(requestId)
                .orElseThrow(() -> notFound("Equipment request not found"));
        boolean canReadAll = hasAuthority(actor, "requests:read_all");
        boolean ownRequest = request.getRequester().getId().equals(actor.getId());
        boolean sameDepartment = hasAuthority(actor, "requests:approve_department")
                && actor.getDepartment() != null && request.getDepartment() != null
                && request.getDepartment().getId().equals(actor.getDepartment().getId());
        if (!canReadAll && !ownRequest && !sameDepartment) {
            throw notFound("Equipment request not found");
        }
        return request;
    }

    private EquipmentRequest findLocked(Long requestId) {
        return requests.findByIdForUpdate(requestId)
                .orElseThrow(() -> notFound("Equipment request not found"));
    }

    private void requireReviewAccess(EquipmentRequest request, UserAccount actor) {
        if (hasAuthority(actor, "requests:approve_all")) return;
        boolean sameDepartment = hasAuthority(actor, "requests:approve_department")
                && actor.getDepartment() != null && request.getDepartment() != null
                && request.getDepartment().getId().equals(actor.getDepartment().getId());
        if (!sameDepartment) throw notFound("Equipment request not found");
    }

    private void ensureNotOwnRequest(EquipmentRequest request, UserAccount actor) {
        if (request.getRequester().getId().equals(actor.getId())) {
            throw forbidden("You cannot approve or reject your own equipment request");
        }
    }

    private static void requireStatus(EquipmentRequest request, EquipmentRequestStatus expected) {
        if (request.getStatus() != expected) {
            throw conflict("This request must be " + expected.name().replace('_', ' ').toLowerCase()
                    + " before this action");
        }
    }

    private EquipmentRequestResponse toResponse(EquipmentRequest request) {
        return new EquipmentRequestResponse(request.getId(), request.getCategory(), request.getItemName(),
                request.getJustification(), request.getStatus(), toUserSummary(request.getRequester()),
                request.getDepartment() == null ? null : request.getDepartment().getId(),
                request.getDepartment() == null ? null : request.getDepartment().getName(),
                request.getDecisionNote(), toUserSummary(request.getReviewedBy()), request.getDecidedAt(),
                request.getProcurementVendor(), request.getPurchaseReference(), request.getPurchaseCost(),
                request.getExpectedDeliveryDate(), request.getProcurementNotes(),
                toUserSummary(request.getProcurementUpdatedBy()), request.getReceivedAt(),
                request.getAllocatedAssetId(), request.getFulfilledAt(), request.getCreatedAt(), request.getUpdatedAt());
    }

    private static RequestUserSummary toUserSummary(UserAccount user) {
        if (user == null) return null;
        return new RequestUserSummary(user.getId(), user.getFirstName(), user.getLastName(), user.getEmail(),
                user.getDepartment() == null ? null : user.getDepartment().getName());
    }

    private static boolean hasAuthority(UserAccount actor, String authority) {
        return actor.getAuthorities().stream().map(GrantedAuthority::getAuthority).anyMatch(authority::equals);
    }

    private static void requireAuthority(UserAccount actor, String authority) {
        if (!hasAuthority(actor, authority)) throw forbidden("You cannot perform this equipment request action");
    }

    private static String clean(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static ResponseStatusException forbidden(String message) {
        return new ResponseStatusException(HttpStatus.FORBIDDEN, message);
    }

    private static ResponseStatusException notFound(String message) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, message);
    }

    private static ResponseStatusException conflict(String message) {
        return new ResponseStatusException(HttpStatus.CONFLICT, message);
    }
}
