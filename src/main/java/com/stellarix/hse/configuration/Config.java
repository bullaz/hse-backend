package com.stellarix.hse.configuration;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import org.springframework.security.crypto.password.PasswordEncoder;

import com.stellarix.hse.entity.Hse;
import com.stellarix.hse.entity.HseInduction;
import com.stellarix.hse.entity.PpeItem;
import com.stellarix.hse.entity.PpeItemResult;
import com.stellarix.hse.entity.PpeRequirement;
import com.stellarix.hse.entity.PpeVerificationLog;
import com.stellarix.hse.entity.Site;
import com.stellarix.hse.repository.HseRepository;
import com.stellarix.hse.repository.HseInductionRepository;
import com.stellarix.hse.repository.PpeItemRepository;
import com.stellarix.hse.repository.PpeItemResultRepository;
import com.stellarix.hse.repository.PpeRequirementRepository;
import com.stellarix.hse.repository.PpeVerificationLogRepository;
import com.stellarix.hse.repository.SiteRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Configuration
@EnableScheduling
@RequiredArgsConstructor
@Slf4j
public class Config {

    private final HseRepository hseRepo;
    private final PasswordEncoder passwordEncoder;
    private final SiteRepository siteRepo;
    private final PpeItemRepository ppeItemRepo;
    private final PpeRequirementRepository requirementRepo;
    private final HseInductionRepository inductionRepo;
    private final PpeVerificationLogRepository logRepo;
    private final PpeItemResultRepository itemResultRepo;

