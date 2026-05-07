package com.mailmanager.repository;

import com.mailmanager.domain.ImportJobError;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ImportJobErrorRepository extends JpaRepository<ImportJobError, Long> {
}
