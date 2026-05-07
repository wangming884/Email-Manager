package com.mailmanager.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mailmanager.api.ApiResponse;
import com.mailmanager.api.JobStatusResponse;
import com.mailmanager.domain.Account;
import com.mailmanager.domain.AccountStatus;
import com.mailmanager.domain.ImportJob;
import com.mailmanager.exception.BadRequestException;
import com.mailmanager.security.AccessGuard;
import com.mailmanager.security.SessionPrincipal;
import com.mailmanager.service.AccountService;
import com.mailmanager.service.ImportService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class AccountController extends BaseController {

    private final AccountService accountService;
    private final ImportService importService;
    private final AccessGuard accessGuard;
    private final ObjectMapper objectMapper;

    @PostMapping(value = "/accounts/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<JobStatusResponse> importAccountsFromFile(
            @RequestPart("file") MultipartFile file,
            @RequestParam(value = "regex_pattern", required = false) String regexPattern,
            HttpServletRequest request
    ) throws Exception {
        SessionPrincipal principal = accessGuard.requireAnyScope("ACCOUNT_WRITE");
        if (file.isEmpty()) {
            throw new BadRequestException("Import file is required");
        }
        ImportJob job = importService.importText(file.getBytes(), regexPattern, principal.actorId());
        return ApiResponse.success(traceId(request), new JobStatusResponse(job.getId(), job.getStatus().name(), job.getProgress(), job.getErrorCount()));
    }

    @PostMapping(value = "/accounts/import", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ApiResponse<JobStatusResponse> importAccountsFromJson(
            @RequestBody ImportRowsRequest importRowsRequest,
            HttpServletRequest request
    ) {
        SessionPrincipal principal = accessGuard.requireAnyScope("ACCOUNT_WRITE");
        List<Map<String, Object>> rows = importRowsRequest.rows() == null ? new ArrayList<>() : importRowsRequest.rows();
        if (rows.isEmpty()) {
            throw new BadRequestException("rows is required");
        }
        ImportJob job = importService.importJson(rows, principal.actorId());
        return ApiResponse.success(traceId(request), new JobStatusResponse(job.getId(), job.getStatus().name(), job.getProgress(), job.getErrorCount()));
    }

    @GetMapping("/accounts")
    public ApiResponse<List<AccountResponse>> listAccounts(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String provider,
            @RequestParam(required = false) String tag,
            @RequestParam(name = "proxy_binding_id", required = false) Long proxyBindingId,
            HttpServletRequest request
    ) {
        accessGuard.requireAnyScope("ACCOUNT_READ", "ACCOUNT_WRITE");
        List<AccountResponse> responses = accountService.findAccounts(status, provider, tag, proxyBindingId).stream()
                .map(this::toResponse)
                .toList();
        return ApiResponse.success(traceId(request), responses);
    }

    @GetMapping("/accounts/{id}")
    public ApiResponse<AccountResponse> getAccount(@PathVariable Long id, HttpServletRequest request) {
        accessGuard.requireAnyScope("ACCOUNT_READ", "ACCOUNT_WRITE");
        return ApiResponse.success(traceId(request), toResponse(accountService.getAccount(id)));
    }

    @PatchMapping("/accounts/{id}")
    public ApiResponse<AccountResponse> updateAccount(@PathVariable Long id,
                                                      @RequestBody UpdateAccountRequest updateAccountRequest,
                                                      HttpServletRequest request) {
        accessGuard.requireAnyScope("ACCOUNT_WRITE");
        Account account = accountService.updateAccount(
                id,
                updateAccountRequest.status(),
                updateAccountRequest.tags(),
                updateAccountRequest.proxyBindingId(),
                updateAccountRequest.imapHost(),
                updateAccountRequest.imapPort(),
                updateAccountRequest.smtpHost(),
                updateAccountRequest.smtpPort()
        );
        return ApiResponse.success(traceId(request), toResponse(account));
    }

    @PostMapping("/accounts/{id}/test")
    public ApiResponse<AccountResponse> testAccount(@PathVariable Long id, HttpServletRequest request) {
        accessGuard.requireAnyScope("ACCOUNT_WRITE");
        return ApiResponse.success(traceId(request), toResponse(accountService.testAccount(id)));
    }

    @GetMapping("/accounts/export")
    public org.springframework.http.ResponseEntity<StreamingResponseBody> exportAccounts(
            @RequestParam(defaultValue = "csv") String format,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String provider,
            @RequestParam(required = false) String tag,
            HttpServletRequest request
    ) {
        accessGuard.requireAnyScope("ACCOUNT_READ");
        List<AccountResponse> rows = accountService.findAccounts(status, provider, tag, null).stream().map(this::toResponse).toList();
        StreamingResponseBody body = outputStream -> {
            if ("json".equalsIgnoreCase(format)) {
                objectMapper.writeValue(outputStream, rows);
                return;
            }
            try (OutputStreamWriter writer = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8)) {
                writer.write("id,email,provider,status,tags,imap_host,imap_port,last_synced_at\n");
                for (AccountResponse row : rows) {
                    writer.write("%d,%s,%s,%s,%s,%s,%d,%s\n".formatted(
                            row.id(),
                            row.email(),
                            row.provider(),
                            row.status(),
                            safe(row.tags()),
                            row.imapHost(),
                            row.imapPort(),
                            safe(row.lastSyncedAt())
                    ));
                }
            }
        };
        String contentType = "json".equalsIgnoreCase(format) ? MediaType.APPLICATION_JSON_VALUE : "text/csv";
        String filename = "accounts." + ("json".equalsIgnoreCase(format) ? "json" : "csv");
        return org.springframework.http.ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
                .contentType(MediaType.parseMediaType(contentType))
                .body(body);
    }

    @GetMapping("/import-jobs")
    public ApiResponse<List<ImportJobResponse>> listImportJobs(HttpServletRequest request) {
        accessGuard.requireAdmin();
        List<ImportJobResponse> jobs = importService.listJobs().stream().map(job -> new ImportJobResponse(
                job.getId(),
                job.getStatus().name(),
                job.getSourceType(),
                job.getProgress(),
                job.getTotalCount(),
                job.getSuccessCount(),
                job.getErrorCount(),
                job.getSummary(),
                job.getCreatedAt()
        )).toList();
        return ApiResponse.success(traceId(request), jobs);
    }

    @GetMapping("/import-jobs/{id}")
    public ApiResponse<ImportJobDetailResponse> getImportJob(@PathVariable Long id, HttpServletRequest request) {
        accessGuard.requireAdmin();
        ImportJob job = importService.getJob(id);
        return ApiResponse.success(traceId(request), new ImportJobDetailResponse(
                job.getId(),
                job.getStatus().name(),
                job.getProgress(),
                job.getTotalCount(),
                job.getSuccessCount(),
                job.getErrorCount(),
                job.getSummary(),
                job.getErrors().stream()
                        .map(error -> new ImportJobErrorResponse(error.getLineNumber(), error.getRawContent(), error.getErrorMessage()))
                        .toList()
        ));
    }

    private AccountResponse toResponse(Account account) {
        return new AccountResponse(
                account.getId(),
                account.getEmail(),
                account.getProvider(),
                account.getStatus().name(),
                account.getTags(),
                account.getImapHost(),
                account.getImapPort(),
                account.getSmtpHost(),
                account.getSmtpPort(),
                account.getLastTestedAt() == null ? null : account.getLastTestedAt().toString(),
                account.getLastSyncedAt() == null ? null : account.getLastSyncedAt().toString(),
                account.getLastErrorMessage(),
                account.getProxyBinding() == null ? null : account.getProxyBinding().getId(),
                account.getProxyBinding() == null ? null : account.getProxyBinding().getName()
        );
    }

    private String safe(String value) {
        return value == null ? "" : value.replace(",", " ");
    }

    public record UpdateAccountRequest(
            AccountStatus status,
            String tags,
            Long proxyBindingId,
            String imapHost,
            Integer imapPort,
            String smtpHost,
            Integer smtpPort
    ) {
    }

    public record AccountResponse(
            Long id,
            String email,
            String provider,
            String status,
            String tags,
            String imapHost,
            Integer imapPort,
            String smtpHost,
            Integer smtpPort,
            String lastTestedAt,
            String lastSyncedAt,
            String lastErrorMessage,
            Long proxyBindingId,
            String proxyBindingName
    ) {
    }

    public record ImportJobResponse(
            Long id,
            String status,
            String sourceType,
            Integer progress,
            Integer totalCount,
            Integer successCount,
            Integer errorCount,
            String summary,
            java.time.OffsetDateTime createdAt
    ) {
    }

    public record ImportJobDetailResponse(
            Long id,
            String status,
            Integer progress,
            Integer totalCount,
            Integer successCount,
            Integer errorCount,
            String summary,
            List<ImportJobErrorResponse> errors
    ) {
    }

    public record ImportJobErrorResponse(Integer lineNumber, String rawContent, String errorMessage) {
    }

    public record ImportRowsRequest(List<Map<String, Object>> rows) {
    }
}
