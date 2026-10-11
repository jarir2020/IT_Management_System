package com.itmanagement.servicedesk.modules.users.service;

import com.itmanagement.servicedesk.modules.shared.ApiMapper;
import com.itmanagement.servicedesk.modules.users.model.CreateUserRequest;
import com.itmanagement.servicedesk.modules.users.model.UpdateUserRequest;
import com.itmanagement.servicedesk.modules.users.model.UserResponse;
import com.itmanagement.servicedesk.modules.users.model.UserAccount;
import com.itmanagement.servicedesk.modules.departments.model.Department;
import com.itmanagement.servicedesk.modules.departments.repository.DepartmentRepository;
import com.itmanagement.servicedesk.modules.access.model.Role;
import com.itmanagement.servicedesk.modules.access.repository.RoleRepository;
import com.itmanagement.servicedesk.modules.users.repository.UserAccountRepository;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class UserService {
    private final UserAccountRepository users;
    private final DepartmentRepository departments;
    private final RoleRepository roles;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserAccountRepository users,
                       DepartmentRepository departments,
                       RoleRepository roles,
                       PasswordEncoder passwordEncoder) {
        this.users = users;
        this.departments = departments;
        this.roles = roles;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public List<UserResponse> listUsers() {
        return users.findAllDetailed().stream().map(ApiMapper::toUser).toList();
    }

    @Transactional
    public UserResponse createUser(CreateUserRequest request) {
        String email = normalizeEmail(request.email());
        ensureEmailAvailable(email, null);
        Department department = resolveDepartment(request.departmentId());
        Set<Role> assignedRoles = resolveRoles(request.roleIds());
        UserAccount user = new UserAccount(
                request.firstName().trim(),
                request.lastName().trim(),
                email,
                passwordEncoder.encode(request.password()),
                true,
                department,
                assignedRoles);
        return ApiMapper.toUser(users.save(user));
    }

    @Transactional
    public UserResponse updateUser(Long id, UpdateUserRequest request, Long actorId) {
        UserAccount user = getUser(id);
        String email = normalizeEmail(request.email());
        ensureEmailAvailable(email, id);
        Department department = resolveDepartment(request.departmentId());
        Set<Role> assignedRoles = resolveRoles(request.roleIds());
        if (id.equals(actorId) && user.getEnabled()
                && hasRole(user.getRoles(), "ADMIN")
                && !hasRole(assignedRoles, "ADMIN")) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "You cannot remove administrator access from your own account");
        }
        if (id.equals(actorId) && !request.enabled()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "You cannot deactivate your own account");
        }
        guardLastAdministrator(user, request.enabled(), assignedRoles);

        user.setFirstName(request.firstName().trim());
        user.setLastName(request.lastName().trim());
        user.setEmail(email);
        user.setDepartment(department);
        user.setRoles(assignedRoles);
        user.setEnabled(request.enabled());
        if (request.password() != null && !request.password().isBlank()) {
            if (request.password().length() < 12) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A new password must contain at least 12 characters");
            }
            user.setPasswordHash(passwordEncoder.encode(request.password()));
        }
        return ApiMapper.toUser(user);
    }

    @Transactional
    public UserResponse setEnabled(Long id, boolean enabled, Long actorId) {
        UserAccount user = getUser(id);
        if (!enabled && id.equals(actorId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "You cannot deactivate your own account");
        }
        guardLastAdministrator(user, enabled, user.getRoles());
        user.setEnabled(enabled);
        return ApiMapper.toUser(user);
    }

    private void guardLastAdministrator(UserAccount user, boolean enabled, Set<Role> replacementRoles) {
        boolean isCurrentlyAdmin = user.getEnabled() && hasRole(user.getRoles(), "ADMIN");
        boolean willRemainAdmin = enabled && hasRole(replacementRoles, "ADMIN");
        if (isCurrentlyAdmin && !willRemainAdmin && users.countEnabledUsersWithRole("ADMIN") <= 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "The last active administrator cannot be deactivated or demoted");
        }
    }

    private static boolean hasRole(Set<Role> roleSet, String code) {
        return roleSet.stream().anyMatch(role -> role.getCode().equals(code));
    }

    private UserAccount getUser(Long id) {
        return users.findDetailedById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User account not found"));
    }

    private void ensureEmailAvailable(String email, Long currentId) {
        boolean inUse = currentId == null
                ? users.existsByEmailIgnoreCase(email)
                : users.existsByEmailIgnoreCaseAndIdNot(email, currentId);
        if (inUse) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "An account with that email already exists");
        }
    }

    private Department resolveDepartment(Long departmentId) {
        if (departmentId == null) return null;
        Department department = departments.findById(departmentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Department not found"));
        if (!department.isActive()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Choose an active department");
        }
        return department;
    }

    private Set<Role> resolveRoles(Set<Long> roleIds) {
        List<Role> found = roles.findAllById(roleIds);
        if (found.size() != new HashSet<>(roleIds).size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "One or more selected roles do not exist");
        }
        return new HashSet<>(found);
    }

    private static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
