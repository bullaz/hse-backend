package com.stellarix.hse.configuration;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.stellarix.hse.entity.Habilitation;
import com.stellarix.hse.entity.Hse;
import com.stellarix.hse.entity.HseInduction;
import com.stellarix.hse.entity.PpeItem;
import com.stellarix.hse.entity.PpeItemResult;
import com.stellarix.hse.entity.PpeRequirement;
import com.stellarix.hse.entity.PpeVerificationLog;
import com.stellarix.hse.entity.Site;
import com.stellarix.hse.entity.ZoneType;
import com.stellarix.hse.repository.HabilitationRepository;
import com.stellarix.hse.repository.HseInductionRepository;
import com.stellarix.hse.repository.HseRepository;
import com.stellarix.hse.repository.PpeItemRepository;
import com.stellarix.hse.repository.PpeItemResultRepository;
import com.stellarix.hse.repository.PpeRequirementRepository;
import com.stellarix.hse.repository.PpeVerificationLogRepository;
import com.stellarix.hse.repository.SiteRepository;
import com.stellarix.hse.repository.ZoneTypeRepository;

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
    private final ZoneTypeRepository zoneTypeRepo;
    private final HabilitationRepository habilitationRepo;

    @Bean
    CommandLineRunner commandLineRunner() {
        return args -> {
            log.info("[Seed] Seeding test data…");

            // ── Admin user ───────────────────────────────────────────────────────
            Hse admin = new Hse();
            admin.setNom("Mahosy");
            admin.setPrenom("Anderson");
            admin.setEmail("andersonmahosi@gmail.com");
            admin.setUsername("andersonmahosi");
            admin.setPassword(passwordEncoder.encode("motdepasse2002"));
            hseRepo.save(admin);

            // ── Zone Types ───────────────────────────────────────────────────────
            ZoneType bureau    = zoneType("Bureau");
            ZoneType zoneIT    = zoneType("Zone IT");
            ZoneType salleServ = zoneType("Salle serveurs");
            ZoneType hTension  = zoneType("Haute tension");

            // ── Habilitations ────────────────────────────────────────────────────
            Habilitation h0 = hab("H0", "Hors tension",         "Non-électricien habilité à travailler hors tension");
            Habilitation b1 = hab("B1", "Basse tension travaux","Électricien habilité pour travaux basse tension");
            Habilitation br = hab("BR", "Rechargement",          "Habilité à effectuer des rechargements et dépannages");
            Habilitation bc = hab("BC", "Consignation",          "Chargé de consignation basse tension");

            // ── PPE Items ────────────────────────────────────────────────────────
            PpeItem helmet  = ppeItemRepo.save(new PpeItem(null, "HELMET",         "Hard Hat"));
            PpeItem glasses = ppeItemRepo.save(new PpeItem(null, "SAFETY_GLASSES", "Safety Glasses"));
            PpeItem vest    = ppeItemRepo.save(new PpeItem(null, "HIGH_VIS_VEST",  "Hi-Vis Vest"));
            PpeItem gloves  = ppeItemRepo.save(new PpeItem(null, "GLOVES",         "Gloves"));
            PpeItem shoes   = ppeItemRepo.save(new PpeItem(null, "SAFETY_SHOES",   "Safety Shoes"));

            // ── Sites ────────────────────────────────────────────────────────────
            // DC Alpha — server room requiring H0 + B1 habilitations
            Site alpha = site("DC Alpha", -18.9168, 47.5361, salleServ, List.of(h0, b1));
            // DC Beta — IT zone requiring H0 only
            Site beta  = site("DC Beta",  -18.9000, 47.5500, zoneIT,    List.of(h0));
            // DC Gamma — office zone, no habilitation required
            Site gamma = site("DC Gamma", -18.9300, 47.5200, bureau,    List.of());
            // DC Delta — high-voltage zone requiring H0 + B1 + BR + BC
            Site delta = site("DC Delta", -18.9500, 47.5100, hTension,  List.of(h0, b1, br, bc));

            // ── PPE Matrix ───────────────────────────────────────────────────────
            req(alpha, "WORK",  helmet, glasses, vest, gloves, shoes);
            req(alpha, "VISIT", helmet, vest, shoes);
            req(beta,  "WORK",  helmet, glasses, vest, shoes);
            req(beta,  "VISIT", helmet, vest);
            req(gamma, "WORK",  helmet, glasses, vest, gloves, shoes);
            req(gamma, "VISIT", helmet, shoes);
            req(delta, "WORK",  helmet, glasses, vest, gloves, shoes);
            req(delta, "VISIT", helmet, vest, shoes);

            // ── Inductions ───────────────────────────────────────────────────────
            // Full habilitations (H0 + B1 + BR + BC) — can work anywhere
            HseInduction jean    = induction("Jean",     "Dupont",   "+261 34 00 00 01", "jean.dupont@stellarix.com",   "Électricien senior",   h0, b1, br, bc);
            HseInduction marie   = induction("Marie",    "Martin",   "+261 34 00 00 02", "marie.martin@stellarix.com",  "Chef de projet HSE",   h0, b1, br, bc);
            // H0 + B1 — can work in server rooms and IT zones
            HseInduction ahmed   = induction("Ahmed",    "Benali",   "+261 34 00 00 03", "ahmed.benali@stellarix.com",  "Technicien réseau",    h0, b1);
            HseInduction sophie  = induction("Sophie",   "Leclerc",  "+261 34 00 00 04", "sophie.leclerc@stellarix.com","Ingénieure systèmes",  h0, b1);
            HseInduction thomas  = induction("Thomas",   "Bernard",  "+261 34 00 00 05", "thomas.bernard@stellarix.com","Technicien câblage",   h0, b1);
            // H0 only — can work in IT zones, not server rooms without warning
            HseInduction fatima  = induction("Fatima",   "Osei",     "+261 34 00 00 06", "fatima.osei@stellarix.com",   "Responsable sécurité", h0);
            HseInduction lucas   = induction("Lucas",    "Petit",    "+261 34 00 00 07", "lucas.petit@stellarix.com",   "Agent de sécurité",    h0);
            HseInduction amara   = induction("Amara",    "Diallo",   "+261 34 00 00 08", "amara.diallo@stellarix.com",  "Visiteur accrédité",   h0);
            // No habilitations — bureau access only, will trigger missing habilitation warning elsewhere
            HseInduction karim   = induction("Karim",    "Ndiaye",   "+261 34 00 00 09", "karim.ndiaye@stellarix.com",  "Stagiaire");
            HseInduction isabelle= induction("Isabelle", "Moreau",   "+261 34 00 00 10", "isabelle.moreau@stellarix.com","Assistante RH");
            HseInduction mohamed = induction("Mohamed",  "Traore",   "+261 34 00 00 11", "mohamed.traore@stellarix.com","Visiteur externe");
            HseInduction chloe   = induction("Chloe",    "Fontaine", "+261 34 00 00 12", "chloe.fontaine@stellarix.com","Auditrice");
            HseInduction david   = induction("David",    "Rakoto",   "+261 34 00 00 13", "david.rakoto@stellarix.com",  "Consultant");
            HseInduction nadia   = induction("Nadia",    "Kone",     "+261 34 00 00 14", "nadia.kone@stellarix.com",    "Responsable qualité",  h0);
            HseInduction pierre  = induction("Pierre",   "Gautier",  "+261 34 00 00 15", "pierre.gautier@stellarix.com","Technicien maintenance",h0, b1);

            // ── Verification Logs ────────────────────────────────────────────────
            // Day -35
            validated(jean,    "WORK",  alpha, at(35, 7, 30), false, items(helmet, glasses, vest, gloves, shoes), conf(0.97f, 0.94f, 0.96f, 0.92f, 0.95f));
            validated(marie,   "WORK",  beta,  at(35, 8, 15), false, items(helmet, glasses, vest, shoes),         conf(0.96f, 0.91f, 0.93f, 0.94f));
            validated(ahmed,   "VISIT", alpha, at(35, 9,  0), false, items(helmet, vest, shoes),                  conf(0.95f, 0.92f, 0.94f));
            validated(sophie,  "VISIT", gamma, at(35, 10, 0), false, items(helmet, shoes),                        conf(0.97f, 0.95f));

            // Day -28
            rejected(thomas,   "WORK",  alpha, at(28, 8,  0), false, items(helmet, glasses, vest, gloves, shoes), det(false, true,  true,  true,  true),  conf(0.05f, 0.93f, 0.95f, 0.91f, 0.96f));
            rejected(fatima,   "WORK",  beta,  at(28, 9,  0), false, items(helmet, glasses, vest, shoes),         det(true,  false, true,  true),          conf(0.96f, 0.07f, 0.94f, 0.93f));
            validated(lucas,   "WORK",  gamma, at(28, 8, 30), false, items(helmet, glasses, vest, gloves, shoes), conf(0.95f, 0.92f, 0.94f, 0.90f, 0.97f));
            validated(amara,   "VISIT", beta,  at(28, 11, 0), false, items(helmet, vest),                         conf(0.96f, 0.93f));

            // Day -21
            rejected(karim,    "WORK",  gamma, at(21, 7, 45), false, items(helmet, glasses, vest, gloves, shoes), det(true,  true,  false, true,  true),  conf(0.97f, 0.92f, 0.06f, 0.91f, 0.95f));
            rejected(isabelle, "WORK",  alpha, at(21, 8, 30), false, items(helmet, glasses, vest, gloves, shoes), det(true,  true,  true,  true,  false), conf(0.96f, 0.93f, 0.95f, 0.90f, 0.04f));
            validated(jean,    "WORK",  beta,  at(21, 9,  0), false, items(helmet, glasses, vest, shoes),         conf(0.97f, 0.91f, 0.94f, 0.96f));
            validated(marie,   "VISIT", alpha, at(21, 10, 0), false, items(helmet, vest, shoes),                  conf(0.95f, 0.93f, 0.94f));
            validated(mohamed, "VISIT", gamma, at(21, 14, 0), false, items(helmet, shoes),                        conf(0.97f, 0.95f));

            // Day -14
            rejected(ahmed,    "WORK",  alpha, at(14, 8,  0), false, items(helmet, glasses, vest, gloves, shoes), det(true,  true,  true,  false, true),  conf(0.96f, 0.94f, 0.95f, 0.08f, 0.93f));
            rejected(sophie,   "WORK",  gamma, at(14, 9,  0), false, items(helmet, glasses, vest, gloves, shoes), det(true,  false, true,  false, true),  conf(0.95f, 0.09f, 0.93f, 0.06f, 0.94f));
            validated(thomas,  "WORK",  beta,  at(14, 8, 45), false, items(helmet, glasses, vest, shoes),         conf(0.96f, 0.92f, 0.94f, 0.95f));
            validated(fatima,  "WORK",  alpha, at(14, 10, 0), false, items(helmet, glasses, vest, gloves, shoes), conf(0.97f, 0.93f, 0.96f, 0.91f, 0.95f));
            validated(chloe,   "VISIT", alpha, at(14, 11, 0), false, items(helmet, vest, shoes),                  conf(0.96f, 0.94f, 0.95f));
            validated(david,   "VISIT", beta,  at(14, 13, 0), false, items(helmet, vest),                         conf(0.97f, 0.93f));

            // Day -10 — offline scenarios
            validated(lucas,   "WORK",  gamma, at(10, 7, 30), true,  items(helmet, glasses, vest, gloves, shoes), conf(0.95f, 0.91f, 0.94f, 0.90f, 0.97f));
            rejected(amara,    "WORK",  gamma, at(10, 8,  0), true,  items(helmet, glasses, vest, gloves, shoes), det(true,  true,  true,  false, true),  conf(0.96f, 0.92f, 0.95f, 0.07f, 0.94f));
            validated(karim,   "VISIT", alpha, at(10, 9,  0), true,  items(helmet, vest, shoes),                  conf(0.97f, 0.94f, 0.95f));
            validated(jean,    "WORK",  alpha, at(10, 8, 30), false, items(helmet, glasses, vest, gloves, shoes), conf(0.98f, 0.94f, 0.96f, 0.92f, 0.97f));
            validated(isabelle,"VISIT", gamma, at(10, 14, 0), false, items(helmet, shoes),                        conf(0.96f, 0.95f));

            // Day -7 — visit rejections
            rejected(nadia,    "VISIT", alpha, at(7, 10,  0), false, items(helmet, vest, shoes),                  det(false, true,  true),                conf(0.06f, 0.93f, 0.94f));
            rejected(pierre,   "VISIT", beta,  at(7, 11,  0), false, items(helmet, vest),                         det(true,  false),                       conf(0.95f, 0.05f));
            rejected(mohamed,  "VISIT", alpha, at(7, 12,  0), false, items(helmet, vest, shoes),                  det(true,  true,  false),                conf(0.97f, 0.92f, 0.06f));
            validated(marie,   "WORK",  gamma, at(7, 8,   0), false, items(helmet, glasses, vest, gloves, shoes), conf(0.96f, 0.93f, 0.95f, 0.91f, 0.97f));
            validated(ahmed,   "WORK",  beta,  at(7, 9,   0), false, items(helmet, glasses, vest, shoes),         conf(0.97f, 0.91f, 0.94f, 0.95f));
            validated(chloe,   "VISIT", gamma, at(7, 13,  0), false, items(helmet, shoes),                        conf(0.96f, 0.94f));

            // Day -5 — high traffic
            validated(thomas,  "WORK",  alpha, at(5, 7, 30), false, items(helmet, glasses, vest, gloves, shoes), conf(0.95f, 0.91f, 0.94f, 0.90f, 0.96f));
            validated(fatima,  "WORK",  alpha, at(5, 8,  0), false, items(helmet, glasses, vest, gloves, shoes), conf(0.97f, 0.93f, 0.96f, 0.92f, 0.95f));
            validated(lucas,   "WORK",  beta,  at(5, 8, 15), false, items(helmet, glasses, vest, shoes),         conf(0.96f, 0.90f, 0.93f, 0.94f));
            rejected(david,    "WORK",  alpha, at(5, 9,  0), false, items(helmet, glasses, vest, gloves, shoes), det(true,  false, false, true,  true),   conf(0.96f, 0.08f, 0.06f, 0.91f, 0.95f));
            validated(amara,   "VISIT", alpha, at(5, 10, 0), false, items(helmet, vest, shoes),                  conf(0.97f, 0.94f, 0.95f));
            validated(karim,   "VISIT", beta,  at(5, 11, 0), false, items(helmet, vest),                         conf(0.96f, 0.93f));
            validated(nadia,   "VISIT", gamma, at(5, 14, 0), false, items(helmet, shoes),                        conf(0.95f, 0.94f));

            // Day -3
            validated(jean,    "WORK",  gamma, at(3, 7, 45), false, items(helmet, glasses, vest, gloves, shoes), conf(0.97f, 0.94f, 0.96f, 0.91f, 0.98f));
            validated(sophie,  "WORK",  beta,  at(3, 8,  0), false, items(helmet, glasses, vest, shoes),         conf(0.95f, 0.91f, 0.94f, 0.93f));
            validated(pierre,  "WORK",  gamma, at(3, 8, 30), true,  items(helmet, glasses, vest, gloves, shoes), conf(0.96f, 0.92f, 0.95f, 0.89f, 0.97f));
            rejected(isabelle, "WORK",  gamma, at(3, 9,  0), true,  items(helmet, glasses, vest, gloves, shoes), det(true,  true,  true,  false, true),  conf(0.96f, 0.91f, 0.94f, 0.05f, 0.95f));
            validated(mohamed, "VISIT", alpha, at(3, 10, 0), false, items(helmet, vest, shoes),                  conf(0.97f, 0.93f, 0.94f));
            validated(chloe,   "VISIT", beta,  at(3, 11, 0), false, items(helmet, vest),                         conf(0.96f, 0.94f));

            // Day -1
            validated(marie,   "WORK",  alpha, at(1, 7, 30), false, items(helmet, glasses, vest, gloves, shoes), conf(0.96f, 0.93f, 0.95f, 0.92f, 0.97f));
            validated(ahmed,   "WORK",  gamma, at(1, 8,  0), false, items(helmet, glasses, vest, gloves, shoes), conf(0.97f, 0.94f, 0.96f, 0.91f, 0.95f));
            rejected(thomas,   "WORK",  beta,  at(1, 9,  0), false, items(helmet, glasses, vest, shoes),         det(false, false, true,  true),          conf(0.04f, 0.06f, 0.94f, 0.93f));
            validated(fatima,  "VISIT", gamma, at(1, 10, 0), false, items(helmet, shoes),                        conf(0.97f, 0.95f));
            validated(lucas,   "VISIT", alpha, at(1, 11, 0), false, items(helmet, vest, shoes),                  conf(0.96f, 0.93f, 0.95f));
            pending(david,     "VISIT", beta,  at(1, 15, 30));

            // Today
            validated(jean,    "WORK",  alpha, at(0, 7, 30), false, items(helmet, glasses, vest, gloves, shoes), conf(0.98f, 0.95f, 0.97f, 0.93f, 0.96f));
            validated(karim,   "WORK",  beta,  at(0, 8,  0), false, items(helmet, glasses, vest, shoes),         conf(0.97f, 0.92f, 0.95f, 0.94f));
            validated(nadia,   "WORK",  gamma, at(0, 8, 15), false, items(helmet, glasses, vest, gloves, shoes), conf(0.96f, 0.93f, 0.94f, 0.91f, 0.97f));
            rejected(pierre,   "WORK",  gamma, at(0, 9,  0), false, items(helmet, glasses, vest, gloves, shoes), det(true,  false, true,  true,  true),  conf(0.97f, 0.06f, 0.95f, 0.92f, 0.96f));
            validated(amara,   "VISIT", alpha, at(0, 10, 0), false, items(helmet, vest, shoes),                  conf(0.96f, 0.94f, 0.95f));
            pending(sophie,    "WORK",  alpha, at(0, 10, 30));
            pending(mohamed,   "VISIT", gamma, at(0, 11,  0));

            log.info("[Seed] Done — {} sites, {} PPE items, {} inductions, {} logs.",
                    siteRepo.count(), ppeItemRepo.count(), inductionRepo.count(), logRepo.count());
        };
    }

    // ── Seed helpers ─────────────────────────────────────────────────────────────

    private ZoneType zoneType(String label) {
        ZoneType z = new ZoneType();
        z.setLabel(label);
        return zoneTypeRepo.save(z);
    }

    private Habilitation hab(String code, String label, String description) {
        Habilitation h = new Habilitation();
        h.setCode(code);
        h.setLabel(label);
        h.setDescription(description);
        return habilitationRepo.save(h);
    }

    private Site site(String name, double lat, double lon, ZoneType zoneType, List<Habilitation> habs) {
        Site s = new Site();
        s.setName(name);
        s.setLatitude(lat);
        s.setLongitude(lon);
        s.setZoneType(zoneType);
        s.setHabilitations(habs);
        return siteRepo.save(s);
    }

    private HseInduction induction(String firstName, String lastName,
                                    String phone, String email, String work,
                                    Habilitation... habs) {
        HseInduction ind = new HseInduction();
        ind.setFirstName(firstName);
        ind.setLastName(lastName);
        ind.setPhone(phone);
        ind.setEmail(email);
        ind.setWork(work);
        ind.setHabilitations(new ArrayList<>(List.of(habs)));
        return inductionRepo.save(ind);
    }

    private void req(Site site, String intent, PpeItem... ppeItems) {
        for (PpeItem item : ppeItems) {
            PpeRequirement r = new PpeRequirement();
            r.setSite(site);
            r.setIntent(intent);
            r.setPpeItem(item);
            requirementRepo.save(r);
        }
    }

    private LocalDateTime at(int daysAgo, int hour, int minute) {
        return LocalDateTime.now()
                .minusDays(daysAgo)
                .withHour(hour).withMinute(minute).withSecond(0).withNano(0);
    }

    private PpeItem[] items(PpeItem... ppeItems) { return ppeItems; }
    private float[]   conf(float... vals)         { return vals; }
    private boolean[] det(boolean... vals)        { return vals; }

    private void validated(HseInduction induction, String intent, Site site,
                            LocalDateTime capturedAt, boolean offline,
                            PpeItem[] ppeItems, float[] confidences) {
        PpeVerificationLog saved = logRepo.save(buildLog(induction, intent, site, "VALIDATED", capturedAt, offline));
        for (int i = 0; i < ppeItems.length; i++) {
            saveResult(saved, ppeItems[i], true, confidences[i]);
        }
    }

    private void rejected(HseInduction induction, String intent, Site site,
                           LocalDateTime capturedAt, boolean offline,
                           PpeItem[] ppeItems, boolean[] detected, float[] confidences) {
        PpeVerificationLog saved = logRepo.save(buildLog(induction, intent, site, "REJECTED", capturedAt, offline));
        List<String> causes = new ArrayList<>();
        for (int i = 0; i < ppeItems.length; i++) {
            saveResult(saved, ppeItems[i], detected[i], confidences[i]);
            if (!detected[i]) causes.add("MISSING_PPE:" + ppeItems[i].getCode());
        }
        saved.setRejectionCauses(causes);
        logRepo.save(saved);
    }

    private void pending(HseInduction induction, String intent, Site site, LocalDateTime capturedAt) {
        logRepo.save(buildLog(induction, intent, site, "PENDING", capturedAt, false));
    }

    private PpeVerificationLog buildLog(HseInduction induction, String intent, Site site,
                                         String status, LocalDateTime capturedAt, boolean offline) {
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
