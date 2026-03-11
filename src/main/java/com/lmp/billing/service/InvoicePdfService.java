package com.lmp.billing.service;

import com.lmp.billing.domain.Order;
import com.lmp.auth.domain.User;
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
 * Service pour générer des factures PDF premium avec OpenPDF.
 * Design : palette charcoal/gold, typographie raffinée, composition aérée.
 */
@Service
public class InvoicePdfService {

    private static final Logger log = LoggerFactory.getLogger(InvoicePdfService.class);

    // ═══════════════════════════════════════════════════════
    // PALETTE PREMIUM — Charcoal + Gold
    // ═══════════════════════════════════════════════════════
    private static final Color CHARCOAL = new Color(34, 40, 49); // #222831 — titres / header
    private static final Color DARK_SLATE = new Color(57, 62, 70); // #393E46 — sous-titres / secondaire
    private static final Color GOLD_ACCENT = new Color(212, 175, 55); // #D4AF37 — accent premium
    private static final Color GOLD_LIGHT = new Color(248, 237, 195); // #F8EDC3 — fond accent doux
    private static final Color TEXT_PRIMARY = new Color(34, 40, 49); // #222831
    private static final Color TEXT_SECONDARY = new Color(120, 120, 130); // #787882
    private static final Color WARM_GRAY = new Color(245, 243, 240); // #F5F3F0 — fond tableau
    private static final Color BORDER_SUBTLE = new Color(225, 220, 215); // #E1DCD7
    private static final Color WHITE = new Color(255, 255, 255);

    // ═══════════════════════════════════════════════════════
    // INFORMATIONS ENTREPRISE
    // ═══════════════════════════════════════════════════════
    private static final String COMPANY_NAME = "LMP DIGITAL SERVICES";
    private static final String COMPANY_TAGLINE = "Marketing Digital & Solutions Web";
    private static final String COMPANY_ADDRESS = "Rue Gatti De Gamond 97";
    private static final String COMPANY_CITY = "1180 Uccle, Belgique";
    private static final String COMPANY_EMAIL = "contact@lmp-services.ca";
    private static final String COMPANY_PHONE = "+32 2 XXX XX XX";
    private static final String COMPANY_WEB = "www.lmp-services.ca";

    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy à HH:mm");

    /**
     * Génère une facture PDF premium pour une commande donnée.
     */
    public byte[] generateInvoicePdf(Order order, User user) {
        log.info("Génération de la facture PDF premium pour la commande #{}", order.getId());

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4, 45, 45, 40, 60);
            PdfWriter writer = PdfWriter.getInstance(document, baos);

            document.open();

            // Dessiner la barre de couleur latérale (accent gold)
            drawAccentBar(writer);

            addPremiumHeader(document, order);
            addCompanyAndClientBlock(document, order, user);
            addInvoiceMetaStrip(document, order);
            addItemsTable(document, order);
            addTotalsBlock(document, order);
            addPaymentInfo(document, order);
            addLegalMentions(document);
            addPremiumFooter(writer);

            document.close();

            log.info("Facture PDF premium générée avec succès pour la commande #{}", order.getId());
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

    // ═══════════════════════════════════════════════════════
    // ACCENT BAR — Bande latérale dorée
    // ═══════════════════════════════════════════════════════

    private void drawAccentBar(PdfWriter writer) {
        PdfContentByte cb = writer.getDirectContentUnder();
        // Barre verticale dorée sur le côté gauche
        cb.setColorFill(GOLD_ACCENT);
        cb.rectangle(0, 0, 6, PageSize.A4.getHeight());
        cb.fill();

        // Petite barre horizontale dorée en haut
        cb.rectangle(0, PageSize.A4.getHeight() - 3, PageSize.A4.getWidth(), 3);
        cb.fill();
    }

    // ═══════════════════════════════════════════════════════
    // HEADER — Nom entreprise + numéro de facture
    // ═══════════════════════════════════════════════════════

