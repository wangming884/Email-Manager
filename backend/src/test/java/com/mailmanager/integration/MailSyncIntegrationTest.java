package com.mailmanager.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.icegreen.greenmail.junit5.GreenMailExtension;
import com.icegreen.greenmail.util.GreenMailUtil;
import com.icegreen.greenmail.util.ServerSetupTest;
import com.mailmanager.domain.Account;
import com.mailmanager.domain.AccountStatus;
import com.mailmanager.domain.MailMessage;
import com.mailmanager.repository.AccountRepository;
import com.mailmanager.repository.MailMessageRepository;
import com.mailmanager.service.SecretCryptoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
class MailSyncIntegrationTest {

    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.4")
            .withDatabaseName("mail_manager")
            .withUsername("mail_manager")
            .withPassword("mail_manager");

    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7.4-alpine").withExposedPorts(6379);

    @RegisterExtension
    static GreenMailExtension greenMail = new GreenMailExtension(new com.icegreen.greenmail.util.ServerSetup[]{
            ServerSetupTest.SMTP,
            ServerSetupTest.IMAP
    });

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private MailMessageRepository mailMessageRepository;

    @Autowired
    private SecretCryptoService secretCryptoService;

    @BeforeEach
    void setUpMailbox() {
        mailMessageRepository.deleteAll();
        accountRepository.deleteAll();
        greenMail.setUser("receiver@test.com", "receiver@test.com", "secret123");

        Account account = new Account();
        account.setEmail("receiver@test.com");
        account.setProvider("test.com");
        account.setStatus(AccountStatus.ACTIVE);
        account.setImapHost("127.0.0.1");
        account.setImapPort(greenMail.getImap().getPort());
        account.setImapSslEnabled(false);
        account.setSmtpHost("127.0.0.1");
        account.setSmtpPort(greenMail.getSmtp().getPort());
        account.setSmtpSslEnabled(false);
        account = accountRepository.save(account);

        var credential = new com.mailmanager.domain.AccountCredential();
        credential.setAccount(account);
        credential.setEncryptedPassword(secretCryptoService.encrypt("secret123"));
        account.setCredential(credential);
        accountRepository.save(account);

        GreenMailUtil.sendTextEmailTest("receiver@test.com", "sender@test.com", "验证码通知", "您的验证码是 123456");
    }

    @Test
    void shouldSyncAndParseIncomingMail() throws Exception {
        String adminToken = mockMvc.perform(post("/api/v1/admin/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"admin","password":"admin123456"}
                                """))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String token = objectMapper.readTree(adminToken).path("data").path("access_token").asText();

        mockMvc.perform(post("/api/v1/sync/jobs")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"account_ids":[]}
                                """))
                .andExpect(status().isOk());

        Instant deadline = Instant.now().plus(Duration.ofSeconds(15));
        while (Instant.now().isBefore(deadline) && mailMessageRepository.count() == 0) {
            Thread.sleep(500);
        }

        assertThat(mailMessageRepository.count()).isGreaterThan(0);
        MailMessage mailMessage = mailMessageRepository.findAll().get(0);
        assertThat(mailMessage.getParsedType()).isEqualTo("VERIFICATION_CODE");
        assertThat(mailMessage.getParsedJson()).contains("123456");
    }
}
