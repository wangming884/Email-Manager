package com.mailmanager.repository;

import com.mailmanager.domain.ParserRule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ParserRuleRepository extends JpaRepository<ParserRule, Long> {

    List<ParserRule> findByEnabledTrue();
}
