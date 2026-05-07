package com.mailmanager.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "parser_rules")
public class ParserRule extends BaseEntity {

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private ParserRuleType type;

    @Column(name = "subject_keyword", length = 255)
    private String subjectKeyword;

    @Column(name = "regex_pattern", nullable = false, length = 1000)
    private String regexPattern;

    @Column(nullable = false)
    private boolean enabled = true;

    @Column(name = "description", length = 500)
    private String description;
}
