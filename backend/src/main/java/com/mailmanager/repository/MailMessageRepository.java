package com.mailmanager.repository;

import com.mailmanager.domain.MailMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface MailMessageRepository extends JpaRepository<MailMessage, Long>, JpaSpecificationExecutor<MailMessage> {

    Optional<MailMessage> findByAccountIdAndExternalMessageId(Long accountId, String externalMessageId);
}
