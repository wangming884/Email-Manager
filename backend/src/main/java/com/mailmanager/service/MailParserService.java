package com.mailmanager.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mailmanager.domain.ParserRule;
import com.mailmanager.domain.ParserRuleType;
import com.mailmanager.repository.ParserRuleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class MailParserService {

    private final ParserRuleRepository parserRuleRepository;
    private final ObjectMapper objectMapper;

    public ParsedMailResult parse(String subject, String body) {
        List<ParserRule> rules = parserRuleRepository.findByEnabledTrue();
        for (ParserRule rule : rules) {
            try {
                if (rule.getSubjectKeyword() != null && !rule.getSubjectKeyword().isBlank()) {
                    if (subject == null || !subject.contains(rule.getSubjectKeyword())) {
                        continue;
                    }
                }
                Pattern pattern = Pattern.compile(rule.getRegexPattern(), Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
                Matcher matcher = pattern.matcher(body != null ? body : "");
                if (matcher.find()) {
                    Map<String, Object> payload = new LinkedHashMap<>();
                    payload.put("rule_name", rule.getName());
                    payload.put("type", rule.getType().name());
                    switch (rule.getType()) {
                        case VERIFICATION_CODE -> {
                            if (matcher.groupCount() >= 1) {
                                payload.put("verification_code", matcher.group(1));
                            }
                        }
                        case ACTIVATION_LINK -> {
                            if (matcher.groupCount() >= 1) {
                                payload.put("activation_link", matcher.group(1));
                            }
                        }
                        case CUSTOM_REGEX -> payload.put("matches", matcher.group());
                    }
                    return new ParsedMailResult(rule.getName(), rule.getType(), toJson(payload));
                }
            } catch (Exception e) {
                // Log error and continue with next rule
                System.err.println("Failed to apply parser rule '" + rule.getName() + "': " + e.getMessage());
            }
        }
        return new ParsedMailResult(null, null, "{}");
    }

    public record ParsedMailResult(String ruleName, ParserRuleType type, String jsonPayload) {
    }

    private String toJson(Map<String, Object> payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to serialize parsed payload", exception);
        }
    }
}
