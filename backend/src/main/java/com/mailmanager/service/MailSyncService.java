package com.mailmanager.service;

import com.mailmanager.config.SyncProperties;
import com.mailmanager.domain.Account;
import com.mailmanager.domain.AccountStatus;
import com.mailmanager.domain.MailMessage;
import com.mailmanager.domain.SyncJob;
import com.mailmanager.domain.SyncJobStatus;
import com.mailmanager.exception.BadRequestException;
import com.mailmanager.repository.AccountRepository;
import com.mailmanager.repository.MailMessageRepository;
import com.mailmanager.repository.SyncJobRepository;
import jakarta.mail.Address;
import jakarta.mail.BodyPart;
import jakarta.mail.Folder;
import jakarta.mail.Message;
import jakarta.mail.Multipart;
import jakarta.mail.Session;
import jakarta.mail.Store;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MailSyncService {

    private final AccountRepository accountRepository;
    private final MailMessageRepository mailMessageRepository;
    private final SyncJobRepository syncJobRepository;
    private final SecretCryptoService secretCryptoService;
    private final SyncProperties syncProperties;
    private final MailParserService mailParserService;
    private final WebhookService webhookService;
    private final RealtimeEventService realtimeEventService;
    private final AccountService accountService;
    @Qualifier("syncTaskExecutor")
    private final Executor syncTaskExecutor;

    public SyncJob createSyncJob(List<Long> accountIds, String requestedBy) {
        List<Account> accounts = (accountIds == null || accountIds.isEmpty())
                ? accountRepository.findByStatus(AccountStatus.ACTIVE)
                : accountRepository.findAllById(accountIds).stream()
                .filter(account -> account.getStatus() == AccountStatus.ACTIVE)
                .toList();
        if (accounts.isEmpty()) {
            throw new BadRequestException("No active accounts available for sync");
        }
        SyncJob syncJob = new SyncJob();
        syncJob.setRequestedBy(requestedBy);
        syncJob.setTotalCount(accounts.size());
        syncJobRepository.save(syncJob);
        syncTaskExecutor.execute(() -> runSync(syncJob.getId(), accounts));
        return syncJob;
    }

    public SyncJob getJob(Long jobId) {
        return syncJobRepository.findById(jobId).orElseThrow(() -> new BadRequestException("Sync job not found"));
    }

    private void runSync(Long jobId, List<Account> accounts) {
        SyncJob syncJob = getJob(jobId);
        syncJob.setStatus(SyncJobStatus.RUNNING);
        syncJobRepository.save(syncJob);
        int processed = 0;
        int batchSize = 5; // Save progress every 5 accounts to reduce DB pressure
        for (Account account : accounts) {
            processed++;
            try {
                syncAccount(account);
                syncJob.setSuccessCount(syncJob.getSuccessCount() + 1);
            } catch (Exception exception) {
                syncJob.setErrorCount(syncJob.getErrorCount() + 1);
                accountService.markSyncResult(account, false, exception.getMessage());
            }
            
            // Batch save to reduce database pressure
            if (processed % batchSize == 0 || processed == accounts.size()) {
                syncJob.setProgress((int) Math.floor((processed * 100.0) / Math.max(accounts.size(), 1)));
                syncJobRepository.save(syncJob);
            }
        }
        syncJob.setStatus(syncJob.getErrorCount() > 0 ? SyncJobStatus.COMPLETED : SyncJobStatus.COMPLETED);
        syncJob.setProgress(100);
        syncJob.setSummary("Synced %d/%d accounts".formatted(syncJob.getSuccessCount(), syncJob.getTotalCount()));
        syncJobRepository.save(syncJob);
    }

    private void syncAccount(Account account) throws Exception {
        Properties properties = new Properties();
        properties.put("mail.store.protocol", "imap");
        properties.put("mail.imap.host", account.getImapHost());
        properties.put("mail.imap.port", String.valueOf(account.getImapPort()));
        properties.put("mail.imap.ssl.enable", String.valueOf(account.isImapSslEnabled()));
        properties.put("mail.imap.connectiontimeout", "10000"); // 10 seconds
        properties.put("mail.imap.timeout", "10000"); // 10 seconds
        if (account.getProxyBinding() != null) {
            properties.put("mail.imap.socks.host", account.getProxyBinding().getHost());
            properties.put("mail.imap.socks.port", String.valueOf(account.getProxyBinding().getPort()));
        }
        Session session = Session.getInstance(properties);
        Store store = null;
        Folder inbox = null;
        try {
            store = session.getStore("imap");
            store.connect(account.getImapHost(), account.getEmail(),
                    secretCryptoService.decrypt(account.getCredential().getEncryptedPassword()));
            inbox = store.getFolder("INBOX");
            inbox.open(Folder.READ_ONLY);
            int count = Math.min(inbox.getMessageCount(), syncProperties.fetchLimit());
            if (count == 0) {
                accountService.markSyncResult(account, true, null);
                return;
            }
            Message[] messages = inbox.getMessages(Math.max(1, inbox.getMessageCount() - count + 1), inbox.getMessageCount());
            for (Message message : messages) {
                try {
                    saveMessage(account, message);
                } catch (Exception e) {
                    // Log and continue with next message
                    System.err.println("Failed to save message: " + e.getMessage());
                }
            }
            accountService.markSyncResult(account, true, null);
        } finally {
            // Ensure resources are always closed
            if (inbox != null && inbox.isOpen()) {
                try {
                    inbox.close(false);
                } catch (Exception e) {
                    System.err.println("Failed to close inbox: " + e.getMessage());
                }
            }
            if (store != null && store.isConnected()) {
                try {
                    store.close();
                } catch (Exception e) {
                    System.err.println("Failed to close store: " + e.getMessage());
                }
            }
        }
    }

    private void saveMessage(Account account, Message message) throws Exception {
        String[] messageIdHeaders = message.getHeader("Message-ID");
        String externalMessageId = (messageIdHeaders != null && messageIdHeaders.length > 0)
                ? messageIdHeaders[0]
                : (message.getSubject() != null ? message.getSubject() : "unknown") + "-" + 
                  (message.getReceivedDate() != null ? message.getReceivedDate().getTime() : System.currentTimeMillis());
        
        if (mailMessageRepository.findByAccountIdAndExternalMessageId(account.getId(), externalMessageId).isPresent()) {
            return;
        }
        String body = extractBody(message);
        MailParserService.ParsedMailResult parsed = mailParserService.parse(message.getSubject(), body);
        MailMessage mailMessage = new MailMessage();
        mailMessage.setAccount(account);
        mailMessage.setExternalMessageId(externalMessageId);
        mailMessage.setFromEmail(joinAddresses(message.getFrom()));
        mailMessage.setToEmail(joinAddresses(message.getRecipients(Message.RecipientType.TO)));
        mailMessage.setSubject(message.getSubject());
        mailMessage.setRawContent(body);
        mailMessage.setParsedJson(parsed.jsonPayload());
        mailMessage.setParsedType(parsed.type() == null ? null : parsed.type().name());
        mailMessage.setMatchedRuleName(parsed.ruleName());
        mailMessage.setReceivedAt(message.getReceivedDate() == null
                ? OffsetDateTime.now()
                : message.getReceivedDate().toInstant().atOffset(ZoneOffset.UTC));
        mailMessage = mailMessageRepository.save(mailMessage);
        realtimeEventService.publishMailReceived(Map.of(
                "mail_id", mailMessage.getId(),
                "account_email", account.getEmail(),
                "subject", mailMessage.getSubject() != null ? mailMessage.getSubject() : "",
                "parsed_type", mailMessage.getParsedType() != null ? mailMessage.getParsedType() : ""
        ));
        if (parsed.type() != null) {
            webhookService.dispatchMailReceived(mailMessage);
        }
    }

    private String joinAddresses(Address[] addresses) {
        if (addresses == null || addresses.length == 0) {
            return null;
        }
        return Arrays.stream(addresses).map(Address::toString).collect(Collectors.joining(","));
    }

    private String extractBody(Message message) throws Exception {
        Object content = message.getContent();
        if (content instanceof String text) {
            return text;
        }
        if (content instanceof Multipart multipart) {
            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < multipart.getCount(); i++) {
                BodyPart bodyPart = multipart.getBodyPart(i);
                Object bodyContent = bodyPart.getContent();
                if (bodyContent instanceof String text) {
                    builder.append(text);
                } else if (bodyContent instanceof java.io.InputStream inputStream) {
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
                        builder.append(reader.lines().collect(Collectors.joining("\n")));
                    }
                }
            }
            return builder.toString();
        }
        return String.valueOf(content);
    }
}
