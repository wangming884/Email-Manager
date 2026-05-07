package com.mailmanager.controller;

import com.mailmanager.api.ApiResponse;
import com.mailmanager.api.JobStatusResponse;
import com.mailmanager.domain.SyncJob;
import com.mailmanager.security.AccessGuard;
import com.mailmanager.security.SessionPrincipal;
import com.mailmanager.service.MailSyncService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/sync")
@RequiredArgsConstructor
public class SyncController extends BaseController {

    private final MailSyncService mailSyncService;
    private final AccessGuard accessGuard;

    @PostMapping("/jobs")
    public ApiResponse<JobStatusResponse> createSyncJob(@RequestBody SyncRequest request, HttpServletRequest servletRequest) {
        SessionPrincipal principal = accessGuard.requireAnyScope("SYNC_TRIGGER");
        SyncJob syncJob = mailSyncService.createSyncJob(request.accountIds(), principal.actorId());
        return ApiResponse.success(traceId(servletRequest), new JobStatusResponse(syncJob.getId(), syncJob.getStatus().name(), syncJob.getProgress(), syncJob.getErrorCount()));
    }

    @GetMapping("/jobs/{id}")
    public ApiResponse<JobStatusResponse> getSyncJob(@PathVariable Long id, HttpServletRequest servletRequest) {
        accessGuard.requireAnyScope("SYNC_TRIGGER", "ACCOUNT_READ");
        SyncJob syncJob = mailSyncService.getJob(id);
        return ApiResponse.success(traceId(servletRequest), new JobStatusResponse(syncJob.getId(), syncJob.getStatus().name(), syncJob.getProgress(), syncJob.getErrorCount()));
    }

    public record SyncRequest(List<Long> accountIds) {
    }
}
