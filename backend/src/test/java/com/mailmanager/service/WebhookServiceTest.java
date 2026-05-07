package com.mailmanager.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mailmanager.config.WebhookProperties;
import com.mailmanager.repository.WebhookDeliveryRepository;
import com.mailmanager.repository.WebhookEndpointRepository;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;

class WebhookServiceTest {

    @Test
    void shouldGenerateDeterministicSignature() {
        WebhookService service = new WebhookService(
                Mockito.mock(WebhookEndpointRepository.class),
                Mockito.mock(WebhookDeliveryRepository.class),
                new WebhookProperties(5, 3),
                new ObjectMapper()
        );

        String signature = service.sign("{\"event\":\"mail.received\"}", "2026-05-07T00:00:00Z", "shared-secret");

        assertThat(signature).isEqualTo("b92126734ee072b4eb8119d6866fcbe103fb906a662e30f7626b33ad6fe5aca8");
    }
}