    private void addPremiumHeader(Document document, Order order) throws DocumentException {
        PdfPTable headerTable = new PdfPTable(2);
        headerTable.setWidthPercentage(100);
        headerTable.setWidths(new float[] { 1.2f, 0.8f });
        headerTable.setSpacingAfter(5);

        // ── Gauche : Nom de l'entreprise ──
        PdfPCell logoCell = new PdfPCell();
        logoCell.setBorder(Rectangle.NO_BORDER);
        logoCell.setPaddingBottom(15);
        logoCell.setPaddingLeft(10);

        Font companyFont = new Font(Font.HELVETICA, 22, Font.BOLD, CHARCOAL);
        Paragraph company = new Paragraph(COMPANY_NAME, companyFont);
        company.setSpacingAfter(2);
        logoCell.addElement(company);

        Font taglineFont = new Font(Font.HELVETICA, 9, Font.NORMAL, TEXT_SECONDARY);
        Paragraph tagline = new Paragraph(COMPANY_TAGLINE, taglineFont);
        logoCell.addElement(tagline);

        headerTable.addCell(logoCell);

        // ── Droite : FACTURE + numéro ──
        PdfPCell titleCell = new PdfPCell();
        titleCell.setBorder(Rectangle.NO_BORDER);
        titleCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        titleCell.setPaddingBottom(15);

        Font factureFont = new Font(Font.HELVETICA, 28, Font.BOLD, GOLD_ACCENT);
        Paragraph facture = new Paragraph("FACTURE", factureFont);
        facture.setAlignment(Element.ALIGN_RIGHT);
        titleCell.addElement(facture);

        Font numFont = new Font(Font.HELVETICA, 10, Font.NORMAL, TEXT_SECONDARY);
        Paragraph num = new Paragraph("N° " + generateInvoiceNumber(order), numFont);
        num.setAlignment(Element.ALIGN_RIGHT);
        num.setSpacingBefore(2);
        titleCell.addElement(num);

        headerTable.addCell(titleCell);
        document.add(headerTable);

        // Ligne de séparation élégante : fine dorée + épaisse charcoal
        addDualSeparator(document);
    }

    // ═══════════════════════════════════════════════════════
    // BLOCS ENTREPRISE / CLIENT
    // ═══════════════════════════════════════════════════════

    private void addCompanyAndClientBlock(Document document, Order order, User user) throws DocumentException {
        document.add(new Paragraph(" ", new Font(Font.HELVETICA, 8)));

        PdfPTable infoTable = new PdfPTable(2);
        infoTable.setWidthPercentage(100);
        infoTable.setWidths(new float[] { 1, 1 });
        infoTable.setSpacingAfter(15);

        // ── Gauche : Émetteur ──
        PdfPCell companyCell = new PdfPCell();
        companyCell.setBorder(Rectangle.NO_BORDER);
        companyCell.setPaddingRight(25);
        companyCell.setPaddingLeft(10);

        Font labelFont = new Font(Font.HELVETICA, 8, Font.BOLD, GOLD_ACCENT);
        Font valueFont = new Font(Font.HELVETICA, 9, Font.NORMAL, TEXT_PRIMARY);
        Font valueSmallFont = new Font(Font.HELVETICA, 8, Font.NORMAL, TEXT_SECONDARY);

        Paragraph emLabel = new Paragraph("ÉMETTEUR", labelFont);
        emLabel.setSpacingAfter(6);
        companyCell.addElement(emLabel);

        companyCell.addElement(new Paragraph(COMPANY_NAME, new Font(Font.HELVETICA, 10, Font.BOLD, TEXT_PRIMARY)));
        companyCell.addElement(new Paragraph(COMPANY_ADDRESS, valueFont));
        companyCell.addElement(new Paragraph(COMPANY_CITY, valueFont));

        Paragraph emailLine = new Paragraph(COMPANY_EMAIL, valueSmallFont);
        emailLine.setSpacingBefore(4);
        companyCell.addElement(emailLine);
        companyCell.addElement(new Paragraph(COMPANY_WEB, valueSmallFont));

        infoTable.addCell(companyCell);

        // ── Droite : Client (fond subtle) ──
        PdfPCell clientCell = new PdfPCell();
        clientCell.setBorder(Rectangle.NO_BORDER);
        clientCell.setBackgroundColor(WARM_GRAY);
        clientCell.setPadding(15);
        clientCell.setBorderWidth(0.5f);
        clientCell.setBorderColor(BORDER_SUBTLE);
        // Arrondi simulé par bordure subtile
        clientCell.setBorder(Rectangle.BOX);
        clientCell.setBorderColor(BORDER_SUBTLE);
        clientCell.setBorderWidth(0.5f);

        Paragraph clientLabel = new Paragraph("FACTURÉ À", labelFont);
        clientLabel.setSpacingAfter(6);
        clientCell.addElement(clientLabel);

        String clientName = user.getDisplayName();
        if (clientName == null || clientName.trim().isEmpty()) {
            clientName = user.getEmail();
        }
        clientCell.addElement(new Paragraph(clientName, new Font(Font.HELVETICA, 10, Font.BOLD, TEXT_PRIMARY)));
        clientCell.addElement(new Paragraph(user.getEmail(), valueFont));

        if (user.getPhone() != null && !user.getPhone().isEmpty()) {
            Paragraph phoneLine = new Paragraph("Tél: " + user.getPhone(), valueSmallFont);
            phoneLine.setSpacingBefore(3);
            clientCell.addElement(phoneLine);
        }

        // Adresse de facturation
        if (order.getBillingAddress() != null && !order.getBillingAddress().isEmpty()) {
            Paragraph addrLine = new Paragraph(order.getBillingAddress(), valueSmallFont);
            addrLine.setSpacingBefore(3);
            clientCell.addElement(addrLine);
            if (order.getBillingCity() != null) {
                String cityLine = order.getBillingPostalCode() != null
                        ? order.getBillingPostalCode() + " " + order.getBillingCity()
                        : order.getBillingCity();
                clientCell.addElement(new Paragraph(cityLine, valueSmallFont));
            }
            if (order.getBillingCountry() != null) {
                clientCell.addElement(new Paragraph(order.getBillingCountry(), valueSmallFont));
            }
        } else if (user.getAddress() != null && !user.getAddress().isEmpty()) {
            Paragraph addrLine = new Paragraph(user.getAddress(), valueSmallFont);
            addrLine.setSpacingBefore(3);
            clientCell.addElement(addrLine);
            if (user.getCity() != null) {
                String cityLine = user.getPostalCode() != null
                        ? user.getPostalCode() + " " + user.getCity()
                        : user.getCity();
                clientCell.addElement(new Paragraph(cityLine, valueSmallFont));
            }
            if (user.getCountry() != null) {
                clientCell.addElement(new Paragraph(user.getCountry(), valueSmallFont));
            }
        }

        infoTable.addCell(clientCell);
        document.add(infoTable);
    }

