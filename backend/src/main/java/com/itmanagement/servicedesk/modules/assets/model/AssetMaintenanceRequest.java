package com.itmanagement.servicedesk.modules.assets.model;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

public record AssetMaintenanceRequest(
        @NotBlank @Size(max = 180) String summary,
        @Size(max = 100) String vendor,
        @Size(max = 5000) String notes,
        @DecimalMin("0.00") @Digits(integer = 10, fraction = 2) BigDecimal cost,
        @NotNull @PastOrPresent LocalDate startedOn) {
}
