package com.example.E_voting_System.controller;

import com.example.E_voting_System.dto.VoterRegisterRequest;
import com.example.E_voting_System.entity.Candidate;
import com.example.E_voting_System.entity.Voter;
import com.example.E_voting_System.entity.VoteTrack;
import com.example.E_voting_System.entity.VotingWindow;
import com.example.E_voting_System.repository.VoteTrackRepository;
import com.example.E_voting_System.service.CandidateService;
import com.example.E_voting_System.service.VoterService;
import com.example.E_voting_System.service.VotingWindowService;
import com.example.E_voting_System.util.AadhaarValidator;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Controller
@RequestMapping("/voter")
@RequiredArgsConstructor
public class VoterController {

    private final VoterService        voterService;
    private final CandidateService    candidateService;
    private final VotingWindowService votingWindowService;
    private final VoteTrackRepository voteTrackRepository;

    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("dd-MM-yyyy hh:mm a");

    // ── Ballot Dashboard ──────────────────────────────────────

    @GetMapping
    public String dashboard(HttpSession session, Model model) {
        Long voterId = getVoterIdFromSession(session);
        if (voterId == null) return "redirect:/voter/login";

        try {
            Voter voter = voterService.getById(voterId);

            if (voter.getStatus() != Voter.Status.APPROVED) {
                model.addAttribute("error",
                        "Your registration is not approved yet. Status: " + voter.getStatus());
                return "error";
            }

            // Guard: already voted → details page
            if (voter.isHasVoted()) return "redirect:/voter/already-voted";

            // ── Voting window check ───────────────────────────
            if (!votingWindowService.isVotingOpen()) {
                Optional<VotingWindow> window = votingWindowService.getActiveWindow();
                String message;
                if (window.isEmpty()) {
                    message = "Voting has not been scheduled yet. Please check back later.";
                } else {
                    String start = window.get().getStartTime().format(FMT);
                    String end   = window.get().getEndTime().format(FMT);
                    if (LocalDateTime.now().isBefore(window.get().getStartTime())) {
                        message = "Voting has not started yet. " +
                                "It will open on " + start + " and close at " + end + ".";
                    } else {
                        message = "Voting has closed. It ended at " + end + ". " +
                                "Thank you for your participation.";
                    }
                }
                model.addAttribute("windowMessage", message);
                model.addAttribute("voter", voter);
                return "voting-closed";
            }

            // ── Load candidates for voter's ward only ─────────
            List<Candidate> candidates = candidateService
                    .getCandidatesByWard(voter.getWardName());
            if (candidates == null) candidates = new ArrayList<>();

            if (candidates.isEmpty()) {
                log.warn("No candidates found for ward: {}", voter.getWardName());
                model.addAttribute("windowMessage",
                        "No candidates are registered for your ward: "
                                + voter.getWardName() + ". Please contact the Election Commission.");
                model.addAttribute("voter", voter);
                return "voting-closed";
            }

            model.addAttribute("voter",      voter);
            model.addAttribute("candidates", candidates);
            return "vote";

        } catch (Exception e) {
            log.error("Error loading ballot for voter ID: {}", voterId, e);
            model.addAttribute("error", "Could not load the voting page right now. Please try again shortly.");
            return "error";
        }
    }

    // ── Register Page ─────────────────────────────────────────

    @GetMapping("/register")
    public String registerForm(Model model) {
        if (!model.containsAttribute("request")) {
            model.addAttribute("request", new VoterRegisterRequest());
        }
        return "register";
    }

    @PostMapping("/register")
    public String register(@Valid @ModelAttribute("request") VoterRegisterRequest request,
                           BindingResult bindingResult, Model model,
                           RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) return "register";

