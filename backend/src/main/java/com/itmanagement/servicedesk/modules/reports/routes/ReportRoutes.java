package com.itmanagement.servicedesk.modules.reports.routes;

/** API paths owned by the Reports and Data Exchange module. */
public final class ReportRoutes {
    public static final String BASE_PATH = "/api/v1/reports";
    public static final String ASSETS_XLSX = "/assets.xlsx";
    public static final String ASSETS_PDF = "/assets.pdf";
    public static final String TICKETS_XLSX = "/tickets.xlsx";
    public static final String TICKETS_PDF = "/tickets.pdf";
    public static final String REQUESTS_XLSX = "/requests.xlsx";
    public static final String REQUESTS_PDF = "/requests.pdf";
    public static final String BUNDLE_ZIP = "/bundle.zip";
    public static final String ASSET_IMPORT = "/assets/import";
    public static final String ASSET_IMPORT_TEMPLATE = "/assets/import-template.xlsx";

    private ReportRoutes() {
    }
}
