package com.mailmanager.api;

public record JobStatusResponse(
        Long jobId,
        String status,
        int progress,
        int errorCount
) {
}
