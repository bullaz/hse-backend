package com.stellarix.hse.service;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import javax.imageio.ImageIO;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.springframework.stereotype.Service;

import com.stellarix.hse.dto.VerificationLogResponse;
import com.stellarix.hse.entity.WorkPermit;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class PdfService {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final float MARGIN = 50f;
    private static final float PAGE_WIDTH = PDRectangle.A4.getWidth();
    private static final float PAGE_HEIGHT = PDRectangle.A4.getHeight();

    private final EmailService emailService;

    public byte[] generatePermitPdf(WorkPermit permit) throws Exception {
        byte[] qrBytes = emailService.generateQrPng(permit.getPermitId(), 200);

        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);

            PDType1Font bold   = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
            PDType1Font normal = new PDType1Font(Standard14Fonts.FontName.HELVETICA);

            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                float y = PAGE_HEIGHT - MARGIN;

                // Header band
                cs.setNonStrokingColor(0.137f, 0.133f, 0.941f); // #2322F0
                cs.addRect(0, y - 10, PAGE_WIDTH, 40);
                cs.fill();
                cs.setNonStrokingColor(1f, 1f, 1f);
                writeText(cs, bold, 16, MARGIN, y + 14, "Stellarix Safe — Permis de Travail");

                y -= 60;
                cs.setNonStrokingColor(0f, 0f, 0f);

                // Permit ID
                writeText(cs, bold, 22, MARGIN, y, permit.getPermitId());
                y -= 30;

                // QR code
                BufferedImage qrImage = ImageIO.read(new ByteArrayInputStream(qrBytes));
                PDImageXObject qrPdf = LosslessFactory.createFromImage(doc, qrImage);
                cs.drawImage(qrPdf, MARGIN, y - 180, 180, 180);

                // Details table (right of QR)
                float detailX = MARGIN + 200;
                float detailY = y - 20;
                String name = permit.getInduction().getFirstName() + " " + permit.getInduction().getLastName();
                String zoneLabel = permit.getSite().getZoneType() != null ? " (" + permit.getSite().getZoneType().getLabel() + ")" : "";

                detailY = labelValue(cs, bold, normal, detailX, detailY, "Titulaire :", name);
                detailY = labelValue(cs, bold, normal, detailX, detailY, "Site :", permit.getSite().getName() + zoneLabel);
                detailY = labelValue(cs, bold, normal, detailX, detailY, "Début :", permit.getStartDatetime().format(FMT));
                detailY = labelValue(cs, bold, normal, detailX, detailY, "Fin :", permit.getEndDatetime().format(FMT));
                detailY = labelValue(cs, bold, normal, detailX, detailY, "Statut :", permit.getStatus());
                if (permit.getDescription() != null && !permit.getDescription().isBlank()) {
                    labelValue(cs, bold, normal, detailX, detailY, "Objet :", permit.getDescription());
                }

                y -= 200;

                // Footer
                cs.setNonStrokingColor(0.5f, 0.5f, 0.5f);
                writeText(cs, normal, 9, MARGIN, MARGIN, "Généré le " + LocalDateTime.now().format(FMT) + " — Stellarix Safe");
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            doc.save(baos);
            return baos.toByteArray();
        }
    }

    public byte[] generateLogsExportPdf(List<VerificationLogResponse> logs,
                                         LocalDateTime from, LocalDateTime to,
                                         String siteName, String intent) throws Exception {
        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);

            PDType1Font bold   = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
            PDType1Font normal = new PDType1Font(Standard14Fonts.FontName.HELVETICA);

            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                float y = PAGE_HEIGHT - MARGIN;

                // Header
                cs.setNonStrokingColor(0.137f, 0.133f, 0.941f);
                cs.addRect(0, y - 10, PAGE_WIDTH, 40);
                cs.fill();
                cs.setNonStrokingColor(1f, 1f, 1f);
                writeText(cs, bold, 14, MARGIN, y + 14, "Stellarix Safe — Journal des Vérifications HSE");

                y -= 50;
                cs.setNonStrokingColor(0f, 0f, 0f);

                // Filter summary
                String period = from.format(FMT) + " → " + to.format(FMT);
                String filters = "Période : " + period
                        + (siteName != null ? "  |  Site : " + siteName : "")
                        + (intent != null ? "  |  Type : " + intent : "");
                writeText(cs, normal, 9, MARGIN, y, filters);
                y -= 20;

                // Column headers
                float[] cols = {MARGIN, 150, 240, 320, 390, 460, 490};
                String[] headers = {"Nom", "Prénom", "Site", "Type", "Date/Heure", "Accès", "Causes"};
                cs.setNonStrokingColor(0.9f, 0.9f, 0.9f);
                cs.addRect(MARGIN - 2, y - 12, PAGE_WIDTH - 2 * MARGIN + 4, 18);
                cs.fill();
                cs.setNonStrokingColor(0f, 0f, 0f);
                for (int i = 0; i < headers.length; i++) {
                    writeText(cs, bold, 8, cols[i], y, headers[i]);
                }
                y -= 18;

                // Rows
                boolean shade = false;
                for (VerificationLogResponse log : logs) {
                    if (y < MARGIN + 20) break; // avoid overflowing page
                    if (shade) {
                        cs.setNonStrokingColor(0.97f, 0.97f, 0.97f);
                        cs.addRect(MARGIN - 2, y - 10, PAGE_WIDTH - 2 * MARGIN + 4, 15);
                        cs.fill();
                        cs.setNonStrokingColor(0f, 0f, 0f);
                    }
                    writeText(cs, normal, 7, cols[0], y, log.getInduction().getLastName());
                    writeText(cs, normal, 7, cols[1], y, log.getInduction().getFirstName());
                    writeText(cs, normal, 7, cols[2], y, truncate(log.getSiteName(), 12));
                    writeText(cs, normal, 7, cols[3], y, log.getIntent());
                    writeText(cs, normal, 7, cols[4], y, log.getCapturedAt().format(FMT));
                    writeText(cs, normal, 7, cols[5], y, log.getStatus());
                    if (log.getRejectionCauses() != null && !log.getRejectionCauses().isEmpty()) {
                        writeText(cs, normal, 6, cols[6], y, truncate(String.join(", ", log.getRejectionCauses()), 20));
                    }
                    y -= 14;
                    shade = !shade;
                }

                // Footer
                cs.setNonStrokingColor(0.5f, 0.5f, 0.5f);
                writeText(cs, normal, 9, MARGIN, MARGIN,
                        "Total : " + logs.size() + " entrée(s)  |  Généré le " + LocalDateTime.now().format(FMT));
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            doc.save(baos);
            return baos.toByteArray();
        }
    }

    private void writeText(PDPageContentStream cs, PDType1Font font, float size,
                            float x, float y, String text) throws Exception {
        cs.beginText();
        cs.setFont(font, size);
        cs.newLineAtOffset(x, y);
        cs.showText(text != null ? text : "");
        cs.endText();
    }

    private float labelValue(PDPageContentStream cs, PDType1Font bold, PDType1Font normal,
                              float x, float y, String label, String value) throws Exception {
        writeText(cs, bold, 10, x, y, label);
        writeText(cs, normal, 10, x + 90, y, value);
        return y - 18;
    }

    private String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() > max ? s.substring(0, max - 1) + "…" : s;
    }
}
