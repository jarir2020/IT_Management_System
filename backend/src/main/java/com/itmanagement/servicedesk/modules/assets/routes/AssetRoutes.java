package com.itmanagement.servicedesk.modules.assets.routes;

/** API paths owned by the Asset Management module. */
public final class AssetRoutes {
    public static final String BASE_PATH = "/api/v1/assets";
    public static final String BY_ID = "/{assetId}";
    public static final String ASSIGNMENTS = BY_ID + "/assignments";
    public static final String RETURN = ASSIGNMENTS + "/{assignmentId}/return";
    public static final String MAINTENANCE = BY_ID + "/maintenance";
    public static final String MAINTENANCE_BY_ID = MAINTENANCE + "/{maintenanceId}";
    public static final String DOCUMENTS = BY_ID + "/documents";
    public static final String DOCUMENT_DOWNLOAD = DOCUMENTS + "/{documentId}/download";
    public static final String DOCUMENT_BY_ID = DOCUMENTS + "/{documentId}";
    public static final String USERS = "/users";
    public static final String RETIRE = BY_ID + "/retire";

    private AssetRoutes() {
    }
}