    // ═══════════════════════════════════════════════════════
    // MÉTADONNÉES — Bande d'infos (date, commande, statut)
    // ═══════════════════════════════════════════════════════

    private void addInvoiceMetaStrip(Document document, Order order) throws DocumentException {
        PdfPTable metaTable = new PdfPTable(4);
        metaTable.setWidthPercentage(100);
        metaTable.setWidths(new float[] { 1, 1, 1, 1 });
        metaTable.setSpacingAfter(20);

        Font metaLabelFont = new Font(Font.HELVETICA, 7, Font.BOLD, WHITE);
        Font metaValueFont = new Font(Font.HELVETICA, 9, Font.NORMAL, WHITE);

        LocalDateTime invoiceDate = order.getPaidAt() != null ? order.getPaidAt() : order.getCreatedAt();

        String[][] metaData = {
                { "DATE DE FACTURE", invoiceDate.format(dateFormatter) },
                { "N° COMMANDE", "#" + order.getId() },
                { "DATE DE PAIEMENT", order.getPaidAt() != null ? order.getPaidAt().format(dateFormatter) : "—" },
                { "STATUT", getStatusLabel(order.getStatus().name()) }
        };

        for (String[] meta : metaData) {
            PdfPCell cell = new PdfPCell();
            cell.setBackgroundColor(CHARCOAL);
            cell.setPadding(10);
            cell.setPaddingTop(8);
            cell.setPaddingBottom(10);
            cell.setBorderColor(DARK_SLATE);
            cell.setBorderWidth(0.5f);

            Paragraph label = new Paragraph(meta[0], metaLabelFont);
            label.setAlignment(Element.ALIGN_CENTER);
            label.setSpacingAfter(3);
            cell.addElement(label);

            Paragraph value = new Paragraph(meta[1], metaValueFont);
            value.setAlignment(Element.ALIGN_CENTER);
            cell.addElement(value);

            metaTable.addCell(cell);
        }

        document.add(metaTable);
    }

    // ═══════════════════════════════════════════════════════
    // TABLEAU DES ARTICLES
    // ═══════════════════════════════════════════════════════

