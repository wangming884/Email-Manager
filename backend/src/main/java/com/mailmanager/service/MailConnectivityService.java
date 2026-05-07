package com.mailmanager.service;

import com.mailmanager.config.SyncProperties;
import com.mailmanager.domain.Account;
import com.mailmanager.domain.AccountStatus;
import jakarta.mail.Session;
import jakarta.mail.Store;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class MailConnectivityService {

    private final SecretCryptoService secretCryptoService;
    private final SyncProperties syncProperties;
    private final ConcurrentHashMap<String, Semaphore> providerSemaphores = new ConcurrentHashMap<>();

    public void testAndUpdateStatus(Account account) {
        account.setStatus(AccountStatus.TESTING);
        account.setLastTestedAt(OffsetDateTime.now());
        Semaphore semaphore = providerSemaphores.computeIfAbsent(account.getProvider(),
                key -> new Semaphore(Math.max(1, syncProperties.providerConcurrency())));
        boolean acquired = false;
        try {
            semaphore.acquire();
            acquired = true;
            Thread.sleep(ThreadLocalRandom.current()
                    .nextInt(syncProperties.jitterMinMillis(), syncProperties.jitterMaxMillis() + 1));
            Properties properties = new Properties();
            properties.put("mail.store.protocol", "imap");
            properties.put("mail.imap.host", account.getImapHost());
            properties.put("mail.imap.port", String.valueOf(account.getImapPort()));
            properties.put("mail.imap.ssl.enable", String.valueOf(account.isImapSslEnabled()));
            if (account.getProxyBinding() != null) {
                properties.put("mail.imap.socks.host", account.getProxyBinding().getHost());
                properties.put("mail.imap.socks.port", String.valueOf(account.getProxyBinding().getPort()));
            }
            Session session = Session.getInstance(properties);
            try (Store store = session.getStore("imap")) {
                store.connect(account.getImapHost(), account.getEmail(),
                        secretCryptoService.decrypt(account.getCredential().getEncryptedPassword()));
            }
            account.setStatus(AccountStatus.ACTIVE);
            account.setLastErrorMessage(null);
        } catch (Exception exception) {
            account.setStatus(AccountStatus.INVALID);
            account.setLastErrorMessage(exception.getMessage());
        } finally {
            if (acquired) {
                semaphore.release();
            }
        }
    }
}
