package com.itmanagement.servicedesk.modules.requests.routes;

/** API paths owned by the Equipment Requests module. */
public final class EquipmentRequestRoutes {
    public static final String BASE_PATH = "/api/v1/requests";
    public static final String BY_ID = "/{requestId}";
    public static final String APPROVE = BY_ID + "/approve";
    public static final String REJECT = BY_ID + "/reject";
    public static final String PROCUREMENT_NEEDED = BY_ID + "/procurement-needed";
    public static final String PURCHASE = BY_ID + "/purchase";
    public static final String RECEIVED = BY_ID + "/received";
    public static final String ALLOCATE = BY_ID + "/allocate";
    public static final String CANCEL = BY_ID + "/cancel";

    private EquipmentRequestRoutes() {
    }
}