    private void addItemsTable(Document document, Order order) throws DocumentException {
        PdfPTable itemsTable = new PdfPTable(4);
        itemsTable.setWidthPercentage(100);
        itemsTable.setWidths(new float[] { 3.5f, 0.8f, 1.2f, 1.2f });
        itemsTable.setSpacingAfter(5);

        Font headerFont = new Font(Font.HELVETICA, 8, Font.BOLD, GOLD_ACCENT);
        Font itemFont = new Font(Font.HELVETICA, 10, Font.NORMAL, TEXT_PRIMARY);
        Font itemBoldFont = new Font(Font.HELVETICA, 10, Font.BOLD, TEXT_PRIMARY);

        // ── En-têtes du tableau ──
        String[] headers = { "DESCRIPTION", "QTÉ", "PRIX UNITAIRE", "TOTAL" };
        int[] alignments = { Element.ALIGN_LEFT, Element.ALIGN_CENTER, Element.ALIGN_RIGHT, Element.ALIGN_RIGHT };

        for (int i = 0; i < headers.length; i++) {
            PdfPCell cell = new PdfPCell(new Phrase(headers[i], headerFont));
            cell.setBackgroundColor(WHITE);
            cell.setPadding(10);
            cell.setPaddingBottom(8);
            cell.setHorizontalAlignment(alignments[i]);
            cell.setBorderWidthTop(0);
            cell.setBorderWidthLeft(0);
            cell.setBorderWidthRight(0);
            cell.setBorderWidthBottom(1.5f);
            cell.setBorderColorBottom(GOLD_ACCENT);
            itemsTable.addCell(cell);
        }

        // ── Ligne du service ──
        String serviceName = order.getServiceName() != null ? order.getServiceName() : "Service LMP";
        String currency = order.getCurrency() != null ? order.getCurrency() : "EUR";

        // Description
        PdfPCell descCell = new PdfPCell();
        descCell.setPadding(12);
        descCell.setBorderWidthTop(0);
        descCell.setBorderWidthLeft(0);
        descCell.setBorderWidthRight(0);
        descCell.setBorderWidthBottom(0.5f);
        descCell.setBorderColorBottom(BORDER_SUBTLE);
        descCell.setBackgroundColor(WARM_GRAY);

        Paragraph serviceNamePara = new Paragraph(serviceName, itemBoldFont);
        descCell.addElement(serviceNamePara);
        if (order.getNotes() != null && !order.getNotes().isEmpty()) {
            Font notesFont = new Font(Font.HELVETICA, 8, Font.ITALIC, TEXT_SECONDARY);
            Paragraph notesPara = new Paragraph(order.getNotes(), notesFont);
            notesPara.setSpacingBefore(3);
            descCell.addElement(notesPara);
        }
        itemsTable.addCell(descCell);

        // Quantité
        PdfPCell qtyCell = createItemCell("1", itemFont, Element.ALIGN_CENTER);
        itemsTable.addCell(qtyCell);

        // Prix unitaire
        PdfPCell priceCell = createItemCell(formatPrice(order.getTotalAmount(), currency), itemFont,
                Element.ALIGN_RIGHT);
        itemsTable.addCell(priceCell);

        // Total
        PdfPCell totalCell = createItemCell(formatPrice(order.getTotalAmount(), currency), itemBoldFont,
                Element.ALIGN_RIGHT);
        itemsTable.addCell(totalCell);

        document.add(itemsTable);
    }

