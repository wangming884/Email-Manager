package com.mailmanager.security;

import com.mailmanager.domain.ActorType;

import java.util.List;

public record SessionTokenPayload(
        ActorType actorType,
        String actorId,
        List<String> scopes
) {
}
