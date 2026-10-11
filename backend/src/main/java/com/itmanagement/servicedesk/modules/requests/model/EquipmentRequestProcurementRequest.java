package com.itmanagement.servicedesk.modules.requests.model;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

public record EquipmentRequestProcurementRequest(
        @NotBlank @Size(max = 100) String vendor,
        @Size(max = 100) String purchaseReference,
        @NotNull @DecimalMin("0.00") BigDecimal cost,
        @NotNull @FutureOrPresent LocalDate expectedDeliveryDate,
        @Size(max = 2000) String notes) {
}
