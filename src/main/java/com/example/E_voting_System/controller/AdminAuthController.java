package com.example.E_voting_System.controller;

import com.example.E_voting_System.blockchain.Block;
import com.example.E_voting_System.blockchain.BlockChainService;
import com.example.E_voting_System.entity.VotingWindow;
import com.example.E_voting_System.service.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminAuthController {

    private final VoterService          voterService;
    private final AdminService          adminService;
    private final EmailOtpService       emailOtpService;
    private final VotingWindowService   votingWindowService;
    private final ElectionResultService electionResultService;
    private final BlockChainService     blockChainService;
    private final ElectionSetupService  electionSetupService;

    private static final int  MAX_ATTEMPTS    = 5;
    private static final long LOCKOUT_MINUTES = 15L;
    private static final int  MAX_FIELD_LEN   = 100;

    private final ConcurrentHashMap<String, Integer> loginAttempts = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler =
            Executors.newSingleThreadScheduledExecutor();

    // ── Voting Window ─────────────────────────────────────────

    @PostMapping("/voting-window")
    public String setVotingWindow(
            @RequestParam String startTime,
            @RequestParam String endTime,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        if (!isAdminLoggedIn(session)) return "redirect:/admin/login";

        if (!electionSetupService.isCandidatesLocked()) {
            redirectAttributes.addFlashAttribute("error",
                    "Lock the candidate setup first — the ballot must be finalized " +
                            "before you can open the voting booth.");
            return "redirect:/admin/dashboard";
        }

        VotingWindow window = new VotingWindow();
        window.setStartTime(LocalDateTime.parse(startTime,
                DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm")));
        window.setEndTime(LocalDateTime.parse(endTime,
                DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm")));
        window.setActive(true);

        votingWindowService.deactivateAll();
        votingWindowService.save(window);

        redirectAttributes.addFlashAttribute("success", "Voting window set successfully.");
        return "redirect:/admin/dashboard";
    }

    // ── Candidate Setup Lock ──────────────────────────────────

    @PostMapping("/lock-candidates")
    public String lockCandidates(HttpSession session, RedirectAttributes ra) {
        if (!isAdminLoggedIn(session)) return "redirect:/admin/login";
        electionSetupService.lockCandidates();
        ra.addFlashAttribute("success",
                "Candidate setup locked. The final candidate list is now public, " +
                        "and no further nominations, edits, or approvals are allowed.");
        return "redirect:/admin/dashboard";
    }

    @PostMapping("/unlock-candidates")
    public String unlockCandidates(HttpSession session, RedirectAttributes ra) {
        if (!isAdminLoggedIn(session)) return "redirect:/admin/login";
        electionSetupService.unlockCandidates();
        ra.addFlashAttribute("success",
                "Candidate setup unlocked. Nominations and edits are allowed again.");
        return "redirect:/admin/dashboard";
    }

    // ── Publish Final Results ─────────────────────────────────

    @PostMapping("/publish-results")
    public String publishResults(HttpSession session, RedirectAttributes ra) {
        if (!isAdminLoggedIn(session)) return "redirect:/admin/login";

        try {
            electionResultService.publishResults();
            ra.addFlashAttribute("success", "Final results have been published.");
        } catch (IllegalStateException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/dashboard";
    }

    // ── Login ─────────────────────────────────────────────────

    @GetMapping("/login")
    public String loginPage(HttpSession session, Model model) {
        if (isAdminLoggedIn(session)) {
            model.addAttribute("alreadyLoggedIn", true);
            model.addAttribute("adminName", session.getAttribute("adminUsername"));
        }
        return "admin/admin-login";
    }

    @PostMapping("/login")
    public String adminLogin(@RequestParam String username,
                             @RequestParam String password,
                             HttpSession session,
                             RedirectAttributes ra,
                             HttpServletRequest request) {

        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isBlank()) ip = request.getRemoteAddr();
        final String resolvedIp = ip;

        int attempts = loginAttempts.getOrDefault(resolvedIp, 0);
        if (attempts >= MAX_ATTEMPTS) {
            ra.addFlashAttribute("error",
                    "Too many failed attempts. Please wait " + LOCKOUT_MINUTES + " minutes.");
            return "redirect:/admin/login";
        }

        if (adminService.authenticate(username.trim(), password)) {
            loginAttempts.remove(resolvedIp);

            request.changeSessionId();

            session.setAttribute("adminLoggedIn", true);
            session.setAttribute("adminUsername", username.trim());
            session.setMaxInactiveInterval(30 * 60);
            return "redirect:/admin/dashboard";
        }

        loginAttempts.merge(resolvedIp, 1, Integer::sum);
        scheduler.schedule(() -> loginAttempts.remove(resolvedIp),
                LOCKOUT_MINUTES, TimeUnit.MINUTES);

        int remaining = MAX_ATTEMPTS - loginAttempts.getOrDefault(resolvedIp, 0);
        ra.addFlashAttribute("error",
                "Invalid credentials. " + Math.max(remaining, 0) + " attempts remaining.");
        return "redirect:/admin/login";
    }

    // ── Dashboard ─────────────────────────────────────────────

    @GetMapping("/dashboard")
    public String dashboard(HttpSession session, Model model) {
        if (!isAdminLoggedIn(session)) return "redirect:/admin/login";

        model.addAttribute("adminName",         session.getAttribute("adminUsername"));
        model.addAttribute("totalVotes",        adminService.getTotalVoteCount());
        model.addAttribute("totalCandidates",   adminService.getCandidateCount());
        model.addAttribute("totalVoters",       adminService.getRegisteredVoterCount());
        model.addAttribute("pendingCandidates", adminService.getPendingCandidateCount());
        model.addAttribute("pendingVoters",     voterService.countPendingVoters());
        model.addAttribute("resultsPublished",  electionResultService.resultsPublished());

        adminService.isChainValidAsync();

        Optional<VotingWindow> window = votingWindowService.getActiveWindow();
        model.addAttribute("votingWindow", window.orElse(null));
        model.addAttribute("votingOpen",   votingWindowService.isVotingOpen());

        model.addAttribute("candidatesLocked",   electionSetupService.isCandidatesLocked());
        model.addAttribute("candidatesLockedAt", electionSetupService.getLockedAt());

        return "admin/admin-dashboard";
    }

    // ── Add Ballot Card ───────────────────────────────────────

    @GetMapping("/add-candidate")
    public String addCandidateForm(HttpSession session) {
        if (!isAdminLoggedIn(session)) return "redirect:/admin/login";
        return "admin/add-candidate";
    }

    @PostMapping("/add-candidate")
    public String submitCandidate(
            @RequestParam(required = false, defaultValue = "") String partyName,
            @RequestParam(required = false, defaultValue = "") String partySymbol,
            @RequestParam(required = false, defaultValue = "") String wardName,
            HttpSession session,
            RedirectAttributes ra) {

        if (!isAdminLoggedIn(session)) return "redirect:/admin/login";

        if (electionSetupService.isCandidatesLocked()) {
            ra.addFlashAttribute("error", "Candidate setup is locked. Unlock it from the dashboard first.");
            return "redirect:/admin/dashboard";
        }

        String party  = partyName.trim();
        String symbol = partySymbol.trim();
        String ward   = wardName.trim();

        if (party.isBlank() || symbol.isBlank() || ward.isBlank()) {
            ra.addFlashAttribute("error", "Party name, symbol and ward are all required.");
            return "redirect:/admin/add-candidate";
        }
        if (party.length() > MAX_FIELD_LEN || ward.length() > MAX_FIELD_LEN) {
            ra.addFlashAttribute("error", "Input too long. Max 100 characters per field.");
            return "redirect:/admin/add-candidate";
        }

        boolean added = adminService.registerCandidateRequest(party, symbol, ward);
        if (!added) {
            ra.addFlashAttribute("error",
                    "'" + party + "' is already registered in ward '" + ward + "'.");
            return "redirect:/admin/add-candidate";
        }

        ra.addFlashAttribute("success",
                "Party '" + party + "' added to ward '" + ward + "' ballot successfully.");
        return "redirect:/admin/dashboard";
    }

    // ── Verify Candidate (legacy dead-end) ────────────────────

    @GetMapping("/verify-candidate")
    public String verifyCandidatePage(HttpSession session) {
        if (!isAdminLoggedIn(session)) return "redirect:/admin/login";
        return "redirect:/admin/add-candidate";
    }

    @PostMapping("/verify-candidate")
    public String confirmCandidate(@RequestParam String otp,
                                   HttpSession session,
                                   RedirectAttributes ra) {
        if (!isAdminLoggedIn(session)) return "redirect:/admin/login";

        String   email     = (String)   session.getAttribute("verifierEmail");
        String[] candidate = (String[]) session.getAttribute("pendingCandidate");

        if (candidate == null) {
            ra.addFlashAttribute("error", "Session expired. Please start again.");
            return "redirect:/admin/add-candidate";
        }

        boolean otpValid = (email == null) || emailOtpService.verifyOtp(email, otp.trim());
        if (!otpValid) {
            ra.addFlashAttribute("error", "Invalid or expired OTP.");
            return "redirect:/admin/verify-candidate";
        }

        boolean added = adminService.registerCandidateRequest(
                candidate[0], candidate[1], candidate[2]);

        session.removeAttribute("pendingCandidate");
        session.removeAttribute("verifierEmail");

        if (!added) {
            ra.addFlashAttribute("error",
                    "'" + candidate[0] + "' is already registered in ward '" + candidate[2] + "'.");
            return "redirect:/admin/add-candidate";
        }

        ra.addFlashAttribute("success",
                "Party '" + candidate[0] + "' added to ward '" + candidate[2] + "' ballot.");
        return "redirect:/admin/dashboard";
    }

    // ── Blockchain Audit ───────────────────────────────────────

    @GetMapping("/audit")
    public String auditChain(HttpSession session, Model model) {
        if (!isAdminLoggedIn(session)) return "redirect:/admin/login";

        boolean valid = adminService.isChainValid();
        model.addAttribute("chainValid",   valid);
        model.addAttribute("auditMessage", valid
                ? "All vote records are intact. No tampering detected."
                : "Chain integrity FAILED. One or more records may have been tampered with.");

        List<Block> chain = blockChainService.getChain();
        model.addAttribute("blockCount", chain.size());

        if (!chain.isEmpty()) {
            Block latest = chain.get(chain.size() - 1);
            model.addAttribute("latestBlockIndex", latest.getBlockIndex());
            model.addAttribute("latestBlockHash", latest.getHash());
            model.addAttribute("latestBlockTimestamp", latest.getTimestamp());
        }

        return "admin/audit";
    }

    // ── Voter Management ──────────────────────────────────────

    @GetMapping("/voters")
    public String voterManagement(HttpSession session, Model model) {
        if (!isAdminLoggedIn(session)) return "redirect:/admin/login";

        model.addAttribute("pendingVoters",  voterService.getPendingVoters());
        model.addAttribute("approvedVoters", voterService.getApprovedVoters());
        model.addAttribute("rejectedVoters", voterService.getRejectedVoters());
        return "admin/voter-management";
    }

    @PostMapping("/voters/{id}/approve")
    public String manualApprove(@PathVariable Long id,
                                HttpSession session,
                                RedirectAttributes attrs) {
        if (!isAdminLoggedIn(session)) return "redirect:/admin/login";
        voterService.approveVoter(id);
        attrs.addFlashAttribute("success", "Voter manually approved.");
        return "redirect:/admin/voters";
    }

    @PostMapping("/voters/{id}/reject")
    public String manualReject(@PathVariable Long id,
                               @RequestParam String reason,
                               HttpSession session,
                               RedirectAttributes attrs) {
        if (!isAdminLoggedIn(session)) return "redirect:/admin/login";
        voterService.rejectVoter(id, reason);
        attrs.addFlashAttribute("success", "Voter rejected: " + reason);
        return "redirect:/admin/voters";
    }

    // ── Pending Candidates ────────────────────────────────────

    @GetMapping("/candidates/pending")
    public String pendingCandidates(HttpSession session, Model model) {
        if (!isAdminLoggedIn(session)) return "redirect:/admin/login";
        model.addAttribute("candidates", adminService.getPendingCandidates());
        return "admin/pending-candidates";
    }

    @PostMapping("/candidates/approve/{id}")
    public String approveCandidate(@PathVariable Long id,
                                   HttpSession session,
                                   RedirectAttributes ra) {
        if (!isAdminLoggedIn(session)) return "redirect:/admin/login";

        if (electionSetupService.isCandidatesLocked()) {
            ra.addFlashAttribute("error", "Candidate setup is locked. Unlock it from the dashboard first.");
            return "redirect:/admin/dashboard";
        }

        adminService.approveCandidate(id);
        ra.addFlashAttribute("success", "Ballot card approved.");
        return "redirect:/admin/candidates/pending";
    }

    @PostMapping("/candidates/reject/{id}")
    public String rejectCandidate(@PathVariable Long id,
                                  HttpSession session,
                                  RedirectAttributes ra) {
        if (!isAdminLoggedIn(session)) return "redirect:/admin/login";

        if (electionSetupService.isCandidatesLocked()) {
            ra.addFlashAttribute("error", "Candidate setup is locked. Unlock it from the dashboard first.");
            return "redirect:/admin/dashboard";
        }

        adminService.rejectCandidate(id);
        ra.addFlashAttribute("success", "Ballot card rejected.");
        return "redirect:/admin/candidates/pending";
    }

    // ── Logout ────────────────────────────────────────────────

    @PostMapping("/logout")
    public String adminLogout(HttpSession session) {
        session.invalidate();
        return "redirect:/admin/login?logout=true";
    }

    // ── Session guard ─────────────────────────────────────────

    private boolean isAdminLoggedIn(HttpSession session) {
        return Boolean.TRUE.equals(session.getAttribute("adminLoggedIn"));
    }
}