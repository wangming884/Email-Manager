package com.mailmanager.controller;

import com.mailmanager.api.ApiResponse;
import com.mailmanager.security.AccessGuard;
import com.mailmanager.service.OverviewService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/overview")
@RequiredArgsConstructor
public class OverviewController extends BaseController {

    private final OverviewService overviewService;
    private final AccessGuard accessGuard;

    @GetMapping
    public ApiResponse<Map<String, Object>> metrics(HttpServletRequest request) {
        accessGuard.requireAdmin();
        return ApiResponse.success(traceId(request), overviewService.metrics());
    }
}
