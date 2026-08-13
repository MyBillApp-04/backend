package com.mybill.MyBill_Backend.dto;

import com.mybill.MyBill_Backend.entity.QuotationStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuotationDTO {
    private UUID id;

    @NotNull(message = "Client ID is required")
    private UUID clientId;

    @NotBlank(message = "Client name is required")
    @Size(max = 200, message = "Client name must be 200 characters or fewer")
    private String clientName;

    private String quotationNumber;
    private QuotationStatus status;
    private LocalDateTime issueDate;
    private LocalDateTime validUntilDate;

    @Size(max = 5000, message = "Notes must be 5000 characters or fewer")
    private String notes;

    @Size(max = 10000, message = "Terms and conditions must be 10000 characters or fewer")
    private String termsAndConditions;

    private String pdfUrl;
    private String pdfPath;

    @DecimalMin(value = "0.00", message = "Subtotal cannot be negative")
    @Digits(integer = 12, fraction = 2, message = "Subtotal can have at most 2 decimal places")
    private Double subtotal;

    @DecimalMin(value = "0.00", message = "Discount cannot be negative")
    @Digits(integer = 12, fraction = 2, message = "Discount can have at most 2 decimal places")
    private Double discount;

    @DecimalMin(value = "0.00", message = "Gross amount cannot be negative")
    @Digits(integer = 12, fraction = 2, message = "Gross amount can have at most 2 decimal places")
    private Double grossAmount;

    @DecimalMin(value = "0.00", message = "Total amount cannot be negative")
    @Digits(integer = 12, fraction = 2, message = "Total amount can have at most 2 decimal places")
    private Double totalAmount;

    @DecimalMin(value = "0.00", message = "Net payable cannot be negative")
    @Digits(integer = 12, fraction = 2, message = "Net payable can have at most 2 decimal places")
    private Double netPayable;

    private Integer version;
    private String publicToken;
    private String publicResponseUrl;
    private LocalDateTime tokenExpiresAt;
    private LocalDateTime tokenRevokedAt;

    @Size(max = 50, message = "Client response status must be 50 characters or fewer")
    private String clientResponseStatus;

    private LocalDateTime respondedAt;

    @Size(max = 2000, message = "Discussion message must be 2000 characters or fewer")
    private String discussionMessage;

    @NotEmpty(message = "Quotation must have at least one item")
    @Valid
    private List<QuotationItemDTO> items;

    public static QuotationDTOBuilder builder() { return new QuotationDTOBuilder(); }

    public static class QuotationDTOBuilder {
        private UUID id;
        private UUID clientId;
        private String clientName;
        private String quotationNumber;
        private QuotationStatus status;
        private LocalDateTime issueDate;
        private LocalDateTime validUntilDate;
        private String notes;
        private String termsAndConditions;
        private String pdfUrl;
        private String pdfPath;
        private Double subtotal;
        private Double discount;
        private Double grossAmount;
        private Double totalAmount;
        private Double netPayable;
        private Integer version;
        private String publicToken;
        private String publicResponseUrl;
        private LocalDateTime tokenExpiresAt;
        private LocalDateTime tokenRevokedAt;
        private String clientResponseStatus;
        private LocalDateTime respondedAt;
        private String discussionMessage;
        private List<QuotationItemDTO> items;

        public QuotationDTOBuilder id(UUID id) { this.id = id; return this; }
        public QuotationDTOBuilder clientId(UUID clientId) { this.clientId = clientId; return this; }
        public QuotationDTOBuilder clientName(String clientName) { this.clientName = clientName; return this; }
        public QuotationDTOBuilder quotationNumber(String quotationNumber) { this.quotationNumber = quotationNumber; return this; }
        public QuotationDTOBuilder status(QuotationStatus status) { this.status = status; return this; }
        public QuotationDTOBuilder issueDate(LocalDateTime issueDate) { this.issueDate = issueDate; return this; }
        public QuotationDTOBuilder validUntilDate(LocalDateTime validUntilDate) { this.validUntilDate = validUntilDate; return this; }
        public QuotationDTOBuilder notes(String notes) { this.notes = notes; return this; }
        public QuotationDTOBuilder termsAndConditions(String termsAndConditions) { this.termsAndConditions = termsAndConditions; return this; }
        public QuotationDTOBuilder pdfUrl(String pdfUrl) { this.pdfUrl = pdfUrl; return this; }
        public QuotationDTOBuilder pdfPath(String pdfPath) { this.pdfPath = pdfPath; return this; }
        public QuotationDTOBuilder subtotal(Double subtotal) { this.subtotal = subtotal; return this; }
        public QuotationDTOBuilder discount(Double discount) { this.discount = discount; return this; }
        public QuotationDTOBuilder grossAmount(Double grossAmount) { this.grossAmount = grossAmount; return this; }
        public QuotationDTOBuilder totalAmount(Double totalAmount) { this.totalAmount = totalAmount; return this; }
        public QuotationDTOBuilder netPayable(Double netPayable) { this.netPayable = netPayable; return this; }
        public QuotationDTOBuilder version(Integer version) { this.version = version; return this; }
        public QuotationDTOBuilder publicToken(String publicToken) { this.publicToken = publicToken; return this; }
        public QuotationDTOBuilder publicResponseUrl(String publicResponseUrl) { this.publicResponseUrl = publicResponseUrl; return this; }
        public QuotationDTOBuilder tokenExpiresAt(LocalDateTime tokenExpiresAt) { this.tokenExpiresAt = tokenExpiresAt; return this; }
        public QuotationDTOBuilder tokenRevokedAt(LocalDateTime tokenRevokedAt) { this.tokenRevokedAt = tokenRevokedAt; return this; }
        public QuotationDTOBuilder clientResponseStatus(String clientResponseStatus) { this.clientResponseStatus = clientResponseStatus; return this; }
        public QuotationDTOBuilder respondedAt(LocalDateTime respondedAt) { this.respondedAt = respondedAt; return this; }
        public QuotationDTOBuilder discussionMessage(String discussionMessage) { this.discussionMessage = discussionMessage; return this; }
        public QuotationDTOBuilder items(List<QuotationItemDTO> items) { this.items = items; return this; }

        public QuotationDTO build() {
            QuotationDTO dto = new QuotationDTO();
            dto.id = this.id;
            dto.clientId = this.clientId;
            dto.clientName = this.clientName;
            dto.quotationNumber = this.quotationNumber;
            dto.status = this.status;
            dto.issueDate = this.issueDate;
            dto.validUntilDate = this.validUntilDate;
            dto.notes = this.notes;
            dto.termsAndConditions = this.termsAndConditions;
            dto.pdfUrl = this.pdfUrl;
            dto.pdfPath = this.pdfPath;
            dto.subtotal = this.subtotal;
            dto.discount = this.discount;
            dto.grossAmount = this.grossAmount;
            dto.totalAmount = this.totalAmount;
            dto.netPayable = this.netPayable;
            dto.version = this.version;
            dto.publicToken = this.publicToken;
            dto.publicResponseUrl = this.publicResponseUrl;
            dto.tokenExpiresAt = this.tokenExpiresAt;
            dto.tokenRevokedAt = this.tokenRevokedAt;
            dto.clientResponseStatus = this.clientResponseStatus;
            dto.respondedAt = this.respondedAt;
            dto.discussionMessage = this.discussionMessage;
            dto.items = this.items;
            return dto;
        }
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getClientId() { return clientId; }
    public void setClientId(UUID clientId) { this.clientId = clientId; }

    public String getClientName() { return clientName; }
    public void setClientName(String clientName) { this.clientName = clientName; }

    public String getQuotationNumber() { return quotationNumber; }
    public void setQuotationNumber(String quotationNumber) { this.quotationNumber = quotationNumber; }

    public QuotationStatus getStatus() { return status; }
    public void setStatus(QuotationStatus status) { this.status = status; }

    public LocalDateTime getIssueDate() { return issueDate; }
    public void setIssueDate(LocalDateTime issueDate) { this.issueDate = issueDate; }

    public LocalDateTime getValidUntilDate() { return validUntilDate; }
    public void setValidUntilDate(LocalDateTime validUntilDate) { this.validUntilDate = validUntilDate; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public String getTermsAndConditions() { return termsAndConditions; }
    public void setTermsAndConditions(String termsAndConditions) { this.termsAndConditions = termsAndConditions; }

    public String getPdfUrl() { return pdfUrl; }
    public void setPdfUrl(String pdfUrl) { this.pdfUrl = pdfUrl; }

    public String getPdfPath() { return pdfPath; }
    public void setPdfPath(String pdfPath) { this.pdfPath = pdfPath; }

    public Double getSubtotal() { return subtotal; }
    public void setSubtotal(Double subtotal) { this.subtotal = subtotal; }

    public Double getDiscount() { return discount; }
    public void setDiscount(Double discount) { this.discount = discount; }

    public Double getGrossAmount() { return grossAmount; }
    public void setGrossAmount(Double grossAmount) { this.grossAmount = grossAmount; }

    public Double getTotalAmount() { return totalAmount; }
    public void setTotalAmount(Double totalAmount) { this.totalAmount = totalAmount; }

    public Double getNetPayable() { return netPayable; }
    public void setNetPayable(Double netPayable) { this.netPayable = netPayable; }

    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }

    public String getPublicToken() { return publicToken; }
    public void setPublicToken(String publicToken) { this.publicToken = publicToken; }

    public String getPublicResponseUrl() { return publicResponseUrl; }
    public void setPublicResponseUrl(String publicResponseUrl) { this.publicResponseUrl = publicResponseUrl; }

    public LocalDateTime getTokenExpiresAt() { return tokenExpiresAt; }
    public void setTokenExpiresAt(LocalDateTime tokenExpiresAt) { this.tokenExpiresAt = tokenExpiresAt; }

    public LocalDateTime getTokenRevokedAt() { return tokenRevokedAt; }
    public void setTokenRevokedAt(LocalDateTime tokenRevokedAt) { this.tokenRevokedAt = tokenRevokedAt; }

    public String getClientResponseStatus() { return clientResponseStatus; }
    public void setClientResponseStatus(String clientResponseStatus) { this.clientResponseStatus = clientResponseStatus; }

    public LocalDateTime getRespondedAt() { return respondedAt; }
    public void setRespondedAt(LocalDateTime respondedAt) { this.respondedAt = respondedAt; }

    public String getDiscussionMessage() { return discussionMessage; }
    public void setDiscussionMessage(String discussionMessage) { this.discussionMessage = discussionMessage; }

    public List<QuotationItemDTO> getItems() { return items; }
    public void setItems(List<QuotationItemDTO> items) { this.items = items; }
}
