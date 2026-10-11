package com.itmanagement.servicedesk.security;

import com.itmanagement.servicedesk.modules.users.model.UserAccount;
import com.itmanagement.servicedesk.modules.users.repository.UserAccountRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.filter.OncePerRequestFilter;

/** Reloads the session principal so account and permission changes take effect on the next API request. */
public class CurrentAccountFilter extends OncePerRequestFilter {
    private final UserAccountRepository users;
    private final SecurityContextRepository securityContextRepository;

    public CurrentAccountFilter(UserAccountRepository users,
                                SecurityContextRepository securityContextRepository) {
        this.users = users;
        this.securityContextRepository = securityContextRepository;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith(request.getContextPath() + "/api/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        Authentication existingAuthentication = SecurityContextHolder.getContext().getAuthentication();
        if (existingAuthentication == null
                || !existingAuthentication.isAuthenticated()
                || !(existingAuthentication.getPrincipal() instanceof UserAccount sessionUser)) {
            filterChain.doFilter(request, response);
            return;
        }

        UserAccount currentUser = users.findDetailedById(sessionUser.getId()).orElse(null);
        if (currentUser == null || !currentUser.isEnabled()) {
            new SecurityContextLogoutHandler().logout(request, response, existingAuthentication);
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.setContentType("application/json");
            response.getWriter().write("{\"message\":\"Your account is inactive. Sign in again or contact an administrator.\"}");
            return;
        }

        UsernamePasswordAuthenticationToken refreshedAuthentication =
                UsernamePasswordAuthenticationToken.authenticated(currentUser, null, currentUser.getAuthorities());
        refreshedAuthentication.setDetails(existingAuthentication.getDetails());

        SecurityContext refreshedContext = SecurityContextHolder.createEmptyContext();
        refreshedContext.setAuthentication(refreshedAuthentication);
        SecurityContextHolder.setContext(refreshedContext);
        securityContextRepository.saveContext(refreshedContext, request, response);

        filterChain.doFilter(request, response);
    }
}
