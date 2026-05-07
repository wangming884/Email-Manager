package com.mailmanager.controller;

import com.mailmanager.api.ApiResponse;
import com.mailmanager.service.AdminAuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminAuthController extends BaseController {

    private final AdminAuthService adminAuthService;

    @PostMapping("/login")
    public ApiResponse<Map<String, String>> login(@RequestBody LoginRequest request, HttpServletRequest servletRequest) {
        String token = adminAuthService.login(request.username(), request.password());
        return ApiResponse.success(traceId(servletRequest), Map.of("access_token", token, "token_type", "Bearer"));
    }

    public record LoginRequest(
            @NotBlank(message = "username is required")
            String username,
            @NotBlank(message = "password is required")
            String password
    ) {
    }
}
