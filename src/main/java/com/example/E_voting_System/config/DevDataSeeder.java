package com.example.E_voting_System.config;


import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import com.example.E_voting_System.entity.Candidate;
import com.example.E_voting_System.entity.VotingWindow;
import com.example.E_voting_System.repository.CandidateRepository;
import com.example.E_voting_System.repository.VotingWindowRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Runs once every time the app starts.
 *
 * 1. If no active voting window exists, creates a long-lived one.
 * 2. Ensures every ward in ALL_WARDS has all STANDARD_PARTIES as
 *    ballot slots (a Candidate row exists for that party+ward pair).
 *
 * IMPORTANT: unlike the old version, this checks presence PER (ward, party)
 * pair — not "does this ward have any candidate at all". The old check
 * meant a ward that only had one manually-added party (e.g. Madurai West
 * only had "Tvk") was skipped forever and never got the other 4 parties.
 * This version fills in exactly what's missing and leaves everything else
 * untouched. Safe to run on every startup.
 *
 * UPDATED: presence is now checked across ALL statuses (PENDING, APPROVED,
 * REJECTED) for that ward, not just APPROVED. Previously this only looked
 * at APPROVED rows — so once a real applicant registered for a party
 * (flipping that row's status to PENDING while awaiting admin approval),
 * the seeder would no longer see an APPROVED row for that party in that
 * ward and would insert a SECOND blank row underneath the pending
 * application on the very next restart. Checking all statuses means a
 * party+ward slot that already exists — in any status — is left alone.
 *
 * UPDATED AGAIN: ALL_WARDS now holds all 234 Tamil Nadu Legislative
 * Assembly constituencies (in official AC-number order, 1 through 234),
 * replacing the earlier placeholder list of 23 city-level wards. This
 * list MUST stay in sync with the <select> options on the voter
 * registration page — a voter who registers under a constituency name
 * that isn't in this list will see an empty ballot at voting time,
 * because findByWardName() won't match anything.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DevDataSeeder implements CommandLineRunner {

    private final VotingWindowRepository votingWindowRepository;
    private final CandidateRepository    candidateRepository;

    // All 234 Tamil Nadu Legislative Assembly constituencies, in official
    // AC-number order (1. Gummidipoondi ... 234. Killiyoor). Must match
    // the voter-registration ward dropdown exactly, name for name.
    private static final List<String> ALL_WARDS = List.of(
            "Gummidipoondi",
            "Ponneri",
            "Tiruttani",
            "Thiruvallur",
            "Poonamallee",
            "Avadi",
            "Maduravoyal",
            "Ambattur",
            "Madavaram",
            "Thiruvottiyur",
            "Dr. Radhakrishnan Nagar",
            "Perambur",
            "Kolathur",
            "Villivakkam",
            "Thiru-Vi-Ka-Nagar",
            "Egmore",
            "Royapuram",
            "Harbour",
            "Chepauk-Thiruvallikeni",
            "Thousand Lights",
            "Anna Nagar",
            "Virugampakkam",
            "Saidapet",
            "T. Nagar",
            "Mylapore",
            "Velachery",
            "Shozhinganallur",
            "Alandur",
            "Sriperumbudur",
            "Pallavaram",
            "Tambaram",
            "Chengalpattu",
            "Thiruporur",
            "Cheyyur",
            "Madurantakam",
            "Uthiramerur",
            "Kancheepuram",
            "Arakkonam",
            "Sholingur",
            "Katpadi",
            "Ranipet",
            "Arcot",
            "Vellore",
            "Anaikattu",
            "Kilvaithinankuppam",
            "Gudiyattam",
            "Vaniyambadi",
            "Ambur",
            "Jolarpet",
            "Tirupattur",
            "Uthangarai",
            "Bargur",
            "Krishnagiri",
            "Veppanahalli",
            "Hosur",
            "Thalli",
            "Palacode",
            "Pennagaram",
            "Dharmapuri",
            "Pappireddippatti",
            "Harur",
            "Chengam",
            "Tiruvannamalai",
            "Kilpennathur",
            "Kalasapakkam",
            "Polur",
            "Arani",
            "Cheyyar",
            "Vandavasi",
            "Gingee",
            "Mailam",
            "Tindivanam",
            "Vanur",
            "Villupuram",
            "Vikravandi",
            "Tirukkoyilur",
            "Ulundurpettai",
            "Rishivandiyam",
            "Sankarapuram",
            "Kallakurichi",
            "Gangavalli",
            "Attur",
            "Yercaud",
            "Omalur",
            "Mettur",
            "Edappadi",
            "Sankari",
            "Salem (West)",
            "Salem (North)",
            "Salem (South)",
            "Veerapandi",
            "Rasipuram",
            "Senthamangalam",
            "Namakkal",
            "Paramathi-Velur",
            "Tiruchengodu",
            "Kumarapalayam",
            "Erode (East)",
            "Erode (West)",
            "Modakkurichi",
            "Dharapuram",
            "Kangayam",
            "Perundurai",
            "Bhavani",
            "Anthiyur",
            "Gobichettipalayam",
            "Bhavanisagar",
            "Udhagamandalam",
            "Gudalur",
            "Coonoor",
            "Mettupalayam",
            "Avanashi",
            "Tiruppur (North)",
            "Tiruppur (South)",
            "Palladam",
            "Sulur",
            "Kavundampalayam",
            "Coimbatore (North)",
            "Thondamuthur",
            "Coimbatore (South)",
            "Singanallur",
            "Kinathukadavu",
            "Pollachi",
            "Valparai",
            "Udumalaipettai",
            "Madathukulam",
            "Palani",
            "Oddanchatram",
            "Athoor",
            "Nilakkottai",
            "Natham",
            "Dindigul",
            "Vedasandur",
            "Aravakurichi",
            "Karur",
            "Krishnarayapuram",
            "Kulithalai",
            "Manapparai",
            "Srirangam",
            "Tiruchirappalli (West)",
            "Tiruchirappalli (East)",
            "Thiruverumbur",
            "Lalgudi",
            "Manachanallur",
            "Musiri",
            "Thuraiyur",
            "Perambalur",
            "Kunnam",
            "Ariyalur",
            "Jayankondam",
            "Tittakudi",
            "Vriddhachalam",
            "Neyveli",
            "Panruti",
            "Cuddalore",
            "Kurinjipadi",
            "Bhuvanagiri",
            "Chidambaram",
            "Kattumannarkoil",
            "Sirkazhi",
            "Mayiladuthurai",
            "Poompuhar",
            "Nagapattinam",
            "Kilvelur",
            "Vedaranyam",
            "Thiruthuraipoondi",
            "Mannargudi",
            "Thiruvarur",
            "Nannilam",
            "Thiruvidaimarudur",
            "Kumbakonam",
            "Papanasam",
            "Thiruvaiyaru",
            "Thanjavur",
            "Orathanadu",
            "Pattukkottai",
            "Peravurani",
            "Gandharvakottai",
            "Viralimalai",
            "Pudukkottai",
            "Thirumayam",
            "Alangudi",
            "Aranthangi",
            "Karaikudi",
            "Tiruppattur",
            "Sivaganga",
            "Manamadurai",
            "Melur",
            "Madurai East",
            "Sholavandan",
            "Madurai North",
            "Madurai South",
            "Madurai Central",
            "Madurai West",
            "Thiruparankundram",
            "Tirumangalam",
            "Usilampatti",
            "Andipatti",
            "Periyakulam",
            "Bodinayakanur",
            "Cumbum",
            "Rajapalayam",
            "Srivilliputhur",
            "Sattur",
            "Sivakasi",
            "Virudhunagar",
            "Aruppukkottai",
            "Tiruchuli",
            "Paramakudi",
            "Tiruvadanai",
            "Ramanathapuram",
            "Mudhukulathur",
            "Vilathikulam",
            "Thoothukkudi",
            "Tiruchendur",
            "Srivaikuntam",
            "Ottapidaram",
            "Kovilpatti",
            "Sankarankovil",
            "Vasudevanallur",
            "Kadayanallur",
            "Tenkasi",
            "Alangulam",
            "Tirunelveli",
            "Ambasamudram",
            "Palayamkottai",
            "Nanguneri",
            "Radhapuram",
            "Kanniyakumari",
            "Nagercoil",
            "Colachel",
            "Padmanabhapuram",
            "Vilavancode",
            "Killiyoor"
    );

    /**
     * Canonical party definition + known aliases/shorthands. Aliases let us
     * recognize a party that was already added manually through
     * /admin/add-candidate under a shorthand name (e.g. "Tvk" instead of
     * "Tamilaga Vettri Kazhagam", or "AIADMK" instead of the full name) so
     * we don't insert a duplicate row for the same party in that ward.
     */
    private record PartyDef(String name, String symbol, Set<String> aliases) {}

    private static final List<PartyDef> STANDARD_PARTIES = List.of(
            new PartyDef("Tamilaga Vettri Kazhagam", "📯",
                    Set.of("tvk", "tamilaga vettri kazhagam")),
            new PartyDef("Dravida Munnetra Kazhagam", "🌅",
                    Set.of("dmk", "dravida munnetra kazhagam")),
            new PartyDef("All India Anna Dravida Munnetra Kazhagam", "🌿",
                    Set.of("aiadmk", "admk", "all india anna dravida munnetra kazhagam")),
            new PartyDef("Naam Tamilar Katchi", "🧑‍🌾",
                    Set.of("ntk", "naam tamilar katchi")),
            new PartyDef("Independent", "🗳️",
                    Set.of("independent", "ind"))
    );

    @Override
    public void run(String... args) {
        seedVotingWindow();
        backfillCandidates();
    }

    private void seedVotingWindow() {
        boolean hasActive = votingWindowRepository.findTopByActiveTrueOrderByIdDesc().isPresent();
        if (hasActive) {
            log.info("DevDataSeeder: active voting window already exists, skipping.");
            return;
        }
        VotingWindow window = new VotingWindow();
        window.setStartTime(LocalDateTime.now().minusDays(1));
        window.setEndTime(LocalDateTime.now().plusDays(365));
        window.setActive(true);
        votingWindowRepository.save(window);
        log.info("DevDataSeeder: created default 1-year voting window.");
    }

    private void backfillCandidates() {
        int inserted = 0;

        // Single query for every ward at once instead of one query per ward
        // (was 234 round-trips on every startup — now it's 1).
        List<Candidate> allExisting = candidateRepository.findByWardNameIn(ALL_WARDS);

        // Group existing candidates by ward, normalizing party names per ward
        // so the alias/duplicate check below works exactly as before.
        Map<String, Set<String>> existingByWard = allExisting.stream()
                .collect(Collectors.groupingBy(
                        Candidate::getWardName,
                        Collectors.mapping(c -> normalize(c.getPartyName()), Collectors.toSet())
                ));

        List<Candidate> toInsert = new ArrayList<>();

        for (String ward : ALL_WARDS) {
            Set<String> existingNormalized = existingByWard.getOrDefault(ward, Set.of());

            for (PartyDef party : STANDARD_PARTIES) {
                boolean alreadyPresent =
                        existingNormalized.contains(normalize(party.name()))
                                || party.aliases().stream().anyMatch(existingNormalized::contains);

                if (alreadyPresent) continue;

                Candidate c = new Candidate();
                c.setPartyName(party.name());
                c.setPartySymbol(party.symbol());
                c.setWardName(ward);
                c.setStatus(Candidate.Status.APPROVED);
                c.setVoteCount(0);
                toInsert.add(c);
                inserted++;
                log.info("DevDataSeeder: added missing party '{}' to ward '{}'.",
                        party.name(), ward);
            }
        }

        // Single batch insert instead of one INSERT per missing row.
        if (!toInsert.isEmpty()) {
            candidateRepository.saveAll(toInsert);
        }

        if (inserted == 0) {
            log.info("DevDataSeeder: all wards already have the full standard party set.");
        } else {
            log.info("DevDataSeeder: backfilled {} missing candidate row(s) across all wards.",
                    inserted);
        }
    }
    private String normalize(String s) {
        return s == null ? "" : s.trim().toLowerCase(Locale.ROOT);
    }
}