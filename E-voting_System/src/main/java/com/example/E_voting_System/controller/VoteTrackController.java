package com.example.E_voting_System.controller;

import com.example.E_voting_System.service.OtpService;
import com.example.E_voting_System.service.VoteTrackService;
import com.example.E_voting_System.stream.SseStreamService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.Map;

/**
 * ✅ IMPROVED VoteTrackController
 * - Better error handling
 * - Improved logging
 * - Session validation
 * - Response messages
 */
@Slf4j
@Controller
@RequestMapping("/voter")
@RequiredArgsConstructor
public class VoteTrackController {

    private final VoteTrackService voteTrackService;
    private final OtpService otpService;
    private final SseStreamService sseStreamService;

    /**
     * Display voting page with candidates
     * ✅ Protected - requires session
     */
    @GetMapping
    public String votePage(HttpSession session, Model model) {
        Long voterId = (Long) session.getAttribute("voterId");

        if (voterId == null) {
            log.warn("⚠️ Unauthorized access attempt to voting page");
            return "redirect:/voter/login";
        }

        try {
            model.addAttribute("results", voteTrackService.getResults());
            log.info("✓ Voting page loaded for voter: {}", voterId);
            return "vote";
        } catch (Exception e) {
            log.error("❌ Error loading voting page: {}", e.getMessage(), e);
            model.addAttribute("error", "Failed to load candidates. Please try again.");
            return "redirect:/voter/login";
        }
    }

    /**
     * Cast a vote
     * ✅ Validates session, candidate exists, and prevents duplicate votes
     */
    @PostMapping("/cast")
    public String castVote(@RequestParam(required = false) Long candidateId,
                           HttpSession session,
                           Model model) {
        Long voterId = (Long) session.getAttribute("voterId");

        // Validate session
        if (voterId == null) {
            log.warn("⚠️ Vote cast attempt without valid session");
            return "redirect:/voter/login";
        }

        // Validate candidate ID
        if (candidateId == null || candidateId <= 0) {
            log.warn("⚠️ Invalid candidate ID: {}", candidateId);
            model.addAttribute("error", "Invalid candidate selected. Please try again.");
            model.addAttribute("results", voteTrackService.getResults());
            return "vote";
        }

        try {
            String result = voteTrackService.castVote(voterId, candidateId);

            // Publish real-time update
            sseStreamService.publishEvent(voteTrackService.getResultsMap());

            log.info("✓ Vote successfully cast - Voter: {}, Candidate: {}", voterId, candidateId);
            model.addAttribute("message", result);

            return "vote-success";

        } catch (IllegalArgumentException e) {
            log.warn("⚠️ Vote validation error - Voter: {}, Error: {}", voterId, e.getMessage());
            model.addAttribute("error", e.getMessage());
            model.addAttribute("results", voteTrackService.getResults());
            return "vote";
        } catch (Exception e) {
            log.error("❌ Error casting vote - Voter: {}, Candidate: {}, Error: {}",
                    voterId, candidateId, e.getMessage(), e);
            model.addAttribute("error", "An error occurred while casting your vote. Please try again.");
            model.addAttribute("results", voteTrackService.getResults());
            return "vote";
        }
    }

    /**
     * Display live results with real-time updates
     */
    @GetMapping("/live")
    public String liveResults(Model model) {
        try {
            model.addAttribute("results", voteTrackService.getResults());
            log.info("✓ Live results page accessed");
            return "live-results";
        } catch (Exception e) {
            log.error("❌ Error loading live results: {}", e.getMessage(), e);
            model.addAttribute("error", "Failed to load results. Please try again.");
            return "error";
        }
    }

    /**
     * Display final results (election concluded)
     */
    @GetMapping("/final")
    public String finalResults(Model model) {
        try {
            model.addAttribute("results", voteTrackService.getResults());
            log.info("✓ Final results page accessed");
            return "final-results";
        } catch (Exception e) {
            log.error("❌ Error loading final results: {}", e.getMessage(), e);
            model.addAttribute("error", "Failed to load final results. Please try again.");
            return "error";
        }
    }

    /**
     * Health check endpoint for debugging
     */
    @GetMapping("/health")
    @ResponseBody
    public Map<String, Object> healthCheck() {
        return Map.of(
                "status", "UP",
                "timestamp", new Date(),
                "service", "E-Voting System"
        );
    }
}

