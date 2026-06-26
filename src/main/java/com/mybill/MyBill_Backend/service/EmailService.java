package com.mybill.MyBill_Backend.service;

import com.mybill.MyBill_Backend.dto.EmailTemplateRequest;
import com.mybill.MyBill_Backend.dto.SendEmailRequest;
import com.mybill.MyBill_Backend.entity.*;
import com.mybill.MyBill_Backend.repository.EmailLogRepository;
import com.mybill.MyBill_Backend.repository.EmailTemplateRepository;
import com.mybill.MyBill_Backend.util.SecurityUtils;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine stringTemplateEngine;
    private final EmailTemplateRepository templateRepository;
    private final EmailLogRepository emailLogRepository;
    private final InvoiceService invoiceService;
    private final InvoicePdfService invoicePdfService;
    private final SecurityUtils securityUtils;

    @Value("${app.email.from:no-reply@mybill.local}")
    private String fromAddress;

    @Value("${app.email.retry.max-attempts:3}")
    private int maxRetryAttempts;

    @Transactional(readOnly = true)
    public List<EmailTemplate> listTemplates() {
        return templateRepository.findAvailableTemplates(
                securityUtils.getCurrentUserId()
        );
    }

    @Transactional
    public EmailTemplate createTemplate(EmailTemplateRequest request) {
        EmailTemplate template = EmailTemplate.builder()
                .user(securityUtils.getCurrentUser())
                .templateType(request.getTemplateType())
                .subject(request.getSubject())
                .htmlBody(request.getHtmlBody())
                .build();
        return templateRepository.save(template);
    }

    @Transactional
    public EmailTemplate updateTemplate(UUID templateId, EmailTemplateRequest request) {
        EmailTemplate template = templateRepository.findByTemplateIdAndUserId(templateId, securityUtils.getCurrentUserId())
                .orElseThrow(() -> new RuntimeException("Email template not found"));

        template.setTemplateType(request.getTemplateType());
        template.setSubject(request.getSubject());
        template.setHtmlBody(request.getHtmlBody());
        return templateRepository.save(template);
    }

    @Transactional
    public void deleteTemplate(UUID templateId) {
        EmailTemplate template = templateRepository.findByTemplateIdAndUserId(templateId, securityUtils.getCurrentUserId())
                .orElseThrow(() -> new RuntimeException("Email template not found"));
        template.setIsDeleted(true);
        template.setDeletedAt(LocalDateTime.now());
        templateRepository.save(template);
    }

    @Transactional
    public EmailLog sendEmail(SendEmailRequest request) {
        EmailTemplate template = resolveTemplate(request.getTemplateType());
        Map<String, Object> variables = enrichVariables(request);
        String subject = render(template.getSubject(), variables);
        String body = render(template.getHtmlBody(), variables);

        EmailLog log = EmailLog.builder()
                .user(securityUtils.getCurrentUser())
                .recipient(request.getTo())
                .subject(subject)
                .body(body)
                .status(EmailStatus.PENDING)
                .templateType(template.getTemplateType())
                .invoiceId(request.getInvoiceId())
                .attemptCount(0)
                .build();

        log = emailLogRepository.save(log);
        return attemptSend(log, Boolean.TRUE.equals(request.getAttachInvoicePdf()));
    }

    @Transactional(readOnly = true)
    public List<EmailLog> getLogs() {
        return emailLogRepository.findByUserIdOrderByCreatedAtDesc(securityUtils.getCurrentUserId());
    }

    @Transactional(readOnly = true)
    public Page<EmailLog> getLogs(EmailStatus status, Pageable pageable) {
        Long userId = securityUtils.getCurrentUserId();
        if (status != null) {
            return emailLogRepository.findByUserIdAndStatusOrderByCreatedAtDesc(userId, status, pageable);
        }
        return emailLogRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
    }

    @Transactional(readOnly = true)
    public EmailLog getLog(UUID emailLogId) {
        return emailLogRepository.findByEmailLogIdAndUserId(emailLogId, securityUtils.getCurrentUserId())
                .orElseThrow(() -> new RuntimeException("Email log not found"));
    }

    @Transactional
    public void retryFailedEmails() {
        List<EmailLog> failed = emailLogRepository.findTop25ByStatusAndNextRetryAtLessThanEqualOrderByCreatedAtAsc(
                EmailStatus.FAILED,
                LocalDateTime.now()
        );

        failed.stream()
                .filter(log -> log.getAttemptCount() == null || log.getAttemptCount() < maxRetryAttempts)
                .forEach(log -> attemptSend(log, false));
    }

    @Transactional
    public EmailLog sendPaymentReminder(Invoice invoice, String recipient, String templateType) {
        EmailTemplate template = resolveTemplate(invoice.getUser().getId(), templateType);
        Map<String, Object> variables = new HashMap<>();
        variables.put("invoiceNumber", invoice.getInvoiceNumber());
        variables.put("clientName", invoice.getClient() != null ? invoice.getClient().getName() : "");
        variables.put("remainingAmount", invoice.getRemainingAmount());
        variables.put("dueDate", invoice.getDueDate());

        EmailLog log = EmailLog.builder()
                .user(invoice.getUser())
                .recipient(recipient)
                .subject(render(template.getSubject(), variables))
                .body(render(template.getHtmlBody(), variables))
                .status(EmailStatus.PENDING)
                .templateType(template.getTemplateType())
                .invoiceId(invoice.getId())
                .attemptCount(0)
                .build();

        log = emailLogRepository.save(log);
        return attemptSend(log, false);
    }

    public boolean reminderSentRecently(UUID invoiceId, String templateType, LocalDateTime since) {
        return emailLogRepository.existsByInvoiceIdAndTemplateTypeAndCreatedAtAfter(invoiceId, templateType, since);
    }

    public EmailLog attemptSend(EmailLog log, boolean attachInvoicePdf) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, attachInvoicePdf);
            helper.setFrom(fromAddress);
            helper.setTo(log.getRecipient());
            helper.setSubject(log.getSubject());
            helper.setText(log.getBody(), true);

            if (attachInvoicePdf && log.getInvoiceId() != null) {
                byte[] pdfBytes = invoicePdfService.generateInvoicePdf(log.getInvoiceId());
                helper.addAttachment("invoice-" + log.getInvoiceId() + ".pdf", new ByteArrayResource(pdfBytes));
            }

            mailSender.send(message);
            log.setStatus(EmailStatus.SENT);
            log.setSentAt(LocalDateTime.now());
            log.setErrorMessage(null);
            log.setNextRetryAt(null);
        } catch (MessagingException | RuntimeException exception) {
            log.setStatus(EmailStatus.FAILED);
            log.setErrorMessage(exception.getMessage());
            int attempts = log.getAttemptCount() == null ? 0 : log.getAttemptCount();
            log.setNextRetryAt(LocalDateTime.now().plusMinutes(Math.min(60, 5L * (attempts + 1))));
        }

        log.setAttemptCount((log.getAttemptCount() == null ? 0 : log.getAttemptCount()) + 1);
        return emailLogRepository.save(log);
    }

    private EmailTemplate resolveTemplate(String templateType) {
        Long userId = securityUtils.getCurrentUserId();
        return resolveTemplate(userId, templateType);
    }

    private EmailTemplate resolveTemplate(Long userId, String templateType) {
        return templateRepository.findFirstByUserIdAndTemplateTypeAndIsDeletedFalse(userId, templateType)
                .or(() -> templateRepository.findFirstByUserIsNullAndTemplateTypeAndIsDeletedFalse(templateType))
                .orElseThrow(() -> new RuntimeException("Email template not found: " + templateType));
    }

    private Map<String, Object> enrichVariables(SendEmailRequest request) {
        Map<String, Object> variables = new HashMap<>();
        if (request.getVariables() != null) {
            variables.putAll(request.getVariables());
        }

        if (request.getInvoiceId() != null) {
            Invoice invoice = invoiceService.getInvoiceById(request.getInvoiceId());
            variables.put("invoiceNumber", invoice.getInvoiceNumber());
            variables.put("totalAmount", invoice.getTotalAmount());
            variables.put("remainingAmount", invoice.getRemainingAmount());
            variables.put("clientName", invoice.getClient() != null ? invoice.getClient().getName() : "");
        }

        return variables;
    }

    private String render(String template, Map<String, Object> variables) {
        Context context = new Context();
        context.setVariables(variables);
        String thymeleafTemplate = template.replace("{{", "[[").replace("}}", "]]");
        return stringTemplateEngine.process(thymeleafTemplate, context);
    }
}
