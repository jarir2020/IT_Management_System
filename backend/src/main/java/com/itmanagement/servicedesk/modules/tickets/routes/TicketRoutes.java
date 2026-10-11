package com.itmanagement.servicedesk.modules.tickets.routes;

/** API paths owned by the Tickets module. */
public final class TicketRoutes {
    public static final String BASE_PATH = "/api/v1/tickets";
    public static final String BY_ID = "/{ticketId}";
    public static final String AGENTS = "/agents";
    public static final String STATUS = BY_ID + "/status";
    public static final String ASSIGNEE = BY_ID + "/assignee";
    public static final String COMMENTS = BY_ID + "/comments";
    public static final String ATTACHMENTS = BY_ID + "/attachments";
    public static final String ATTACHMENT_DOWNLOAD = ATTACHMENTS + "/{attachmentId}/download";
    public static final String ATTACHMENT_BY_ID = ATTACHMENTS + "/{attachmentId}";

    private TicketRoutes() {
    }
}
