package com.itmanagement.servicedesk.modules.auth.routes;

/** Route names shared by the authentication controller and security configuration. */
public final class AuthRoutes {
    public static final String BASE_PATH = "/api/v1/auth";
    public static final String CSRF = "/csrf";
    public static final String LOGIN = "/login";
    public static final String ME = "/me";
    public static final String LOGOUT = "/logout";

    private AuthRoutes() {
    }
}