    @Bean
    CommandLineRunner commandLineRunner() {
        return args -> {
            if (logRepo.count() > 0) {
                log.info("[Seed] Data already exists — skipping seed.");
                return;
            }
            log.info("[Seed] Seeding test data…");

            // ── Admin user ───────────────────────────────────────────────────────
            Hse admin = new Hse();
            admin.setNom("Mahosy");
            admin.setPrenom("Anderson");
            admin.setEmail("andersonmahosi@gmail.com");
            admin.setUsername("andersonmahosi");
            admin.setPassword(passwordEncoder.encode("motdepasse2002"));
            hseRepo.save(admin);

            // ── Sites ────────────────────────────────────────────────────────────
            Site alpha = siteRepo.save(new Site(null, "DC Alpha", -18.9168, 47.5361));
            Site beta  = siteRepo.save(new Site(null, "DC Beta",  -18.9000, 47.5500));
            Site gamma = siteRepo.save(new Site(null, "DC Gamma", -18.9300, 47.5200));

            // ── PPE Items ────────────────────────────────────────────────────────
            PpeItem helmet  = ppeItemRepo.save(new PpeItem(null, "HELMET",         "Hard Hat"));
            PpeItem glasses = ppeItemRepo.save(new PpeItem(null, "SAFETY_GLASSES", "Safety Glasses"));
            PpeItem vest    = ppeItemRepo.save(new PpeItem(null, "HIGH_VIS_VEST",  "Hi-Vis Vest"));
            PpeItem gloves  = ppeItemRepo.save(new PpeItem(null, "GLOVES",         "Gloves"));
            PpeItem shoes   = ppeItemRepo.save(new PpeItem(null, "SAFETY_SHOES",   "Safety Shoes"));

            // ── PPE Matrix ───────────────────────────────────────────────────────
            // DC Alpha: full kit for workers, reduced for visitors
            req(alpha, "WORK",  helmet, glasses, vest, gloves, shoes);
            req(alpha, "VISIT", helmet, vest, shoes);
            // DC Beta: no gloves for workers, helmet+vest only for visitors
            req(beta,  "WORK",  helmet, glasses, vest, shoes);
            req(beta,  "VISIT", helmet, vest);
            // DC Gamma: full kit for workers, minimal for visitors
            req(gamma, "WORK",  helmet, glasses, vest, gloves, shoes);
            req(gamma, "VISIT", helmet, shoes);

            // ── Inductions (15 workers — 2 pages at 10/page) ────────────────────
            induction("Jean",     "Dupont");
            induction("Marie",    "Martin");
            induction("Ahmed",    "Benali");
            induction("Sophie",   "Leclerc");
            induction("Thomas",   "Bernard");
            induction("Fatima",   "Osei");
            induction("Lucas",    "Petit");
            induction("Amara",    "Diallo");
            induction("Karim",    "Ndiaye");
            induction("Isabelle", "Moreau");
            induction("Mohamed",  "Traore");
            induction("Chloe",    "Fontaine");
            induction("David",    "Rakoto");
            induction("Nadia",    "Kone");
            induction("Pierre",   "Gautier");

            // ── Verification Logs ────────────────────────────────────────────────
            // Covers every scenario:
            //   • All 3 statuses: VALIDATED, REJECTED, PENDING
            //   • Both intents: WORK, VISIT
            //   • All 3 sites
            //   • Online and offline (with syncedAt)
            //   • Every individual PPE item missing in isolation
            //   • Multiple items missing simultaneously
            //   • REJECTED on both WORK and VISIT
            //   • Offline REJECTED
            //   • 60+ entries across 12 days → exercises pagination (6 pages at 10/page)

            // ── Day -35 — warm-up traffic (all validated) ────────────────────────
            validated("Jean",   "Dupont",   "WORK",  alpha, at(35, 7, 30), false,
                      items(helmet, glasses, vest, gloves, shoes), conf(0.97f, 0.94f, 0.96f, 0.92f, 0.95f));
            validated("Marie",  "Martin",   "WORK",  beta,  at(35, 8, 15), false,
                      items(helmet, glasses, vest, shoes),         conf(0.96f, 0.91f, 0.93f, 0.94f));
            validated("Ahmed",  "Benali",   "VISIT", alpha, at(35, 9,  0), false,
                      items(helmet, vest, shoes),                  conf(0.95f, 0.92f, 0.94f));
            validated("Sophie", "Leclerc",  "VISIT", gamma, at(35, 10, 0), false,
                      items(helmet, shoes),                        conf(0.97f, 0.95f));

            // ── Day -28 — first rejections ───────────────────────────────────────
            // Scenario: HELMET missing (WORK, Alpha)
            rejected("Thomas",  "Bernard",  "WORK",  alpha, at(28, 8,  0), false,
                     items(helmet, glasses, vest, gloves, shoes),
                     det(false, true,  true,  true,  true),
                     conf(0.05f, 0.93f, 0.95f, 0.91f, 0.96f));
            // Scenario: SAFETY_GLASSES missing (WORK, Beta)
            rejected("Fatima",  "Osei",     "WORK",  beta,  at(28, 9,  0), false,
                     items(helmet, glasses, vest, shoes),
                     det(true,  false, true,  true),
                     conf(0.96f, 0.07f, 0.94f, 0.93f));
            validated("Lucas",  "Petit",    "WORK",  gamma, at(28, 8, 30), false,
                      items(helmet, glasses, vest, gloves, shoes), conf(0.95f, 0.92f, 0.94f, 0.90f, 0.97f));
            validated("Amara",  "Diallo",   "VISIT", beta,  at(28, 11, 0), false,
                      items(helmet, vest),                         conf(0.96f, 0.93f));

            // ── Day -21 — vest and shoes missing scenarios ───────────────────────
            // Scenario: HIGH_VIS_VEST missing (WORK, Gamma)
            rejected("Karim",   "Ndiaye",   "WORK",  gamma, at(21, 7, 45), false,
                     items(helmet, glasses, vest, gloves, shoes),
                     det(true,  true,  false, true,  true),
                     conf(0.97f, 0.92f, 0.06f, 0.91f, 0.95f));
            // Scenario: SAFETY_SHOES missing (WORK, Alpha)
            rejected("Isabelle","Moreau",   "WORK",  alpha, at(21, 8, 30), false,
                     items(helmet, glasses, vest, gloves, shoes),
                     det(true,  true,  true,  true,  false),
                     conf(0.96f, 0.93f, 0.95f, 0.90f, 0.04f));
            validated("Jean",   "Dupont",   "WORK",  beta,  at(21, 9,  0), false,
                      items(helmet, glasses, vest, shoes),         conf(0.97f, 0.91f, 0.94f, 0.96f));
            validated("Marie",  "Martin",   "VISIT", alpha, at(21, 10, 0), false,
                      items(helmet, vest, shoes),                  conf(0.95f, 0.93f, 0.94f));
            validated("Mohamed","Traore",   "VISIT", gamma, at(21, 14, 0), false,
                      items(helmet, shoes),                        conf(0.97f, 0.95f));

            // ── Day -14 — gloves and multi-item missing scenarios ────────────────
            // Scenario: GLOVES missing (WORK, Alpha — gloves required here)
            rejected("Ahmed",   "Benali",   "WORK",  alpha, at(14, 8,  0), false,
                     items(helmet, glasses, vest, gloves, shoes),
                     det(true,  true,  true,  false, true),
                     conf(0.96f, 0.94f, 0.95f, 0.08f, 0.93f));
            // Scenario: GLASSES + GLOVES missing (WORK, Gamma — worst case)
            rejected("Sophie",  "Leclerc",  "WORK",  gamma, at(14, 9,  0), false,
                     items(helmet, glasses, vest, gloves, shoes),
                     det(true,  false, true,  false, true),
                     conf(0.95f, 0.09f, 0.93f, 0.06f, 0.94f));
            validated("Thomas", "Bernard",  "WORK",  beta,  at(14, 8, 45), false,
                      items(helmet, glasses, vest, shoes),         conf(0.96f, 0.92f, 0.94f, 0.95f));
            validated("Fatima", "Osei",     "WORK",  alpha, at(14, 10, 0), false,
                      items(helmet, glasses, vest, gloves, shoes), conf(0.97f, 0.93f, 0.96f, 0.91f, 0.95f));
            validated("Chloe",  "Fontaine", "VISIT", alpha, at(14, 11, 0), false,
                      items(helmet, vest, shoes),                  conf(0.96f, 0.94f, 0.95f));
            validated("David",  "Rakoto",   "VISIT", beta,  at(14, 13, 0), false,
                      items(helmet, vest),                         conf(0.97f, 0.93f));

            // ── Day -10 — offline scenarios ──────────────────────────────────────
            // Scenario: offline WORK validated
            validated("Lucas",  "Petit",    "WORK",  gamma, at(10, 7, 30), true,
                      items(helmet, glasses, vest, gloves, shoes), conf(0.95f, 0.91f, 0.94f, 0.90f, 0.97f));
            // Scenario: offline WORK rejected (GLOVES missing)
            rejected("Amara",   "Diallo",   "WORK",  gamma, at(10, 8,  0), true,
                     items(helmet, glasses, vest, gloves, shoes),
                     det(true,  true,  true,  false, true),
                     conf(0.96f, 0.92f, 0.95f, 0.07f, 0.94f));
            // Scenario: offline VISIT validated
            validated("Karim",  "Ndiaye",   "VISIT", alpha, at(10, 9,  0), true,
                      items(helmet, vest, shoes),                  conf(0.97f, 0.94f, 0.95f));
            validated("Jean",   "Dupont",   "WORK",  alpha, at(10, 8, 30), false,
                      items(helmet, glasses, vest, gloves, shoes), conf(0.98f, 0.94f, 0.96f, 0.92f, 0.97f));
            validated("Isabelle","Moreau",  "VISIT", gamma, at(10, 14, 0), false,
                      items(helmet, shoes),                        conf(0.96f, 0.95f));

            // ── Day -7 — visit rejections ────────────────────────────────────────
            // Scenario: HELMET missing (VISIT, Alpha)
            rejected("Nadia",   "Kone",     "VISIT", alpha, at(7, 10,  0), false,
                     items(helmet, vest, shoes),
                     det(false, true,  true),
                     conf(0.06f, 0.93f, 0.94f));
            // Scenario: HIGH_VIS_VEST missing (VISIT, Beta)
            rejected("Pierre",  "Gautier",  "VISIT", beta,  at(7, 11,  0), false,
                     items(helmet, vest),
                     det(true,  false),
                     conf(0.95f, 0.05f));
            // Scenario: SAFETY_SHOES missing (VISIT, Alpha)
            rejected("Mohamed", "Traore",   "VISIT", alpha, at(7, 12,  0), false,
                     items(helmet, vest, shoes),
                     det(true,  true,  false),
                     conf(0.97f, 0.92f, 0.06f));
            validated("Marie",  "Martin",   "WORK",  gamma, at(7, 8,   0), false,
                      items(helmet, glasses, vest, gloves, shoes), conf(0.96f, 0.93f, 0.95f, 0.91f, 0.97f));
            validated("Ahmed",  "Benali",   "WORK",  beta,  at(7, 9,   0), false,
                      items(helmet, glasses, vest, shoes),         conf(0.97f, 0.91f, 0.94f, 0.95f));
            validated("Chloe",  "Fontaine", "VISIT", gamma, at(7, 13,  0), false,
                      items(helmet, shoes),                        conf(0.96f, 0.94f));

            // ── Day -5 — high traffic day ────────────────────────────────────────
            validated("Thomas", "Bernard",  "WORK",  alpha, at(5, 7, 30), false,
                      items(helmet, glasses, vest, gloves, shoes), conf(0.95f, 0.91f, 0.94f, 0.90f, 0.96f));
            validated("Fatima", "Osei",     "WORK",  alpha, at(5, 8,  0), false,
                      items(helmet, glasses, vest, gloves, shoes), conf(0.97f, 0.93f, 0.96f, 0.92f, 0.95f));
            validated("Lucas",  "Petit",    "WORK",  beta,  at(5, 8, 15), false,
                      items(helmet, glasses, vest, shoes),         conf(0.96f, 0.90f, 0.93f, 0.94f));
            // Scenario: GLASSES + VEST missing (WORK, Alpha — severe)
            rejected("David",   "Rakoto",   "WORK",  alpha, at(5, 9,  0), false,
                     items(helmet, glasses, vest, gloves, shoes),
                     det(true,  false, false, true,  true),
                     conf(0.96f, 0.08f, 0.06f, 0.91f, 0.95f));
            validated("Amara",  "Diallo",   "VISIT", alpha, at(5, 10, 0), false,
                      items(helmet, vest, shoes),                  conf(0.97f, 0.94f, 0.95f));
            validated("Karim",  "Ndiaye",   "VISIT", beta,  at(5, 11, 0), false,
                      items(helmet, vest),                         conf(0.96f, 0.93f));
            validated("Nadia",  "Kone",     "VISIT", gamma, at(5, 14, 0), false,
                      items(helmet, shoes),                        conf(0.95f, 0.94f));

            // ── Day -3 — mixed bag ───────────────────────────────────────────────
            validated("Jean",   "Dupont",   "WORK",  gamma, at(3, 7, 45), false,
                      items(helmet, glasses, vest, gloves, shoes), conf(0.97f, 0.94f, 0.96f, 0.91f, 0.98f));
            validated("Sophie", "Leclerc",  "WORK",  beta,  at(3, 8,  0), false,
                      items(helmet, glasses, vest, shoes),         conf(0.95f, 0.91f, 0.94f, 0.93f));
            // Scenario: offline WORK validated (Gamma, different worker)
            validated("Pierre", "Gautier",  "WORK",  gamma, at(3, 8, 30), true,
                      items(helmet, glasses, vest, gloves, shoes), conf(0.96f, 0.92f, 0.95f, 0.89f, 0.97f));
            // Scenario: GLOVES missing (WORK, Gamma — offline rejected)
            rejected("Isabelle","Moreau",   "WORK",  gamma, at(3, 9,  0), true,
                     items(helmet, glasses, vest, gloves, shoes),
                     det(true,  true,  true,  false, true),
                     conf(0.96f, 0.91f, 0.94f, 0.05f, 0.95f));
            validated("Mohamed","Traore",   "VISIT", alpha, at(3, 10, 0), false,
                      items(helmet, vest, shoes),                  conf(0.97f, 0.93f, 0.94f));
            validated("Chloe",  "Fontaine", "VISIT", beta,  at(3, 11, 0), false,
                      items(helmet, vest),                         conf(0.96f, 0.94f));

            // ── Day -1 — pre-today ───────────────────────────────────────────────
            validated("Marie",  "Martin",   "WORK",  alpha, at(1, 7, 30), false,
                      items(helmet, glasses, vest, gloves, shoes), conf(0.96f, 0.93f, 0.95f, 0.92f, 0.97f));
            validated("Ahmed",  "Benali",   "WORK",  gamma, at(1, 8,  0), false,
                      items(helmet, glasses, vest, gloves, shoes), conf(0.97f, 0.94f, 0.96f, 0.91f, 0.95f));
            // Scenario: HELMET + GLASSES missing (WORK, Beta — most critical)
            rejected("Thomas",  "Bernard",  "WORK",  beta,  at(1, 9,  0), false,
                     items(helmet, glasses, vest, shoes),
                     det(false, false, true,  true),
                     conf(0.04f, 0.06f, 0.94f, 0.93f));
            validated("Fatima", "Osei",     "VISIT", gamma, at(1, 10, 0), false,
                      items(helmet, shoes),                        conf(0.97f, 0.95f));
            validated("Lucas",  "Petit",    "VISIT", alpha, at(1, 11, 0), false,
                      items(helmet, vest, shoes),                  conf(0.96f, 0.93f, 0.95f));
            // Scenario: PENDING (not yet processed)
            pending("David",    "Rakoto",   "VISIT", beta,  at(1, 15, 30));

            // ── Today — current shift ────────────────────────────────────────────
            validated("Jean",   "Dupont",   "WORK",  alpha, at(0, 7, 30), false,
                      items(helmet, glasses, vest, gloves, shoes), conf(0.98f, 0.95f, 0.97f, 0.93f, 0.96f));
            validated("Karim",  "Ndiaye",   "WORK",  beta,  at(0, 8,  0), false,
                      items(helmet, glasses, vest, shoes),         conf(0.97f, 0.92f, 0.95f, 0.94f));
            validated("Nadia",  "Kone",     "WORK",  gamma, at(0, 8, 15), false,
                      items(helmet, glasses, vest, gloves, shoes), conf(0.96f, 0.93f, 0.94f, 0.91f, 0.97f));
            // Scenario: SAFETY_GLASSES missing (WORK, Gamma — today's rejection)
            rejected("Pierre",  "Gautier",  "WORK",  gamma, at(0, 9,  0), false,
                     items(helmet, glasses, vest, gloves, shoes),
                     det(true,  false, true,  true,  true),
                     conf(0.97f, 0.06f, 0.95f, 0.92f, 0.96f));
            validated("Amara",  "Diallo",   "VISIT", alpha, at(0, 10, 0), false,
                      items(helmet, vest, shoes),                  conf(0.96f, 0.94f, 0.95f));
            // Scenario: PENDING (just submitted, inference running)
            pending("Sophie",   "Leclerc",  "WORK",  alpha, at(0, 10, 30));
            pending("Mohamed",  "Traore",   "VISIT", gamma, at(0, 11,  0));

            log.info("[Seed] Done — {} sites, {} PPE items, {} requirements, {} inductions, {} logs.",
                    siteRepo.count(), ppeItemRepo.count(), requirementRepo.count(),
                    inductionRepo.count(), logRepo.count());
        };
    }

