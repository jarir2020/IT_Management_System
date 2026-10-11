package com.itmanagement.servicedesk.modules.users.controller;

import com.itmanagement.servicedesk.modules.users.model.CreateUserRequest;
import com.itmanagement.servicedesk.modules.users.model.UpdateUserRequest;
import com.itmanagement.servicedesk.modules.users.model.UserResponse;
import com.itmanagement.servicedesk.modules.users.model.UserStatusRequest;
import com.itmanagement.servicedesk.modules.users.model.UserAccount;
import com.itmanagement.servicedesk.modules.users.service.UserService;
import com.itmanagement.servicedesk.modules.users.routes.UserRoutes;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(UserRoutes.BASE_PATH)
public class UserController {
    private final UserService users;

    public UserController(UserService users) {
        this.users = users;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('users:read')")
    public List<UserResponse> listUsers() {
        return users.listUsers();
    }

    @PostMapping
    @PreAuthorize("hasAuthority('users:write')")
    public UserResponse createUser(@Valid @RequestBody CreateUserRequest request,
                                   @AuthenticationPrincipal UserAccount actor) {
        return users.createUser(request, actor);
    }

    @PutMapping(UserRoutes.BY_ID)
    @PreAuthorize("hasAuthority('users:write')")
    public UserResponse updateUser(@PathVariable Long id,
                                   @Valid @RequestBody UpdateUserRequest request,
                                   @AuthenticationPrincipal UserAccount actor) {
        return users.updateUser(id, request, actor);
    }

    @PatchMapping(UserRoutes.STATUS)
    @PreAuthorize("hasAuthority('users:write')")
    public UserResponse setEnabled(@PathVariable Long id,
                                   @RequestBody UserStatusRequest request,
                                   @AuthenticationPrincipal UserAccount actor) {
        return users.setEnabled(id, request.enabled(), actor);
    }
}