    private PdfPCell createItemCell(String text, Font font, int alignment) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setPadding(12);
        cell.setHorizontalAlignment(alignment);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setBorderWidthTop(0);
        cell.setBorderWidthLeft(0);
        cell.setBorderWidthRight(0);
        cell.setBorderWidthBottom(0.5f);
        cell.setBorderColorBottom(BORDER_SUBTLE);
        cell.setBackgroundColor(WARM_GRAY);
        return cell;
    }

    // ═══════════════════════════════════════════════════════
    // TOTAUX — Bloc aligné à droite
    // ═══════════════════════════════════════════════════════

    private void addTotalsBlock(Document document, Order order) throws DocumentException {
        document.add(new Paragraph(" ", new Font(Font.HELVETICA, 6)));

        String currency = order.getCurrency() != null ? order.getCurrency() : "EUR";

        PdfPTable totalsTable = new PdfPTable(2);
        totalsTable.setWidthPercentage(42);
        totalsTable.setHorizontalAlignment(Element.ALIGN_RIGHT);
        totalsTable.setWidths(new float[] { 1.2f, 1f });
        totalsTable.setSpacingAfter(15);

        Font labelFont = new Font(Font.HELVETICA, 9, Font.NORMAL, TEXT_SECONDARY);
        Font valueFont = new Font(Font.HELVETICA, 9, Font.NORMAL, TEXT_PRIMARY);
        Font totalLabelFont = new Font(Font.HELVETICA, 11, Font.BOLD, CHARCOAL);
        Font totalValueFont = new Font(Font.HELVETICA, 14, Font.BOLD, GOLD_ACCENT);

        // Calcul TVA
        java.math.BigDecimal totalAmount = order.getTotalAmount();
        java.math.BigDecimal tvaRate = new java.math.BigDecimal("0.21");
        java.math.BigDecimal htAmount = totalAmount.divide(tvaRate.add(java.math.BigDecimal.ONE), 2,
                java.math.RoundingMode.HALF_UP);
        java.math.BigDecimal tvaAmount = totalAmount.subtract(htAmount);

        // Sous-total HT
        addTotalRow(totalsTable, "Sous-total HT", formatPrice(htAmount, currency), labelFont, valueFont, WHITE,
                BORDER_SUBTLE);

        // TVA
        addTotalRow(totalsTable, "TVA (21%)", formatPrice(tvaAmount, currency), labelFont, valueFont, WHITE,
                BORDER_SUBTLE);

        // ── TOTAL TTC — fond accent ──
        PdfPCell totalLabelCell = new PdfPCell(new Phrase("TOTAL TTC", totalLabelFont));
        totalLabelCell.setPadding(12);
        totalLabelCell.setBackgroundColor(GOLD_LIGHT);
        totalLabelCell.setBorder(Rectangle.NO_BORDER);
        totalLabelCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        totalsTable.addCell(totalLabelCell);

        PdfPCell totalValueCell = new PdfPCell(new Phrase(formatPrice(totalAmount, currency), totalValueFont));
        totalValueCell.setPadding(12);
        totalValueCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        totalValueCell.setBackgroundColor(GOLD_LIGHT);
        totalValueCell.setBorder(Rectangle.NO_BORDER);
        totalValueCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        totalsTable.addCell(totalValueCell);

        document.add(totalsTable);
    }

    private void addTotalRow(PdfPTable table, String label, String value, Font labelFont, Font valueFont, Color bgColor,
            Color borderColor) {
        PdfPCell labelCell = new PdfPCell(new Phrase(label, labelFont));
        labelCell.setPadding(8);
        labelCell.setBackgroundColor(bgColor);
        labelCell.setBorderWidthTop(0);
        labelCell.setBorderWidthLeft(0);
        labelCell.setBorderWidthRight(0);
        labelCell.setBorderWidthBottom(0.5f);
        labelCell.setBorderColorBottom(borderColor);
        table.addCell(labelCell);

        PdfPCell valueCell = new PdfPCell(new Phrase(value, valueFont));
        valueCell.setPadding(8);
        valueCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        valueCell.setBackgroundColor(bgColor);
        valueCell.setBorderWidthTop(0);
        valueCell.setBorderWidthLeft(0);
        valueCell.setBorderWidthRight(0);
        valueCell.setBorderWidthBottom(0.5f);
        valueCell.setBorderColorBottom(borderColor);
        table.addCell(valueCell);
    }

    // ═══════════════════════════════════════════════════════
    // INFORMATIONS DE PAIEMENT
    // ═══════════════════════════════════════════════════════

    private void addPaymentInfo(Document document, Order order) throws DocumentException {
        PdfPTable paymentTable = new PdfPTable(1);
        paymentTable.setWidthPercentage(100);
        paymentTable.setSpacingAfter(15);

        PdfPCell cell = new PdfPCell();
        cell.setBackgroundColor(WARM_GRAY);
        cell.setBorder(Rectangle.BOX);
        cell.setBorderColor(BORDER_SUBTLE);
        cell.setBorderWidth(0.5f);
        cell.setPadding(12);

        Font titleFont = new Font(Font.HELVETICA, 8, Font.BOLD, GOLD_ACCENT);
        Font infoFont = new Font(Font.HELVETICA, 8, Font.NORMAL, TEXT_SECONDARY);

        Paragraph title = new Paragraph("INFORMATIONS DE PAIEMENT", titleFont);
        title.setSpacingAfter(5);
        cell.addElement(title);

        String paymentMethod = order.getPaymentMethod() != null ? order.getPaymentMethod()
                : "Carte bancaire via Stripe";
        cell.addElement(new Paragraph("Méthode : " + paymentMethod, infoFont));

        if (order.getPaidAt() != null) {
            cell.addElement(new Paragraph("Payé le : " + order.getPaidAt().format(dateTimeFormatter), infoFont));
        }

        if (order.getStripePaymentIntentId() != null) {
            cell.addElement(new Paragraph("Référence : " + order.getStripePaymentIntentId(), infoFont));
        }

        paymentTable.addCell(cell);
        document.add(paymentTable);
    }

    // ═══════════════════════════════════════════════════════
    // MENTIONS LÉGALES
    // ═══════════════════════════════════════════════════════

    private void addLegalMentions(Document document) throws DocumentException {
        Font mentionFont = new Font(Font.HELVETICA, 7, Font.NORMAL, TEXT_SECONDARY);

        Paragraph mentions = new Paragraph();
        mentions.setAlignment(Element.ALIGN_LEFT);
        mentions.setSpacingBefore(5);
        mentions.add(new Chunk("Conditions de paiement : Payé par carte bancaire via Stripe. ", mentionFont));
        mentions.add(new Chunk(
                "En cas de retard de paiement, une pénalité de 3 fois le taux d'intérêt légal sera appliquée. ",
                mentionFont));
        mentions.add(new Chunk(
                "Conformément à la loi, une indemnité forfaitaire de 40€ pour frais de recouvrement est due en cas de retard.",
                mentionFont));

        document.add(mentions);
    }

    // ═══════════════════════════════════════════════════════
    // FOOTER PREMIUM
    // ═══════════════════════════════════════════════════════

    private void addPremiumFooter(PdfWriter writer) {
        PdfContentByte cb = writer.getDirectContent();

        // Ligne dorée fine
        cb.setColorStroke(GOLD_ACCENT);
        cb.setLineWidth(0.8f);
        cb.moveTo(45, 55);
        cb.lineTo(550, 55);
        cb.stroke();

        Font footerFont = new Font(Font.HELVETICA, 7, Font.NORMAL, TEXT_SECONDARY);
        Font footerBoldFont = new Font(Font.HELVETICA, 7, Font.BOLD, CHARCOAL);

        ColumnText.showTextAligned(cb, Element.ALIGN_CENTER,
                new Phrase(COMPANY_NAME + "  ·  " + COMPANY_ADDRESS + ", " + COMPANY_CITY, footerFont),
                297.5f, 42, 0);

        ColumnText.showTextAligned(cb, Element.ALIGN_CENTER,
                new Phrase(COMPANY_EMAIL + "  ·  " + COMPANY_WEB, footerFont),
                297.5f, 32, 0);

        ColumnText.showTextAligned(cb, Element.ALIGN_CENTER,
                new Phrase("Document généré le " + LocalDateTime.now().format(dateTimeFormatter)
                        + "  ·  Merci pour votre confiance", footerBoldFont),
                297.5f, 20, 0);
    }

    // ═══════════════════════════════════════════════════════
    // UTILITAIRES
    // ═══════════════════════════════════════════════════════

    private void addDualSeparator(Document document) throws DocumentException {
        // Ligne fine dorée
        PdfPTable lineTable = new PdfPTable(1);
        lineTable.setWidthPercentage(100);

        PdfPCell goldLine = new PdfPCell();
        goldLine.setBorder(Rectangle.NO_BORDER);
        goldLine.setBorderWidthBottom(1f);
        goldLine.setBorderColorBottom(GOLD_ACCENT);
        goldLine.setFixedHeight(1);
        lineTable.addCell(goldLine);

        // Ligne épaisse charcoal
        PdfPCell darkLine = new PdfPCell();
        darkLine.setBorder(Rectangle.NO_BORDER);
        darkLine.setBorderWidthBottom(2.5f);
        darkLine.setBorderColorBottom(CHARCOAL);
        darkLine.setFixedHeight(3);
        lineTable.addCell(darkLine);

        document.add(lineTable);
    }

    private String formatPrice(java.math.BigDecimal amount, String currency) {
        if (amount == null)
            return "0,00 " + currency;

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
