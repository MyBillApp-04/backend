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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.jdbc.core.JdbcTemplate;

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
        "spring.datasource.url=jdbc:h2:mem:invoice_payment_test;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH",
        "spring.flyway.enabled=false",
        "spring.flyway.baseline-on-migrate=true",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@Transactional
class InvoiceApiIT {

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
    private InvoiceRepository invoiceRepository;

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

    private Flyway flyway;

    private User testUser;
    private Client testClient;
    private ClientWork testWork;
    private String jwtToken;

    @BeforeEach
    void setUp() {
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        jdbc.execute("ALTER TABLE customer_notification_templates ALTER COLUMN is_deleted SET DEFAULT false");
        jdbc.execute("ALTER TABLE customer_notification_templates ALTER COLUMN created_at SET DEFAULT CURRENT_TIMESTAMP");
        jdbc.execute("ALTER TABLE customer_notification_templates ALTER COLUMN updated_at SET DEFAULT CURRENT_TIMESTAMP");

        // Run Flyway manually on the Hibernate-created schema to setup sequences and helper tables
        flyway = Flyway.configure()
                .dataSource(dataSource)
                .baselineOnMigrate(true)
                .placeholders(java.util.Collections.singletonMap("createExtensionCommand", "SELECT 1"))
                .validateOnMigrate(false)
                .load();
        flyway.migrate();

        // Bypass rate limits in test environments to prevent 429 errors
        ReflectionTestUtils.setField(rateLimitFilter, "authLimitPerMinute", 1000);
        ReflectionTestUtils.setField(rateLimitFilter, "ipLimitPerMinute", 1000);
        com.github.benmanes.caffeine.cache.Cache<?, ?> cache =
                (com.github.benmanes.caffeine.cache.Cache<?, ?>) ReflectionTestUtils.getField(rateLimitFilter, "counters");
        if (cache != null) {
            cache.invalidateAll();
        }

        // 1. Create and persist User
        testUser = User.builder()
                .name("Integration Test User")
                .email("integration_user@example.com")
                .password("securePassword123")
                .role(Role.CLIENT)
                .provider(AuthProvider.LOCAL)
                .build();
        testUser = userRepository.save(testUser);
        when(invoiceNumberService.generateNextInvoiceNumber(eq(testUser.getId()), any()))
                .thenReturn(new InvoiceNumberService.InvoiceNumberResult(
                        "TST-2627-0001", "2026-2027", 1, 7,
                        null, null, null));

        // 2. Generate JWT Token
        jwtToken = jwtUtil.generateToken(testUser.getEmail());

        // 3. Create and persist Client
        testClient = Client.builder()
                .id(UUID.randomUUID())
                .name("Partner Retailers")
                .email("retail@example.com")
                .user(testUser)
                .isDeleted(false)
                .version(1)
                .build();
        testClient = clientRepository.save(testClient);

        // 4. Create and persist ClientWork
        testWork = ClientWork.builder()
                .id(UUID.randomUUID())
                .description("Logo Design and Brand Guidelines")
                .rate(350.0)
                .quantity(3)
                .amount(1050.0)
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
    void testGenerateInvoiceAndRecordPaymentFlow() throws Exception {
        // --- 1. GENERATE INVOICE ---
        InvoiceRequest invoiceRequest = new InvoiceRequest();
        invoiceRequest.setClientId(testClient.getId());
        invoiceRequest.setWorkIds(Collections.singletonList(testWork.getId()));
        invoiceRequest.setDiscount(50.0);
        invoiceRequest.setNotes("Payment due within 14 days");
        invoiceRequest.setDueDate(LocalDateTime.now().plusDays(14));

        String generateResponse = mockMvc.perform(post("/api/invoice/generate")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invoiceRequest)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Invoice generatedInvoice = objectMapper.readValue(generateResponse, Invoice.class);
        assertThat(generatedInvoice).isNotNull();
        assertThat(generatedInvoice.getId()).isNotNull();
        assertThat(generatedInvoice.getClient().getId()).isEqualTo(testClient.getId());
        assertThat(generatedInvoice.getPaymentStatus()).isEqualTo(PaymentStatus.UNPAID);
        assertThat(generatedInvoice.getSubtotal()).isEqualTo(1050.0);
        assertThat(generatedInvoice.getGrossAmount()).isEqualTo(1000.0); // Subtotal (1050) - Discount (50)
        assertThat(generatedInvoice.getNetPayable()).isEqualTo(1000.0);

        // --- 2. RECORD PARTIAL PAYMENT ---
        InvoiceController.PaymentUpdateRequest partialPayment = new InvoiceController.PaymentUpdateRequest();
        partialPayment.setPaidAmount(400.0);
        partialPayment.setPaymentMode(PaymentMode.UPI);
        partialPayment.setPaymentDate(LocalDateTime.now());

        String partialPayResponse = mockMvc.perform(patch("/api/invoice/" + generatedInvoice.getId() + "/payment")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(partialPayment)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Invoice partiallyPaidInvoice = objectMapper.readValue(partialPayResponse, Invoice.class);
        assertThat(partiallyPaidInvoice.getPaymentStatus()).isEqualTo(PaymentStatus.PARTIALLY_PAID);
        assertThat(partiallyPaidInvoice.getPaidAmount()).isEqualTo(400.0);
        assertThat(partiallyPaidInvoice.getPendingAmount()).isEqualTo(600.0);

        // --- 3. RECORD FULL SETTLEMENT PAYMENT ---
        InvoiceController.PaymentUpdateRequest finalPayment = new InvoiceController.PaymentUpdateRequest();
        // The endpoint accepts the new cumulative paid total, not a payment delta.
        finalPayment.setPaidAmount(1000.0);
        finalPayment.setPaymentMode(PaymentMode.BANK_TRANSFER);
        finalPayment.setPaymentDate(LocalDateTime.now());

        String finalPayResponse = mockMvc.perform(patch("/api/invoice/" + generatedInvoice.getId() + "/payment")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(finalPayment)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Invoice fullyPaidInvoice = objectMapper.readValue(finalPayResponse, Invoice.class);
        assertThat(fullyPaidInvoice.getPaymentStatus()).isEqualTo(PaymentStatus.PAID);
        assertThat(fullyPaidInvoice.getPaidAmount()).isEqualTo(1000.0);
        assertThat(fullyPaidInvoice.getPendingAmount()).isZero();
    }
}
