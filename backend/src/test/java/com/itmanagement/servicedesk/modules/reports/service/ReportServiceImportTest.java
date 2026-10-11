package com.itmanagement.servicedesk.modules.reports.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.doReturn;

import com.itmanagement.servicedesk.modules.assets.repository.AssetMaintenanceRepository;
import com.itmanagement.servicedesk.modules.assets.repository.AssetRepository;
import com.itmanagement.servicedesk.modules.assets.service.AssetService;
import com.itmanagement.servicedesk.modules.reports.model.AssetImportResult;
import com.itmanagement.servicedesk.modules.requests.repository.EquipmentRequestRepository;
import com.itmanagement.servicedesk.modules.tickets.repository.TicketRepository;
import com.itmanagement.servicedesk.modules.users.model.UserAccount;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.server.ResponseStatusException;

class ReportServiceImportTest {
    private static final ValidatorFactory VALIDATOR_FACTORY = Validation.buildDefaultValidatorFactory();
    private static final Validator VALIDATOR = VALIDATOR_FACTORY.getValidator();

    private final AssetRepository assets = mock(AssetRepository.class);
    private final TicketRepository tickets = mock(TicketRepository.class);
    private final EquipmentRequestRepository requests = mock(EquipmentRequestRepository.class);
    private final AssetMaintenanceRepository maintenance = mock(AssetMaintenanceRepository.class);
    private final AssetService assetService = mock(AssetService.class);
    private ReportService reports;

    @BeforeEach
    void setUp() {
        reports = new ReportService(assets, tickets, requests, maintenance, assetService, VALIDATOR);
    }

    @AfterAll
    static void closeValidatorFactory() {
        VALIDATOR_FACTORY.close();
    }

    @Test
    void importReportsRowLevelValidationAndOnlyPersistsValidRows() throws IOException {
        MockMultipartFile workbook = workbook(
                new String[]{"asset_tag", "name", "category"},
                new String[]{"LAP-001", "Laptop", "LAPTOP"},
                new String[]{"bad tag!", "Invalid tag", "MONITOR"});

        AssetImportResult result = reports.importAssets(workbook, accountWithImportPermission());

        assertThat(result.scannedRows()).isEqualTo(2);
        assertThat(result.importedRows()).isEqualTo(1);
        assertThat(result.skippedRows()).isEqualTo(1);
        assertThat(result.issues()).singleElement().satisfies(issue -> {
            assertThat(issue.row()).isEqualTo(3);
            assertThat(issue.message()).contains("letters, numbers");
        });
        verify(assetService).createImportedAsset(any(), any());
    }

    @Test
    void importRejectsWorkbooksWithoutRequiredColumns() throws IOException {
        MockMultipartFile workbook = workbook(
                new String[]{"name", "category"},
                new String[]{"Laptop", "LAPTOP"});

        assertThatThrownBy(() -> reports.importAssets(workbook, accountWithImportPermission()))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception -> {
                    assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(exception.getReason()).contains("asset_tag");
                });
        verify(assetService, never()).createImportedAsset(any(), any());
    }

    @Test
    void reportExportRequiresItsSpecificPermission() {
        UserAccount employee = mock(UserAccount.class);
        doReturn(List.of()).when(employee).getAuthorities();

        assertThatThrownBy(() -> reports.ticketsXlsx(employee))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception ->
                        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));
        verify(tickets, never()).findAllDetailed();
    }

    private static UserAccount accountWithImportPermission() {
        UserAccount actor = mock(UserAccount.class);
        doReturn(List.of(new SimpleGrantedAuthority("reports:assets_import"))).when(actor).getAuthorities();
        return actor;
    }

    private static MockMultipartFile workbook(String[] headers, String[]... rows) throws IOException {
        try (XSSFWorkbook xssf = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            var sheet = xssf.createSheet("Assets");
            writeRow(sheet.createRow(0), headers);
            for (int index = 0; index < rows.length; index++) writeRow(sheet.createRow(index + 1), rows[index]);
            xssf.write(output);
            return new MockMultipartFile("file", "assets.xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", output.toByteArray());
        }
    }

    private static void writeRow(org.apache.poi.ss.usermodel.Row row, String[] values) {
        for (int column = 0; column < values.length; column++) row.createCell(column).setCellValue(values[column]);
    }
}
