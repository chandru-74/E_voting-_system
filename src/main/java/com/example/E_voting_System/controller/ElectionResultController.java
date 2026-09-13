package com.example.E_voting_System.controller;

import com.example.E_voting_System.dto.ResultResponse;
import com.example.E_voting_System.service.CandidateService;
import com.example.E_voting_System.service.ElectionResultService;
import com.example.E_voting_System.service.VoteTrackService;
import com.example.E_voting_System.service.VoterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
// All endpoints start with /api/results
@RequestMapping("/api/results")
@RequiredArgsConstructor
@Tag(name = "Election Results", description = "Publish and retrieve election results")
public class ElectionResultController {

    private final ElectionResultService electionResultService;
    private final CandidateService      candidateService;
    private final VoterService          voterService;
    private final VoteTrackService      voteTrackService;

    // POST /api/results/publish
    // Admin triggers this to publish final results.
    //
    // Spring Security permits this path (see SecurityConfig — it's not
    // gated by a Spring role, same as /admin/**), so the admin check has
    // to happen here instead, using the same HttpSession "adminLoggedIn"
    // flag AdminAuthController sets on /admin/login. Without this, anyone
    // who finds the URL could publish results without ever logging in.
    @PostMapping("/publish")
    @Operation(summary = "Publish election results (admin only)")
    public ResponseEntity<?> publishResults(HttpSession session) {
        if (!isAdminLoggedIn(session)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Admin login required to publish results."));
        }
        try {
            List<ResultResponse> results = electionResultService.publishResults();
            // 200 OK with the results list in the body
            return ResponseEntity.ok(results);
        } catch (IllegalStateException e) {
            // 400 Bad Request — e.g. already published or no candidates
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // POST /api/results/reset
    // Admin triggers this for a FULL ELECTION RESET, e.g. to run a fresh
    // election cycle after results have been published.
    //
    // This is DESTRUCTIVE to vote data:
    //   - Every candidate's vote count is zeroed
    //   - Every voter's hasVoted flag is cleared (they can vote again)
    //   - Every VoteTrack record (vote receipt hash) is deleted
    //   - Every published ElectionResult row is deleted (un-publishes)
    //
    // It does NOT delete candidate or voter records themselves — names,
    // wards, party info, and voter approval status are all untouched.
    // There is no undo once this runs, so the confirming UI (admin
    // dashboard) should use a strong, explicit confirmation dialog before
    // calling this — not the same lightweight confirm as /publish.
    @PostMapping("/reset")
    @Transactional
    @Operation(summary = "Full election reset (admin only) — clears votes, keeps candidates & voters")
    public ResponseEntity<?> resetElection(HttpSession session) {
        if (!isAdminLoggedIn(session)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Admin login required to reset the election."));
        }
        try {
            candidateService.resetAllVoteCounts();
            voterService.resetAllVotingStatus();
            voteTrackService.clearAllVotes();
            electionResultService.resetPublication();

            return ResponseEntity.ok(Map.of(
                    "message", "Election reset successfully. " +
                            "All vote counts, voting status, vote records, and " +
                            "published results were cleared. Candidates and voters were not deleted."
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Reset failed: " + e.getMessage()));
        }
    }

    // GET /api/results
    // Anyone can view published results
    @GetMapping
    @Operation(summary = "Get all published results sorted by votes")
    public ResponseEntity<List<ResultResponse>> getResults() {
        return ResponseEntity.ok(electionResultService.getResults());
    }

    // GET /api/results/published
    // Quick check: have results been published yet?
    @GetMapping("/published")
    @Operation(summary = "Check if results have been published")
    public ResponseEntity<Map<String, Boolean>> isPublished() {
        return ResponseEntity.ok(
                Map.of("published", electionResultService.resultsPublished())
        );
    }

    private boolean isAdminLoggedIn(HttpSession session) {
        return Boolean.TRUE.equals(session.getAttribute("adminLoggedIn"));
    }

    // POST /api/results/full-reset
// Everything /reset does, PLUS permanently deletes every Candidate (party)
// and every Voter row. Use this only when parties are changing for a new
// election cycle and old registrations should not carry over.
//
// Order matters: election_results references candidate_id (FK), so it
// must be cleared before candidates are deleted, or the delete will fail
// on a foreign-key constraint.
    @PostMapping("/full-reset")
    @Transactional
    @Operation(summary = "Full wipe (admin only) — deletes candidates, voters, votes, and results")
    public ResponseEntity<?> fullReset(HttpSession session) {
        if (!isAdminLoggedIn(session)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Admin login required for a full reset."));
        }
        try {
            electionResultService.resetPublication();   // clear election_results first (FK to candidate)
            voteTrackService.clearAllVotes();
            candidateService.deleteAllCandidates();
            voterService.deleteAllVoters();

            return ResponseEntity.ok(Map.of(
                    "message", "Full reset complete. All candidates, voters, vote records, " +
                            "and published results were permanently deleted. " +
                            "Add new parties and let voters re-register for the next election cycle."
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Full reset failed: " + e.getMessage()));
        }
    }
}