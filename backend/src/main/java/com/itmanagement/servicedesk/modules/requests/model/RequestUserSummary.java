package com.itmanagement.servicedesk.modules.requests.model;

public record RequestUserSummary(Long id, String firstName, String lastName,
                                 String email, String departmentName) {
}
