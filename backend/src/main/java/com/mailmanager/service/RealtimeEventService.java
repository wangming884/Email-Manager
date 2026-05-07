package com.mailmanager.service;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class RealtimeEventService {

    private final SimpMessagingTemplate messagingTemplate;

    public void publishImportProgress(Map<String, Object> payload) {
        messagingTemplate.convertAndSend("/topic/import-progress", payload);
    }

    public void publishMailReceived(Map<String, Object> payload) {
        messagingTemplate.convertAndSend("/topic/mail-received", payload);
    }
}
