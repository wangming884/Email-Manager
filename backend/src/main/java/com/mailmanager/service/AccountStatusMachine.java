package com.mailmanager.service;

import com.mailmanager.domain.AccountStatus;
import org.springframework.stereotype.Component;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

@Component
public class AccountStatusMachine {

    private final Map<AccountStatus, Set<AccountStatus>> transitions = Map.of(
            AccountStatus.TESTING, EnumSet.of(AccountStatus.ACTIVE, AccountStatus.INVALID, AccountStatus.LOCKED),
            AccountStatus.ACTIVE, EnumSet.of(AccountStatus.TESTING, AccountStatus.INVALID, AccountStatus.LOCKED),
            AccountStatus.INVALID, EnumSet.of(AccountStatus.TESTING, AccountStatus.LOCKED),
            AccountStatus.LOCKED, EnumSet.of(AccountStatus.TESTING, AccountStatus.INVALID)
    );

    public boolean canTransition(AccountStatus from, AccountStatus to) {
        if (from == to) {
            return true;
        }
        return transitions.getOrDefault(from, Set.of()).contains(to);
    }
}
