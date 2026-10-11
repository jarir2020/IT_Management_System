package com.itmanagement.servicedesk.modules.departments.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record DepartmentRequest(
        @NotBlank @Size(max = 120) String name,
        @NotBlank @Pattern(regexp = "[A-Za-z0-9_-]{2,32}", message = "Use 2-32 letters, numbers, dashes, or underscores") String code,
        @Size(max = 500) String description,
        boolean active) {
}
