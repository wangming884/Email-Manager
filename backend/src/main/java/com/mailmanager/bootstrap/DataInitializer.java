package com.mailmanager.bootstrap;

import com.mailmanager.config.BootstrapProperties;
import com.mailmanager.domain.ClientApp;
import com.mailmanager.domain.ParserRule;
import com.mailmanager.domain.ParserRuleType;
import com.mailmanager.repository.ClientAppRepository;
import com.mailmanager.repository.ParserRuleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final BootstrapProperties bootstrapProperties;
    private final ClientAppRepository clientAppRepository;
    private final ParserRuleRepository parserRuleRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (!bootstrapProperties.enabled()) {
            return;
        }
        clientAppRepository.findByClientId(bootstrapProperties.defaultClientId()).orElseGet(() -> {
            ClientApp clientApp = new ClientApp();
            clientApp.setClientId(bootstrapProperties.defaultClientId());
            clientApp.setName("Default Core Platform");
            clientApp.setScopes("ACCOUNT_READ,ACCOUNT_WRITE,EMAIL_READ,SYNC_TRIGGER");
            clientApp.setClientSecretHash(passwordEncoder.encode(bootstrapProperties.defaultClientSecret()));
            return clientAppRepository.save(clientApp);
        });
        ensureRule("verification-code-default", ParserRuleType.VERIFICATION_CODE, "验证码", "(?<!\\d)(\\d{4,8})(?!\\d)", "Extract 4-8 digit verification code");
        ensureRule("activation-link-default", ParserRuleType.ACTIVATION_LINK, "激活", "(https?://[^\\s\"']+)", "Extract first activation link");
    }

    private void ensureRule(String name, ParserRuleType type, String subjectKeyword, String pattern, String description) {
        boolean exists = parserRuleRepository.findAll().stream().anyMatch(rule -> rule.getName().equals(name));
        if (exists) {
            return;
        }
        ParserRule rule = new ParserRule();
        rule.setName(name);
        rule.setType(type);
        rule.setSubjectKeyword(subjectKeyword);
        rule.setRegexPattern(pattern);
        rule.setDescription(description);
        rule.setEnabled(true);
        parserRuleRepository.save(rule);
    }
}
