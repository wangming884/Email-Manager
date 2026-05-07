package com.mailmanager.controller;

import com.mailmanager.api.ApiResponse;
import com.mailmanager.domain.MailMessage;
import com.mailmanager.exception.NotFoundException;
import com.mailmanager.repository.MailMessageRepository;
import com.mailmanager.security.AccessGuard;
import jakarta.persistence.criteria.Predicate;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/v1/emails")
@RequiredArgsConstructor
public class EmailController extends BaseController {

    private final MailMessageRepository mailMessageRepository;
    private final AccessGuard accessGuard;

    @GetMapping("/query")
    public ApiResponse<List<EmailResponse>> queryEmails(
            @RequestParam(name = "to_email", required = false) String toEmail,
            @RequestParam(name = "subject_keyword", required = false) String subjectKeyword,
            @RequestParam(name = "received_after", required = false) String receivedAfter,
            @RequestParam(name = "account_id", required = false) Long accountId,
            @RequestParam(name = "parsed_type", required = false) String parsedType,
            HttpServletRequest request
    ) {
        accessGuard.requireAnyScope("EMAIL_READ");
        List<EmailResponse> responses = mailMessageRepository.findAll((root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (toEmail != null && !toEmail.isBlank()) {
                // Escape special LIKE characters to prevent SQL injection
                String escapedToEmail = escapeLikePattern(toEmail);
                predicates.add(builder.like(root.get("toEmail"), "%" + escapedToEmail + "%", '\\'));
            }
            if (subjectKeyword != null && !subjectKeyword.isBlank()) {
                // Escape special LIKE characters to prevent SQL injection
                String escapedSubject = escapeLikePattern(subjectKeyword);
                predicates.add(builder.like(root.get("subject"), "%" + escapedSubject + "%", '\\'));
            }
            if (receivedAfter != null && !receivedAfter.isBlank()) {
                predicates.add(builder.greaterThanOrEqualTo(root.get("receivedAt"), OffsetDateTime.parse(receivedAfter)));
            }
            if (accountId != null) {
                predicates.add(builder.equal(root.get("account").get("id"), accountId));
            }
            if (parsedType != null && !parsedType.isBlank()) {
                predicates.add(builder.equal(root.get("parsedType"), parsedType));
            }
            return builder.and(predicates.toArray(new Predicate[0]));
        }).stream().map(this::toResponse).toList();
        return ApiResponse.success(traceId(request), responses);
    }
    
    /**
     * Escape special LIKE pattern characters to prevent SQL injection
     */
    private String escapeLikePattern(String input) {
        return input.replace("\\", "\\\\")
                    .replace("%", "\\%")
                    .replace("_", "\\_");
    }

    @GetMapping("/{id}")
    public ApiResponse<EmailResponse> getEmail(@PathVariable Long id, HttpServletRequest request) {
        accessGuard.requireAnyScope("EMAIL_READ");
        MailMessage mailMessage = mailMessageRepository.findById(id).orElseThrow(() -> new NotFoundException("Mail not found"));
        return ApiResponse.success(traceId(request), toResponse(mailMessage));
    }

    @GetMapping("/export")
    public org.springframework.http.ResponseEntity<StreamingResponseBody> exportEmails(
            @RequestParam(defaultValue = "csv") String format,
            HttpServletRequest request
    ) {
        accessGuard.requireAnyScope("EMAIL_READ");
        List<EmailResponse> rows = mailMessageRepository.findAll().stream().map(this::toResponse).toList();
        StreamingResponseBody body = outputStream -> {
            if ("json".equalsIgnoreCase(format)) {
                new com.fasterxml.jackson.databind.ObjectMapper().writeValue(outputStream, rows);
                return;
            }
            try (OutputStreamWriter writer = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8)) {
                writer.write("id,account_email,to_email,subject,parsed_type,received_at\n");
                for (EmailResponse row : rows) {
                    writer.write("%d,%s,%s,%s,%s,%s\n".formatted(
                            row.id(),
                            safe(row.accountEmail()),
                            safe(row.toEmail()),
                            safe(row.subject()),
                            safe(row.parsedType()),
                            safe(row.receivedAt())
                    ));
                }
            }
        };
        return org.springframework.http.ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=emails." + ("json".equalsIgnoreCase(format) ? "json" : "csv"))
                .contentType(MediaType.parseMediaType("json".equalsIgnoreCase(format) ? MediaType.APPLICATION_JSON_VALUE : "text/csv"))
                .body(body);
    }

    private EmailResponse toResponse(MailMessage mailMessage) {
        return new EmailResponse(
                mailMessage.getId(),
                mailMessage.getAccount().getId(),
                mailMessage.getAccount().getEmail(),
                mailMessage.getFromEmail(),
                mailMessage.getToEmail(),
                mailMessage.getSubject(),
                mailMessage.getRawContent(),
                mailMessage.getParsedJson(),
                mailMessage.getParsedType(),
                mailMessage.getMatchedRuleName(),
                mailMessage.getReceivedAt() == null ? null : mailMessage.getReceivedAt().toString()
        );
    }

    private String safe(String value) {
        return value == null ? "" : value.replace(",", " ").replace("\n", " ");
    }

    public record EmailResponse(
            Long id,
            Long accountId,
            String accountEmail,
            String fromEmail,
            String toEmail,
            String subject,
            String rawContent,
            String parsedJson,
            String parsedType,
            String matchedRuleName,
            String receivedAt
    ) {
    }
}
