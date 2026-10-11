package com.itmanagement.servicedesk.modules.users.routes;

/** Route names owned by the user management module. */
public final class UserRoutes {
    public static final String BASE_PATH = "/api/v1/users";
    public static final String BY_ID = "/{id}";
    public static final String STATUS = BY_ID + "/status";

    private UserRoutes() {
    }
}
