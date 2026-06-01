package com.stellarix.hse.configuration;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import com.stellarix.hse.entity.Company;
import com.stellarix.hse.entity.Habilitation;
import com.stellarix.hse.entity.HseInduction;
import com.stellarix.hse.entity.InductionRole;
import com.stellarix.hse.entity.PpeItem;
import com.stellarix.hse.entity.PpeRequirement;
import com.stellarix.hse.entity.Site;
import com.stellarix.hse.entity.ZoneType;
import com.stellarix.hse.repository.CompanyRepository;
import com.stellarix.hse.repository.HabilitationRepository;
import com.stellarix.hse.repository.HseInductionRepository;
import com.stellarix.hse.repository.InductionRoleRepository;
import com.stellarix.hse.repository.PpeItemRepository;
import com.stellarix.hse.repository.PpeRequirementRepository;
import com.stellarix.hse.repository.SiteRepository;
import com.stellarix.hse.repository.ZoneTypeRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Configuration
@EnableScheduling
@RequiredArgsConstructor
@Slf4j
public class Config {

    private final SiteRepository siteRepo;
    private final PpeItemRepository ppeItemRepo;
    private final PpeRequirementRepository requirementRepo;
    private final HseInductionRepository inductionRepo;
    private final ZoneTypeRepository zoneTypeRepo;
    private final HabilitationRepository habilitationRepo;
    private final CompanyRepository companyRepo;
    private final InductionRoleRepository roleRepo;

    @Bean
    CommandLineRunner commandLineRunner() {
        return args -> {
            if (habilitationRepo.count() > 0) {
                log.info("[Seed] Reference data already present — skipping.");
                return;
            }
            log.info("[Seed] Seeding reference data…");

            // ── Habilitations ────────────────────────────────────────────────────
            Habilitation h0 = hab("H0", "Hors tension",          "Non-électricien habilité à travailler hors tension");
            Habilitation b1 = hab("B1", "Basse tension travaux", "Électricien habilité pour travaux basse tension");
            Habilitation br = hab("BR", "Rechargement",           "Habilité à effectuer des rechargements et dépannages");
            Habilitation bc = hab("BC", "Consignation",           "Chargé de consignation basse tension");

            // ── Zone Types ───────────────────────────────────────────────────────
            ZoneType salleServ = zoneType("Salle serveurs", h0);
            ZoneType zoneIT    = zoneType("Zone IT", h0);
            ZoneType bureau    = zoneType("Bureau");
            ZoneType hTension  = zoneType("Haute tension", h0, b1, br, bc);

            // ── PPE Items ────────────────────────────────────────────────────────
            PpeItem helmet  = ppeItemRepo.save(new PpeItem(null, "HELMET",         "Casque de sécurité"));
            PpeItem glasses = ppeItemRepo.save(new PpeItem(null, "SAFETY_GLASSES", "Lunettes de protection"));
            PpeItem vest    = ppeItemRepo.save(new PpeItem(null, "HIGH_VIS_VEST",  "Gilet haute visibilité"));
            PpeItem gloves  = ppeItemRepo.save(new PpeItem(null, "GLOVES",         "Gants de protection"));
            PpeItem shoes   = ppeItemRepo.save(new PpeItem(null, "SAFETY_SHOES",   "Chaussures de sécurité"));

            // ── Sites ────────────────────────────────────────────────────────────
            site("Site A", -18.9168, 47.5361, salleServ);
            site("Site B", -18.9000, 47.5500, zoneIT);
            site("Site C", -18.9300, 47.5200, bureau);
            site("Site D", -18.9500, 47.5100, hTension);

            // ── PPE Matrix ───────────────────────────────────────────────────────
            req(salleServ, "WORK",  helmet, glasses, vest, gloves, shoes);
            req(salleServ, "VISIT", helmet, vest, shoes);
            req(zoneIT,    "WORK",  helmet, glasses, vest, shoes);
            req(zoneIT,    "VISIT", helmet, vest);
            req(bureau,    "WORK",  helmet, glasses, vest, gloves, shoes);
            req(bureau,    "VISIT", helmet, shoes);
            req(hTension,  "WORK",  helmet, glasses, vest, gloves, shoes);
            req(hTension,  "VISIT", helmet, vest, shoes);

            // ── Companies & Roles ────────────────────────────────────────────────
            Company stellarix = company("Stellarix");
            InductionRole stagiaire = role("Stagiaire");

            // ── Inductions ───────────────────────────────────────────────────────
            induction("Anderson", "Mahosy", "+261 34 81 362 05",
                    "andersonmahosi@gmail.com", stellarix, stagiaire, h0);

            log.info("[Seed] Done — {} sites, {} PPE items, {} inductions.",
                    siteRepo.count(), ppeItemRepo.count(), inductionRepo.count());
        };
    }

    // ── Seed helpers ─────────────────────────────────────────────────────────────

    private ZoneType zoneType(String label, Habilitation... habs) {
        ZoneType z = new ZoneType();
        z.setLabel(label);
        z.setHabilitations(new ArrayList<>(List.of(habs)));
        return zoneTypeRepo.save(z);
    }

    private Habilitation hab(String code, String label, String description) {
        Habilitation h = new Habilitation();
        h.setCode(code);
        h.setLabel(label);
        h.setDescription(description);
        return habilitationRepo.save(h);
    }

    private Site site(String name, double lat, double lon, ZoneType zoneType) {
        Site s = new Site();
        s.setName(name);
        s.setLatitude(lat);
        s.setLongitude(lon);
        s.setZoneType(zoneType);
        return siteRepo.save(s);
    }

    private Company company(String name) {
        Company c = new Company();
        c.setName(name);
        return companyRepo.save(c);
    }

    private InductionRole role(String name) {
        InductionRole r = new InductionRole();
        r.setName(name);
        return roleRepo.save(r);
    }

    private HseInduction induction(String firstName, String lastName,
                                    String phone, String email,
                                    Company company, InductionRole role,
                                    Habilitation... habs) {
        HseInduction ind = new HseInduction();
        ind.setFirstName(firstName);
        ind.setLastName(lastName);
        ind.setPhone(phone);
        ind.setEmail(email);
        ind.setCompany(company);
        ind.setRole(role);
        ind.setHabilitations(new ArrayList<>(List.of(habs)));
        return inductionRepo.save(ind);
    }

    private void req(ZoneType zoneType, String intent, PpeItem... ppeItems) {
        for (PpeItem item : ppeItems) {
            PpeRequirement r = new PpeRequirement();
            r.setZoneType(zoneType);
            r.setIntent(intent);
            r.setPpeItem(item);
            requirementRepo.save(r);
        }
    }

}
