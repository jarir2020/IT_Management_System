package com.itmanagement.servicedesk.modules.reports.service;

import com.itmanagement.servicedesk.modules.assets.model.Asset;
import com.itmanagement.servicedesk.modules.assets.model.AssetCategory;
import com.itmanagement.servicedesk.modules.assets.model.AssetRequest;
import com.itmanagement.servicedesk.modules.assets.repository.AssetMaintenanceCostProjection;
import com.itmanagement.servicedesk.modules.assets.repository.AssetMaintenanceRepository;
import com.itmanagement.servicedesk.modules.assets.repository.AssetRepository;
import com.itmanagement.servicedesk.modules.assets.service.AssetService;
import com.itmanagement.servicedesk.modules.requests.model.EquipmentRequest;
import com.itmanagement.servicedesk.modules.requests.repository.EquipmentRequestRepository;
import com.itmanagement.servicedesk.modules.reports.model.AssetImportIssue;
import com.itmanagement.servicedesk.modules.reports.model.AssetImportResult;
import com.itmanagement.servicedesk.modules.tickets.model.Ticket;
import com.itmanagement.servicedesk.modules.tickets.repository.TicketRepository;
import com.itmanagement.servicedesk.modules.users.model.UserAccount;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ReportService {
    private static final int MAX_IMPORT_ROWS = 1000;
    private static final long MAX_IMPORT_BYTES = 10L * 1024L * 1024L;
    private static final String[] ASSET_HEADERS = {
            "Asset ID", "Asset tag", "Name", "Category", "Status", "Manufacturer", "Model number",
            "Serial number", "Purchase date", "Purchase cost", "Warranty expires", "Assigned to",
            "Useful life (months)", "Salvage value", "Accumulated depreciation", "Net book value",
            "Maintenance expenses"
    };
    private static final String[] ASSET_PDF_HEADERS = {
            "Asset", "Status", "Purchase cost", "Accumulated depreciation", "Net book value", "Maintenance expenses"
    };
    private static final String[] TICKET_HEADERS = {
            "Ticket ID", "Title", "Requester", "Assignee", "Category", "Priority", "Status", "Created", "Updated"
    };
    private static final String[] REQUEST_HEADERS = {
            "Request ID", "Item", "Category", "Requester", "Department", "Status", "Vendor",
            "Purchase reference", "Cost", "Expected delivery", "Allocated asset ID", "Created"
    };
    private static final String[] IMPORT_HEADERS = {
            "asset_tag", "name", "category", "description", "manufacturer", "model_number",
            "serial_number", "purchase_date", "purchase_cost", "warranty_expires_on",
            "useful_life_months", "salvage_value"
    };

    private final AssetRepository assets;
    private final TicketRepository tickets;
    private final EquipmentRequestRepository requests;
    private final AssetMaintenanceRepository maintenance;
    private final AssetService assetService;
    private final Validator validator;

    public ReportService(AssetRepository assets, TicketRepository tickets,
                         EquipmentRequestRepository requests, AssetMaintenanceRepository maintenance,
                         AssetService assetService,
                         Validator validator) {
        this.assets = assets;
        this.tickets = tickets;
        this.requests = requests;
        this.maintenance = maintenance;
        this.assetService = assetService;
        this.validator = validator;
    }

    public byte[] assetsXlsx(UserAccount actor) {
        return xlsx("Assets", ASSET_HEADERS, assetRows(actor));
    }

    public byte[] assetsPdf(UserAccount actor) {
        return pdf("Asset Inventory Report", ASSET_PDF_HEADERS, assetPdfRows(actor));
    }

    public byte[] ticketsXlsx(UserAccount actor) {
        return xlsx("Tickets", TICKET_HEADERS, ticketRows(actor));
    }

    public byte[] ticketsPdf(UserAccount actor) {
        return pdf("Service Ticket Report", TICKET_HEADERS, ticketRows(actor));
    }

    public byte[] requestsXlsx(UserAccount actor) {
        return xlsx("Requests", REQUEST_HEADERS, requestRows(actor));
    }

    public byte[] requestsPdf(UserAccount actor) {
        return pdf("Equipment Request Report", REQUEST_HEADERS, requestRows(actor));
    }

    public byte[] bundleZip(UserAccount actor) {
        try (ByteArrayOutputStream output = new ByteArrayOutputStream();
             ZipOutputStream zip = new ZipOutputStream(output)) {
            boolean included = false;
            if (hasAuthority(actor, "reports:assets")) {
                List<String[]> rows = assetRows(actor);
                addEntry(zip, "assets.xlsx", xlsx("Assets", ASSET_HEADERS, rows));
                addEntry(zip, "assets.pdf", pdf("Asset Inventory Report", ASSET_PDF_HEADERS, assetPdfRows(actor)));
                included = true;
            }
            if (hasAuthority(actor, "reports:tickets")) {
                List<String[]> rows = ticketRows(actor);
                addEntry(zip, "tickets.xlsx", xlsx("Tickets", TICKET_HEADERS, rows));
                addEntry(zip, "tickets.pdf", pdf("Service Ticket Report", TICKET_HEADERS, rows));
                included = true;
            }
            if (hasAuthority(actor, "reports:requests") || hasAuthority(actor, "reports:requests_department")) {
                List<String[]> rows = requestRows(actor);
                addEntry(zip, "requests.xlsx", xlsx("Requests", REQUEST_HEADERS, rows));
                addEntry(zip, "requests.pdf", pdf("Equipment Request Report", REQUEST_HEADERS, rows));
                included = true;
            }
            if (!included) throw forbidden("You do not have access to any reports");
            zip.finish();
            return output.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("Could not create the report bundle", exception);
        }
    }

    public byte[] assetImportTemplate() {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Assets");
            writeHeader(workbook, sheet, IMPORT_HEADERS);
            for (int column = 0; column < IMPORT_HEADERS.length; column++) sheet.setColumnWidth(column, 24 * 256);

            Sheet instructions = workbook.createSheet("Instructions");
            instructions.createRow(0).createCell(0).setCellValue("Required: asset_tag, name, category");
            instructions.createRow(1).createCell(0).setCellValue("Category values: LAPTOP, DESKTOP, MONITOR, PRINTER, SOFTWARE_LICENSE, OTHER");
            instructions.createRow(2).createCell(0).setCellValue("Use yyyy-MM-dd for purchase_date and warranty_expires_on. Cost uses a decimal amount.");
            instructions.createRow(3).createCell(0).setCellValue("For depreciation reporting, optionally provide useful_life_months and salvage_value.");
            instructions.createRow(4).createCell(0).setCellValue("Delete blank/example rows before import. Maximum 1,000 asset rows and 10 MB.");
            instructions.setColumnWidth(0, 100 * 256);
            workbook.write(output);
            return output.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("Could not create the import template", exception);
        }
    }

    public AssetImportResult importAssets(MultipartFile file, UserAccount actor) {
        if (file == null || file.isEmpty()) throw badRequest("Choose an .xlsx workbook to import");
        if (file.getSize() > MAX_IMPORT_BYTES) throw badRequest("The import workbook cannot exceed 10 MB");
        String filename = file.getOriginalFilename();
        if (filename == null || !filename.toLowerCase(Locale.ROOT).endsWith(".xlsx")) {
            throw badRequest("Upload an Excel .xlsx workbook");
        }

        try (XSSFWorkbook workbook = new XSSFWorkbook(file.getInputStream())) {
            if (workbook.getNumberOfSheets() == 0) throw badRequest("The workbook has no sheets");
            Sheet sheet = workbook.getSheetAt(0);
            if (sheet.getLastRowNum() > MAX_IMPORT_ROWS) {
                throw badRequest("The workbook can contain at most 1,000 asset rows");
            }
            Map<String, Integer> columns = readHeader(sheet.getRow(0));
            for (String required : List.of("asset_tag", "name", "category")) {
                if (!columns.containsKey(required)) throw badRequest("Missing required column: " + required);
            }

            DataFormatter formatter = new DataFormatter(Locale.ROOT);
            int scanned = 0;
            int imported = 0;
            List<AssetImportIssue> issues = new ArrayList<>();
            for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (isBlankRow(row, formatter)) continue;
                scanned++;
                try {
                    AssetRequest request = parseAsset(row, columns, formatter);
                    Set<ConstraintViolation<AssetRequest>> violations = validator.validate(request);
                    if (!violations.isEmpty()) {
                        String message = violations.stream().map(ConstraintViolation::getMessage)
                                .sorted().distinct().reduce((left, right) -> left + "; " + right).orElse("Invalid asset row");
                        throw new IllegalArgumentException(message);
                    }
                    assetService.createImportedAsset(request, actor);
                    imported++;
                } catch (IllegalArgumentException | ResponseStatusException exception) {
                    String message = exception instanceof ResponseStatusException response
                            ? response.getReason() : exception.getMessage();
                    issues.add(new AssetImportIssue(rowIndex + 1, message == null ? "Invalid asset row" : message));
                }
            }
            return new AssetImportResult(scanned, imported, scanned - imported, List.copyOf(issues));
        } catch (IOException | org.apache.poi.ooxml.POIXMLException exception) {
            throw badRequest("The uploaded workbook could not be read. Download a new template and try again.");
        }
    }

    private List<String[]> assetRows(UserAccount actor) {
        requireAuthority(actor, "reports:assets");
        List<Asset> assetData = assets.findAllByOrderByUpdatedAtDesc();
        if (assetData.isEmpty()) return List.of();
        Map<Long, BigDecimal> maintenanceCosts = new HashMap<>();
        maintenance.sumCostsByAssetIds(assetData.stream().map(Asset::getId).toList())
                .forEach(item -> maintenanceCosts.put(item.getAssetId(), item.getTotalCost()));
        LocalDate asOf = LocalDate.now();
        return assetData.stream().map(asset -> {
            BigDecimal maintenanceTotal = maintenanceCosts.getOrDefault(asset.getId(), BigDecimal.ZERO);
            String[] depreciation = depreciationValues(asset, asOf);
            return new String[]{
                value(asset.getId()), asset.getAssetTag(), asset.getName(), asset.getCategory().name(),
                asset.getStatus().name(), value(asset.getManufacturer()), value(asset.getModelNumber()),
                value(asset.getSerialNumber()), value(asset.getPurchaseDate()), value(asset.getPurchaseCost()),
                value(asset.getWarrantyExpiresOn()), userEmail(asset.getAssignedTo()),
                value(asset.getUsefulLifeMonths()), value(asset.getSalvageValue()), depreciation[0], depreciation[1],
                maintenanceTotal.setScale(2, RoundingMode.HALF_UP).toPlainString()
            };
        }).toList();
    }

    private List<String[]> assetPdfRows(UserAccount actor) {
        requireAuthority(actor, "reports:assets");
        List<Asset> assetData = assets.findAllByOrderByUpdatedAtDesc();
        if (assetData.isEmpty()) return List.of();
        Map<Long, BigDecimal> maintenanceCosts = new HashMap<>();
        maintenance.sumCostsByAssetIds(assetData.stream().map(Asset::getId).toList())
                .forEach(item -> maintenanceCosts.put(item.getAssetId(), item.getTotalCost()));
        LocalDate asOf = LocalDate.now();
        return assetData.stream().map(asset -> {
            String[] depreciation = depreciationValues(asset, asOf);
            return new String[]{asset.getAssetTag(), asset.getStatus().name(), value(asset.getPurchaseCost()),
                    depreciation[0], depreciation[1], maintenanceCosts.getOrDefault(asset.getId(), BigDecimal.ZERO)
                    .setScale(2, RoundingMode.HALF_UP).toPlainString()};
        }).toList();
    }

    private static String[] depreciationValues(Asset asset, LocalDate asOf) {
        if (asset.getPurchaseCost() == null || asset.getPurchaseDate() == null || asset.getUsefulLifeMonths() == null) {
            return new String[]{"", ""};
        }
        BigDecimal cost = asset.getPurchaseCost();
        BigDecimal salvage = asset.getSalvageValue() == null ? BigDecimal.ZERO : asset.getSalvageValue();
        BigDecimal depreciableAmount = cost.subtract(salvage).max(BigDecimal.ZERO);
        long elapsedMonths = Math.max(0, ChronoUnit.MONTHS.between(asset.getPurchaseDate(), asOf));
        long depreciatedMonths = Math.min(elapsedMonths, asset.getUsefulLifeMonths());
        BigDecimal accumulated = depreciableAmount.multiply(BigDecimal.valueOf(depreciatedMonths))
                .divide(BigDecimal.valueOf(asset.getUsefulLifeMonths()), 2, RoundingMode.HALF_UP);
        BigDecimal bookValue = cost.subtract(accumulated).max(salvage).setScale(2, RoundingMode.HALF_UP);
        return new String[]{accumulated.toPlainString(), bookValue.toPlainString()};
    }

    private List<String[]> ticketRows(UserAccount actor) {
        requireAuthority(actor, "reports:tickets");
        return tickets.findAllDetailed().stream().map(ticket -> new String[]{
                value(ticket.getId()), ticket.getTitle(), userEmail(ticket.getRequester()),
                userEmail(ticket.getAssignee()), ticket.getCategory().name(), ticket.getPriority().name(),
                ticket.getStatus().name(), value(ticket.getCreatedAt()), value(ticket.getUpdatedAt())
        }).toList();
    }

    private List<String[]> requestRows(UserAccount actor) {
        List<EquipmentRequest> data;
        if (hasAuthority(actor, "reports:requests")) {
            data = requests.findAllDetailed();
        } else if (hasAuthority(actor, "reports:requests_department") && actor.getDepartment() != null) {
            data = requests.findAllForDepartment(actor.getDepartment().getId());
        } else {
            throw forbidden("You cannot view equipment request reports");
        }
        return data.stream().map(request -> new String[]{
                value(request.getId()), request.getItemName(), request.getCategory().name(),
                userEmail(request.getRequester()), value(request.getDepartment() == null ? null : request.getDepartment().getName()),
                request.getStatus().name(), value(request.getProcurementVendor()), value(request.getPurchaseReference()),
                value(request.getPurchaseCost()), value(request.getExpectedDeliveryDate()),
                value(request.getAllocatedAssetId()), value(request.getCreatedAt())
        }).toList();
    }

    private byte[] xlsx(String sheetName, String[] headers, List<String[]> rows) {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet(sheetName);
            writeHeader(workbook, sheet, headers);
            for (int index = 0; index < rows.size(); index++) {
                Row row = sheet.createRow(index + 1);
                String[] values = rows.get(index);
                for (int column = 0; column < values.length; column++) {
                    row.createCell(column).setCellValue(values[column] == null ? "" : values[column]);
                }
            }
            if (!rows.isEmpty()) sheet.setAutoFilter(new org.apache.poi.ss.util.CellRangeAddress(0, rows.size(), 0, headers.length - 1));
            sheet.createFreezePane(0, 1);
            for (int column = 0; column < headers.length; column++) sheet.autoSizeColumn(column);
            workbook.write(output);
            return output.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("Could not create an Excel report", exception);
        }
    }

    private byte[] pdf(String title, String[] headers, List<String[]> rows) {
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            int rowIndex = 0;
            do {
                PDPage page = new PDPage(PDRectangle.A4);
                document.addPage(page);
                try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                    writePdfText(content, title, 40, 798, 14, true);
                    writePdfText(content, "Generated " + java.time.OffsetDateTime.now(), 40, 780, 8, false);
                    writePdfText(content, truncate(String.join(" | ", headers), 135), 40, 758, 7, true);
                    writePdfText(content, "-".repeat(125), 40, 746, 6, false);
                    float y = 732;
                    while (rowIndex < rows.size() && y > 42) {
                        writePdfText(content, truncate(String.join(" | ", rows.get(rowIndex)), 135), 40, y, 6.5f, false);
                        y -= 13;
                        rowIndex++;
                    }
                }
            } while (rowIndex < rows.size());
            document.save(output);
            return output.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("Could not create a PDF report", exception);
        }
    }

    private static void writePdfText(PDPageContentStream content, String text, float x, float y,
                                     float size, boolean bold) throws IOException {
        Standard14Fonts.FontName fontName = bold
                ? Standard14Fonts.FontName.HELVETICA_BOLD : Standard14Fonts.FontName.HELVETICA;
        content.beginText();
        content.setFont(new PDType1Font(fontName), size);
        content.newLineAtOffset(x, y);
        content.showText(ascii(text));
        content.endText();
    }

    private static void writeHeader(XSSFWorkbook workbook, Sheet sheet, String[] headers) {
        Row header = sheet.createRow(0);
        Font font = workbook.createFont();
        font.setBold(true);
        CellStyle style = workbook.createCellStyle();
        style.setFont(font);
        for (int column = 0; column < headers.length; column++) {
            Cell cell = header.createCell(column);
            cell.setCellValue(headers[column]);
            cell.setCellStyle(style);
        }
    }

    private static Map<String, Integer> readHeader(Row header) {
        if (header == null) throw badRequest("The first sheet needs a header row");
        DataFormatter formatter = new DataFormatter(Locale.ROOT);
        Map<String, Integer> columns = new LinkedHashMap<>();
        for (Cell cell : header) {
            String name = normalizeHeader(formatter.formatCellValue(cell));
            if (!name.isBlank()) columns.put(name, cell.getColumnIndex());
        }
        return columns;
    }

    private static AssetRequest parseAsset(Row row, Map<String, Integer> columns, DataFormatter formatter) {
        String tag = cellText(row, columns, "asset_tag", formatter);
        String name = cellText(row, columns, "name", formatter);
        String categoryText = cellText(row, columns, "category", formatter);
        if (tag.isBlank() || name.isBlank() || categoryText.isBlank()) {
            throw new IllegalArgumentException("Asset tag, name, and category are required");
        }
        AssetCategory category;
        try {
            category = AssetCategory.valueOf(categoryText.trim().toUpperCase(Locale.ROOT).replace(' ', '_').replace('-', '_'));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Unknown category: " + categoryText);
        }
        return new AssetRequest(tag, name, category,
                nullable(cellText(row, columns, "description", formatter)),
                nullable(cellText(row, columns, "manufacturer", formatter)),
                nullable(cellText(row, columns, "model_number", formatter)),
                nullable(cellText(row, columns, "serial_number", formatter)),
                cellDate(row, columns, "purchase_date", formatter),
                cellDecimal(row, columns, "purchase_cost", formatter),
                cellDate(row, columns, "warranty_expires_on", formatter),
                cellInteger(row, columns, "useful_life_months", formatter),
                cellDecimal(row, columns, "salvage_value", formatter));
    }

    private static String cellText(Row row, Map<String, Integer> columns, String name, DataFormatter formatter) {
        Integer column = columns.get(name);
        if (column == null || row == null) return "";
        Cell cell = row.getCell(column, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
        if (cell == null) return "";
        if (cell.getCellType() == org.apache.poi.ss.usermodel.CellType.FORMULA) {
            throw new IllegalArgumentException("Formula cells are not accepted");
        }
        return formatter.formatCellValue(cell).trim();
    }

    private static LocalDate cellDate(Row row, Map<String, Integer> columns, String name, DataFormatter formatter) {
        Integer column = columns.get(name);
        if (column == null || row == null) return null;
        Cell cell = row.getCell(column, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
        if (cell == null || cell.getCellType() == org.apache.poi.ss.usermodel.CellType.BLANK) return null;
        if (cell.getCellType() == org.apache.poi.ss.usermodel.CellType.FORMULA) {
            throw new IllegalArgumentException("Formula cells are not accepted");
        }
        if (cell.getCellType() == org.apache.poi.ss.usermodel.CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
            return cell.getLocalDateTimeCellValue().toLocalDate();
        }
        String value = formatter.formatCellValue(cell).trim();
        try {
            return LocalDate.parse(value, DateTimeFormatter.ISO_LOCAL_DATE);
        } catch (DateTimeParseException exception) {
            throw new IllegalArgumentException(name + " must use yyyy-MM-dd");
        }
    }

    private static BigDecimal cellDecimal(Row row, Map<String, Integer> columns, String name, DataFormatter formatter) {
        Integer column = columns.get(name);
        if (column == null || row == null) return null;
        Cell cell = row.getCell(column, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
        if (cell == null || cell.getCellType() == org.apache.poi.ss.usermodel.CellType.BLANK) return null;
        if (cell.getCellType() == org.apache.poi.ss.usermodel.CellType.FORMULA) {
            throw new IllegalArgumentException("Formula cells are not accepted");
        }
        String value = cell.getCellType() == org.apache.poi.ss.usermodel.CellType.NUMERIC
                ? BigDecimal.valueOf(cell.getNumericCellValue()).stripTrailingZeros().toPlainString()
                : formatter.formatCellValue(cell).replace(",", "").trim();
        try {
            return new BigDecimal(value);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(name + " must be a decimal amount");
        }
    }

    private static Integer cellInteger(Row row, Map<String, Integer> columns, String name, DataFormatter formatter) {
        Integer column = columns.get(name);
        if (column == null || row == null) return null;
        Cell cell = row.getCell(column, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
        if (cell == null || cell.getCellType() == org.apache.poi.ss.usermodel.CellType.BLANK) return null;
        if (cell.getCellType() == org.apache.poi.ss.usermodel.CellType.FORMULA) {
            throw new IllegalArgumentException("Formula cells are not accepted");
        }
        String value = cell.getCellType() == org.apache.poi.ss.usermodel.CellType.NUMERIC
                ? BigDecimal.valueOf(cell.getNumericCellValue()).stripTrailingZeros().toPlainString()
                : formatter.formatCellValue(cell).trim();
        try {
            return new BigDecimal(value).intValueExact();
        } catch (NumberFormatException | ArithmeticException exception) {
            throw new IllegalArgumentException(name + " must be a whole number");
        }
    }

    private static boolean isBlankRow(Row row, DataFormatter formatter) {
        if (row == null) return true;
        for (Cell cell : row) {
            if (cell.getCellType() != org.apache.poi.ss.usermodel.CellType.BLANK
                    && !formatter.formatCellValue(cell).isBlank()) return false;
        }
        return true;
    }

    private static String normalizeHeader(String value) {
        return value.trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "_").replaceAll("^_|_$", "");
    }

    private static String nullable(String value) { return value == null || value.isBlank() ? null : value.trim(); }
    private static String value(Object value) { return value == null ? "" : value.toString(); }
    private static String userEmail(UserAccount user) { return user == null ? "" : user.getEmail(); }

    private static String truncate(String value, int max) {
        return ascii(value).length() <= max ? ascii(value) : ascii(value).substring(0, max - 3) + "...";
    }

    private static String ascii(String value) {
        return value == null ? "" : value.replaceAll("[^\\x20-\\x7E]", "?");
    }

    private static void addEntry(ZipOutputStream zip, String name, byte[] bytes) throws IOException {
        zip.putNextEntry(new ZipEntry(name));
        zip.write(bytes);
        zip.closeEntry();
    }

    private static boolean hasAuthority(UserAccount actor, String authority) {
        return actor.getAuthorities().stream().map(GrantedAuthority::getAuthority).anyMatch(authority::equals);
    }

    private static void requireAuthority(UserAccount actor, String authority) {
        if (!hasAuthority(actor, authority)) throw forbidden("You do not have access to this report");
    }

    private static ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }

    private static ResponseStatusException forbidden(String message) {
        return new ResponseStatusException(HttpStatus.FORBIDDEN, message);
    }
}
