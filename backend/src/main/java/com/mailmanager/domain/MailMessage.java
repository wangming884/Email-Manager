package com.mailmanager.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

@Getter
@Setter
@Entity
@Table(name = "mail_messages", indexes = {
    @Index(name = "idx_account_external_msg", columnList = "account_id,external_message_id", unique = true),
    @Index(name = "idx_received_at", columnList = "received_at"),
    @Index(name = "idx_parsed_type", columnList = "parsed_type"),
    @Index(name = "idx_to_email", columnList = "to_email"),
    @Index(name = "idx_subject", columnList = "subject")
})
public class MailMessage extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @Column(name = "external_message_id", length = 255)
    private String externalMessageId;

    @Column(name = "from_email", length = 255)
    private String fromEmail;

    @Column(name = "to_email", length = 255)
    private String toEmail;

    @Column(length = 500)
    private String subject;

    @Column(name = "raw_content", columnDefinition = "TEXT")
    private String rawContent;

    @Column(name = "parsed_json", columnDefinition = "TEXT")
    private String parsedJson;

    @Column(name = "parsed_type", length = 50)
    private String parsedType;

    @Column(name = "matched_rule_name", length = 255)
    private String matchedRuleName;

    @Column(name = "received_at", nullable = false)
    private OffsetDateTime receivedAt;
}
