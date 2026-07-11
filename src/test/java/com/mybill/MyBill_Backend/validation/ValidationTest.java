package com.mybill.MyBill_Backend.validation;

import com.mybill.MyBill_Backend.dto.ClientRequest;
import com.mybill.MyBill_Backend.dto.ExpenseDTO;
import com.mybill.MyBill_Backend.dto.InvoiceFilterDTO;
import com.mybill.MyBill_Backend.dto.InvoiceRequest;
import com.mybill.MyBill_Backend.dto.RecurringInvoiceScheduleDTO;
import com.mybill.MyBill_Backend.dto.sync.SyncChangeDto;
import com.mybill.MyBill_Backend.dto.sync.SyncRequest;
import com.mybill.MyBill_Backend.entity.BusinessProfile;
import com.mybill.MyBill_Backend.entity.ClientWork;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ValidationTest {

    private static ValidatorFactory validatorFactory;
    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterAll
    static void tearDownValidator() {
        validatorFactory.close();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "Rahul Sharma",
            "Acme Traders",
            "A & B Services",
            "Client-42",
            "O'Connor Associates",
            "North/South Logistics"
    })
    void clientRequestAcceptsValidNames(String name) {
        ClientRequest request = validClientRequest();
        request.setName(name);

        assertNoViolationOn(request, "name");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "",
            "   ",
            "Client<>",
            "Acme 🚀",
            "A very long client name that keeps going past the supported limit for a single client display name in forms and invoices with extra suffix"
    })
    void clientRequestRejectsInvalidNames(String name) {
        ClientRequest request = validClientRequest();
        request.setName(name);

        assertViolationOn(request, "name");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "9876543210",
            "8123456789",
            "+919876543210",
            "+91 9876543210",
            "+91-9876543210"
    })
    void clientRequestAcceptsValidIndianPhones(String phone) {
        ClientRequest request = validClientRequest();
        request.setPhone(phone);

        assertNoViolationOn(request, "phone");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "",
            "12345",
            "5123456789",
            "987654321",
            "98765432101",
            "+1 9876543210",
            "phone"
    })
    void clientRequestRejectsInvalidPhones(String phone) {
        ClientRequest request = validClientRequest();
        request.setPhone(phone);

        assertViolationOn(request, "phone");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "client@example.com",
            "first.last@example.co.in",
            "billing+test@mybill.app",
            "name_123@sub.domain.com"
    })
    void clientRequestAcceptsValidEmails(String email) {
        ClientRequest request = validClientRequest();
        request.setEmail(email);

        assertNoViolationOn(request, "email");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "plainaddress",
            "missing-at.example.com",
            "name@",
            "@example.com",
            "name@example"
    })
    void clientRequestRejectsInvalidEmails(String email) {
        ClientRequest request = validClientRequest();
        request.setEmail(email);

        assertViolationOn(request, "email");
    }

    @Test
    void invoiceRequestAcceptsValidInvoiceDatesAndDiscount() {
        InvoiceRequest request = validInvoiceRequest();

        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    void invoiceRequestRejectsMissingClient() {
        InvoiceRequest request = validInvoiceRequest();
        request.setClientId(null);

        assertViolationOn(request, "clientId");
    }

    @Test
    void invoiceRequestRejectsEmptyWorkSelection() {
        InvoiceRequest request = validInvoiceRequest();
        request.setWorkIds(List.of());

        assertViolationOn(request, "workIds");
    }

    @Test
    void invoiceRequestRejectsFutureInvoiceDate() {
        InvoiceRequest request = validInvoiceRequest();
        request.setInvoiceDate(LocalDateTime.now().plusDays(1));

        assertViolationOn(request, "invoiceDate");
    }

    @Test
    void invoiceRequestRejectsDueDateBeforeInvoiceDate() {
        InvoiceRequest request = validInvoiceRequest();
        request.setInvoiceDate(LocalDateTime.of(2026, 7, 10, 10, 0));
        request.setDueDate(LocalDateTime.of(2026, 7, 9, 10, 0));

        assertViolationOn(request, "dueDateOnOrAfterInvoiceDate");
    }

    @ParameterizedTest
    @ValueSource(strings = {"0.00", "1.00", "99.99", "1000000.10"})
    void invoiceRequestAcceptsValidDiscounts(String value) {
        InvoiceRequest request = validInvoiceRequest();
        request.setDiscount(Double.valueOf(value));

        assertNoViolationOn(request, "discount");
    }

    @ParameterizedTest
    @ValueSource(strings = {"-0.01", "12.345"})
    void invoiceRequestRejectsInvalidDiscounts(String value) {
        InvoiceRequest request = validInvoiceRequest();
        request.setDiscount(Double.valueOf(value));

        assertViolationOn(request, "discount");
    }

    @Test
    void invoiceFilterAcceptsValidRanges() {
        InvoiceFilterDTO filter = validInvoiceFilter();

        assertThat(validator.validate(filter)).isEmpty();
    }

    @Test
    void invoiceFilterRejectsEndDateBeforeStartDate() {
        InvoiceFilterDTO filter = validInvoiceFilter();
        filter.setStartDate(LocalDateTime.of(2026, 7, 10, 10, 0));
        filter.setEndDate(LocalDateTime.of(2026, 7, 9, 10, 0));

        assertViolationOn(filter, "endDateOnOrAfterStartDate");
    }

    @Test
    void invoiceFilterRejectsMaxAmountBelowMinAmount() {
        InvoiceFilterDTO filter = validInvoiceFilter();
        filter.setMinAmount(500.00);
        filter.setMaxAmount(100.00);

        assertViolationOn(filter, "maxAmountGreaterThanOrEqualToMinAmount");
    }

    @Test
    void invoiceFilterRejectsNegativeAmountsAndLongQuery() {
        InvoiceFilterDTO filter = validInvoiceFilter();
        filter.setQuery("x".repeat(121));
        filter.setMinAmount(-1.00);
        filter.setMaxAmount(-0.01);

        Set<ConstraintViolation<InvoiceFilterDTO>> violations = validator.validate(filter);

        assertThat(violations).extracting(violation -> violation.getPropertyPath().toString())
                .contains("query", "minAmount", "maxAmount");
    }

    @Test
    void syncRequestAcceptsValidBatch() {
        SyncRequest request = validSyncRequest();

        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    void syncRequestRejectsOversizedPageAndInvalidPolicy() {
        SyncRequest request = validSyncRequest();
        request.setPageSize(501);
        request.setConflictPolicy("INVALID");

        Set<ConstraintViolation<SyncRequest>> violations = validator.validate(request);

        assertThat(violations).extracting(violation -> violation.getPropertyPath().toString())
                .contains("pageSize", "conflictPolicy");
    }

    @Test
    void syncRequestRejectsInvalidNestedChange() {
        SyncRequest request = validSyncRequest();
        SyncChangeDto change = request.getChanges().get(0);
        change.setOperation("replace");
        change.setPayload(null);

        Set<ConstraintViolation<SyncRequest>> violations = validator.validate(request);

        assertThat(violations).extracting(violation -> violation.getPropertyPath().toString())
                .contains("changes[0].operation", "changes[0].payload");
    }

    @ParameterizedTest
    @ValueSource(strings = {"0.01", "1.00", "999999.99"})
    void expenseAcceptsValidAmounts(String amount) {
        ExpenseDTO dto = validExpense();
        dto.setAmount(new BigDecimal(amount));

        assertNoViolationOn(dto, "amount");
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-1.00", "12.345"})
    void expenseRejectsInvalidAmounts(String amount) {
        ExpenseDTO dto = validExpense();
        dto.setAmount(new BigDecimal(amount));

        assertViolationOn(dto, "amount");
    }

    @Test
    void expenseRejectsFutureExpenseDate() {
        ExpenseDTO dto = validExpense();
        dto.setExpenseDate(LocalDate.now().plusDays(1));

        assertViolationOn(dto, "expenseDate");
    }

    @ParameterizedTest
    @ValueSource(strings = {"0.00", "18.50"})
    void expenseAcceptsValidTaxAmounts(String amount) {
        ExpenseDTO dto = validExpense();
        dto.setTaxAmount(new BigDecimal(amount));

        assertNoViolationOn(dto, "taxAmount");
    }

    @ParameterizedTest
    @ValueSource(strings = {"-0.01", "18.555"})
    void expenseRejectsInvalidTaxAmounts(String amount) {
        ExpenseDTO dto = validExpense();
        dto.setTaxAmount(new BigDecimal(amount));

        assertViolationOn(dto, "taxAmount");
    }

    @Test
    void recurringScheduleAcceptsValidDateRange() {
        RecurringInvoiceScheduleDTO dto = validSchedule();

        assertThat(validator.validate(dto)).isEmpty();
    }

    @Test
    void recurringScheduleRejectsMissingClient() {
        RecurringInvoiceScheduleDTO dto = validSchedule();
        dto.setClientId(null);

        assertViolationOn(dto, "clientId");
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-1.00", "123.456"})
    void recurringScheduleRejectsInvalidAmounts(String amount) {
        RecurringInvoiceScheduleDTO dto = validSchedule();
        dto.setAmount(new BigDecimal(amount));

        assertViolationOn(dto, "amount");
    }

    @Test
    void recurringScheduleRejectsEndDateBeforeStartDate() {
        RecurringInvoiceScheduleDTO dto = validSchedule();
        dto.setStartDate(LocalDate.of(2026, 7, 10));
        dto.setEndDate(LocalDate.of(2026, 7, 9));

        assertViolationOn(dto, "endDateOnOrAfterStartDate");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "27AAPFU0939F1ZV",
            "09AAACH7409R1ZZ",
            ""
    })
    void businessProfileAcceptsValidGstinValues(String gstin) {
        BusinessProfile profile = validBusinessProfile();
        profile.setGstin(gstin);

        assertNoViolationOn(profile, "gstin");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "27AAPFU0939F1Z",
            "27aapfu0939f1zv",
            "INVALIDGSTIN1234",
            "99ABCDE1234F0Z!"
    })
    void businessProfileRejectsInvalidGstinValues(String gstin) {
        BusinessProfile profile = validBusinessProfile();
        profile.setGstin(gstin);

        assertViolationOn(profile, "gstin");
    }

    @Test
    void businessProfileRejectsInvalidPhoneAndEmail() {
        BusinessProfile profile = validBusinessProfile();
        profile.setPhone("12345");
        profile.setEmail("bad-email");

        Set<ConstraintViolation<BusinessProfile>> violations = validator.validate(profile);

        assertThat(violations).extracting(ConstraintViolation::getPropertyPath)
                .map(Object::toString)
                .contains("phone", "email");
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 100})
    void clientWorkAcceptsPositiveQuantity(int quantity) {
        ClientWork work = validWork();
        work.setQuantity(quantity);

        assertNoViolationOn(work, "quantity");
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1})
    void clientWorkRejectsNonPositiveQuantity(int quantity) {
        ClientWork work = validWork();
        work.setQuantity(quantity);

        assertViolationOn(work, "quantity");
    }

    @ParameterizedTest
    @ValueSource(doubles = {0.01, 1.00, 1250.50})
    void clientWorkAcceptsValidRates(double rate) {
        ClientWork work = validWork();
        work.setRate(rate);

        assertNoViolationOn(work, "rate");
    }

    @ParameterizedTest
    @ValueSource(doubles = {0.0, -1.0, 12.345})
    void clientWorkRejectsInvalidRates(double rate) {
        ClientWork work = validWork();
        work.setRate(rate);

        assertViolationOn(work, "rate");
    }

    @Test
    void clientWorkRejectsFutureWorkDate() {
        ClientWork work = validWork();
        work.setDate(LocalDateTime.now().plusMinutes(1));

        assertViolationOn(work, "date");
    }

    private static ClientRequest validClientRequest() {
        ClientRequest request = new ClientRequest();
        request.setName("Acme Traders");
        request.setPhone("9876543210");
        request.setEmail("client@example.com");
        request.setAddress("Main Road");
        return request;
    }

    private static InvoiceRequest validInvoiceRequest() {
        InvoiceRequest request = new InvoiceRequest();
        request.setClientId(UUID.randomUUID());
        request.setWorkIds(List.of(UUID.randomUUID()));
        request.setInvoiceDate(LocalDateTime.now().minusDays(1));
        request.setDueDate(LocalDateTime.now().plusDays(7));
        request.setDiscount(10.00);
        return request;
    }

    private static InvoiceFilterDTO validInvoiceFilter() {
        InvoiceFilterDTO filter = new InvoiceFilterDTO();
        filter.setQuery("Acme");
        filter.setStartDate(LocalDateTime.now().minusDays(30));
        filter.setEndDate(LocalDateTime.now());
        filter.setMinAmount(10.00);
        filter.setMaxAmount(1000.00);
        return filter;
    }

    private static SyncRequest validSyncRequest() {
        SyncChangeDto change = new SyncChangeDto();
        change.setChangeId(UUID.randomUUID().toString());
        change.setEntityType("client");
        change.setEntityId(UUID.randomUUID().toString());
        change.setOperation("upsert");
        change.setPayload(Map.of("name", "Acme"));
        change.setCreatedAt(LocalDateTime.now().minusMinutes(1));

        return SyncRequest.builder()
                .deviceId("device-1")
                .changes(List.of(change))
                .pageSize(100)
                .conflictPolicy("CLIENT_WINS")
                .build();
    }

    private static ExpenseDTO validExpense() {
        return ExpenseDTO.builder()
                .description("Office supplies")
                .amount(new BigDecimal("100.00"))
                .category("OFFICE")
                .expenseDate(LocalDate.now())
                .taxAmount(new BigDecimal("18.00"))
                .build();
    }

    private static RecurringInvoiceScheduleDTO validSchedule() {
        return RecurringInvoiceScheduleDTO.builder()
                .clientId(UUID.randomUUID())
                .description("Monthly service")
                .amount(new BigDecimal("500.00"))
                .billingCycle("MONTHLY")
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusMonths(3))
                .build();
    }

    private static BusinessProfile validBusinessProfile() {
        return BusinessProfile.builder()
                .businessName("Acme Traders")
                .ownerName("Owner")
                .phone("9876543210")
                .email("owner@example.com")
                .gstin("27AAPFU0939F1ZV")
                .build();
    }

    private static ClientWork validWork() {
        return ClientWork.builder()
                .description("Consulting")
                .rate(100.00)
                .quantity(1)
                .amount(100.00)
                .date(LocalDateTime.now().minusHours(1))
                .build();
    }

    private static <T> void assertViolationOn(T target, String property) {
        assertThat(validator.validate(target))
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains(property);
    }

    private static <T> void assertNoViolationOn(T target, String property) {
        assertThat(validator.validate(target))
                .extracting(violation -> violation.getPropertyPath().toString())
                .doesNotContain(property);
    }
}
