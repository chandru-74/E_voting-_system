package com.example.E_voting_System.controller;

import com.example.E_voting_System.dto.PartyResultResponse;
import com.example.E_voting_System.dto.ResultResponse;
import com.example.E_voting_System.service.CandidateService;
import com.example.E_voting_System.service.ElectionResultService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Controller
@RequiredArgsConstructor
public class PublicResultsController {

    private final CandidateService      candidateService;
    private final ElectionResultService electionResultService;

    /**
     * Live count — deliberately shows RAW VOTE COUNTS ONLY while voting
     * is in progress. Once results are officially published, the ward
     * blocks below add a winner badge per ward (see live-results.html) —
     * but the aggregate party standings table remains reserved for
     * /final-results only.
     */
    @GetMapping("/live-results")
    public String liveResults(Model model, HttpSession session) {

        List<ResultResponse> allResults = candidateService.getAllCandidates().stream()
                .map(c -> new ResultResponse(
                        c.getId(),
                        c.getPartyName(),
                        c.getPartySymbol(),
                        c.getWardName(),
                        c.getVoteCount(),
                        false
                ))
                .toList();

        Map<String, List<ResultResponse>> wardResults = groupByWard(allResults);
        List<PartyResultResponse> partyResults = aggregateVoteTotals(allResults);

        long totalVotes = allResults.stream()
                .mapToLong(ResultResponse::totalVotes)
                .sum();

        model.addAttribute("wardResults",  wardResults);
        model.addAttribute("partyResults", partyResults);
        model.addAttribute("totalVotes",   totalVotes);
        model.addAttribute("totalWards",   wardResults.size());
        model.addAttribute("lastUpdated",
                LocalDateTime.now().format(
                        DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss")));
        model.addAttribute("published", electionResultService.resultsPublished());

        // Only shown so the "Admin Dashboard" button on this page renders
        // for an admin who's actually logged in — same session flag every
        // other admin-gated check in this app uses. Regular voters never
        // see this attribute as true.
        model.addAttribute("isAdmin",
                Boolean.TRUE.equals(session.getAttribute("adminLoggedIn")));

        return "live-results";
    }

    /**
     * Final results — only rendered once officially published. The winner
     * is decided by number of wards WON (strict highest vote count in that
     * ward; a tie credits no one). "totalWards" counts EVERY ward, including
     * ones where no one voted — an uncontested ward still counted as a ward
     * available to be won, it just wasn't won by anyone.
     */
    @GetMapping("/final-results")
    public String finalResults(Model model) {
        boolean published = electionResultService.resultsPublished();

        List<ResultResponse> raw = published ? electionResultService.getResults() : List.of();

        List<PartyResultResponse> partyResults = aggregateByWardsWon(raw);
        Map<String, List<ResultResponse>> wardResults = groupByWard(raw);

        model.addAttribute("published", published);
        model.addAttribute("results", partyResults);       // party totals + wardsWon + leading
        model.addAttribute("wardResults", wardResults);    // per-ward breakdown (votes + % share)
        model.addAttribute("totalWards", wardResults.size());

        return "final-results";
    }

    // Groups candidate-level results by ward, sorted by votes desc within
    // each ward. LinkedHashMap keeps ward insertion order stable.
    private Map<String, List<ResultResponse>> groupByWard(List<ResultResponse> results) {
        return results.stream()
                .collect(Collectors.groupingBy(
                        ResultResponse::wardName,
                        LinkedHashMap::new,
                        Collectors.collectingAndThen(
                                Collectors.toList(),
                                list -> list.stream()
                                        .sorted((a, b) -> Long.compare(b.totalVotes(), a.totalVotes()))
                                        .toList()
                        )
                ));
    }

    /**
     * LIVE RESULTS ONLY. Plain vote totals per party, summed across all
     * wards, sorted by votes desc. No winner is declared — `leading` is
     * always false and `wardsWon` is always 0 (unused by the live template).
     */
    private List<PartyResultResponse> aggregateVoteTotals(List<ResultResponse> results) {
        Map<String, List<ResultResponse>> byParty = results.stream()
                .collect(Collectors.groupingBy(ResultResponse::partyName));

        return byParty.entrySet().stream()
                .map(e -> new PartyResultResponse(
                        e.getKey(),
                        e.getValue().get(0).partySymbol(),
                        e.getValue().stream().mapToLong(ResultResponse::totalVotes).sum(),
                        0,      // wardsWon — not computed for live count
                        false)) // leading — no winner declared during live count
                .sorted(Comparator.comparingLong(PartyResultResponse::totalVotes).reversed())
                .toList();
    }

    /**
     * FINAL RESULTS ONLY. Groups candidate-level results by party name,
     * sums votes across all wards, and counts how many wards each party
     * WON (strict highest vote count in that ward; a tie for first place
     * credits no one). Overall "leading" party = most wards won, ties
     * broken by total votes.
     */
    private List<PartyResultResponse> aggregateByWardsWon(List<ResultResponse> results) {
        Map<String, List<ResultResponse>> byParty = results.stream()
                .collect(Collectors.groupingBy(ResultResponse::partyName));

        Map<String, List<ResultResponse>> byWard = results.stream()
                .collect(Collectors.groupingBy(ResultResponse::wardName));

        Map<String, Integer> wardsWonByParty = new HashMap<>();
        for (List<ResultResponse> wardCandidates : byWard.values()) {
            long maxVotes = wardCandidates.stream()
                    .mapToLong(ResultResponse::totalVotes)
                    .max()
                    .orElse(0);

            if (maxVotes == 0) continue;

            List<String> topParties = wardCandidates.stream()
                    .filter(r -> r.totalVotes() == maxVotes)
                    .map(ResultResponse::partyName)
                    .distinct()
                    .toList();

            if (topParties.size() == 1) {
                wardsWonByParty.merge(topParties.get(0), 1, Integer::sum);
            }
        }

        List<PartyResultResponse> totals = byParty.entrySet().stream()
                .map(e -> new PartyResultResponse(
                        e.getKey(),
                        e.getValue().get(0).partySymbol(),
                        e.getValue().stream().mapToLong(ResultResponse::totalVotes).sum(),
                        wardsWonByParty.getOrDefault(e.getKey(), 0),
                        false))
                .sorted(
                        Comparator.comparingInt(PartyResultResponse::wardsWon).reversed()
                                .thenComparing(Comparator.comparingLong(PartyResultResponse::totalVotes).reversed())
                )
                .toList();

        if (totals.isEmpty()) return totals;

        String topParty = totals.get(0).partyName();
        return totals.stream()
                .map(p -> p.partyName().equals(topParty)
                        ? new PartyResultResponse(p.partyName(), p.partySymbol(), p.totalVotes(), p.wardsWon(), true)
                        : p)
                .toList();
    }

    /**
     * Election Results Centre — landing page with two options: Live Results
     * and Final Results. No model attributes needed; it's a static hub.
     */
    @GetMapping("/election-results")
    public String electionResultsHub() {
        return "election-results";
    }

    /**
     * Live count for the Election Results Centre flow only. Same data as
     * /live-results, but rendered with the "election-results-live" template
     * (Home/Admin buttons swapped for Results Centre navigation).
     */
    @GetMapping("/election-results/live")
    public String electionResultsLive(Model model, HttpSession session) {
        List<ResultResponse> allResults = candidateService.getAllCandidates().stream()
                .map(c -> new ResultResponse(
                        c.getId(), c.getPartyName(), c.getPartySymbol(),
                        c.getWardName(), c.getVoteCount(), false))
                .toList();

        Map<String, List<ResultResponse>> wardResults = groupByWard(allResults);
        List<PartyResultResponse> partyResults = aggregateVoteTotals(allResults);

        long totalVotes = allResults.stream().mapToLong(ResultResponse::totalVotes).sum();

        model.addAttribute("wardResults",  wardResults);
        model.addAttribute("partyResults", partyResults);
        model.addAttribute("totalVotes",   totalVotes);
        model.addAttribute("totalWards",   wardResults.size());
        model.addAttribute("lastUpdated",
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss")));
        model.addAttribute("published", electionResultService.resultsPublished());

        return "election-results-live";
    }

    /**
     * Final results for the Election Results Centre flow only. Same data as
     * /final-results, rendered with the "election-results-final" template.
     */
    @GetMapping("/election-results/final")
    public String electionResultsFinal(Model model) {
        boolean published = electionResultService.resultsPublished();
        List<ResultResponse> raw = published ? electionResultService.getResults() : List.of();

        model.addAttribute("published", published);
        model.addAttribute("results", aggregateByWardsWon(raw));
        model.addAttribute("wardResults", groupByWard(raw));
        model.addAttribute("totalWards", groupByWard(raw).size());

        return "election-results-final";
    }
}