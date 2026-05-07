package com.mailmanager.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "webhook_endpoints")
public class WebhookEndpoint extends BaseEntity {

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Column(nullable = false, length = 500)
    private String url;

    @Column(name = "event_type", nullable = false, length = 50)
    private String eventType;

    @Column(name = "subject_keyword", length = 255)
    private String subjectKeyword;

    @Column(name = "shared_secret", nullable = false, length = 255)
    private String sharedSecret;

    @Column(nullable = false)
    private boolean active = true;
}