    // ── Seed helpers ─────────────────────────────────────────────────────────────

    private void req(Site site, String intent, PpeItem... ppeItems) {
        for (PpeItem item : ppeItems) {
            PpeRequirement r = new PpeRequirement();
            r.setSite(site);
            r.setIntent(intent);
            r.setPpeItem(item);
            requirementRepo.save(r);
        }
    }

    private void induction(String firstName, String lastName) {
        HseInduction ind = new HseInduction();
        ind.setFirstName(firstName);
        ind.setLastName(lastName);
        inductionRepo.save(ind);
    }

    private LocalDateTime at(int daysAgo, int hour, int minute) {
        return LocalDateTime.now()
                .minusDays(daysAgo)
                .withHour(hour).withMinute(minute).withSecond(0).withNano(0);
    }

    private PpeItem[] items(PpeItem... ppeItems) { return ppeItems; }
    private float[]   conf(float... vals)         { return vals; }
    private boolean[] det(boolean... vals)        { return vals; }

    private void validated(String firstName, String lastName, String intent, Site site,
                            LocalDateTime capturedAt, boolean offline,
                            PpeItem[] ppeItems, float[] confidences) {
        PpeVerificationLog saved = logRepo.save(buildLog(firstName, lastName, intent, site, "VALIDATED", capturedAt, offline));
        for (int i = 0; i < ppeItems.length; i++) {
            saveResult(saved, ppeItems[i], true, confidences[i]);
        }
    }

