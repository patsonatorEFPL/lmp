package com.lmp.service.invoice;

import com.lmp.domain.entity.Order;
import com.lmp.domain.entity.User;
import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Service pour générer des factures PDF professionnelles avec OpenPDF.
 * Les factures sont générées à la demande et mises en cache temporairement.
 */
@Service
public class InvoicePdfService {
    
    private static final Logger log = LoggerFactory.getLogger(InvoicePdfService.class);
    
    // Couleurs de la charte graphique LMP
    private static final Color PRIMARY_COLOR = new Color(102, 126, 234); // #667eea
    private static final Color SECONDARY_COLOR = new Color(118, 75, 162); // #764ba2
    private static final Color TEXT_COLOR = new Color(51, 51, 51);
    private static final Color LIGHT_GRAY = new Color(248, 249, 250);
    private static final Color BORDER_COLOR = new Color(222, 226, 230);
    
    // Informations de l'entreprise
    private static final String COMPANY_NAME = "LMP";
    private static final String COMPANY_ADDRESS = "Rue Gatti De Gamond 97";
    private static final String COMPANY_CITY = "1180 Uccle, Belgique";
    private static final String COMPANY_EMAIL = "contact@lmp-services.ca";
    private static final String COMPANY_PHONE = "+32 2 XXX XX XX";
    
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy à HH:mm");
    
    /**
     * Génère une facture PDF pour une commande donnée.
     * 
     * @param order La commande pour laquelle générer la facture
     * @param user L'utilisateur propriétaire de la commande
     * @return Les bytes du PDF généré
     */
    public byte[] generateInvoicePdf(Order order, User user) {
        log.info("Génération de la facture PDF pour la commande #{}", order.getId());
        
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4, 50, 50, 50, 50);
            PdfWriter writer = PdfWriter.getInstance(document, baos);
            
            document.open();
            
            // Ajouter le contenu
            addHeader(document, order);
            addCompanyAndClientInfo(document, order, user);
            addInvoiceDetails(document, order);
            addItemsTable(document, order);
            addTotals(document, order);
            addLegalMentions(document);
            addFooter(document, writer);
            
            document.close();
            
