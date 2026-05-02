package com.supply.invoice.service;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.supply.common.exception.BusinessException;
import com.supply.common.exception.ErrorCode;
import com.supply.invoice.dto.InvoiceItemResponse;
import com.supply.invoice.dto.InvoiceResponse;
import com.supply.invoice.entity.InvoiceStatus;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;

@Service
public class InvoicePdfService {

    private static final Font TITLE_FONT;
    private static final Font HEADER_FONT;
    private static final Font NORMAL_FONT;
    private static final Font SMALL_FONT;

    static {
        try {
            byte[] fontBytes;
            try (InputStream fontStream = InvoicePdfService.class.getResourceAsStream("/fonts/DejaVuSans.ttf")) {
                if (fontStream == null) {
                    throw new RuntimeException("DejaVuSans.ttf bulunamadı");
                }
                fontBytes = fontStream.readAllBytes();
            }
            BaseFont bf = BaseFont.createFont(
                    "DejaVuSans.ttf",
                    BaseFont.IDENTITY_H,
                    BaseFont.EMBEDDED,
                    true,
                    fontBytes,
                    null);
            TITLE_FONT  = new Font(bf, 18, Font.BOLD);
            HEADER_FONT = new Font(bf, 11, Font.BOLD);
            NORMAL_FONT = new Font(bf, 10, Font.NORMAL);
            SMALL_FONT  = new Font(bf,  9, Font.NORMAL);
        } catch (DocumentException | IOException e) {
            throw new ExceptionInInitializerError("DejaVuSans.ttf yüklenemedi: " + e.getMessage());
        }
    }

    public byte[] generate(InvoiceResponse invoice) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4, 36, 36, 54, 36);
            PdfWriter.getInstance(document, baos);
            document.open();

            addTitle(document);
            addMetadata(document, invoice);
            addCustomerInfo(document, invoice);
            addItemsTable(document, invoice);
            addFooter(document);

            document.close();
            return baos.toByteArray();

        } catch (Exception e) {
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, "PDF oluşturulamadı");
        }
    }

    private void addTitle(Document document) throws DocumentException {
        Paragraph title = new Paragraph("FİŞ", TITLE_FONT);
        title.setAlignment(Element.ALIGN_CENTER);
        title.setSpacingAfter(16);
        document.add(title);
    }

    private void addMetadata(Document document, InvoiceResponse invoice) throws DocumentException {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setSpacingAfter(12);

        addMetaRow(table, "Fiş No:", invoice.getId().toString());
        addMetaRow(table, "Tarih:", invoice.getInvoiceDate().toString());
        addMetaRow(table, "Durum:", invoice.getStatus() == InvoiceStatus.OPEN ? "Açık" : "Kapalı");

        document.add(table);
    }

    private void addMetaRow(PdfPTable table, String label, String value) {
        PdfPCell labelCell = new PdfPCell(new Phrase(label, HEADER_FONT));
        labelCell.setBorder(0);
        labelCell.setPaddingBottom(4);

        PdfPCell valueCell = new PdfPCell(new Phrase(value, NORMAL_FONT));
        valueCell.setBorder(0);
        valueCell.setPaddingBottom(4);

        table.addCell(labelCell);
        table.addCell(valueCell);
    }

    private void addCustomerInfo(Document document, InvoiceResponse invoice) throws DocumentException {
        Paragraph section = new Paragraph("Müşteri Bilgileri", HEADER_FONT);
        section.setSpacingBefore(8);
        section.setSpacingAfter(4);
        document.add(section);

        document.add(new Paragraph("Ad: " + invoice.getCustomer().getName(), NORMAL_FONT));

        if (invoice.getCustomer().getPhone() != null) {
            document.add(new Paragraph("Tel: " + invoice.getCustomer().getPhone(), NORMAL_FONT));
        }

        if (invoice.getCustomer().getAddress() != null) {
            document.add(new Paragraph("Adres: " + invoice.getCustomer().getAddress(), NORMAL_FONT));
        }

        Paragraph spacer = new Paragraph(" ");
        spacer.setSpacingAfter(8);
        document.add(spacer);
    }

    private void addItemsTable(Document document, InvoiceResponse invoice) throws DocumentException {
        PdfPTable table = new PdfPTable(5);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{3f, 1.2f, 1.2f, 1.5f, 1.5f});
        table.setSpacingAfter(12);

        addTableHeader(table, "Ürün");
        addTableHeader(table, "Birim");
        addTableHeader(table, "Miktar");
        addTableHeader(table, "Birim Fiyat");
        addTableHeader(table, "Tutar");

        for (InvoiceItemResponse item : invoice.getItems()) {
            addTableCell(table, item.getProduct().getName(), Element.ALIGN_LEFT);
            addTableCell(table, item.getProduct().getUnit().toString(), Element.ALIGN_CENTER);
            addTableCell(table, item.getQuantity().stripTrailingZeros().toPlainString(), Element.ALIGN_RIGHT);
            addTableCell(table, formatAmount(item.getUnitPrice()), Element.ALIGN_RIGHT);
            addTableCell(table, formatAmount(item.getLineTotal()), Element.ALIGN_RIGHT);
        }

        addTotalRow(table, invoice.getTotalAmount());

        document.add(table);
    }

    private void addTableHeader(PdfPTable table, String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, HEADER_FONT));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setPadding(6);
        cell.setGrayFill(0.85f);
        table.addCell(cell);
    }

    private void addTableCell(PdfPTable table, String text, int alignment) {
        PdfPCell cell = new PdfPCell(new Phrase(text, NORMAL_FONT));
        cell.setHorizontalAlignment(alignment);
        cell.setPadding(5);
        table.addCell(cell);
    }

    private void addTotalRow(PdfPTable table, BigDecimal total) {
        PdfPCell emptyCell = new PdfPCell(new Phrase(""));
        emptyCell.setBorder(0);
        emptyCell.setColspan(4);
        table.addCell(emptyCell);

        PdfPCell totalLabel = new PdfPCell(new Phrase("TOPLAM: " + formatAmount(total), HEADER_FONT));
        totalLabel.setHorizontalAlignment(Element.ALIGN_RIGHT);
        totalLabel.setPadding(5);
        table.addCell(totalLabel);
    }

    private void addFooter(Document document) throws DocumentException {
        Paragraph footer = new Paragraph("Bu fiş elektronik olarak oluşturulmuştur.", SMALL_FONT);
        footer.setAlignment(Element.ALIGN_CENTER);
        footer.setSpacingBefore(20);
        document.add(footer);
    }

    private String formatAmount(BigDecimal amount) {
        return String.format("%.2f ₺", amount);
    }
}