    private void rejected(String firstName, String lastName, String intent, Site site,
                           LocalDateTime capturedAt, boolean offline,
                           PpeItem[] ppeItems, boolean[] detected, float[] confidences) {
        PpeVerificationLog saved = logRepo.save(buildLog(firstName, lastName, intent, site, "REJECTED", capturedAt, offline));
        for (int i = 0; i < ppeItems.length; i++) {
            saveResult(saved, ppeItems[i], detected[i], confidences[i]);
        }
    }

    private void pending(String firstName, String lastName, String intent, Site site, LocalDateTime capturedAt) {
        logRepo.save(buildLog(firstName, lastName, intent, site, "PENDING", capturedAt, false));
    }

    private PpeVerificationLog buildLog(String firstName, String lastName, String intent, Site site,
                                         String status, LocalDateTime capturedAt, boolean offline) {
        HseInduction induction = inductionRepo.findByFirstNameIgnoreCaseAndLastNameIgnoreCase(firstName, lastName)
                .orElseThrow(() -> new IllegalStateException("Induction not found for seed: " + firstName + " " + lastName));
        PpeVerificationLog entry = new PpeVerificationLog();
        entry.setLogId(UUID.randomUUID());
        entry.setInduction(induction);
        entry.setIntent(intent);
        entry.setSite(site);
        entry.setStatus(status);
        entry.setCapturedAt(capturedAt);
        entry.setOffline(offline);
        if (offline) entry.setSyncedAt(capturedAt.plusMinutes(18));
        return entry;
    }

    private void saveResult(PpeVerificationLog saved, PpeItem item,
                             boolean detected, float confidence) {
        PpeItemResult r = new PpeItemResult();
        r.setVerificationLog(saved);
        r.setPpeItem(item);
        r.setDetected(detected);
        r.setConfidence(confidence);
        itemResultRepo.save(r);
    }
}
