package com.mailmanager.controller;

import com.mailmanager.api.ApiResponse;
import com.mailmanager.domain.WebhookEndpoint;
import com.mailmanager.repository.WebhookEndpointRepository;
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
@RequestMapping("/api/v1/webhooks")
@RequiredArgsConstructor
public class WebhookController extends BaseController {

    private final WebhookEndpointRepository webhookEndpointRepository;
    private final AccessGuard accessGuard;

    @GetMapping
    public ApiResponse<List<WebhookResponse>> list(HttpServletRequest request) {
        accessGuard.requireAdmin();
        return ApiResponse.success(traceId(request), webhookEndpointRepository.findAll().stream().map(endpoint ->
                new WebhookResponse(endpoint.getId(), endpoint.getName(), endpoint.getUrl(), endpoint.getEventType(),
                        endpoint.getSubjectKeyword(), endpoint.isActive())).toList());
    }

    @PostMapping
    public ApiResponse<WebhookResponse> create(@RequestBody CreateWebhookRequest createRequest, HttpServletRequest request) {
        accessGuard.requireAdmin();
        WebhookEndpoint endpoint = new WebhookEndpoint();
        endpoint.setName(createRequest.name());
        endpoint.setUrl(createRequest.url());
        endpoint.setEventType(createRequest.eventType());
        endpoint.setSubjectKeyword(createRequest.subjectKeyword());
        endpoint.setSharedSecret(createRequest.sharedSecret());
        endpoint.setActive(createRequest.active() == null || createRequest.active());
        endpoint = webhookEndpointRepository.save(endpoint);
        return ApiResponse.success(traceId(request), new WebhookResponse(
                endpoint.getId(), endpoint.getName(), endpoint.getUrl(), endpoint.getEventType(),
                endpoint.getSubjectKeyword(), endpoint.isActive()
        ));
    }

    public record CreateWebhookRequest(
            @NotBlank(message = "name is required")
            String name,
            @NotBlank(message = "url is required")
            String url,
            @NotBlank(message = "event_type is required")
            String eventType,
            String subjectKeyword,
            @NotBlank(message = "shared_secret is required")
            String sharedSecret,
            Boolean active
    ) {
    }

    public record WebhookResponse(Long id, String name, String url, String eventType, String subjectKeyword, boolean active) {
    }
}
