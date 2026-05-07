package com.mailmanager.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mailmanager.domain.ParserRule;
import com.mailmanager.domain.ParserRuleType;
import com.mailmanager.repository.ParserRuleRepository;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MailParserServiceTest {

    @Test
    void shouldExtractVerificationCode() {
        ParserRuleRepository repository = Mockito.mock(ParserRuleRepository.class);
        ParserRule rule = new ParserRule();
        rule.setName("code");
        rule.setType(ParserRuleType.VERIFICATION_CODE);
        rule.setSubjectKeyword("验证码");
        rule.setRegexPattern("(?<!\\d)(\\d{6})(?!\\d)");
        Mockito.when(repository.findByEnabledTrue()).thenReturn(List.of(rule));

        MailParserService service = new MailParserService(repository, new ObjectMapper());
        MailParserService.ParsedMailResult result = service.parse("您的验证码", "请使用验证码 654321 完成登录");

        assertThat(result.ruleName()).isEqualTo("code");
        assertThat(result.type()).isEqualTo(ParserRuleType.VERIFICATION_CODE);
        assertThat(result.jsonPayload()).contains("654321");
    }
}
