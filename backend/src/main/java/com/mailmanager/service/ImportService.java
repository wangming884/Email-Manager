package com.mailmanager.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mailmanager.domain.Account;
import com.mailmanager.domain.ImportJob;
import com.mailmanager.domain.ImportJobError;
import com.mailmanager.domain.ImportJobStatus;
import com.mailmanager.domain.ProxyBinding;
import com.mailmanager.exception.BadRequestException;
import com.mailmanager.repository.ImportJobRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class ImportService {

    private final ImportJobRepository importJobRepository;
    private final ObjectMapper objectMapper;
    private final AccountService accountService;
    private final SecretCryptoService secretCryptoService;
    private final MailConnectivityService mailConnectivityService;
    private final RealtimeEventService realtimeEventService;
    @Qualifier("importTaskExecutor")
    private final Executor importTaskExecutor;

    public ImportJob importJson(List<Map<String, Object>> rows, String requestedBy) {
        ImportJob importJob = new ImportJob();
        importJob.setSourceType("JSON");
        importJob.setRequestedBy(requestedBy);
        importJob.setTotalCount(rows.size());
        importJobRepository.save(importJob);
        importTaskExecutor.execute(() -> processRows(importJob.getId(), normalizeRows(rows)));
        return importJob;
    }

    public ImportJob importText(byte[] content, String regexPattern, String requestedBy) {
        String source = new String(content, StandardCharsets.UTF_8);
        List<ImportRow> rows = parseTextRows(source, regexPattern);
        ImportJob importJob = new ImportJob();
        importJob.setSourceType("TEXT");
        importJob.setRegexPattern(regexPattern);
        importJob.setRequestedBy(requestedBy);
        importJob.setTotalCount(rows.size());
        importJobRepository.save(importJob);
        importTaskExecutor.execute(() -> processRows(importJob.getId(), rows));
        return importJob;
    }

    public List<ImportJob> listJobs() {
        return importJobRepository.findAll();
    }

    public ImportJob getJob(Long jobId) {
        return importJobRepository.findById(jobId).orElseThrow(() -> new BadRequestException("Import job not found"));
    }

    private List<ImportRow> normalizeRows(List<Map<String, Object>> rows) {
        return objectMapper.convertValue(rows, new TypeReference<List<ImportRow>>() {
        });
    }

    private List<ImportRow> parseTextRows(String source, String regexPattern) {
        Pattern pattern = Pattern.compile(regexPattern == null || regexPattern.isBlank()
                ? "^(?<email>[^:]+):(?<password>[^:]+)(:(?<proxy>[^:]+))?$"
                : regexPattern);
        List<ImportRow> rows = new ArrayList<>();
        String[] lines = source.split("\\R");
        for (String rawLine : lines) {
            if (rawLine.isBlank()) {
                continue;
            }
            Matcher matcher = pattern.matcher(rawLine.trim());
            if (!matcher.matches()) {
                throw new BadRequestException("Text import line does not match regex: " + rawLine);
            }
            rows.add(new ImportRow(
                    matcher.group("email"),
                    matcher.group("password"),
                    inferProvider(matcher.group("email")),
                    hostForProvider(inferProvider(matcher.group("email"))),
                    993,
                    true,
                    null,
                    587,
                    true,
                    null,
                    matcher.group("proxy")
            ));
        }
        return rows;
    }

    private void processRows(Long jobId, List<ImportRow> rows) {
        ImportJob importJob = getJob(jobId);
        importJob.setStatus(ImportJobStatus.RUNNING);
        importJobRepository.save(importJob);
        int processed = 0;
        int batchSize = 10; // Save progress every 10 rows to reduce DB pressure
        for (ImportRow row : rows) {
            processed++;
            try {
                Optional<ProxyBinding> proxyBinding = row.proxyBindingName() == null
                        ? Optional.empty()
                        : accountService.findProxyBindingByName(row.proxyBindingName());
                Account account = accountService.buildAccount(
                        row.email(),
                        row.provider() == null ? inferProvider(row.email()) : row.provider(),
                        row.imapHost() == null ? hostForProvider(inferProvider(row.email())) : row.imapHost(),
                        row.imapPort() == null ? 993 : row.imapPort(),
                        row.imapSslEnabled() == null || row.imapSslEnabled(),
                        row.smtpHost(),
                        row.smtpPort() == null ? 587 : row.smtpPort(),
                        row.smtpSslEnabled() == null || row.smtpSslEnabled(),
                        row.tags(),
                        proxyBinding.orElse(null),
                        secretCryptoService.encrypt(row.password())
                );
                account = accountService.save(account);
                mailConnectivityService.testAndUpdateStatus(account);
                accountService.save(account);
                importJob.setSuccessCount(importJob.getSuccessCount() + 1);
            } catch (Exception exception) {
                ImportJobError error = new ImportJobError();
                error.setImportJob(importJob);
                error.setLineNumber(processed);
                error.setRawContent(row.email());
                error.setErrorMessage(exception.getMessage() != null ? exception.getMessage() : exception.getClass().getSimpleName());
                importJob.getErrors().add(error);
                importJob.setErrorCount(importJob.getErrorCount() + 1);
            }
            
            // Batch save to reduce database pressure
            if (processed % batchSize == 0 || processed == rows.size()) {
                importJob.setProgress((int) Math.floor((processed * 100.0) / Math.max(rows.size(), 1)));
                importJobRepository.save(importJob);
                realtimeEventService.publishImportProgress(Map.of(
                        "job_id", importJob.getId(),
                        "status", importJob.getStatus().name(),
                        "progress", importJob.getProgress(),
                        "error_count", importJob.getErrorCount()
                ));
            }
        }
        importJob.setStatus(importJob.getErrorCount() > 0 ? ImportJobStatus.COMPLETED : ImportJobStatus.COMPLETED);
        importJob.setSummary("Imported %d/%d accounts".formatted(importJob.getSuccessCount(), importJob.getTotalCount()));
        importJob.setProgress(100);
        importJobRepository.save(importJob);
    }

    private String inferProvider(String email) {
        int atIndex = email.indexOf("@");
        return atIndex > 0 ? email.substring(atIndex + 1).toLowerCase() : "custom";
    }

    private String hostForProvider(String provider) {
        if (provider.contains("gmail")) {
            return "imap.gmail.com";
        }
        if (provider.contains("outlook") || provider.contains("hotmail") || provider.contains("live")) {
            return "outlook.office365.com";
        }
        if (provider.contains("qq")) {
            return "imap.qq.com";
        }
        return "imap." + provider;
    }

    public record ImportRow(
            String email,
            String password,
            String provider,
            String imapHost,
            Integer imapPort,
            Boolean imapSslEnabled,
            String smtpHost,
            Integer smtpPort,
            Boolean smtpSslEnabled,
            String tags,
            String proxyBindingName
    ) {
    }
}
