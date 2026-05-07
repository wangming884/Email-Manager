package com.mailmanager.security;

import com.mailmanager.domain.ActorType;
import com.mailmanager.exception.ForbiddenException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class AccessGuard {

    public SessionPrincipal currentPrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof SessionPrincipal principal)) {
            throw new ForbiddenException("Authentication required");
        }
        return principal;
    }

    public SessionPrincipal requireAdmin() {
        SessionPrincipal principal = currentPrincipal();
        if (principal.actorType() != ActorType.ADMIN) {
            throw new ForbiddenException("Admin access required");
        }
        return principal;
    }

    public SessionPrincipal requireAnyScope(String... scopes) {
        SessionPrincipal principal = currentPrincipal();
        if (principal.actorType() == ActorType.ADMIN) {
            return principal;
        }
        List<String> granted = principal.scopes();
        for (String scope : scopes) {
            if (granted.contains(scope)) {
                return principal;
            }
        }
        throw new ForbiddenException("Insufficient scope");
    }
}
