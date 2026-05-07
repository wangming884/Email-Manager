package com.mailmanager.controller;

import com.mailmanager.api.ApiResponse;
import com.mailmanager.domain.ProxyBinding;
import com.mailmanager.repository.ProxyBindingRepository;
import com.mailmanager.security.AccessGuard;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/proxy-bindings")
@RequiredArgsConstructor
public class ProxyBindingController extends BaseController {

    private final ProxyBindingRepository proxyBindingRepository;
    private final AccessGuard accessGuard;

    @GetMapping
    public ApiResponse<List<ProxyBindingResponse>> list(HttpServletRequest request) {
        accessGuard.requireAdmin();
        return ApiResponse.success(traceId(request), proxyBindingRepository.findAll().stream()
                .map(binding -> new ProxyBindingResponse(
                        binding.getId(),
                        binding.getName(),
                        binding.getProtocol(),
                        binding.getHost(),
                        binding.getPort(),
                        binding.getUsername(),
                        binding.getDescription()
                ))
                .toList());
    }

    @PostMapping
    public ApiResponse<ProxyBindingResponse> create(@RequestBody CreateProxyBindingRequest createRequest, HttpServletRequest request) {
        accessGuard.requireAdmin();
        ProxyBinding binding = new ProxyBinding();
        binding.setName(createRequest.name());
        binding.setProtocol(createRequest.protocol());
        binding.setHost(createRequest.host());
        binding.setPort(createRequest.port());
        binding.setUsername(createRequest.username());
        binding.setPassword(createRequest.password());
        binding.setDescription(createRequest.description());
        binding = proxyBindingRepository.save(binding);
        return ApiResponse.success(traceId(request), new ProxyBindingResponse(
                binding.getId(), binding.getName(), binding.getProtocol(), binding.getHost(),
                binding.getPort(), binding.getUsername(), binding.getDescription()
        ));
    }

    public record CreateProxyBindingRequest(
            @NotBlank(message = "name is required")
            String name,
            @NotBlank(message = "protocol is required")
            String protocol,
            @NotBlank(message = "host is required")
            String host,
            @NotNull(message = "port is required")
            Integer port,
            String username,
            String password,
            String description
    ) {
    }

    public record ProxyBindingResponse(
            Long id,
            String name,
            String protocol,
            String host,
            Integer port,
            String username,
            String description
    ) {
    }
}
