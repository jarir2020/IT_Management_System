package com.itmanagement.servicedesk.modules.requests.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record EquipmentRequestNote(@NotBlank @Size(max = 2000) String note) {
}
