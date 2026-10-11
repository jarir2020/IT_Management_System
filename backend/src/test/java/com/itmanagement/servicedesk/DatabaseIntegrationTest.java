package com.itmanagement.servicedesk;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.itmanagement.servicedesk.modules.access.repository.PermissionRepository;
import com.itmanagement.servicedesk.modules.access.repository.RoleRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.flywaydb.core.Flyway;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(properties = {
        "app.bootstrap.admin-email=",
        "app.bootstrap.admin-password=",
        "app.ticket-upload-dir=${java.io.tmpdir}/itsm-test-ticket-uploads",
        "app.asset-upload-dir=${java.io.tmpdir}/itsm-test-asset-uploads"
})
@AutoConfigureMockMvc
class DatabaseIntegrationTest {
    @Container
    private static final MySQLContainer MYSQL = new MySQLContainer(DockerImageName.parse("mysql:8.4"))
            .withDatabaseName("it_service_desk")
            .withUsername("itsm_test")
            .withPassword("itsm_test_password");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry properties) {
        properties.add("spring.datasource.url", MYSQL::getJdbcUrl);
        properties.add("spring.datasource.username", MYSQL::getUsername);
        properties.add("spring.datasource.password", MYSQL::getPassword);
    }

    @Autowired
    private Flyway flyway;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private PermissionRepository permissions;

    @Autowired
    private RoleRepository roles;

    @Autowired
    private MockMvc mvc;

    @Test
    void flywayAppliesAllMigrationsAndSeedsAuditPermissionForAdmin() {
        assertThat(flyway.info().applied()).hasSize(6);
        assertThat(jdbc.queryForObject("select count(*) from audit_events", Long.class)).isZero();
        assertThat(permissions.findByCode("audit:read")).isPresent();
        assertThat(roles.findByCode("ADMIN")).isPresent();
        assertThat(roles.findByCode("ADMIN").orElseThrow().getPermissions())
                .anyMatch(permission -> permission.getCode().equals("audit:read"));
    }

    @Test
    void auditEndpointAllowsThePermissionAndRejectsOtherSignedInUsers() throws Exception {
        mvc.perform(get("/api/v1/audit").with(user("auditor").authorities(() -> "audit:read")))
                .andExpect(status().isOk());

        mvc.perform(get("/api/v1/audit").with(user("employee")))
                .andExpect(status().isForbidden());
    }
}
