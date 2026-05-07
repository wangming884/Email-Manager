package com.mailmanager.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "import_jobs")
public class ImportJob extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ImportJobStatus status = ImportJobStatus.PENDING;

    @Column(nullable = false)
    private Integer totalCount = 0;

    @Column(nullable = false)
    private Integer successCount = 0;

    @Column(nullable = false)
    private Integer errorCount = 0;

    @Column(nullable = false)
    private Integer progress = 0;

    @Column(name = "source_type", nullable = false, length = 30)
    private String sourceType;

    @Column(name = "regex_pattern", length = 500)
    private String regexPattern;

    @Column(name = "requested_by", length = 120)
    private String requestedBy;

    @Column(length = 1000)
    private String summary;

    @OneToMany(mappedBy = "importJob", cascade = CascadeType.ALL, orphanRemoval = true, fetch = jakarta.persistence.FetchType.EAGER)
    private List<ImportJobError> errors = new ArrayList<>();
}
