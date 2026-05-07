package com.mailmanager.controller;

import com.mailmanager.api.ApiResponse;
import com.mailmanager.service.ClientAppService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController extends BaseController {

    private final ClientAppService clientAppService;

    @PostMapping("/token")
    public ApiResponse<Map<String, String>> token(@RequestBody TokenRequest request, HttpServletRequest servletRequest) {
        String token = clientAppService.issueClientToken(request.clientId(), request.clientSecret());
        return ApiResponse.success(traceId(servletRequest), Map.of("access_token", token, "token_type", "Bearer"));
    }

    public record TokenRequest(
            @NotBlank(message = "client_id is required")
            String clientId,
            @NotBlank(message = "client_secret is required")
            String clientSecret
    ) {
    }
}
