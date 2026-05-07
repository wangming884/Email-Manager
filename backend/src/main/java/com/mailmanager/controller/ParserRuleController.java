package com.mailmanager.controller;

import com.mailmanager.api.ApiResponse;
import com.mailmanager.domain.ParserRule;
import com.mailmanager.domain.ParserRuleType;
import com.mailmanager.repository.ParserRuleRepository;
import com.mailmanager.security.AccessGuard;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/parser-rules")
@RequiredArgsConstructor
public class ParserRuleController extends BaseController {

    private final ParserRuleRepository parserRuleRepository;
    private final AccessGuard accessGuard;

    @GetMapping
    public ApiResponse<List<ParserRuleResponse>> list(HttpServletRequest request) {
        accessGuard.requireAdmin();
        return ApiResponse.success(traceId(request), parserRuleRepository.findAll().stream().map(rule -> new ParserRuleResponse(
                rule.getId(),
                rule.getName(),
                rule.getType().name(),
                rule.getSubjectKeyword(),
                rule.getRegexPattern(),
                rule.isEnabled(),
                rule.getDescription()
        )).toList());
    }

    @PostMapping
    public ApiResponse<ParserRuleResponse> create(@RequestBody CreateParserRuleRequest createRequest, HttpServletRequest request) {
        accessGuard.requireAdmin();
        ParserRule rule = new ParserRule();
        rule.setName(createRequest.name());
        rule.setType(createRequest.type());
        rule.setSubjectKeyword(createRequest.subjectKeyword());
        rule.setRegexPattern(createRequest.regexPattern());
        rule.setEnabled(createRequest.enabled() == null || createRequest.enabled());
        rule.setDescription(createRequest.description());
        rule = parserRuleRepository.save(rule);
        return ApiResponse.success(traceId(request), new ParserRuleResponse(
                rule.getId(), rule.getName(), rule.getType().name(), rule.getSubjectKeyword(),
                rule.getRegexPattern(), rule.isEnabled(), rule.getDescription()
        ));
    }

    public record CreateParserRuleRequest(
            @NotBlank(message = "name is required")
            String name,
            ParserRuleType type,
            String subjectKeyword,
            @NotBlank(message = "regex_pattern is required")
            String regexPattern,
            Boolean enabled,
            String description
    ) {
    }

    public record ParserRuleResponse(Long id, String name, String type, String subjectKeyword, String regexPattern,
                                     boolean enabled, String description) {
    }
}
