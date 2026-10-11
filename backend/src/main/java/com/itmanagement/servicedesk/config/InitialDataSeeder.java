package com.itmanagement.servicedesk.config;

import com.itmanagement.servicedesk.modules.access.model.Permission;
import com.itmanagement.servicedesk.modules.access.repository.PermissionRepository;
import com.itmanagement.servicedesk.modules.access.model.Role;
import com.itmanagement.servicedesk.modules.access.repository.RoleRepository;
import com.itmanagement.servicedesk.modules.users.model.UserAccount;
import com.itmanagement.servicedesk.modules.users.repository.UserAccountRepository;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

@Component
public class InitialDataSeeder implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(InitialDataSeeder.class);

    private static final Map<String, String> PERMISSIONS = Map.ofEntries(
            Map.entry("users:read", "View user accounts"),
            Map.entry("users:write", "Create, update, and disable user accounts"),
            Map.entry("departments:read", "View departments"),
            Map.entry("departments:write", "Create, update, and deactivate departments"),
            Map.entry("roles:read", "View roles and their permissions"),
            Map.entry("tickets:read", "View tickets the account is allowed to access"),
            Map.entry("tickets:read_all", "View every ticket in the service desk"),
            Map.entry("tickets:create", "Create tickets"),
            Map.entry("tickets:update", "Update ticket details"),
            Map.entry("tickets:assign", "Assign tickets to support staff"),
            Map.entry("tickets:status", "Move tickets through the support workflow"),
            Map.entry("tickets:comment", "Add public ticket comments"),
            Map.entry("tickets:comment_internal", "Add internal ticket notes"),
            Map.entry("tickets:attach", "Upload and download ticket attachments"),
            Map.entry("tickets:delete", "Delete tickets and manage all ticket records"),
            Map.entry("assets:read", "View and search the complete asset inventory"),
            Map.entry("assets:read_own", "View assets currently assigned to the account"),
            Map.entry("assets:create", "Add assets to the inventory"),
            Map.entry("assets:update", "Update asset details"),
            Map.entry("assets:assign", "Assign and return assets"),
            Map.entry("assets:maintenance", "Record asset repairs and maintenance"),
            Map.entry("assets:document", "Upload and remove asset documents"),
            Map.entry("assets:delete", "Retire assets from active inventory"),
            Map.entry("requests:read_own", "View equipment requests created by the account"),
            Map.entry("requests:create", "Submit equipment requests"),
            Map.entry("requests:cancel", "Cancel pending equipment requests created by the account"),
            Map.entry("requests:approve_department", "Review requests submitted by the account's department"),
            Map.entry("requests:approve_all", "Review equipment requests across all departments"),
            Map.entry("requests:read_all", "View the complete equipment request queue"),
            Map.entry("requests:allocate", "Allocate inventory and route unavailable requests to procurement"),
            Map.entry("requests:procure", "Record purchase details and receive ordered equipment"),
            Map.entry("reports:read", "Open reports and data exchange tools"),
            Map.entry("reports:assets", "Export the full asset inventory"),
            Map.entry("reports:tickets", "Export the service desk ticket queue"),
            Map.entry("reports:requests", "Export equipment requests across departments"),
            Map.entry("reports:requests_department", "Export equipment requests for the account's department"),
            Map.entry("reports:assets_import", "Import assets from an Excel workbook"),
            Map.entry("audit:read", "View the system audit trail"));

    private static final Set<String> REQUESTER_PERMISSIONS = Set.of(
            "departments:read", "tickets:read", "tickets:create", "tickets:comment", "tickets:attach",
            "assets:read_own", "requests:read_own", "requests:create", "requests:cancel");

    private static final Set<String> SUPPORT_PERMISSIONS = Set.of(
            "departments:read", "tickets:read", "tickets:read_all", "tickets:create", "tickets:update",
            "tickets:assign", "tickets:status", "tickets:comment", "tickets:comment_internal", "tickets:attach",
            "reports:read", "reports:tickets");

    private static final Set<String> ASSET_MANAGER_PERMISSIONS = Set.of(
            "departments:read", "assets:read", "assets:read_own", "assets:create", "assets:update",
            "assets:assign", "assets:maintenance", "assets:document", "assets:delete",
            "requests:read_own", "requests:read_all", "requests:allocate",
            "reports:read", "reports:assets", "reports:requests", "reports:assets_import");

    private static final Set<String> DEPARTMENT_MANAGER_PERMISSIONS = withPermissions(
            REQUESTER_PERMISSIONS, "requests:approve_department", "reports:read", "reports:requests_department");

    private static final Set<String> PROCUREMENT_PERMISSIONS = withPermissions(
            REQUESTER_PERMISSIONS, "requests:read_all", "requests:procure", "reports:read", "reports:requests");

    private static final Map<String, RoleDefinition> ROLES = Map.of(
            "ADMIN", new RoleDefinition("Administrator", "Manage users, access, and system settings", Set.copyOf(PERMISSIONS.keySet())),
            "EMPLOYEE", new RoleDefinition("Employee", "Create tickets and request equipment", REQUESTER_PERMISSIONS),
            "SUPPORT_AGENT", new RoleDefinition("Support Agent", "Handle support tickets", SUPPORT_PERMISSIONS),
            "IT_ASSET_MANAGER", new RoleDefinition("IT Asset Manager", "Manage inventory, assignments, and maintenance", ASSET_MANAGER_PERMISSIONS),
            "DEPARTMENT_MANAGER", new RoleDefinition("Department Manager", "Review department equipment requests", DEPARTMENT_MANAGER_PERMISSIONS),
            "PROCUREMENT_FINANCE", new RoleDefinition("Procurement/Finance", "Manage purchasing and expenses", PROCUREMENT_PERMISSIONS));

    private final PermissionRepository permissions;
    private final RoleRepository roles;
    private final UserAccountRepository users;
    private final PasswordEncoder passwordEncoder;
    private final String adminEmail;
    private final String adminPassword;

    public InitialDataSeeder(PermissionRepository permissions,
                             RoleRepository roles,
                             UserAccountRepository users,
                             PasswordEncoder passwordEncoder,
                             @Value("${app.bootstrap.admin-email:}") String adminEmail,
                             @Value("${app.bootstrap.admin-password:}") String adminPassword) {
        this.permissions = permissions;
        this.roles = roles;
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.adminEmail = adminEmail;
        this.adminPassword = adminPassword;
    }

    @Transactional
    @Override
    public void run(ApplicationArguments arguments) {
        Map<String, Permission> permissionEntities = new LinkedHashMap<>();
        PERMISSIONS.forEach((code, description) -> {
            Permission permission = permissions.findByCode(code)
                    .orElseGet(() -> permissions.save(new Permission(code, description)));
            if (!permission.getDescription().equals(description)) {
                permission.setDescription(description);
                permission = permissions.save(permission);
            }
            permissionEntities.put(code, permission);
        });

        ROLES.forEach((code, definition) -> {
            Role role = roles.findByCode(code)
                    .orElseGet(() -> new Role(code, definition.name(), definition.description(), true));
            Set<Permission> rolePermissions = new LinkedHashSet<>();
            definition.permissions().stream()
                    .map(permissionEntities::get)
                    .filter(permission -> permission != null)
                    .forEach(rolePermissions::add);
            role.setPermissions(rolePermissions);
            roles.save(role);
        });

        boolean emailSet = adminEmail != null && !adminEmail.isBlank();
        boolean passwordSet = adminPassword != null && !adminPassword.isBlank();
        if (emailSet != passwordSet) {
            throw new IllegalStateException("Set both BOOTSTRAP_ADMIN_EMAIL and BOOTSTRAP_ADMIN_PASSWORD, or leave both empty.");
        }
        if (!emailSet) {
            log.warn("No bootstrap administrator created. Set BOOTSTRAP_ADMIN_EMAIL and BOOTSTRAP_ADMIN_PASSWORD to provision one.");
            return;
        }

        String normalizedEmail = adminEmail.trim().toLowerCase(Locale.ROOT);
        if (users.existsByEmailIgnoreCase(normalizedEmail)) {
            return;
        }
        Role adminRole = roles.findByCode("ADMIN")
                .orElseThrow(() -> new IllegalStateException("Built-in ADMIN role is missing"));
        UserAccount administrator = new UserAccount("System", "Administrator", normalizedEmail,
                passwordEncoder.encode(adminPassword), true, null, Set.of(adminRole));
        users.save(administrator);
        log.info("Created the bootstrap administrator account {}", normalizedEmail);
    }

    private record RoleDefinition(String name, String description, Set<String> permissions) {
    }

    private static Set<String> withPermissions(Set<String> base, String... additional) {
        Set<String> combined = new LinkedHashSet<>(base);
        combined.addAll(Set.of(additional));
        return Set.copyOf(combined);
    }
}
