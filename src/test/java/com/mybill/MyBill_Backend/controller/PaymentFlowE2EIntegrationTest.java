package com.mybill.MyBill_Backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mybill.MyBill_Backend.dto.InvoiceRequest;
import com.mybill.MyBill_Backend.entity.*;
import com.mybill.MyBill_Backend.repository.*;
import com.mybill.MyBill_Backend.security.JwtUtil;
import com.mybill.MyBill_Backend.security.RateLimitFilter;
import com.mybill.MyBill_Backend.service.InvoiceNumberService;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:payment_flow_e2e_test;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH",
        "spring.flyway.enabled=false",
        "spring.flyway.baseline-on-migrate=true",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@Transactional
class PaymentFlowE2EIntegrationTest {

    static {
        com.mybill.MyBill_Backend.MigrationPreprocessor.process();
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private ClientWorkRepository clientWorkRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RateLimitFilter rateLimitFilter;

    @MockitoBean
    private InvoiceNumberService invoiceNumberService;

    @Autowired
    private javax.sql.DataSource dataSource;

    private User testUser;
    private Client testClient;
    private ClientWork testWork;
    private String jwtToken;

    @BeforeEach
    void setUp() {
        Flyway flyway = Flyway.configure()
                .dataSource(dataSource)
                .baselineOnMigrate(true)
                .placeholders(Collections.singletonMap("createExtensionCommand", "SELECT 1"))
                .validateOnMigrate(false)
                .load();
        flyway.migrate();

        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        try {
            jdbc.execute("ALTER TABLE customer_notification_templates ALTER COLUMN is_deleted SET DEFAULT false");
            jdbc.execute("ALTER TABLE customer_notification_templates ALTER COLUMN created_at SET DEFAULT CURRENT_TIMESTAMP");
            jdbc.execute("ALTER TABLE customer_notification_templates ALTER COLUMN updated_at SET DEFAULT CURRENT_TIMESTAMP");
        } catch (Exception ignored) {
        }

        ReflectionTestUtils.setField(rateLimitFilter, "authLimitPerMinute", 1000);
        ReflectionTestUtils.setField(rateLimitFilter, "ipLimitPerMinute", 1000);
        com.github.benmanes.caffeine.cache.Cache<?, ?> cache =
                (com.github.benmanes.caffeine.cache.Cache<?, ?>) ReflectionTestUtils.getField(rateLimitFilter, "counters");
        if (cache != null) {
            cache.invalidateAll();
        }

        testUser = User.builder()
                .name("Payment E2E User")
                .email("payment_e2e_" + UUID.randomUUID() + "@example.com")
                .password("securePassword123")
                .role(Role.CLIENT)
                .provider(AuthProvider.LOCAL)
                .build();
        testUser = userRepository.save(testUser);

        when(invoiceNumberService.generateNextInvoiceNumber(eq(testUser.getId()), any()))
                .thenReturn(new InvoiceNumberService.InvoiceNumberResult(
                        "PAY-2026-0001", "2026-2027", 1, 7,
                        null, null, null));

        jwtToken = jwtUtil.generateToken(testUser.getEmail());

        testClient = Client.builder()
                .id(UUID.randomUUID())
                .name("Acme Corp Payments")
                .email("billing@acme.com")
                .user(testUser)
                .isDeleted(false)
                .version(1)
                .build();
        testClient = clientRepository.save(testClient);

        testWork = ClientWork.builder()
                .id(UUID.randomUUID())
                .description("System Integration Services")
                .rate(500.0)
                .quantity(4)
                .amount(2000.0)
                .date(LocalDateTime.now())
                .billed(false)
                .client(testClient)
                .user(testUser)
                .isDeleted(false)
                .version(1)
                .build();
        testWork = clientWorkRepository.save(testWork);
    }

    @Test
    void verifiesCompletePaymentLifecycleAndBalanceTracking() throws Exception {
        // 1. Generate Invoice from Client Work
        InvoiceRequest invoiceRequest = new InvoiceRequest();
        invoiceRequest.setClientId(testClient.getId());
        invoiceRequest.setWorkIds(Collections.singletonList(testWork.getId()));
        invoiceRequest.setDiscount(200.0);
        invoiceRequest.setNotes("Payment due in 7 days");
        invoiceRequest.setDueDate(LocalDateTime.now().plusDays(7));

        String generateResponse = mockMvc.perform(post("/api/invoice/generate")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invoiceRequest)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Invoice invoice = objectMapper.readValue(generateResponse, Invoice.class);
        assertThat(invoice).isNotNull();
        assertThat(invoice.getId()).isNotNull();
        assertThat(invoice.getPaymentStatus()).isEqualTo(PaymentStatus.UNPAID);
        assertThat(invoice.getSubtotal()).isEqualTo(2000.0);
        assertThat(invoice.getNetPayable()).isEqualTo(1800.0); // 2000 - 200 discount

        // 2. Record Partial Payment (800.00)
        InvoiceController.PaymentUpdateRequest partialPayment = new InvoiceController.PaymentUpdateRequest();
        partialPayment.setPaidAmount(800.0);
        partialPayment.setPaymentMode(PaymentMode.UPI);
        partialPayment.setPaymentDate(LocalDateTime.now());

        String partialPayResponse = mockMvc.perform(patch("/api/invoice/" + invoice.getId() + "/payment")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(partialPayment)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Invoice partiallyPaid = objectMapper.readValue(partialPayResponse, Invoice.class);
        assertThat(partiallyPaid.getPaymentStatus()).isEqualTo(PaymentStatus.PARTIALLY_PAID);
        assertThat(partiallyPaid.getPaidAmount()).isEqualTo(800.0);
        assertThat(partiallyPaid.getPendingAmount()).isEqualTo(1000.0);

        // 3. Record Full Payment (1800.00 cumulative)
        InvoiceController.PaymentUpdateRequest finalPayment = new InvoiceController.PaymentUpdateRequest();
        finalPayment.setPaidAmount(1800.0);
        finalPayment.setPaymentMode(PaymentMode.BANK_TRANSFER);
        finalPayment.setPaymentDate(LocalDateTime.now());

        String finalPayResponse = mockMvc.perform(patch("/api/invoice/" + invoice.getId() + "/payment")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(finalPayment)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Invoice fullyPaid = objectMapper.readValue(finalPayResponse, Invoice.class);
        assertThat(fullyPaid.getPaymentStatus()).isEqualTo(PaymentStatus.PAID);
        assertThat(fullyPaid.getPaidAmount()).isEqualTo(1800.0);
        assertThat(fullyPaid.getPendingAmount()).isZero();
    }
}
