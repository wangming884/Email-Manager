package com.mailmanager.controller;

import com.mailmanager.api.ApiResponse;
import com.mailmanager.repository.ApiAuditLogRepository;
import com.mailmanager.security.AccessGuard;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/audit-logs")
@RequiredArgsConstructor
public class AuditLogController extends BaseController {

    private final ApiAuditLogRepository apiAuditLogRepository;
    private final AccessGuard accessGuard;

    @GetMapping
    public ApiResponse<List<AuditLogResponse>> list(HttpServletRequest request) {
        accessGuard.requireAdmin();
        List<AuditLogResponse> logs = apiAuditLogRepository.findAll().stream().map(log -> new AuditLogResponse(
                log.getId(),
                log.getTraceId(),
                log.getActorType().name(),
                log.getActorId(),
                log.getMethod(),
                log.getPath(),
                log.getStatusCode(),
                log.getClientIp(),
                log.getCreatedAt()
        )).toList();
        return ApiResponse.success(traceId(request), logs);
    }

    public record AuditLogResponse(Long id, String traceId, String actorType, String actorId, String method,
                                   String path, Integer statusCode, String clientIp, java.time.OffsetDateTime createdAt) {
    }
}
