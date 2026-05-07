package com.mailmanager.service;

import com.mailmanager.domain.AccountStatus;
import com.mailmanager.repository.AccountRepository;
import com.mailmanager.repository.ApiAuditLogRepository;
import com.mailmanager.repository.ImportJobRepository;
import com.mailmanager.repository.MailMessageRepository;
import com.mailmanager.repository.WebhookEndpointRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class OverviewService {

    private final AccountRepository accountRepository;
    private final ImportJobRepository importJobRepository;
    private final MailMessageRepository mailMessageRepository;
    private final WebhookEndpointRepository webhookEndpointRepository;
    private final ApiAuditLogRepository apiAuditLogRepository;

    public Map<String, Object> metrics() {
        return Map.of(
                "total_accounts", accountRepository.count(),
                "active_accounts", accountRepository.findByStatus(AccountStatus.ACTIVE).size(),
                "invalid_accounts", accountRepository.findByStatus(AccountStatus.INVALID).size(),
                "import_jobs", importJobRepository.count(),
                "emails", mailMessageRepository.count(),
                "webhooks", webhookEndpointRepository.count(),
                "api_audit_logs", apiAuditLogRepository.count()
        );
    }
}
