package com.example.E_voting_System.controller;

import com.example.E_voting_System.blockchain.BlockChainService;
import com.example.E_voting_System.dto.VotePayLoad;
import com.example.E_voting_System.entity.VoteTrack;
import com.example.E_voting_System.exception.ResourceNotFoundException;
import com.example.E_voting_System.exception.VoterAlreadyVotedException;
import com.example.E_voting_System.service.CandidateService;
import com.example.E_voting_System.service.VoterService;
import com.example.E_voting_System.service.VoteTrackService;
import com.example.E_voting_System.stream.SseStreamService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * TRANSACTION SCOPE:
 *   ┌── @Transactional ─────────────────────────────┐
 *   │  1. hasVoted() check         (READ)            │
 *   │  2. recordVote()             (INSERT)          │
 *   │  3. incrementVoteCount()     (UPDATE)          │
 *   │  4. markAsVoted()            (UPDATE)          │
 *   └───────────────────────────────────────────────┘
 *      5. blockchain.addBlock()   ← OUTSIDE (intentional)
 *      6. sseStreamService.broadcast() ← OUTSIDE (intentional)
 */
@Slf4j
@Controller
@RequestMapping("/api/vote")
@RequiredArgsConstructor
public class VoteTrackController {

    private final VoteTrackService  voteTrackService;
    private final VoterService      voterService;
    private final CandidateService  candidateService;
    private final BlockChainService blockChainService;
    private final SseStreamService  sseStreamService;

    private static final DateTimeFormatter TIMESTAMP_FMT =
            DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");

    // Public-facing Voter ID format shown on the vote-recorded page.
    // Built from the internal DB id — NOT stored anywhere, computed
    // fresh each time so there's no schema change required.
    private static final String VOTER_ID_FMT = "TN-VOTER-%06d";

    // ── Cast Vote ─────────────────────────────────────────────

    @PostMapping("/cast")
    @Transactional
    public String castVote(@Valid @ModelAttribute VotePayLoad payload,
                           HttpSession session,
                           Model model) {

        Long voterId = (Long) session.getAttribute("voterId");
        if (voterId == null) return "redirect:/voter/login";

        VoteTrack track;

        try {
            // Step 1: atomic claim — replaces the old hasVoted()-then-write
            // race. If this returns false, either the voter already voted
            // earlier, or lost a concurrent race to another request — either
            // way, no vote is recorded here.
            if (!voterService.claimVoteSlot(voterId)) {
                return "redirect:/voter/already-voted";
            }

            // Step 2: record vote (no voterId persisted — see VoteTrack)
            track = voteTrackService.recordVote(voterId, payload.getCandidateId());

            // Step 3: increment candidate vote count
            candidateService.incrementVoteCount(payload.getCandidateId());

        } catch (Exception e) {
            log.error("Vote transaction failed for voter ID: {}", voterId, e);
            model.addAttribute("error",
                    "An error occurred while casting your vote. Please try again.");
            model.addAttribute("voter",      voterService.getVoterById(voterId));
            model.addAttribute("candidates", candidateService.getAllCandidates());
            return "vote";
        }

        // ── Post-transaction (outside @Transactional) ──

        try {
            String blockData = String.format(
                    "{\"voteHash\":\"%s\",\"candidateId\":%d,\"votedAt\":\"%s\"}",
                    track.getVoteHash(),
                    payload.getCandidateId(),
                    track.getVotedAt()
            );
            blockChainService.addBlock(blockData);
        } catch (Exception e) {
            log.error("Blockchain addBlock failed for hash: {} — vote still recorded",
                    track.getVoteHash(), e);
        }

        try {
            sseStreamService.broadcast(
                    "vote-update",
                    "{\"candidateId\":" + payload.getCandidateId() + "}"
            );
        } catch (Exception e) {
            log.warn("SSE broadcast failed after vote by voter ID: {}", voterId, e);
        }

        // Receipt lives ONLY in this session from now on — there is no DB
        // lookup path from voterId back to this vote. This is the deliberate
        // trade-off for real ballot secrecy: once the session ends, the
        // specific receipt is not recoverable by anyone, including the voter
        // re-logging in later. "You voted" (Voter.hasVoted) is still visible;
        // "what you voted for" is not, by design.
        session.setAttribute("voteReceiptHash", track.getVoteHash());
        session.setAttribute("voteReceiptAt",   track.getVotedAt());

        model.addAttribute("receiptHash", track.getVoteHash());
        model.addAttribute("voterId",     String.format(VOTER_ID_FMT, voterId));
        model.addAttribute("timestamp",   LocalDateTime.now().format(TIMESTAMP_FMT));
        return "vote-recorded";
    }

    // ── Track Vote by Hash ────────────────────────────────────

    @GetMapping("/track-vote")
    public String trackVote(@RequestParam(required = false) String hash, Model model) {

        if (hash == null || hash.isBlank()) {
            model.addAttribute("message", "Please provide a vote hash to search.");
            return "track-vote";  // → templates/track-vote.html
        }

        try {
            VoteTrack vote = voteTrackService.findByHash(hash.trim());
            model.addAttribute("vote", vote);
        } catch (ResourceNotFoundException e) {
            model.addAttribute("message",
                    "No vote found for hash: " + hash + ". Please check for typos.");
        }

        return "track-vote";
    }
}