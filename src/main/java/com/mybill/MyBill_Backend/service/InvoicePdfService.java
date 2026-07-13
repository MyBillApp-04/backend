package com.mybill.MyBill_Backend.service;

import com.mybill.MyBill_Backend.entity.BusinessProfile;
import com.mybill.MyBill_Backend.entity.ClientWork;
import com.mybill.MyBill_Backend.entity.Invoice;
import com.mybill.MyBill_Backend.repository.ClientWorkRepository;
import com.mybill.MyBill_Backend.repository.InvoiceRepository;
import com.mybill.MyBill_Backend.util.SecurityUtils;
import com.itextpdf.barcodes.BarcodeQRCode;
import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.io.image.ImageData;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.events.Event;
import com.itextpdf.kernel.events.IEventHandler;
import com.itextpdf.kernel.events.PdfDocumentEvent;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.geom.Rectangle;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfPage;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.CompressionConstants;
import com.itextpdf.kernel.pdf.WriterProperties;
import com.itextpdf.kernel.pdf.canvas.PdfCanvas;
import com.itextpdf.kernel.pdf.xobject.PdfFormXObject;
import com.itextpdf.layout.Canvas;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.element.Link;
import com.itextpdf.kernel.pdf.action.PdfAction;
import com.itextpdf.kernel.pdf.PdfAnnotationBorder;
import com.itextpdf.layout.properties.HorizontalAlignment;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.itextpdf.layout.properties.VerticalAlignment;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class InvoicePdfService {

    private final InvoiceRepository invoiceRepository;
    private final ClientWorkRepository workRepository;
    private final BusinessProfileService businessProfileService;
    private final SecurityUtils securityUtils;

    @PersistenceContext
    private EntityManager entityManager;

    @Value("${app.upload-dir:uploads}")
    private String uploadDir;

    public InvoicePdfService(InvoiceRepository invoiceRepository,
                             ClientWorkRepository workRepository,
                             BusinessProfileService businessProfileService,
                             SecurityUtils securityUtils) {
        this.invoiceRepository = invoiceRepository;
        this.workRepository = workRepository;
        this.businessProfileService = businessProfileService;
        this.securityUtils = securityUtils;
    }

    public byte[] generateInvoicePdf(UUID invoiceId) {
        ByteArrayOutputStream out = new ByteArrayOutputStream(32 * 1024);
        generateInvoicePdf(invoiceId, out);
        return out.toByteArray();
    }

    public void generateInvoicePdf(UUID invoiceId, OutputStream out) {
        Long userId = securityUtils.getCurrentUserId();
        generateInvoicePdf(invoiceId, out, userId);
    }

    public void generateInvoicePdf(UUID invoiceId, OutputStream out, Long userId) {
        Invoice invoice = invoiceRepository.findByIdAndUserId(invoiceId, userId)
                .orElseThrow(() -> new RuntimeException("Invoice not found or access denied"));

        List<ClientWork> works = workRepository.findByInvoiceIdAndUserId(invoiceId, userId);

        BusinessProfile profile = businessProfileService.getProfile();
        if (profile == null) {
            profile = new BusinessProfile();
            profile.setBusinessName("My Business");
        }

        String customTerms = null;
        String customPaymentNote = null;
        String customUpiId = null;
        String templateStyle = "CLASSIC";
        String themeColor = "#225378";
        String fontFamily = "HELVETICA";
        boolean showLogo = true;
        String taxIdLabel = "";
        String taxIdValue = "";

        try {
            List<Object[]> settingsList = entityManager.createNativeQuery(
                            "SELECT terms_and_conditions, payment_note, upi_id, template_style, theme_color, font_family, show_logo, tax_id_label, tax_id_value FROM invoice_settings WHERE user_id = :userId")
                    .setParameter("userId", userId)
                    .getResultList();

            if (!settingsList.isEmpty()) {
                Object[] row = settingsList.get(0);
                if (row[0] != null) customTerms = row[0].toString();
                if (row[1] != null) customPaymentNote = row[1].toString();
                if (row[2] != null) customUpiId = row[2].toString();
                if (row[3] != null) templateStyle = row[3].toString();
                if (row[4] != null) themeColor = row[4].toString();
                if (row[5] != null) fontFamily = row[5].toString();
                if (row[6] != null) showLogo = (Boolean) row[6];
                if (row[7] != null) taxIdLabel = row[7].toString();
                if (row[8] != null) taxIdValue = row[8].toString();
            }
        } catch (Exception ignored) {}

        String finalTerms = (customTerms != null && !customTerms.isBlank()) ? customTerms : profile.getTermsAndConditions();
        String finalUpiId = (customUpiId != null && !customUpiId.isBlank()) ? customUpiId : profile.getUpiId();

        try {
            PdfWriter writer = new PdfWriter(out, new WriterProperties()
                    .setCompressionLevel(CompressionConstants.BEST_COMPRESSION)
                    .setFullCompressionMode(true));
            PdfDocument pdf = new PdfDocument(writer);
            Document document = new Document(pdf, PageSize.A4);
            document.setMargins(36, 40, 50, 40);

            pdf.addEventHandler(PdfDocumentEvent.END_PAGE,
                    new PageNumberEventHandler(showLogo ? loadFooterLogo() : null));

            String fontName = StandardFonts.HELVETICA;
            String boldFontName = StandardFonts.HELVETICA_BOLD;
            if ("COURIER".equalsIgnoreCase(fontFamily)) {
                fontName = StandardFonts.COURIER;
                boldFontName = StandardFonts.COURIER_BOLD;
            } else if ("TIMES".equalsIgnoreCase(fontFamily)) {
                fontName = StandardFonts.TIMES_ROMAN;
                boldFontName = StandardFonts.TIMES_BOLD;
            }

            PdfFont bold = PdfFontFactory.createFont(boldFontName);
            PdfFont regular = PdfFontFactory.createFont(fontName);
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd-MMM-yyyy", Locale.ENGLISH);

            DeviceRgb accent = new DeviceRgb(34, 83, 120);
            try {
                if (themeColor != null && themeColor.startsWith("#") && themeColor.length() == 7) {
                    int r = Integer.parseInt(themeColor.substring(1, 3), 16);
                    int g = Integer.parseInt(themeColor.substring(3, 5), 16);
                    int b = Integer.parseInt(themeColor.substring(5, 7), 16);
                    accent = new DeviceRgb(r, g, b);
                }
            } catch (Exception ignored) {}

            DeviceRgb dark = new DeviceRgb(34, 40, 49);
            DeviceRgb muted = new DeviceRgb(105, 112, 121);
            DeviceRgb line = new DeviceRgb(220, 225, 230);
            DeviceRgb tableHeader = new DeviceRgb(238, 243, 248);

            String invoiceDate = formatDate(firstNotNull(invoice.getInvoiceDate(), invoice.getCreatedDate()), fmt);
            String dueDateStr = invoice.getDueDate() != null ? invoice.getDueDate().format(fmt) : "-";
            String invoiceNo = display(invoice.getInvoiceNumber());
            String financialYear = invoice.getFinancialYear() != null ? invoice.getFinancialYear() : "-";

            String clientName = invoice.getClient() != null ? display(invoice.getClient().getName()).toUpperCase() : "CLIENT";
            String clientAddress = invoice.getClient() != null ? invoice.getClient().getAddress() : null;
            String clientPhone = invoice.getClient() != null ? invoice.getClient().getPhone() : null;

            double subtotal = invoice.getSubtotal() != null
                    ? invoice.getSubtotal()
                    : works.stream()
                            .mapToDouble(w -> w.getAmount() != null ? w.getAmount() : 0.0)
                            .sum();
            double grandTotal = invoice.getTotalAmount() != null ? invoice.getTotalAmount() : subtotal;
            double balanceAdjustment = grandTotal - subtotal;
            double netPayable = (invoice.getNetPayable() != null && invoice.getNetPayable() > 0)
                    ? invoice.getNetPayable()
                    : grandTotal;

            Table header = new Table(UnitValue.createPercentArray(new float[]{58, 42}))
                    .useAllAvailableWidth()
                    .setMarginBottom(18);

            Cell leftHeader = new Cell()
                    .setBorder(Border.NO_BORDER)
                    .setPadding(0);

            leftHeader.add(new Paragraph(display(profile.getBusinessName()))
                    .setFont(bold)
                    .setFontSize(18)
                    .setFontColor(dark)
                    .setMarginBottom(12));

            addOptionalLine(leftHeader, profile.getAddress(), regular, muted, 9);
            addOptionalLine(leftHeader, labelValue("Phone", profile.getPhone()), regular, muted, 9);
            addOptionalLine(leftHeader, labelValue("Email", profile.getEmail()), regular, muted, 9);
            addOptionalLine(leftHeader, labelValue("GSTIN", profile.getGstin()), regular, muted, 9);
            if (taxIdLabel != null && !taxIdLabel.trim().isEmpty() && taxIdValue != null && !taxIdValue.trim().isEmpty()) {
                addOptionalLine(leftHeader, labelValue(taxIdLabel.trim(), taxIdValue.trim()), regular, muted, 9);
            }

            leftHeader.add(new Paragraph("\nBill To")
                    .setFont(bold)
                    .setFontSize(10)
                    .setFontColor(accent)
                    .setMarginTop(12)
                    .setMarginBottom(5));

            leftHeader.add(new Paragraph(clientName)
                    .setFont(bold)
                    .setFontSize(10)
                    .setFontColor(dark)
                    .setMarginBottom(2));

            addOptionalLine(leftHeader, clientAddress, regular, muted, 9);
            addOptionalLine(leftHeader, labelValue("Phone", clientPhone), regular, muted, 9);

            header.addCell(leftHeader);

            Cell rightHeader = new Cell()
                    .setBorder(Border.NO_BORDER)
                    .setPadding(0)
                    .setTextAlignment(TextAlignment.RIGHT);

            byte[] logoBytes = showLogo ? loadImage(profile.getLogoPath(), 600, 240, true) : null;
            if (logoBytes != null) {
                rightHeader.add(new Image(ImageDataFactory.create(logoBytes))
                        .setMaxWidth(130)
                        .setMaxHeight(65)
                        .setHorizontalAlignment(HorizontalAlignment.RIGHT)
                        .setMarginBottom(18));
            } else {
                rightHeader.add(new Paragraph("")
                        .setHeight(65)
                        .setMarginBottom(18));
            }

            rightHeader.add(infoLine("Invoice No", invoiceNo, regular, muted));
            rightHeader.add(infoLine("Invoice Date", invoiceDate, regular, muted));
            rightHeader.add(infoLine("Financial Year", financialYear, regular, muted));
            rightHeader.add(infoLine("Due Date", dueDateStr, regular, muted));

            header.addCell(rightHeader);
            document.add(header);

            document.add(new Paragraph("")
                    .setBorderBottom(new SolidBorder(line, 0.8f))
                    .setMarginBottom(18));

            Table itemTable = new Table(UnitValue.createPercentArray(new float[]{14, 46, 14, 10, 16}))
                    .useAllAvailableWidth()
                    .setMarginTop(4);

            for (String h : new String[]{"Date", "Description", "Rate", "Qty", "Amount"}) {
                com.itextpdf.layout.properties.TextAlignment align = h.equals("Description") ? TextAlignment.LEFT : TextAlignment.RIGHT;
                DeviceRgb headerTextColor = "MODERN".equalsIgnoreCase(templateStyle) ? new DeviceRgb(255, 255, 255) : dark;
                Cell headerCell = new Cell()
                        .add(new Paragraph(h)
                                .setFont(bold)
                                .setFontSize(9)
                                .setFontColor(headerTextColor))
                        .setPaddingTop(8)
                        .setPaddingBottom(8)
                        .setPaddingLeft(7)
                        .setPaddingRight(7)
                        .setTextAlignment(align);

                if ("MODERN".equalsIgnoreCase(templateStyle)) {
                    headerCell.setBackgroundColor(accent)
                              .setBorder(Border.NO_BORDER);
                } else if ("MINIMAL".equalsIgnoreCase(templateStyle)) {
                    headerCell.setBackgroundColor(ColorConstants.WHITE)
                              .setBorderBottom(new SolidBorder(accent, 1.2f))
                              .setBorderTop(Border.NO_BORDER)
                              .setBorderLeft(Border.NO_BORDER)
                              .setBorderRight(Border.NO_BORDER);
                } else {
                    headerCell.setBackgroundColor(tableHeader)
                              .setBorder(Border.NO_BORDER);
                }
                itemTable.addHeaderCell(headerCell);
            }

            for (ClientWork work : works) {
                String workDate = work.getDate() != null ? work.getDate().format(fmt) : "";
                double rateValue = work.getRate() != null ? work.getRate() : 0.0;
                int quantity = work.getQuantity() != null ? work.getQuantity() : 0;
                double amount = work.getAmount() != null ? work.getAmount() : 0.0;

                itemTable.addCell(rowCell(workDate, regular, muted, line, TextAlignment.RIGHT));
                itemTable.addCell(rowCell(display(work.getDescription()), regular, dark, line, TextAlignment.LEFT));
                itemTable.addCell(rowCell(formatAmount(rateValue), regular, dark, line, TextAlignment.RIGHT));
                itemTable.addCell(rowCell(String.valueOf(quantity), regular, dark, line, TextAlignment.RIGHT));
                itemTable.addCell(rowCell(formatAmount(amount), regular, dark, line, TextAlignment.RIGHT));
            }

            if (works.isEmpty()) {
                itemTable.addCell(new Cell(1, 5)
                        .add(new Paragraph("No work items found")
                                .setFont(regular)
                                .setFontSize(10)
                                .setFontColor(muted))
                        .setTextAlignment(TextAlignment.CENTER)
                        .setPadding(18)
                        .setBorderBottom(new SolidBorder(line, 0.7f))
                        .setBorderTop(Border.NO_BORDER)
                        .setBorderLeft(Border.NO_BORDER)
                        .setBorderRight(Border.NO_BORDER));
            }

            document.add(itemTable);

            Table totals = new Table(UnitValue.createPercentArray(new float[]{70, 15, 15}))
                    .useAllAvailableWidth()
                    .setMarginTop(14)
                    .setMarginBottom(20);

            if (Math.abs(balanceAdjustment) >= 0.01) {
                totals.addCell(blankCell());
                totals.addCell(totalLabelCell("Balance Adjustment", regular, dark, line, false));
                totals.addCell(totalAmountCell("INR " + formatAmount(balanceAdjustment), regular, dark, line, false));
            }

            totals.addCell(blankCell());
            totals.addCell(totalLabelCell("Grand Total", bold, dark, line, true));
            totals.addCell(totalAmountCell("INR " + formatAmount(grandTotal), bold, accent, line, true));

            document.add(totals);

            byte[] qrBytes = loadImage(profile.getQrImagePath(), 256, 256, true);
            boolean hasQrCode = qrBytes != null || notEmpty(finalUpiId);
            boolean hasUpi = notEmpty(finalUpiId);

            float[] columnWidths;
            if (hasQrCode && hasUpi) {
                columnWidths = new float[]{42, 16, 18, 24};
            } else if (hasQrCode) {
                columnWidths = new float[]{52, 20, 28};
            } else {
                columnWidths = new float[]{68, 32};
            }

            Table footer = new Table(UnitValue.createPercentArray(columnWidths))
                    .useAllAvailableWidth();

            Cell bankCell = new Cell()
                    .setBorder(new SolidBorder(line, 0.7f))
                    .setPadding(12);

            bankCell.add(new Paragraph("Payment Details")
                    .setFont(bold)
                    .setFontSize(10)
                    .setFontColor(accent)
                    .setMarginBottom(6));

            boolean hasPayment = false;

            if (notEmpty(profile.getBankName())) {
                bankCell.add(detail("Bank", profile.getBankName(), regular, dark));
                hasPayment = true;
            }

            if (notEmpty(profile.getAccountNumber())) {
                bankCell.add(detail("A/C", profile.getAccountNumber(), regular, dark));
                hasPayment = true;
            }

            if (notEmpty(profile.getIfsc())) {
                bankCell.add(detail("IFSC", profile.getIfsc(), regular, dark));
                hasPayment = true;
            }

            if (notEmpty(finalUpiId)) {
                bankCell.add(detail("UPI", finalUpiId, regular, dark));
                hasPayment = true;
            }

            if (!hasPayment) {
                bankCell.add(new Paragraph("Payment details not added")
                        .setFont(regular)
                        .setFontSize(8.5f)
                        .setFontColor(muted));
            }

            footer.addCell(bankCell);

            if (hasUpi && !hasQrCode) {
                Cell payCell = new Cell()
                        .setBorder(new SolidBorder(line, 0.7f))
                        .setPadding(10)
                        .setTextAlignment(TextAlignment.CENTER)
                        .setVerticalAlignment(VerticalAlignment.MIDDLE);

                Paragraph payBtn = new Paragraph()
                        .setBackgroundColor(accent)
                        .setPadding(6)
                        .setMarginLeft(4f)
                        .setMarginRight(4f)
                        .setTextAlignment(TextAlignment.CENTER);

                Link link = new Link("Pay Now", PdfAction.createURI(upiPaymentPayload(profile.getBusinessName(), finalUpiId, netPayable)));
                link.setFont(bold)
                        .setFontSize(9.5f)
                        .setFontColor(ColorConstants.WHITE);
                link.getLinkAnnotation().setBorder(new PdfAnnotationBorder(0, 0, 0));

                payBtn.add(link);
                payCell.add(payBtn);
                footer.addCell(payCell);
            }

            if (hasQrCode) {
                Cell qrCell = new Cell()
                        .setBorder(new SolidBorder(line, 0.7f))
                        .setPadding(10)
                        .setTextAlignment(TextAlignment.CENTER)
                        .setVerticalAlignment(VerticalAlignment.MIDDLE);

                if (qrBytes != null) {
                    qrCell.add(new Image(ImageDataFactory.create(qrBytes))
                            .setMaxWidth(72)
                            .setMaxHeight(72)
                            .setHorizontalAlignment(HorizontalAlignment.CENTER));
                } else {
                    addGeneratedUpiQr(qrCell, profile.getBusinessName(), finalUpiId, netPayable, pdf);
                }

                // Add a "Pay Now" button below the QR code
                Paragraph payBtn = new Paragraph()
                        .setBackgroundColor(accent)
                        .setPadding(4)
                        .setMarginLeft(2f)
                        .setMarginRight(2f)
                        .setTextAlignment(TextAlignment.CENTER)
                        .setMarginTop(4);

                Link link = new Link("Pay Now", PdfAction.createURI(upiPaymentPayload(profile.getBusinessName(), finalUpiId, netPayable)));
                link.setFont(bold)
                        .setFontSize(8.0f)
                        .setFontColor(ColorConstants.WHITE);
                link.getLinkAnnotation().setBorder(new PdfAnnotationBorder(0, 0, 0));

                payBtn.add(link);
                qrCell.add(payBtn);

                footer.addCell(qrCell);
            }

            Cell signCell = new Cell()
                    .setBorder(new SolidBorder(line, 0.7f))
                    .setPadding(12)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setVerticalAlignment(VerticalAlignment.MIDDLE);

            byte[] signatureBytes = loadImage(profile.getSignaturePath(), 520, 180, true);
            if (signatureBytes != null) {
                signCell.add(new Image(ImageDataFactory.create(signatureBytes))
                        .setMaxWidth(130)
                        .setMaxHeight(42)
                        .setHorizontalAlignment(HorizontalAlignment.CENTER)
                        .setMarginBottom(6));
            } else {
                signCell.add(new Paragraph("\n\n____________________")
                        .setFont(regular)
                        .setFontSize(8)
                        .setFontColor(muted)
                        .setMarginBottom(4));
            }

            signCell.add(new Paragraph("Authorized Signature")
                    .setFont(bold)
                    .setFontSize(10)
                    .setFontColor(dark));

            footer.addCell(signCell);
            document.add(footer);

            String finalNote = (invoice.getNotes() != null && !invoice.getNotes().isBlank()) ? invoice.getNotes() : customPaymentNote;
            if (finalNote == null || finalNote.isBlank()) {
                finalNote = profile.getThankYouNote();
            }

            if (notEmpty(finalNote) || notEmpty(finalTerms)) {
                Table notes = new Table(UnitValue.createPercentArray(new float[]{100}))
                        .useAllAvailableWidth()
                        .setMarginTop(16);

                Cell notesCell = new Cell()
                        .setBorder(Border.NO_BORDER)
                        .setPadding(0);

                if (notEmpty(finalNote)) {
                    notesCell.add(new Paragraph(finalNote)
                            .setFont(regular)
                            .setFontSize(9)
                            .setFontColor(muted)
                            .setTextAlignment(TextAlignment.CENTER)
                            .setMarginBottom(8));
                }

                if (notEmpty(finalTerms)) {
                    notesCell.add(new Paragraph("Terms & Conditions")
                            .setFont(bold)
                            .setFontSize(9)
                            .setFontColor(accent)
                            .setMarginBottom(3));

                    notesCell.add(new Paragraph(finalTerms)
                            .setFont(regular)
                            .setFontSize(8.5f)
                            .setFontColor(muted)
                            .setFixedLeading(12));
                }

                notes.addCell(notesCell);
                document.add(notes);
            }

            document.close();
        } catch (IOException e) {
            throw new RuntimeException("Error generating invoice PDF: " + e.getMessage(), e);
        }

    }

    private Paragraph infoLine(String label, String value, PdfFont regular, DeviceRgb muted) {
        return new Paragraph(label + ": " + value)
                .setFont(regular)
                .setFontSize(9)
                .setFontColor(muted)
                .setMarginBottom(3);
    }

    private Cell rowCell(String text, PdfFont font, DeviceRgb color, DeviceRgb line, TextAlignment align) {
        return new Cell()
                .add(new Paragraph(text)
                        .setFont(font)
                        .setFontSize(9)
                        .setFontColor(color))
                .setTextAlignment(align)
                .setPaddingTop(8)
                .setPaddingBottom(8)
                .setPaddingLeft(7)
                .setPaddingRight(7)
                .setBorderBottom(new SolidBorder(line, 0.7f))
                .setBorderTop(Border.NO_BORDER)
                .setBorderLeft(Border.NO_BORDER)
                .setBorderRight(Border.NO_BORDER);
    }

    private Cell blankCell() {
        return new Cell()
                .setBorder(Border.NO_BORDER)
                .setPadding(4);
    }

    private Cell totalLabelCell(String label, PdfFont font, DeviceRgb color, DeviceRgb line, boolean strong) {
        return new Cell()
                .add(new Paragraph(label)
                        .setFont(font)
                        .setFontSize(strong ? 11 : 10)
                        .setFontColor(color))
                .setTextAlignment(TextAlignment.RIGHT)
                .setPaddingTop(8)
                .setPaddingBottom(8)
                .setBorderTop(new SolidBorder(line, 0.8f))
                .setBorderBottom(new SolidBorder(line, 0.8f))
                .setBorderLeft(Border.NO_BORDER)
                .setBorderRight(Border.NO_BORDER);
    }

    private Cell totalAmountCell(String amount, PdfFont font, DeviceRgb color, DeviceRgb line, boolean strong) {
        return new Cell()
                .add(new Paragraph(amount)
                        .setFont(font)
                        .setFontSize(strong ? 11 : 10)
                        .setFontColor(color))
                .setTextAlignment(TextAlignment.RIGHT)
                .setPaddingTop(8)
                .setPaddingBottom(8)
                .setBorderTop(new SolidBorder(line, 0.8f))
                .setBorderBottom(new SolidBorder(line, 0.8f))
                .setBorderLeft(Border.NO_BORDER)
                .setBorderRight(Border.NO_BORDER);
    }

    private Paragraph detail(String label, String value, PdfFont regular, DeviceRgb dark) {
        return new Paragraph(label + ": " + value)
                .setFont(regular)
                .setFontSize(8.5f)
                .setFontColor(dark)
                .setMarginBottom(2);
    }

    private void addOptionalLine(Cell cell, String value, PdfFont font, DeviceRgb color, float size) {
        if (!notEmpty(value)) return;

        cell.add(new Paragraph(value)
                .setFont(font)
                .setFontSize(size)
                .setFontColor(color)
                .setFixedLeading(13)
                .setMarginBottom(1));
    }

    private String labelValue(String label, String value) {
        return notEmpty(value) ? label + ": " + value : null;
    }

    private LocalDateTime firstNotNull(LocalDateTime first, LocalDateTime second) {
        return first != null ? first : second;
    }

    private String formatDate(LocalDateTime value, DateTimeFormatter fmt) {
        return value != null ? value.format(fmt) : "-";
    }

    private String formatAmount(double amount) {
        return String.format("%.2f", amount);
    }

    private String display(String value) {
        return notEmpty(value) ? value.trim() : "-";
    }

    private boolean notEmpty(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private void addGeneratedUpiQr(Cell qrCell, String businessName, String upiId, double amount, PdfDocument pdf) {
        try {
            BarcodeQRCode barcodeQRCode = new BarcodeQRCode(upiPaymentPayload(businessName, upiId, amount));
            PdfFormXObject qrObject = barcodeQRCode.createFormXObject(ColorConstants.BLACK, pdf);

            qrCell.add(new Image(qrObject)
                    .setMaxWidth(72)
                    .setMaxHeight(72)
                    .setHorizontalAlignment(HorizontalAlignment.CENTER));
        } catch (Exception e) {
            qrCell.add(new Paragraph("UPI\n" + display(upiId))
                    .setFontSize(8.5f)
                    .setFontColor(ColorConstants.GRAY)
                    .setTextAlignment(TextAlignment.CENTER));
        }
    }

    private String upiPaymentPayload(String businessName, String upiId, double amount) {
        StringBuilder payload = new StringBuilder("upi://pay?pa=")
                .append(urlEncode(upiId));

        if (notEmpty(businessName)) {
            payload.append("&pn=").append(urlEncode(businessName));
        }

        payload.append("&am=").append(formatAmount(amount));
        payload.append("&cu=INR");
        return payload.toString();
    }

    private String urlEncode(String value) {
        if (value == null) return "";
        return URLEncoder.encode(value.trim(), StandardCharsets.UTF_8).replace("+", "%20");
    }

    private java.nio.file.Path resolveUploadPath(String path) {
        if (path == null || path.isBlank()) return null;

        try {
            String filename = path.startsWith("/uploads/")
                    ? path.substring("/uploads/".length())
                    : path.startsWith("uploads/")
                    ? path.substring("uploads/".length())
                    : path;

            java.nio.file.Path uploadsRoot = Paths.get(uploadDir).toAbsolutePath().normalize();
            java.nio.file.Path filePath = uploadsRoot.resolve(filename).normalize();

            if (!filePath.startsWith(uploadsRoot) || !Files.isRegularFile(filePath)) {
                return null;
            }

            return Files.isReadable(filePath) ? filePath : null;
        } catch (Exception ignored) {
            return null;
        }
    }

    /**
     * Handles existing uploads created before upload-time optimization. The
     * original stays intact; only the invoice copy is resized and re-encoded.
     */
    private byte[] loadImage(String path, int maxWidth, int maxHeight, boolean forcePng) {
        java.nio.file.Path sourcePath = resolveUploadPath(path);
        if (sourcePath == null) return null;

        try {
            BufferedImage source = ImageIO.read(sourcePath.toFile());
            if (source == null) return null;

            double scale = Math.min(1d,
                    Math.min((double) maxWidth / source.getWidth(), (double) maxHeight / source.getHeight()));
            int width = Math.max(1, (int) Math.round(source.getWidth() * scale));
            int height = Math.max(1, (int) Math.round(source.getHeight() * scale));
            boolean usePng = forcePng || source.getColorModel().hasAlpha();
            BufferedImage resized = new BufferedImage(
                    width,
                    height,
                    usePng ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB);
            Graphics2D graphics = resized.createGraphics();
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            graphics.drawImage(source, 0, 0, width, height, null);
            graphics.dispose();

            if (usePng) {
                ByteArrayOutputStream output = new ByteArrayOutputStream(16 * 1024);
                return ImageIO.write(resized, "png", output) ? output.toByteArray() : null;
            }

            ImageWriter writer = ImageIO.getImageWritersByFormatName("jpeg").next();
            try (ByteArrayOutputStream output = new ByteArrayOutputStream(16 * 1024);
                 ImageOutputStream stream = ImageIO.createImageOutputStream(output)) {
                writer.setOutput(stream);
                ImageWriteParam parameters = writer.getDefaultWriteParam();
                parameters.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                parameters.setCompressionQuality(0.78f);
                writer.write(null, new IIOImage(resized, null, null), parameters);
                return output.toByteArray();
            } finally {
                writer.dispose();
            }
        } catch (Exception ignored) {
            return null;
        }
    }

    private static class PageNumberEventHandler implements IEventHandler {

        private final ImageData footerLogo;

        private PageNumberEventHandler(ImageData footerLogo) {
            this.footerLogo = footerLogo;
        }

        @Override
        public void handleEvent(Event event) {
            PdfDocumentEvent docEvent = (PdfDocumentEvent) event;
            PdfDocument pdfDoc = docEvent.getDocument();
            PdfPage page = docEvent.getPage();
            Rectangle pageSize = page.getPageSize();

            // Draw the footer after the invoice content so tables or page
            // backgrounds cannot cover the MyBill mark.
            PdfCanvas pdfCanvas = new PdfCanvas(page.newContentStreamAfter(), page.getResources(), pdfDoc);
            Canvas canvas = new Canvas(pdfCanvas, pageSize);

            float footerY = 22;
            if (footerLogo != null) {
                pdfCanvas.addImageFittedIntoRectangle(
                        footerLogo,
                        new Rectangle(40, footerY - 6, 22, 22),
                        false
                );
            }

            Paragraph generatedBy = new Paragraph("Generated securely via MyBill App")
                    .setFontSize(8)
                    .setFontColor(ColorConstants.GRAY);

            canvas.showTextAligned(generatedBy, footerLogo == null ? 40 : 66,
                    footerY, TextAlignment.LEFT);

            String pageText = String.format("Page %d of %d", pdfDoc.getPageNumber(page), pdfDoc.getNumberOfPages());

            Paragraph pageParagraph = new Paragraph(pageText)
                    .setFontSize(8)
                    .setFontColor(ColorConstants.GRAY);

            canvas.showTextAligned(pageParagraph, pageSize.getWidth() - 40, footerY, TextAlignment.RIGHT);
            canvas.close();
        }
    }

    private ImageData loadFooterLogo() {
        try (InputStream input = getClass().getResourceAsStream("/static/logo.png")) {
            if (input == null) return null;

            // Read the source and immediately downscale to avoid OOM with large PNGs
            BufferedImage source = ImageIO.read(input);
            if (source == null) return null;

            int srcWidth = source.getWidth();
            int srcHeight = source.getHeight();

            // If the image is huge (>200px), pre-scale it first
            if (srcWidth > 200 || srcHeight > 200) {
                double preScale = Math.min(200d / srcWidth, 200d / srcHeight);
                int preWidth = Math.max(1, (int) Math.round(srcWidth * preScale));
                int preHeight = Math.max(1, (int) Math.round(srcHeight * preScale));
                BufferedImage preScaled = new BufferedImage(preWidth, preHeight, BufferedImage.TYPE_INT_ARGB);
                Graphics2D g = preScaled.createGraphics();
                g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                g.drawImage(source, 0, 0, preWidth, preHeight, null);
                g.dispose();
                source = preScaled;
            }

            BufferedImage cropped = trimTransparentMargin(source);
            // Use TYPE_INT_RGB (no alpha) with white background to prevent black showing through
            BufferedImage thumbnail = new BufferedImage(64, 64, BufferedImage.TYPE_INT_RGB);
            Graphics2D graphics = thumbnail.createGraphics();
            // Fill white background first
            graphics.setColor(java.awt.Color.WHITE);
            graphics.fillRect(0, 0, 64, 64);
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            graphics.setRenderingHint(RenderingHints.KEY_RENDERING,
                    RenderingHints.VALUE_RENDER_QUALITY);
            double scale = Math.min(64d / cropped.getWidth(), 64d / cropped.getHeight());
            int width = Math.max(1, (int) Math.round(cropped.getWidth() * scale));
            int height = Math.max(1, (int) Math.round(cropped.getHeight() * scale));
            graphics.drawImage(cropped, (64 - width) / 2, (64 - height) / 2, width, height, null);
            graphics.dispose();

            ByteArrayOutputStream output = new ByteArrayOutputStream(4 * 1024);
            if (!ImageIO.write(thumbnail, "png", output)) return null;
            return ImageDataFactory.create(output.toByteArray());
        } catch (Exception e) {
            return null;
        }
    }

    private BufferedImage trimTransparentMargin(BufferedImage source) {
        if (!source.getColorModel().hasAlpha()) return source;

        int minX = source.getWidth();
        int minY = source.getHeight();
        int maxX = -1;
        int maxY = -1;

        for (int y = 0; y < source.getHeight(); y++) {
            for (int x = 0; x < source.getWidth(); x++) {
                if ((source.getRGB(x, y) >>> 24) != 0) {
                    minX = Math.min(minX, x);
                    minY = Math.min(minY, y);
                    maxX = Math.max(maxX, x);
                    maxY = Math.max(maxY, y);
                }
            }
        }

        if (maxX < minX || maxY < minY) return source;

        final int padding = 12;
        minX = Math.max(0, minX - padding);
        minY = Math.max(0, minY - padding);
        maxX = Math.min(source.getWidth() - 1, maxX + padding);
        maxY = Math.min(source.getHeight() - 1, maxY + padding);
        return source.getSubimage(minX, minY, maxX - minX + 1, maxY - minY + 1);
    }
}
