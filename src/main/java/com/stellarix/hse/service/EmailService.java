package com.stellarix.hse.service;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;
import java.util.EnumMap;

import javax.imageio.ImageIO;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.stellarix.hse.entity.WorkPermit;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final JavaMailSender mailSender;

    @Value("${hse.mail.from}")
    private String fromAddress;

    @Async
    public void sendPermitEmail(WorkPermit permit) {
        String recipientEmail = permit.getInduction().getEmail();
        if (recipientEmail == null || recipientEmail.isBlank()) {
            log.info("No email on file for induction {} — skipping permit email for {}",
                    permit.getInduction().getInductionId(), permit.getPermitId());
            return;
        }
        try {
            byte[] qrBytes = generateQrPng(permit.getPermitId(), 250);
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromAddress);
            helper.setTo(recipientEmail);
            helper.setSubject("Stellarix Safe — Permis de travail " + permit.getPermitId());
            helper.setText(buildHtml(permit), true);
            helper.addInline("qrcode", new org.springframework.core.io.ByteArrayResource(qrBytes), "image/png");
            mailSender.send(message);
            log.info("Permit email sent to {} for permit {}", recipientEmail, permit.getPermitId());
        } catch (Exception e) {
            log.error("Failed to send permit email for {}: {}", permit.getPermitId(), e.getMessage());
        }
    }

    public byte[] generateQrPng(String content, int size) throws Exception {
        EnumMap<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
        hints.put(EncodeHintType.MARGIN, 1);
        QRCodeWriter writer = new QRCodeWriter();
        BitMatrix matrix = writer.encode(content, BarcodeFormat.QR_CODE, size, size, hints);
        BufferedImage image = MatrixToImageWriter.toBufferedImage(matrix);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "PNG", baos);
        return baos.toByteArray();
    }

    private String buildHtml(WorkPermit permit) {
        String name = permit.getInduction().getFirstName() + " " + permit.getInduction().getLastName();
        String siteName = permit.getSite().getName();
        String start = permit.getStartDatetime().format(FMT);
        String end = permit.getEndDatetime().format(FMT);
        String desc = permit.getDescription() != null ? permit.getDescription() : "—";

        return """
                <html><body style="font-family:Arial,sans-serif;color:#1a1a2e;max-width:600px;margin:auto">
                  <div style="background:#2322F0;padding:20px;border-radius:8px 8px 0 0">
                    <h2 style="color:white;margin:0">Stellarix Safe — Permis de Travail</h2>
                  </div>
                  <div style="border:1px solid #e0e0e0;padding:24px;border-top:none;border-radius:0 0 8px 8px">
                    <p>Bonjour <strong>%s</strong>,</p>
                    <p>Votre permis de travail a été émis. Veuillez présenter le QR code ci-dessous à l'entrée du site.</p>
                    <table style="border-collapse:collapse;width:100%%;margin:16px 0">
                      <tr><td style="padding:8px;font-weight:bold;width:140px">ID Permis</td>
                          <td style="padding:8px;font-family:monospace;font-size:16px;color:#2322F0">%s</td></tr>
                      <tr style="background:#f5f5f5"><td style="padding:8px;font-weight:bold">Site</td>
                          <td style="padding:8px">%s</td></tr>
                      <tr><td style="padding:8px;font-weight:bold">Début</td>
                          <td style="padding:8px">%s</td></tr>
                      <tr style="background:#f5f5f5"><td style="padding:8px;font-weight:bold">Fin</td>
                          <td style="padding:8px">%s</td></tr>
                      <tr><td style="padding:8px;font-weight:bold">Description</td>
                          <td style="padding:8px">%s</td></tr>
                    </table>
                    <div style="text-align:center;margin:24px 0">
                      <img src="cid:qrcode" alt="QR Code" style="width:200px;height:200px"/>
                    </div>
                    <p style="font-size:12px;color:#888">Stellarix Safe — Système de conformité HSE</p>
                  </div>
                </body></html>
                """.formatted(name, permit.getPermitId(), siteName, start, end, desc);
    }
}