        try {
            Voter saved = voterService.register(request);

            // Flash attributes survive exactly one redirect — the home
            // page (GET "/") picks these up automatically on the very
            // next request, then they're gone. This is what lets the
            // home page act as the "gate": every registration outcome
            // (approved or rejected) funnels through it before the
            // person can go anywhere else.
            if (saved.getStatus() == Voter.Status.APPROVED) {
                redirectAttributes.addFlashAttribute("regSuccess", true);
                redirectAttributes.addFlashAttribute("regMessage",
                        "✅ Registration approved! You're now eligible to vote in "
                                + saved.getWardName() + ". Login with your Aadhaar and mobile once voting opens.");
            } else {
                redirectAttributes.addFlashAttribute("regSuccess", false);
                redirectAttributes.addFlashAttribute("regMessage",
                        "❌ " + saved.getRejectionReason());
            }

            return "redirect:/";

        } catch (IllegalStateException e) {
            // Not an outcome to show on the home page — this means the
            // form itself needs fixing (e.g. duplicate Aadhaar), so send
            // them back to the same form with the error, not to "/".
            model.addAttribute("error", e.getMessage());
            return "register";
        }
    }

    // ── Verify Aadhaar Format (AJAX, used by the "Verify" button on the
    //    registration page's Aadhaar field) ───────────────────────────
    // Uses the real Verhoeff checksum algorithm (same one UIDAI uses) via
    // AadhaarValidator — catches typos/made-up numbers that merely "look"
    // like a valid 12-digit number but fail the checksum. This still
    // cannot confirm the number was actually issued by UIDAI to a real
    // person; that requires UIDAI's own verification service.
    @GetMapping("/verify-aadhaar")
    @ResponseBody
    public Map<String, Object> verifyAadhaar(@RequestParam String aadhaar) {
        Map<String, Object> response = new HashMap<>();
        String trimmed = aadhaar == null ? "" : aadhaar.trim();

        if (!trimmed.matches("\\d{12}")) {
            response.put("valid", false);
            response.put("reason", "Aadhaar must be exactly 12 digits (you entered "
                    + trimmed.replaceAll("\\D", "").length() + ")");
            return response;
        }
        if (!AadhaarValidator.hasValidFirstDigit(trimmed)) {
            response.put("valid", false);
            response.put("reason", "Aadhaar numbers never start with 0 or 1 — check for a typo");
            return response;
        }
        if (!AadhaarValidator.isValidChecksum(trimmed)) {
            response.put("valid", false);
            response.put("reason", "This number fails Aadhaar's checksum validation — likely a typo");
            return response;
        }

        response.put("valid", true);
        response.put("reason", "Valid Aadhaar number format and checksum");
        return response;
    }

    // ── Already Voted — loads voter + VoteTrack (NO candidate info) ──────────

    @GetMapping("/already-voted")
    public String alreadyVoted(HttpSession session, Model model) {
        Long voterId = getVoterIdFromSession(session);

        if (voterId == null) {
            return "redirect:/voter/login";
        }

        try {
            Voter voter = voterService.getById(voterId);
            model.addAttribute("voter", voter);

            // Receipt is only available if this is the same session the vote
            // was cast in — there is no database lookup path from voterId to
            // vote details anymore (see VoteTrack/Option A note). If it's a
            // fresh login on a later day, the template should just show "you
            // have voted" without a specific hash/timestamp.
            String hash = (String) session.getAttribute("voteReceiptHash");
            Object votedAt = session.getAttribute("voteReceiptAt");
            if (hash != null) {
                model.addAttribute("receiptHash", hash);
                model.addAttribute("receiptVotedAt", votedAt);
            }

        } catch (Exception e) {
            log.warn("Could not load voter details for already-voted page, voterId={}", voterId, e);
        }

        return "already-voted";
    }

    // ── Results (voter view) ──────────────────────────────────

    @GetMapping("/results")
    public String results(HttpSession session, Model model) {
        Long voterId = getVoterIdFromSession(session);
        if (voterId == null) return "redirect:/voter/login";

        try {
            Voter voter = voterService.getById(voterId);

            List<Candidate> candidates = candidateService
                    .getCandidatesByWard(voter.getWardName());
            if (candidates == null) candidates = new ArrayList<>();

            long totalVotes = candidates.stream()
                    .mapToLong(Candidate::getVoteCount).sum();

            model.addAttribute("voter",       voter);
            model.addAttribute("candidates",  candidates);
            model.addAttribute("totalVotes",  totalVotes);
            model.addAttribute("lastUpdated",
                    LocalDateTime.now().format(
                            DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss")));
            return "results";

        } catch (Exception e) {
            log.error("Error loading results for voter ID: {}", voterId, e);
            model.addAttribute("error", "Could not load results right now. Please try again shortly.");            return "error";
        }
    }

    // ── Helper ────────────────────────────────────────────────

    private Long getVoterIdFromSession(HttpSession session) {
        Object obj = session.getAttribute("voterId");
        return (obj instanceof Long) ? (Long) obj : null;
    }
}