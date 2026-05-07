package com.mailmanager.service;

import com.mailmanager.config.AppSecurityProperties;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SecretCryptoServiceTest {

    @Test
    void shouldEncryptAndDecrypt() {
        SecretCryptoService service = new SecretCryptoService(new AppSecurityProperties(
                "admin",
                "admin123456",
                "0123456789abcdef0123456789abcdef",
                "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef",
                720,
                120
        ));

        String encrypted = service.encrypt("super-secret");

        assertThat(encrypted).isNotBlank().isNotEqualTo("super-secret");
        assertThat(service.decrypt(encrypted)).isEqualTo("super-secret");
    }
}
