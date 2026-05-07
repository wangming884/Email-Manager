package com.mailmanager.repository;

import com.mailmanager.domain.AccountCredential;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountCredentialRepository extends JpaRepository<AccountCredential, Long> {
}
