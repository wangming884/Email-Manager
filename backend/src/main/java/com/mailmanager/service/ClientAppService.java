package com.mailmanager.service;

import com.mailmanager.config.AppSecurityProperties;
import com.mailmanager.domain.ActorType;
import com.mailmanager.domain.ClientApp;
import com.mailmanager.exception.ForbiddenException;
import com.mailmanager.exception.NotFoundException;
import com.mailmanager.repository.ClientAppRepository;
import com.mailmanager.security.SessionTokenPayload;
import com.mailmanager.security.TokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ClientAppService {

    private final ClientAppRepository clientAppRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final AppSecurityProperties securityProperties;

    public String issueClientToken(String clientId, String clientSecret) {
        ClientApp clientApp = clientAppRepository.findByClientId(clientId)
                .filter(ClientApp::isEnabled)
                .orElseThrow(() -> new ForbiddenException("Invalid client credentials"));
        if (!passwordEncoder.matches(clientSecret, clientApp.getClientSecretHash())) {
            throw new ForbiddenException("Invalid client credentials");
        }
        List<String> scopes = Arrays.stream(clientApp.getScopes().split(","))
                .map(String::trim)
                .filter(scope -> !scope.isBlank())
                .toList();
        return tokenService.issueToken(
                new SessionTokenPayload(ActorType.CLIENT, clientApp.getClientId(), scopes),
                securityProperties.clientTokenTtlMinutes()
        );
    }

    public ClientApp createClient(String clientId, String name, String clientSecret, String scopes) {
        ClientApp clientApp = new ClientApp();
        clientApp.setClientId(clientId);
        clientApp.setName(name);
        clientApp.setScopes(scopes);
        clientApp.setClientSecretHash(passwordEncoder.encode(clientSecret));
        clientApp.setEnabled(true);
        return clientAppRepository.save(clientApp);
    }

    public List<ClientApp> listClients() {
        return clientAppRepository.findAll();
    }

    public ClientApp getClient(Long id) {
        return clientAppRepository.findById(id).orElseThrow(() -> new NotFoundException("Client app not found"));
    }
}
