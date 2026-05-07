package com.mailmanager.service;

import com.mailmanager.domain.Account;
import com.mailmanager.domain.AccountCredential;
import com.mailmanager.domain.AccountStatus;
import com.mailmanager.domain.ProxyBinding;
import com.mailmanager.exception.BadRequestException;
import com.mailmanager.exception.NotFoundException;
import com.mailmanager.repository.AccountRepository;
import com.mailmanager.repository.ProxyBindingRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;
    private final ProxyBindingRepository proxyBindingRepository;
    private final AccountStatusMachine accountStatusMachine;
    private final MailConnectivityService mailConnectivityService;

    public Account getAccount(Long id) {
        return accountRepository.findById(id).orElseThrow(() -> new NotFoundException("Account not found"));
    }

    public List<Account> findAccounts(String status, String provider, String tag, Long proxyBindingId) {
        Specification<Account> specification = (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (status != null && !status.isBlank()) {
                predicates.add(builder.equal(root.get("status"), AccountStatus.valueOf(status)));
            }
            if (provider != null && !provider.isBlank()) {
                predicates.add(builder.equal(root.get("provider"), provider));
            }
            if (tag != null && !tag.isBlank()) {
                predicates.add(builder.like(root.get("tags"), "%" + tag + "%"));
            }
            if (proxyBindingId != null) {
                predicates.add(builder.equal(root.get("proxyBinding").get("id"), proxyBindingId));
            }
            return builder.and(predicates.toArray(new Predicate[0]));
        };
        return accountRepository.findAll(specification);
    }

    public Account updateAccount(Long id, AccountStatus status, String tags, Long proxyBindingId,
                                 String imapHost, Integer imapPort, String smtpHost, Integer smtpPort) {
        Account account = getAccount(id);
        if (status != null && !accountStatusMachine.canTransition(account.getStatus(), status)) {
            throw new BadRequestException("Invalid account status transition");
        }
        if (status != null) {
            account.setStatus(status);
        }
        if (tags != null) {
            account.setTags(tags);
        }
        if (proxyBindingId != null) {
            ProxyBinding proxyBinding = proxyBindingRepository.findById(proxyBindingId)
                    .orElseThrow(() -> new NotFoundException("Proxy binding not found"));
            account.setProxyBinding(proxyBinding);
        }
        if (imapHost != null) {
            account.setImapHost(imapHost);
        }
        if (imapPort != null) {
            account.setImapPort(imapPort);
        }
        if (smtpHost != null) {
            account.setSmtpHost(smtpHost);
        }
        if (smtpPort != null) {
            account.setSmtpPort(smtpPort);
        }
        return accountRepository.save(account);
    }

    public Account testAccount(Long id) {
        Account account = getAccount(id);
        mailConnectivityService.testAndUpdateStatus(account);
        return accountRepository.save(account);
    }

    public Account save(Account account) {
        return accountRepository.save(account);
    }

    public Account buildAccount(String email, String provider, String imapHost, Integer imapPort, boolean imapSslEnabled,
                                String smtpHost, Integer smtpPort, boolean smtpSslEnabled, String tags,
                                ProxyBinding proxyBinding, String encryptedPassword) {
        Account account = new Account();
        account.setEmail(email);
        account.setProvider(provider);
        account.setImapHost(imapHost);
        account.setImapPort(imapPort);
        account.setImapSslEnabled(imapSslEnabled);
        account.setSmtpHost(smtpHost);
        account.setSmtpPort(smtpPort);
        account.setSmtpSslEnabled(smtpSslEnabled);
        account.setTags(tags);
        account.setStatus(AccountStatus.TESTING);
        account.setProxyBinding(proxyBinding);
        AccountCredential credential = new AccountCredential();
        credential.setAccount(account);
        credential.setEncryptedPassword(encryptedPassword);
        account.setCredential(credential);
        return account;
    }

    public Optional<ProxyBinding> findProxyBindingByName(String name) {
        return proxyBindingRepository.findAll().stream()
                .filter(proxy -> proxy.getName().equalsIgnoreCase(name))
                .findFirst();
    }

    public void markSyncResult(Account account, boolean success, String errorMessage) {
        account.setLastSyncedAt(OffsetDateTime.now());
        if (!success) {
            account.setLastErrorMessage(errorMessage);
        }
        accountRepository.save(account);
    }
}
