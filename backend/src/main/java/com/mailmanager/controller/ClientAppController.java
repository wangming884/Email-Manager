package com.mailmanager.controller;

import com.mailmanager.api.ApiResponse;
import com.mailmanager.domain.ClientApp;
import com.mailmanager.security.AccessGuard;
import com.mailmanager.service.ClientAppService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/client-apps")
@RequiredArgsConstructor
public class ClientAppController extends BaseController {

    private final ClientAppService clientAppService;
    private final AccessGuard accessGuard;

    @GetMapping
    public ApiResponse<List<ClientAppResponse>> list(HttpServletRequest request) {
        accessGuard.requireAdmin();
        return ApiResponse.success(traceId(request), clientAppService.listClients().stream().map(this::toResponse).toList());
    }

    @PostMapping
    public ApiResponse<ClientAppResponse> create(@RequestBody CreateClientAppRequest createRequest, HttpServletRequest request) {
        accessGuard.requireAdmin();
        ClientApp clientApp = clientAppService.createClient(
                createRequest.clientId(),
                createRequest.name(),
                createRequest.clientSecret(),
                createRequest.scopes()
        );
        return ApiResponse.success(traceId(request), toResponse(clientApp));
    }

    private ClientAppResponse toResponse(ClientApp clientApp) {
        return new ClientAppResponse(clientApp.getId(), clientApp.getClientId(), clientApp.getName(), clientApp.getScopes(), clientApp.isEnabled());
    }

    public record CreateClientAppRequest(
            @NotBlank(message = "client_id is required")
            String clientId,
            @NotBlank(message = "name is required")
            String name,
            @NotBlank(message = "client_secret is required")
            String clientSecret,
            @NotBlank(message = "scopes is required")
            String scopes
    ) {
    }

    public record ClientAppResponse(Long id, String clientId, String name, String scopes, boolean enabled) {
    }
}
