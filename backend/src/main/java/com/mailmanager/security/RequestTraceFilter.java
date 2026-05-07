package com.mailmanager.security;

import com.mailmanager.domain.ActorType;
import com.mailmanager.domain.ApiAuditLog;
import com.mailmanager.repository.ApiAuditLogRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestTraceFilter extends OncePerRequestFilter {

    private final ApiAuditLogRepository apiAuditLogRepository;

    public RequestTraceFilter(ApiAuditLogRepository apiAuditLogRepository) {
        this.apiAuditLogRepository = apiAuditLogRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String traceId = UUID.randomUUID().toString();
        request.setAttribute("traceId", traceId);
        response.setHeader("X-Trace-Id", traceId);
        filterChain.doFilter(request, response);
        if (request.getRequestURI().startsWith("/api/")) {
            ApiAuditLog log = new ApiAuditLog();
            log.setTraceId(traceId);
            log.setMethod(request.getMethod());
            log.setPath(request.getRequestURI());
            log.setStatusCode(response.getStatus());
            log.setClientIp(request.getRemoteAddr());
            log.setSummary(request.getQueryString());
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.getPrincipal() instanceof SessionPrincipal principal) {
                log.setActorType(principal.actorType());
                log.setActorId(principal.actorId());
            } else {
                log.setActorType(ActorType.ANONYMOUS);
                log.setActorId("anonymous");
            }
            apiAuditLogRepository.save(log);
        }
    }
}
