package com.stellarix.hse.service;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;
import java.util.EnumMap;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import javax.imageio.ImageIO;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
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
import com.stellarix.hse.entity.Hse;
import com.stellarix.hse.entity.Travaux;
import com.stellarix.hse.entity.WorkPermit;
import com.stellarix.hse.repository.HseRepository;

import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final JavaMailSender mailSender;
    private final HseRepository hseRepository;

    @Value("${hse.mail.from}")
    private String fromAddress;

    @Value("${hse.mail.from.name:Département HSE Stellarix}")
    private String fromName;

    @Value("${hse.mail.phone:+261 00 00 000 00}")
    private String fromPhone;

    @Async
    public void sendWelcomeEmail(String toEmail, String nom, String prenom, String username, String tempPassword, String hseSenderEmail) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
            helper.setFrom(new InternetAddress(fromAddress, fromName));
            helper.setTo(toEmail);
            helper.setSubject("[Stellarix HSE] Votre accès à la plateforme HSE");
            helper.setText("""
                    <!DOCTYPE html><html lang="fr">
                    <head><meta charset="UTF-8"></head>
                    <body style="margin:0;padding:0;background:#f4f4f7;font-family:Arial,Helvetica,sans-serif;color:#1a1a2e">
                      <table width="100%%" cellpadding="0" cellspacing="0" style="background:#f4f4f7;padding:32px 0">
                        <tr><td align="center">
                          <table width="560" cellpadding="0" cellspacing="0" style="max-width:560px;width:100%%">
                            <tr><td style="background:#1a1a2e;border-radius:8px 8px 0 0;padding:28px 32px">
                              <p style="margin:0;font-size:11px;letter-spacing:2px;text-transform:uppercase;color:#8888bb">Stellarix Safe — Système HSE</p>
                              <h1 style="margin:6px 0 0;font-size:20px;font-weight:700;color:#fff">Bienvenue sur la plateforme</h1>
                            </td></tr>
                            <tr><td style="background:#fff;padding:32px;border-left:1px solid #e0e0e0;border-right:1px solid #e0e0e0">
                              <p style="margin:0 0 16px;font-size:15px">Bonjour <strong>%s %s</strong>,</p>
                              <p style="margin:0 0 24px;font-size:14px;color:#555;line-height:1.6">
                                Un compte HSE a été créé pour vous. Voici vos identifiants de connexion :
                              </p>
                              <table width="100%%" cellpadding="0" cellspacing="0" style="border-collapse:collapse;margin-bottom:24px;font-size:14px">
                                <tr><td style="padding:10px 12px;background:#fafafa;font-weight:600;width:140px">Identifiant</td>
                                    <td style="padding:10px 12px;font-family:monospace">%s</td></tr>
                                <tr><td style="padding:10px 12px;background:#fafafa;font-weight:600">Mot de passe</td>
                                    <td style="padding:10px 12px;font-family:monospace;color:#2322F0;font-weight:700">%s</td></tr>
                              </table>
                              <p style="margin:0 0 8px;font-size:13px;color:#e53935;font-weight:600">
                                Vous devrez changer ce mot de passe lors de votre première connexion.
                              </p>
                              <p style="margin:0;font-size:13px;color:#555">
                                Nous vous recommandons également d'activer l'authentification à deux facteurs (TOTP) après votre première connexion.
                              </p>
                            </td></tr>
                            <tr><td style="background:#f0f0f0;border:1px solid #e0e0e0;border-top:none;border-radius:0 0 8px 8px;padding:16px 32px">
                              <p style="margin:0;font-size:12px;color:#555">%s &nbsp;|&nbsp; %s &nbsp;|&nbsp; %s</p>
                              %s
                            </td></tr>
                          </table>
                        </td></tr>
                      </table>
                    </body></html>
                    """.formatted(prenom, nom, username, tempPassword, fromName, fromAddress, fromPhone, noReplyFooterHtml(hseSenderEmail)), true);
            mailSender.send(message);
            log.info("Welcome email sent to {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send welcome email to {}: {}", toEmail, e.getMessage());
        }
    }

    @Async
    public void sendPermitEmailToAll(WorkPermit permit, String hseSenderEmail) {
        if (permit.getIntervenants() == null || permit.getIntervenants().isEmpty()) {
            log.warn("No intervenants on permit {} — skipping email send", permit.getPermitId());
            return;
        }
        permit.getIntervenants().forEach(induction -> sendPermitEmailTo(permit, induction, hseSenderEmail));
    }

    @Async
    public void sendPermitEmail(WorkPermit permit, String hseSenderEmail) {
        if (permit.getInduction() != null) {
            sendPermitEmailTo(permit, permit.getInduction(), hseSenderEmail);
        } else {
            sendPermitEmailToAll(permit, hseSenderEmail);
        }
    }

    private void sendPermitEmailTo(WorkPermit permit, com.stellarix.hse.entity.HseInduction induction, String hseSenderEmail) {
        String recipientEmail = induction.getEmail();
        if (recipientEmail == null || recipientEmail.isBlank()) {
            log.info("No email on file for induction {} — skipping permit email for {}",
                    induction.getInductionId(), permit.getPermitId());
            return;
        }
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(new InternetAddress(fromAddress, fromName));
            helper.setTo(recipientEmail);
            helper.setSubject("[Stellarix Safe] Permis de travail — " + permit.getPermitId());
            helper.setText(buildHtml(permit, induction.getFirstName() + " " + induction.getLastName(), hseSenderEmail), true);

            if (permit.getPermitFileData() != null) {
                String attachName = permit.getPermitFileName() != null ? permit.getPermitFileName() : "permis.pdf";
                String attachType = permit.getPermitFileContentType() != null
                        ? permit.getPermitFileContentType() : "application/octet-stream";
                helper.addAttachment(attachName, new ByteArrayResource(permit.getPermitFileData()), attachType);
            }

            mailSender.send(message);
            log.info("Permit email sent to {} for permit {}", recipientEmail, permit.getPermitId());
        } catch (Exception e) {
            log.error("Failed to send permit email to {} for {}: {}", recipientEmail, permit.getPermitId(), e.getMessage());
        }
    }

    @Async
    public CompletableFuture<Boolean> sendTravauxPermitsEmail(Travaux travaux, java.util.List<WorkPermit> permits, String closureUrl, String hseSenderEmail) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(new InternetAddress(fromAddress, fromName));
            helper.setTo(travaux.getSuperviseurEmail());
            helper.setSubject("[Stellarix HSE] Permis de travail — " + travaux.getTicketNo()
                    + " — " + travaux.getSite().getName());
            helper.setText(buildTravauxHtml(travaux, permits, closureUrl, hseSenderEmail), true);

            for (WorkPermit p : permits) {
                if (p.getPermitFileData() != null) {
                    String name = p.getPermitFileName() != null ? p.getPermitFileName() : p.getPermitId() + ".pdf";
                    String type = p.getPermitFileContentType() != null ? p.getPermitFileContentType() : "application/octet-stream";
                    helper.addAttachment(name, new ByteArrayResource(p.getPermitFileData()), type);
                }
            }

            mailSender.send(message);
            log.info("Travaux permits email sent to {} for ticket {}", travaux.getSuperviseurEmail(), travaux.getTicketNo());
            return CompletableFuture.completedFuture(true);
        } catch (Exception e) {
            log.error("Failed to send travaux permits email for {}: {}", travaux.getTravauxId(), e.getMessage());
            return CompletableFuture.completedFuture(false);
        }
    }

    @Async
    public void sendClosureRequestNotification(Travaux travaux) {
        try {
            List<String> recipients = hseRepository.findByIsAdminFalseAndIsActiveTrue().stream()
                    .map(Hse::getEmail)
                    .toList();
            if (recipients.isEmpty()) {
                log.warn("No active HSE accounts found — closure notification for {} falling back to {}",
                        travaux.getTravauxId(), fromAddress);
                recipients = List.of(fromAddress);
            }

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
            helper.setFrom(new InternetAddress(fromAddress, fromName));
            helper.setTo(recipients.toArray(String[]::new));
            helper.setSubject("[Stellarix HSE] Clôture demandée — " + travaux.getTicketNo());
            helper.setText("""
                    <p>Le superviseur <strong>%s</strong> a demandé la clôture du Travaux.</p>
                    <p>Ticket : <strong>%s</strong> — Site : <strong>%s</strong></p>
                    <p>Veuillez valider la clôture sur la plateforme HSE.</p>
                    """.formatted(
                    travaux.getSuperviseurNom(),
                    travaux.getTicketNo(),
                    travaux.getSite().getName()), true);
            mailSender.send(message);
        } catch (Exception e) {
            log.error("Failed to send closure notification for {}: {}", travaux.getTravauxId(), e.getMessage());
        }
    }

    /** HSE rejected the supervisor's closure submission — distinct from the original
     *  permits email so the supervisor understands this is a "please resubmit" notice,
     *  not a duplicate of what they already received. */
    @Async
    public void sendClosureRejectedEmail(Travaux travaux, String reason, String closureUrl, String hseSenderEmail) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
            helper.setFrom(new InternetAddress(fromAddress, fromName));
            helper.setTo(travaux.getSuperviseurEmail());
            helper.setSubject("[Stellarix HSE] Clôture à revoir — " + travaux.getTicketNo()
                    + " — " + travaux.getSite().getName());

            String reasonBlock = (reason != null && !reason.isBlank())
                    ? """
                      <table width="100%%" cellpadding="0" cellspacing="0" style="margin-bottom:24px">
                        <tr>
                          <td style="background:#fff8f0;border:2px solid #e5a83f;border-radius:8px;padding:16px 20px">
                            <p style="margin:0 0 6px;font-size:10px;letter-spacing:2px;text-transform:uppercase;color:#a16a1f;font-weight:600">Motif</p>
                            <p style="margin:0;font-size:14px;color:#333">%s</p>
                          </td>
                        </tr>
                      </table>
                      """.formatted(reason.trim())
                    : "";

            helper.setText("""
                    <!DOCTYPE html><html lang="fr">
                    <head><meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1.0"></head>
                    <body style="margin:0;padding:0;background:#f4f4f7;font-family:Arial,Helvetica,sans-serif;color:#1a1a2e">
                      <table width="100%%" cellpadding="0" cellspacing="0" style="background:#f4f4f7;padding:32px 0">
                        <tr><td align="center">
                          <table width="620" cellpadding="0" cellspacing="0" style="max-width:620px;width:100%%">
                            <tr>
                              <td style="background:#1a1a2e;border-radius:8px 8px 0 0;padding:28px 32px">
                                <p style="margin:0;font-size:11px;letter-spacing:2px;text-transform:uppercase;color:#8888bb">Stellarix Safe — Système HSE</p>
                                <h1 style="margin:6px 0 0;font-size:22px;font-weight:700;color:#fff">Clôture à revoir</h1>
                              </td>
                            </tr>
                            <tr>
                              <td style="background:#fff;padding:32px;border-left:1px solid #e0e0e0;border-right:1px solid #e0e0e0">
                                <p style="margin:0 0 20px;font-size:15px">Bonjour <strong>%s</strong>,</p>
                                <p style="margin:0 0 24px;font-size:14px;color:#555;line-height:1.6">
                                  Votre demande de clôture pour le ticket <strong>%s</strong> (%s) a été examinée par l'équipe HSE
                                  et nécessite une révision avant validation.
                                </p>
                                %s
                                <p style="margin:0 0 16px;font-size:14px;color:#555">
                                  Merci de soumettre à nouveau le formulaire de clôture via le bouton ci-dessous :
                                </p>
                                <table cellpadding="0" cellspacing="0" style="margin-bottom:8px">
                                  <tr>
                                    <td style="background:#2322F0;border-radius:6px;padding:14px 28px">
                                      <a href="%s" style="color:#fff;text-decoration:none;font-size:15px;font-weight:600">SOUMETTRE À NOUVEAU</a>
                                    </td>
                                  </tr>
                                </table>
                              </td>
                            </tr>
                            <tr>
                              <td style="background:#f0f0f0;border:1px solid #e0e0e0;border-top:none;border-radius:0 0 8px 8px;padding:20px 32px">
                                <p style="margin:0;font-size:12px;color:#555">%s &nbsp;|&nbsp; %s &nbsp;|&nbsp; %s</p>
                                %s
                              </td>
                            </tr>
                          </table>
                        </td></tr>
                      </table>
                    </body></html>
                    """.formatted(
                    travaux.getSuperviseurNom(),
                    travaux.getTicketNo(), travaux.getSite().getName(),
                    reasonBlock,
                    closureUrl,
                    fromName, fromAddress, fromPhone,
                    noReplyFooterHtml(hseSenderEmail)), true);

            mailSender.send(message);
            log.info("Closure rejection email sent to {} for ticket {}", travaux.getSuperviseurEmail(), travaux.getTicketNo());
        } catch (Exception e) {
            log.error("Failed to send closure rejection email for {}: {}", travaux.getTravauxId(), e.getMessage());
        }
    }

    private String buildTravauxHtml(Travaux travaux, java.util.List<WorkPermit> permits, String closureUrl, String hseSenderEmail) {
        String permitRows = permits.stream().map(p -> {
            String type = p.getPermitType() != null ? p.getPermitType().getLabel() : "—";
            String desc = p.getDescription() != null ? p.getDescription() : "—";
            boolean hasFile = p.getPermitFileData() != null;
            return "<tr><td style=\"padding:8px 12px;border-bottom:1px solid #e8e8e8\">%s</td><td style=\"padding:8px 12px;border-bottom:1px solid #e8e8e8\">%s</td><td style=\"padding:8px 12px;border-bottom:1px solid #e8e8e8\">%s</td></tr>"
                    .formatted(p.getPermitId(), type, hasFile ? "Joint en pièce jointe" : desc);
        }).reduce("", String::concat);

        return """
                <!DOCTYPE html><html lang="fr">
                <head><meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1.0"></head>
                <body style="margin:0;padding:0;background:#f4f4f7;font-family:Arial,Helvetica,sans-serif;color:#1a1a2e">
                  <table width="100%%" cellpadding="0" cellspacing="0" style="background:#f4f4f7;padding:32px 0">
                    <tr><td align="center">
                      <table width="620" cellpadding="0" cellspacing="0" style="max-width:620px;width:100%%">
                        <tr>
                          <td style="background:#1a1a2e;border-radius:8px 8px 0 0;padding:28px 32px">
                            <p style="margin:0;font-size:11px;letter-spacing:2px;text-transform:uppercase;color:#8888bb">Stellarix Safe — Système HSE</p>
                            <h1 style="margin:6px 0 0;font-size:22px;font-weight:700;color:#fff">Permis de Travail</h1>
                          </td>
                        </tr>
                        <tr>
                          <td style="background:#fff;padding:32px;border-left:1px solid #e0e0e0;border-right:1px solid #e0e0e0">
                            <p style="margin:0 0 20px;font-size:15px">Bonjour <strong>%s</strong>,</p>
                            <p style="margin:0 0 24px;font-size:14px;color:#555;line-height:1.6">
                              Les permis de travail pour votre intervention ont été validés par l'équipe HSE Stellarix.
                            </p>
                            <table width="100%%" cellpadding="0" cellspacing="0" style="border-collapse:collapse;margin-bottom:24px;font-size:13px">
                              <tr style="background:#fafafa">
                                <td style="padding:10px 12px;font-weight:600">Ticket</td>
                                <td style="padding:10px 12px">%s</td>
                              </tr>
                              <tr><td style="padding:10px 12px;font-weight:600;background:#fafafa">Site</td><td style="padding:10px 12px">%s</td></tr>
                              <tr><td style="padding:10px 12px;font-weight:600;background:#fafafa">Objet</td><td style="padding:10px 12px">%s</td></tr>
                              <tr><td style="padding:10px 12px;font-weight:600;background:#fafafa">Début</td><td style="padding:10px 12px">%s</td></tr>
                              <tr><td style="padding:10px 12px;font-weight:600;background:#fafafa">Fin</td><td style="padding:10px 12px">%s</td></tr>
                            </table>
                            <p style="margin:0 0 12px;font-size:14px;font-weight:600">Permis attachés (%d) :</p>
                            <table width="100%%" cellpadding="0" cellspacing="0" style="border-collapse:collapse;font-size:13px;margin-bottom:28px">
                              <tr style="background:#f0f0fa">
                                <th style="padding:8px 12px;text-align:left">ID</th>
                                <th style="padding:8px 12px;text-align:left">Type</th>
                                <th style="padding:8px 12px;text-align:left">Fichier</th>
                              </tr>
                              %s
                            </table>
                            <p style="margin:0 0 16px;font-size:14px;color:#555">
                              À la fin des travaux, cliquez sur le bouton ci-dessous pour clôturer les travaux :
                            </p>
                            <table cellpadding="0" cellspacing="0" style="margin-bottom:8px">
                              <tr>
                                <td style="background:#2322F0;border-radius:6px;padding:14px 28px">
                                  <a href="%s" style="color:#fff;text-decoration:none;font-size:15px;font-weight:600">CLÔTURER LES TRAVAUX</a>
                                </td>
                              </tr>
                            </table>
                          </td>
                        </tr>
                        <tr>
                          <td style="background:#f0f0f0;border:1px solid #e0e0e0;border-top:none;border-radius:0 0 8px 8px;padding:20px 32px">
                            <p style="margin:0;font-size:12px;color:#555">%s &nbsp;|&nbsp; %s &nbsp;|&nbsp; %s</p>
                            %s
                          </td>
                        </tr>
                      </table>
                    </td></tr>
                  </table>
                </body></html>
                """.formatted(
                travaux.getSuperviseurNom(),
                travaux.getTicketNo(),
                travaux.getSite().getName(),
                travaux.getObjetVisite(),
                travaux.getDateDebut().format(FMT),
                travaux.getDateFin().format(FMT),
                permits.size(),
                permitRows,
                closureUrl,
                fromName, fromAddress, fromPhone,
                noReplyFooterHtml(hseSenderEmail));
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

    private String buildHtml(WorkPermit permit, String fullName, String hseSenderEmail) {
        String siteName = permit.getSite().getName();
        String start = permit.getStartDatetime().format(FMT);
        String end = permit.getEndDatetime().format(FMT);
        String desc = permit.getDescription() != null ? permit.getDescription() : "—";
        String permitTypeLabel = permit.getPermitType() != null ? permit.getPermitType().getLabel() : "—";
        boolean hasAttachment = permit.getPermitFileData() != null;

        String attachmentNote = hasAttachment
                ? "<p style=\"margin:0 0 16px\">Le formulaire officiel de permis est joint à cet e-mail en pièce jointe.</p>"
                : "";

        return """
                <!DOCTYPE html>
                <html lang="fr">
                <head><meta charset="UTF-8"><meta name="viewport" content="width=device-width, initial-scale=1.0"></head>
                <body style="margin:0;padding:0;background-color:#f4f4f7;font-family:Arial,Helvetica,sans-serif;color:#1a1a2e">
                  <table width="100%%" cellpadding="0" cellspacing="0" style="background:#f4f4f7;padding:32px 0">
                    <tr><td align="center">
                      <table width="620" cellpadding="0" cellspacing="0" style="max-width:620px;width:100%%">

                        <!-- Header -->
                        <tr>
                          <td style="background:#1a1a2e;border-radius:8px 8px 0 0;padding:28px 32px">
                            <table width="100%%" cellpadding="0" cellspacing="0">
                              <tr>
                                <td>
                                  <p style="margin:0;font-size:11px;letter-spacing:2px;text-transform:uppercase;color:#8888bb">Stellarix Safe — Système HSE</p>
                                  <h1 style="margin:6px 0 0;font-size:22px;font-weight:700;color:#ffffff">Permis de Travail</h1>
                                </td>
                              </tr>
                            </table>
                          </td>
                        </tr>

                        <!-- Body -->
                        <tr>
                          <td style="background:#ffffff;padding:32px;border-left:1px solid #e0e0e0;border-right:1px solid #e0e0e0">

                            <p style="margin:0 0 20px;font-size:15px;color:#333">Bonjour <strong>%s</strong>,</p>
                            <p style="margin:0 0 24px;font-size:14px;color:#555;line-height:1.6">
                              Un permis de travail a été émis en votre nom. Veuillez conserver cet identifiant
                              et le présenter à l'entrée du site lors de chaque accès.
                            </p>

                            <!-- Permit ID Box -->
                            <table width="100%%" cellpadding="0" cellspacing="0" style="margin-bottom:24px">
                              <tr>
                                <td style="background:#f0f0fa;border:2px solid #2322F0;border-radius:8px;padding:20px;text-align:center">
                                  <p style="margin:0 0 8px;font-size:10px;letter-spacing:2px;text-transform:uppercase;color:#666;font-weight:600">Identifiant du Permis</p>
                                  <p style="margin:0;font-size:22px;font-weight:700;font-family:Courier New,Courier,monospace;color:#2322F0;letter-spacing:3px">%s</p>
                                </td>
                              </tr>
                            </table>

                            <!-- Details table -->
                            <table width="100%%" cellpadding="0" cellspacing="0" style="border-collapse:collapse;margin-bottom:24px;font-size:13px">
                              <tr style="border-bottom:1px solid #e8e8e8">
                                <td style="padding:10px 12px;font-weight:600;color:#444;width:140px;background:#fafafa">Ticket</td>
                                <td style="padding:10px 12px;color:#222">%s</td>
                              </tr>
                              <tr style="border-bottom:1px solid #e8e8e8">
                                <td style="padding:10px 12px;font-weight:600;color:#444;background:#fafafa">Type de permis</td>
                                <td style="padding:10px 12px;color:#222">%s</td>
                              </tr>
                              <tr style="border-bottom:1px solid #e8e8e8">
                                <td style="padding:10px 12px;font-weight:600;color:#444;background:#fafafa">Site</td>
                                <td style="padding:10px 12px;color:#222">%s</td>
                              </tr>
                              <tr style="border-bottom:1px solid #e8e8e8">
                                <td style="padding:10px 12px;font-weight:600;color:#444;background:#fafafa">Début</td>
                                <td style="padding:10px 12px;color:#222">%s</td>
                              </tr>
                              <tr style="border-bottom:1px solid #e8e8e8">
                                <td style="padding:10px 12px;font-weight:600;color:#444;background:#fafafa">Fin</td>
                                <td style="padding:10px 12px;color:#222">%s</td>
                              </tr>
                              <tr>
                                <td style="padding:10px 12px;font-weight:600;color:#444;background:#fafafa">Description</td>
                                <td style="padding:10px 12px;color:#222">%s</td>
                              </tr>
                            </table>

                            %s

                          </td>
                        </tr>

                        <!-- Footer -->
                        <tr>
                          <td style="background:#f0f0f0;border:1px solid #e0e0e0;border-top:none;border-radius:0 0 8px 8px;padding:20px 32px">
                            <p style="margin:0 0 6px;font-size:12px;color:#555">Ce document est émis par le département HSE de Stellarix.</p>
                            <p style="margin:0 0 6px;font-size:12px;color:#555">Pour toute question, contactez-nous :</p>
                            <p style="margin:0 0 12px;font-size:12px;color:#333;font-weight:600">%s &nbsp;|&nbsp; %s &nbsp;|&nbsp; %s</p>
                            %s
                            <p style="margin:0;font-size:11px;color:#999">© Stellarix — Tous droits réservés. Ce message est confidentiel et destiné uniquement à son destinataire.</p>
                          </td>
                        </tr>

                      </table>
                    </td></tr>
                  </table>
                </body>
                </html>
                """.formatted(
                fullName, permit.getPermitId(),
                permit.getTravaux().getTicketNo(), permitTypeLabel, siteName, start, end, desc,
                attachmentNote,
                fromName, fromAddress, fromPhone,
                noReplyFooterHtml(hseSenderEmail));
    }

    /**
     * Every application email is sent from a shared/no-reply mailbox — this points the
     * recipient at whichever HSE staff member actually triggered the send instead. Falls
     * back to the generic HSE address when no authenticated sender is available (e.g. an
     * @Async method called from a background/public context).
     */
    private String noReplyFooterHtml(String hseSenderEmail) {
        String contact = (hseSenderEmail != null && !hseSenderEmail.isBlank()) ? hseSenderEmail : fromAddress;
        return ("<p style=\"margin:0 0 12px;font-size:11px;color:#999\">Cette adresse sert uniquement à l'envoi automatique de messages — merci de ne pas y répondre. "
                + "Pour toute question concernant cet e-mail, veuillez contacter directement : <strong>%s</strong>.</p>").formatted(contact);
    }
}