            log.info("Facture PDF générée avec succès pour la commande #{}", order.getId());
            return baos.toByteArray();
            
        } catch (Exception e) {
            log.error("Erreur lors de la génération de la facture PDF pour la commande #{}: {}", 
                     order.getId(), e.getMessage(), e);
            throw new RuntimeException("Erreur lors de la génération de la facture PDF", e);
        }
    }
    
    /**
     * Génère le numéro de facture au format LMP-YYYY-MM-DD-XXXXX
     */
    public String generateInvoiceNumber(Order order) {
        LocalDateTime date = order.getPaidAt() != null ? order.getPaidAt() : order.getCreatedAt();
        return String.format("LMP-%d-%02d-%02d-%05d",
            date.getYear(), date.getMonthValue(), date.getDayOfMonth(), order.getId());
    }
    
    private void addHeader(Document document, Order order) throws DocumentException {
        // Tableau pour l'en-tête avec logo et titre
        PdfPTable headerTable = new PdfPTable(2);
        headerTable.setWidthPercentage(100);
        headerTable.setWidths(new float[]{1, 1});
        
        // Cellule gauche - Logo/Nom entreprise
        PdfPCell logoCell = new PdfPCell();
        logoCell.setBorder(Rectangle.NO_BORDER);
        logoCell.setPaddingBottom(20);
        
        // Nom de l'entreprise avec style
        Font companyFont = new Font(Font.HELVETICA, 24, Font.BOLD, PRIMARY_COLOR);
        Paragraph companyName = new Paragraph(COMPANY_NAME, companyFont);
        logoCell.addElement(companyName);
        
        Font taglineFont = new Font(Font.HELVETICA, 10, Font.ITALIC, new Color(108, 117, 125));
        Paragraph tagline = new Paragraph("Marketing Digital & Services Web", taglineFont);
        logoCell.addElement(tagline);
        
        headerTable.addCell(logoCell);
        
        // Cellule droite - FACTURE
        PdfPCell titleCell = new PdfPCell();
        titleCell.setBorder(Rectangle.NO_BORDER);
        titleCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        titleCell.setPaddingBottom(20);
        
        Font titleFont = new Font(Font.HELVETICA, 32, Font.BOLD, SECONDARY_COLOR);
        Paragraph title = new Paragraph("FACTURE", titleFont);
        title.setAlignment(Element.ALIGN_RIGHT);
        titleCell.addElement(title);
        
        Font invoiceNumFont = new Font(Font.HELVETICA, 12, Font.NORMAL, TEXT_COLOR);
        Paragraph invoiceNum = new Paragraph("N° " + generateInvoiceNumber(order), invoiceNumFont);
        invoiceNum.setAlignment(Element.ALIGN_RIGHT);
        titleCell.addElement(invoiceNum);
        
        headerTable.addCell(titleCell);
        
        document.add(headerTable);
        
        // Ligne de séparation
        addSeparatorLine(document);
    }
    
    private void addCompanyAndClientInfo(Document document, Order order, User user) throws DocumentException {
        document.add(Chunk.NEWLINE);
        
        PdfPTable infoTable = new PdfPTable(2);
        infoTable.setWidthPercentage(100);
        infoTable.setWidths(new float[]{1, 1});
        
        // Cellule gauche - Informations entreprise
        PdfPCell companyCell = new PdfPCell();
        companyCell.setBorder(Rectangle.NO_BORDER);
        companyCell.setPaddingRight(20);
        
        Font labelFont = new Font(Font.HELVETICA, 10, Font.BOLD, PRIMARY_COLOR);
        Font valueFont = new Font(Font.HELVETICA, 10, Font.NORMAL, TEXT_COLOR);
        
        Paragraph emetteurLabel = new Paragraph("ÉMETTEUR", labelFont);
        companyCell.addElement(emetteurLabel);
        
        companyCell.addElement(new Paragraph(COMPANY_NAME, valueFont));
        companyCell.addElement(new Paragraph(COMPANY_ADDRESS, valueFont));
        companyCell.addElement(new Paragraph(COMPANY_CITY, valueFont));
        companyCell.addElement(new Paragraph("Email: " + COMPANY_EMAIL, valueFont));
        
        infoTable.addCell(companyCell);
        
        // Cellule droite - Informations client
        PdfPCell clientCell = new PdfPCell();
        clientCell.setBorder(Rectangle.NO_BORDER);
        clientCell.setBackgroundColor(LIGHT_GRAY);
        clientCell.setPadding(15);
        
        Paragraph clientLabel = new Paragraph("FACTURÉ À", labelFont);
        clientCell.addElement(clientLabel);
        
        String clientName = user.getDisplayName();
        if (clientName == null || clientName.trim().isEmpty()) {
            clientName = user.getEmail();
        }
        clientCell.addElement(new Paragraph(clientName, valueFont));
        clientCell.addElement(new Paragraph(user.getEmail(), valueFont));
        
        if (user.getPhone() != null && !user.getPhone().isEmpty()) {
            clientCell.addElement(new Paragraph("Tél: " + user.getPhone(), valueFont));
        }
        
        // Adresse de facturation si disponible
        if (order.getBillingAddress() != null && !order.getBillingAddress().isEmpty()) {
            clientCell.addElement(new Paragraph(order.getBillingAddress(), valueFont));
            if (order.getBillingCity() != null) {
                String cityLine = order.getBillingPostalCode() != null 
                    ? order.getBillingPostalCode() + " " + order.getBillingCity()
                    : order.getBillingCity();
                clientCell.addElement(new Paragraph(cityLine, valueFont));
            }
            if (order.getBillingCountry() != null) {
                clientCell.addElement(new Paragraph(order.getBillingCountry(), valueFont));
            }
        } else if (user.getAddress() != null && !user.getAddress().isEmpty()) {
            clientCell.addElement(new Paragraph(user.getAddress(), valueFont));
            if (user.getCity() != null) {
                String cityLine = user.getPostalCode() != null 
                    ? user.getPostalCode() + " " + user.getCity()
                    : user.getCity();
                clientCell.addElement(new Paragraph(cityLine, valueFont));
            }
            if (user.getCountry() != null) {
                clientCell.addElement(new Paragraph(user.getCountry(), valueFont));
            }
        }
        
        infoTable.addCell(clientCell);
        
        document.add(infoTable);
        document.add(Chunk.NEWLINE);
    }
    
    private void addInvoiceDetails(Document document, Order order) throws DocumentException {
        PdfPTable detailsTable = new PdfPTable(4);
        detailsTable.setWidthPercentage(100);
        detailsTable.setWidths(new float[]{1, 1, 1, 1});
        
        Font headerFont = new Font(Font.HELVETICA, 9, Font.BOLD, Color.WHITE);
        Font valueFont = new Font(Font.HELVETICA, 10, Font.NORMAL, TEXT_COLOR);
        
        // En-têtes
        String[] headers = {"Date de facture", "N° Commande", "Date de paiement", "Statut"};
        for (String header : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(header, headerFont));
            cell.setBackgroundColor(PRIMARY_COLOR);
            cell.setPadding(10);
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            detailsTable.addCell(cell);
        }
        
        // Valeurs
        LocalDateTime invoiceDate = order.getPaidAt() != null ? order.getPaidAt() : order.getCreatedAt();
        String[] values = {
            invoiceDate.format(dateFormatter),
            "#" + order.getId(),
            order.getPaidAt() != null ? order.getPaidAt().format(dateFormatter) : "-",
            getStatusLabel(order.getStatus().name())
        };
        
        for (String value : values) {
            PdfPCell cell = new PdfPCell(new Phrase(value, valueFont));
            cell.setPadding(10);
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setBackgroundColor(LIGHT_GRAY);
            detailsTable.addCell(cell);
        }
        
        document.add(detailsTable);
        document.add(Chunk.NEWLINE);
    }
    
    private void addItemsTable(Document document, Order order) throws DocumentException {
        PdfPTable itemsTable = new PdfPTable(4);
        itemsTable.setWidthPercentage(100);
        itemsTable.setWidths(new float[]{3, 1, 1, 1});
        
        Font headerFont = new Font(Font.HELVETICA, 10, Font.BOLD, Color.WHITE);
        Font itemFont = new Font(Font.HELVETICA, 10, Font.NORMAL, TEXT_COLOR);
        Font itemBoldFont = new Font(Font.HELVETICA, 10, Font.BOLD, TEXT_COLOR);
        
        // En-têtes du tableau
        String[] headers = {"Description", "Quantité", "Prix unitaire", "Total"};
        for (String header : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(header, headerFont));
            cell.setBackgroundColor(SECONDARY_COLOR);
            cell.setPadding(12);
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            itemsTable.addCell(cell);
        }
        
        // Ligne du service
        String serviceName = order.getServiceName() != null ? order.getServiceName() : "Service LMP";
        String currency = order.getCurrency() != null ? order.getCurrency() : "EUR";
        
        // Description
        PdfPCell descCell = new PdfPCell();
        descCell.setPadding(12);
        descCell.setBorderColor(BORDER_COLOR);
        Paragraph serviceNamePara = new Paragraph(serviceName, itemBoldFont);
        descCell.addElement(serviceNamePara);
        if (order.getNotes() != null && !order.getNotes().isEmpty()) {
            Font notesFont = new Font(Font.HELVETICA, 9, Font.ITALIC, new Color(108, 117, 125));
            Paragraph notesPara = new Paragraph(order.getNotes(), notesFont);
            descCell.addElement(notesPara);
        }
        itemsTable.addCell(descCell);
        
        // Quantité
        PdfPCell qtyCell = new PdfPCell(new Phrase("1", itemFont));
        qtyCell.setPadding(12);
        qtyCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        qtyCell.setBorderColor(BORDER_COLOR);
        itemsTable.addCell(qtyCell);
        
        // Prix unitaire
        PdfPCell priceCell = new PdfPCell(new Phrase(formatPrice(order.getTotalAmount(), currency), itemFont));
        priceCell.setPadding(12);
        priceCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        priceCell.setBorderColor(BORDER_COLOR);
        itemsTable.addCell(priceCell);
        
        // Total
        PdfPCell totalCell = new PdfPCell(new Phrase(formatPrice(order.getTotalAmount(), currency), itemBoldFont));
        totalCell.setPadding(12);
        totalCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        totalCell.setBorderColor(BORDER_COLOR);
        itemsTable.addCell(totalCell);
        
        document.add(itemsTable);
    }
    
    private void addTotals(Document document, Order order) throws DocumentException {
        document.add(Chunk.NEWLINE);
        
        String currency = order.getCurrency() != null ? order.getCurrency() : "EUR";
        
        PdfPTable totalsTable = new PdfPTable(2);
        totalsTable.setWidthPercentage(40);
        totalsTable.setHorizontalAlignment(Element.ALIGN_RIGHT);
        totalsTable.setWidths(new float[]{1, 1});
        
        Font labelFont = new Font(Font.HELVETICA, 10, Font.NORMAL, TEXT_COLOR);
        Font valueFont = new Font(Font.HELVETICA, 10, Font.NORMAL, TEXT_COLOR);
        Font totalLabelFont = new Font(Font.HELVETICA, 12, Font.BOLD, PRIMARY_COLOR);
        Font totalValueFont = new Font(Font.HELVETICA, 14, Font.BOLD, PRIMARY_COLOR);
        
        // Sous-total HT (considérant TVA incluse pour simplifier)
        java.math.BigDecimal totalAmount = order.getTotalAmount();
        java.math.BigDecimal tvaRate = new java.math.BigDecimal("0.21"); // TVA belge 21%
        java.math.BigDecimal htAmount = totalAmount.divide(tvaRate.add(java.math.BigDecimal.ONE), 2, java.math.RoundingMode.HALF_UP);
        java.math.BigDecimal tvaAmount = totalAmount.subtract(htAmount);
        
        // Sous-total HT
        addTotalRow(totalsTable, "Sous-total HT", formatPrice(htAmount, currency), labelFont, valueFont, LIGHT_GRAY);
        
        // TVA
        addTotalRow(totalsTable, "TVA (21%)", formatPrice(tvaAmount, currency), labelFont, valueFont, Color.WHITE);
        
        // Total TTC
        addTotalRow(totalsTable, "TOTAL TTC", formatPrice(totalAmount, currency), totalLabelFont, totalValueFont, new Color(232, 245, 233));
        
        document.add(totalsTable);
        document.add(Chunk.NEWLINE);
    }
    
    private void addTotalRow(PdfPTable table, String label, String value, Font labelFont, Font valueFont, Color bgColor) {
        PdfPCell labelCell = new PdfPCell(new Phrase(label, labelFont));
        labelCell.setPadding(10);
        labelCell.setBackgroundColor(bgColor);
        labelCell.setBorderColor(BORDER_COLOR);
        table.addCell(labelCell);
        
        PdfPCell valueCell = new PdfPCell(new Phrase(value, valueFont));
        valueCell.setPadding(10);
        valueCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        valueCell.setBackgroundColor(bgColor);
        valueCell.setBorderColor(BORDER_COLOR);
        table.addCell(valueCell);
    }
    
    private void addLegalMentions(Document document) throws DocumentException {
        document.add(Chunk.NEWLINE);
        document.add(Chunk.NEWLINE);
        
        Font mentionFont = new Font(Font.HELVETICA, 8, Font.NORMAL, new Color(108, 117, 125));
        
        Paragraph mentions = new Paragraph();
        mentions.setAlignment(Element.ALIGN_CENTER);
        mentions.add(new Chunk("Conditions de paiement : Payé par carte bancaire via Stripe\n", mentionFont));
        mentions.add(new Chunk("En cas de retard de paiement, une pénalité de 3 fois le taux d'intérêt légal sera appliquée.\n", mentionFont));
        mentions.add(new Chunk("Conformément à la loi, une indemnité forfaitaire de 40€ pour frais de recouvrement est due en cas de retard.\n", mentionFont));
        
        document.add(mentions);
    }
    
    private void addFooter(Document document, PdfWriter writer) throws DocumentException {
        PdfContentByte cb = writer.getDirectContent();
        
        // Ligne de séparation en bas
        cb.setColorStroke(BORDER_COLOR);
        cb.setLineWidth(0.5f);
        cb.moveTo(50, 80);
        cb.lineTo(545, 80);
        cb.stroke();
        
        // Texte du footer
        Font footerFont = new Font(Font.HELVETICA, 8, Font.NORMAL, new Color(108, 117, 125));
        
        ColumnText.showTextAligned(cb, Element.ALIGN_CENTER,
            new Phrase(COMPANY_NAME + " - " + COMPANY_ADDRESS + ", " + COMPANY_CITY, footerFont),
            297.5f, 60, 0);
        
        ColumnText.showTextAligned(cb, Element.ALIGN_CENTER,
            new Phrase("Email: " + COMPANY_EMAIL, footerFont),
            297.5f, 48, 0);
        
        ColumnText.showTextAligned(cb, Element.ALIGN_CENTER,
            new Phrase("Document généré automatiquement - " + LocalDateTime.now().format(dateTimeFormatter), footerFont),
            297.5f, 36, 0);
    }
    
    private void addSeparatorLine(Document document) throws DocumentException {
        PdfPTable lineTable = new PdfPTable(1);
        lineTable.setWidthPercentage(100);
        
        PdfPCell lineCell = new PdfPCell();
        lineCell.setBorder(Rectangle.NO_BORDER);
        lineCell.setBorderWidthBottom(2);
        lineCell.setBorderColorBottom(PRIMARY_COLOR);
        lineCell.setFixedHeight(5);
        
        lineTable.addCell(lineCell);
        document.add(lineTable);
    }
    
    private String formatPrice(java.math.BigDecimal amount, String currency) {
        if (amount == null) return "0,00 " + currency;
        
        java.text.DecimalFormat df = new java.text.DecimalFormat("#,##0.00");
        df.setDecimalFormatSymbols(new java.text.DecimalFormatSymbols(java.util.Locale.FRENCH));
        return df.format(amount) + " " + currency;
    }
    
    private String getStatusLabel(String status) {
        switch (status) {
            case "PENDING":
            case "PAYMENT_PENDING":
                return "En attente";
            case "CONFIRMED":
                return "Confirmée";
            case "PROCESSING":
            case "IN_PROGRESS":
                return "En cours";
            case "COMPLETED":
                return "Terminée";
            case "DELIVERED":
                return "Livrée";
            case "CANCELLED":
                return "Annulée";
            case "REFUNDED":
                return "Remboursée";
            default:
                return status;
        }
    }
}
