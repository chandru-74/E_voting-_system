package com.example.E_voting_System.controller;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import com.example.E_voting_System.dto.LoginRequest;
import com.example.E_voting_System.exception.VoterAlreadyVotedException;
import com.example.E_voting_System.exception.VoterNotFoundException;
import com.example.E_voting_System.service.VoterAuthService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.core.env.Environment;

@Slf4j
@Controller
@RequestMapping("/voter")
@RequiredArgsConstructor
public class AuthController {

    private final VoterAuthService voterAuthService;
    private final Environment environment;

    private static final int    MAX_OTP_REQUESTS   = 3;
    private static final long   OTP_WINDOW_SECONDS = 10 * 60L;
    private static final String OTP_PATTERN        = "\\d{6}";

    private final ConcurrentHashMap<String, long[]> otpRequestTracker = new ConcurrentHashMap<>();

    // ── Phase 1: Show login form ──────────────────────────────

    @GetMapping("/login")
    public String loginPage(Model model) {
        model.addAttribute("loginRequest", new LoginRequest());
        model.addAttribute("isDev", environment.acceptsProfiles(org.springframework.core.env.Profiles.of("dev")));
        return "login";
    }

    // ── Phase 1: Submit Aadhaar + Mobile → Send OTP ──────────

    @PostMapping("/login")
    public String login(@Valid @ModelAttribute("loginRequest") LoginRequest loginRequest,
                        BindingResult bindingResult,
                        HttpSession session,
                        Model model) {

        if (bindingResult.hasErrors()) {
            return "login";
        }

        String mobile = loginRequest.getMobile();

        if (isOtpRateLimited(mobile)) {
            model.addAttribute("loginRequest", loginRequest);
            model.addAttribute("error",
                    "Too many OTP requests for this number. Please wait 10 minutes.");
            return "login";
        }

        try {
            Long pendingVoterId = voterAuthService.initiateLogin(
                    loginRequest.getAadhaar(), mobile);

            session.setAttribute("pendingVoterId", pendingVoterId);
            session.setAttribute("pendingMobile",  mobile);
            recordOtpRequest(mobile);

            model.addAttribute("mobile", mobile);
            return "otp";

        } catch (VoterNotFoundException e) {
            model.addAttribute("loginRequest", loginRequest);
            model.addAttribute("error", e.getMessage());
            return "login";

        } catch (VoterAlreadyVotedException e) {
            // CHANGED: show a red alert directly on the login page instead
            // of redirecting to a separate already-voted details page.
            model.addAttribute("loginRequest", loginRequest);
            model.addAttribute("error",
                    "🗳️ You have already cast your vote. Each voter is allowed to vote only once.");
            return "login";

        } catch (Exception e) {
            log.error("Unexpected error during login", e);
            model.addAttribute("loginRequest", loginRequest);
            model.addAttribute("error", "An error occurred. Please try again.");
            return "login";
        }
    }

    // ── Phase 2: Submit OTP → Authenticate ───────────────────

    @PostMapping({"/verify", "/verify-otp"})
    public String verify(@RequestParam(required = false) String otp,
                         HttpSession session,
                         HttpServletRequest request,
                         Model model) {

        String mobile         = (String) session.getAttribute("pendingMobile");
        Long   pendingVoterId = (Long)   session.getAttribute("pendingVoterId");

        if (mobile == null || pendingVoterId == null) {
            log.warn("OTP verify attempted without a pending session");
            return "redirect:/voter/login";
        }

        if (otp == null || otp.isBlank()) {
            model.addAttribute("error",  "Please enter the OTP.");
            model.addAttribute("mobile", mobile);
            return "otp";
        }

        if (!otp.matches(OTP_PATTERN)) {
            model.addAttribute("error",  "OTP must be exactly 6 digits.");
            model.addAttribute("mobile", mobile);
            return "otp";
        }

        try {
            Long verifiedVoterId = voterAuthService.verifyOtp(mobile, otp);

            var authorities = List.of(new SimpleGrantedAuthority("ROLE_VOTER"));
            var auth = new UsernamePasswordAuthenticationToken(
                    verifiedVoterId.toString(), null, authorities);
            SecurityContextHolder.getContext().setAuthentication(auth);
            request.getSession(true).setAttribute(
                    HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                    SecurityContextHolder.getContext());

            session.removeAttribute("pendingVoterId");
            session.removeAttribute("pendingMobile");
            session.setAttribute("voterId", verifiedVoterId);
            otpRequestTracker.remove(mobile);

            return "redirect:/voter";

        } catch (IllegalArgumentException e) {
            model.addAttribute("error",  e.getMessage());
            model.addAttribute("mobile", mobile);
            return "otp";

        } catch (IllegalStateException e) {
            model.addAttribute("error", e.getMessage());
            return "redirect:/voter/login";

        } catch (Exception e) {
            log.error("Unexpected error during OTP verification", e);
            model.addAttribute("error",  "An error occurred. Please try again.");
            model.addAttribute("mobile", mobile);
            return "otp";
        }
    }

    // ── Rate Limiting Helpers ─────────────────────────────────

    private boolean isOtpRateLimited(String mobile) {
        long[] tracker = otpRequestTracker.get(mobile);
        if (tracker == null) return false;
        long now = Instant.now().getEpochSecond();
        if ((now - tracker[1]) > OTP_WINDOW_SECONDS) {
            otpRequestTracker.remove(mobile);
            return false;
        }
        return tracker[0] >= MAX_OTP_REQUESTS;
    }

    private void recordOtpRequest(String mobile) {
        long now = Instant.now().getEpochSecond();
        otpRequestTracker.compute(mobile, (key, existing) -> {
            if (existing == null) return new long[]{1L, now};
            existing[0]++;
            return existing;
        });
    }
    // ── Resend OTP ────────────────────────────────────────────

    @PostMapping("/resend-otp")
    public String resendOtp(HttpSession session, Model model) {

        String mobile         = (String) session.getAttribute("pendingMobile");
        Long   pendingVoterId = (Long)   session.getAttribute("pendingVoterId");

        if (mobile == null || pendingVoterId == null) {
            log.warn("Resend OTP attempted without a pending session");
            return "redirect:/voter/login";
        }

        if (isOtpRateLimited(mobile)) {
            model.addAttribute("error",
                    "Too many OTP requests for this number. Please wait 10 minutes.");
            model.addAttribute("mobile", mobile);
            return "otp";
        }

        try {
            voterAuthService.resendOtp(mobile, pendingVoterId);
            recordOtpRequest(mobile);
            model.addAttribute("success", "A new OTP has been sent.");
            model.addAttribute("mobile", mobile);
            return "otp";

        } catch (Exception e) {
            log.error("Error resending OTP for voter ID: {}", pendingVoterId, e);
            model.addAttribute("error", "Could not resend OTP. Please try again.");
            model.addAttribute("mobile", mobile);
            return "otp";
        }
    }
}