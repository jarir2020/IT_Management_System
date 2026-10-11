package com.itmanagement.servicedesk.modules.assets.model;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

public record AssetRequest(
        @NotBlank @Pattern(regexp = "[A-Za-z0-9_-]{2,48}", message = "Use 2-48 letters, numbers, dashes, or underscores") String assetTag,
        @NotBlank @Size(max = 140) String name,
        @NotNull AssetCategory category,
        @Size(max = 5000) String description,
        @Size(max = 100) String manufacturer,
        @Size(max = 100) String modelNumber,
        @Size(max = 100) String serialNumber,
        @PastOrPresent LocalDate purchaseDate,
        @DecimalMin("0.00") @Digits(integer = 10, fraction = 2) BigDecimal purchaseCost,
        LocalDate warrantyExpiresOn,
        @Min(1) @Max(1200) Integer usefulLifeMonths,
        @DecimalMin("0.00") @Digits(integer = 10, fraction = 2) BigDecimal salvageValue) {
}
