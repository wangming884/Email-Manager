package com.mailmanager.security;

import com.mailmanager.domain.ActorType;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.io.Serial;
import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

public record SessionPrincipal(
        ActorType actorType,
        String actorId,
        List<String> scopes
) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    public Collection<? extends GrantedAuthority> authorities() {
        Stream<SimpleGrantedAuthority> roleAuthority = Stream.of(new SimpleGrantedAuthority("ROLE_" + actorType.name()));
        Stream<SimpleGrantedAuthority> scopeAuthorities = scopes.stream()
                .map(scope -> new SimpleGrantedAuthority("SCOPE_" + scope));
        return Stream.concat(roleAuthority, scopeAuthorities).toList();
    }
}
