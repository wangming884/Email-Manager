package com.mailmanager.service;

import com.mailmanager.domain.AccountStatus;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AccountStatusMachineTest {

    private final AccountStatusMachine machine = new AccountStatusMachine();

    @Test
    void shouldAllowExpectedTransitions() {
        assertThat(machine.canTransition(AccountStatus.TESTING, AccountStatus.ACTIVE)).isTrue();
        assertThat(machine.canTransition(AccountStatus.ACTIVE, AccountStatus.LOCKED)).isTrue();
        assertThat(machine.canTransition(AccountStatus.LOCKED, AccountStatus.ACTIVE)).isFalse();
    }
}
