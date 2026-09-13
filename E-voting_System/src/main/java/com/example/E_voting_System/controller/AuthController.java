package com.example.E_voting_System.controller;

import com.example.E_voting_System.entity.Voter;
import com.example.E_voting_System.repository.VoterRepository;
import com.example.E_voting_System.service.HashService;
import com.example.E_voting_System.service.OtpService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Slf4j
@Controller
@RequestMapping("/voter")
@RequiredArgsConstructor
public class AuthController {

    private final VoterRepository voterRepository;
    private final HashService hashService;
    private final OtpService otpService;

    /**
     * Display login page
     */
    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

    /**
     * Handle voter login - Send OTP
     * ✅ Validates voter exists before sending OTP
     */
    @PostMapping("/login")
    public String login(@RequestParam(required = false) String aadhaar,
                        @RequestParam(required = false) String mobile,
                        HttpSession session,
                        Model model) {

        // Validate input
        if (aadhaar == null || aadhaar.trim().isEmpty()) {
            log.warn("⚠️ Login attempt with empty Aadhaar");
            model.addAttribute("error", "Please enter your Aadhaar number");
            return "login";
        }

        if (mobile == null || mobile.trim().isEmpty()) {
            log.warn("⚠️ Login attempt with empty mobile number");
            model.addAttribute("error", "Please enter your mobile number");
            return "login";
        }

        // Validate format
        if (!aadhaar.matches("\\d{12}")) {
            log.warn("⚠️ Invalid Aadhaar format: {}", aadhaar);
            model.addAttribute("error", "Aadhaar must be 12 digits");
            return "login";
        }

        if (!mobile.matches("\\d{10}")) {
            log.warn("⚠️ Invalid mobile format: {}", mobile);
            model.addAttribute("error", "Mobile number must be 10 digits");
            return "login";
        }

        try {
            String aadhaarHash = hashService.sha256Hex(aadhaar);
            Voter voter = voterRepository.findByAadhaarHash(aadhaarHash).orElse(null);

            if (voter == null) {
                log.warn("⚠️ Voter not found - Aadhaar hash: {}", aadhaarHash.substring(0, 8) + "...");
                model.addAttribute("error", "Voter not registered in the system");
                return "login";
            }

            // Check if already voted
            if (voter.isHasVoted()) {
                log.warn("⚠️ Already voted - Voter ID: {}", voter.getId());
                model.addAttribute("error", "This voter has already cast their vote");
                return "login";
            }

            // Generate and send OTP
            otpService.generateOtp(mobile, voter.getId());

            // Store data in session
            session.setAttribute("aadhaarHash", aadhaarHash);
            session.setAttribute("voterId", voter.getId());

            log.info("✓ OTP generated and sent - Voter ID: {}, Mobile: {}***",
                    voter.getId(), mobile.substring(0, 6));

            model.addAttribute("mobile", mobile);
            return "otp";

        } catch (Exception e) {
            log.error("❌ Error during login: {}", e.getMessage(), e);
            model.addAttribute("error", "An error occurred. Please try again later.");
            return "login";
        }
    }

    /**
     * Handle OTP verification
     * ✅ Validates OTP and creates user session
     */
    @PostMapping("/verify")
    public String verify(@RequestParam(required = false) String mobile,
                         @RequestParam(required = false) String otp,
                         HttpSession session,
                         Model model) {

        // Validate input
        if (mobile == null || mobile.trim().isEmpty()) {
            log.warn("⚠️ OTP verification attempted without mobile number");
            model.addAttribute("error", "Session expired. Please login again.");
            return "redirect:/voter/login";
        }

        if (otp == null || otp.trim().isEmpty()) {
            log.warn("⚠️ OTP verification with empty OTP");
            model.addAttribute("error", "Please enter the OTP");
            model.addAttribute("mobile", mobile);
            return "otp";
        }

        // Validate OTP format
        if (!otp.matches("\\d{6}")) {
            log.warn("⚠️ Invalid OTP format");
            model.addAttribute("error", "OTP must be 6 digits");
            model.addAttribute("mobile", mobile);
            return "otp";
        }

        try {
            // Verify OTP
            boolean isValid = otpService.verifyOtp(mobile, otp);

            if (!isValid) {
                log.warn("⚠️ Invalid OTP - Mobile: {}***", mobile.substring(0, 6));
                model.addAttribute("error", "Invalid OTP. Please try again.");
                model.addAttribute("mobile", mobile);
                return "otp";
            }

            // Get voter ID
            Long voterId = otpService.getVoterId(mobile);

            if (voterId == null) {
                log.warn("⚠️ Voter ID not found after OTP verification");
                model.addAttribute("error", "Session expired. Please login again.");
                return "redirect:/voter/login";
            }

            // Set session
            session.setAttribute("voterId", voterId);

            // Clear OTP
            otpService.clearOtp(mobile);

            log.info("✓ OTP verified successfully - Voter ID: {}", voterId);

            return "redirect:/voter";

        } catch (Exception e) {
            log.error("❌ Error during OTP verification: {}", e.getMessage(), e);
            model.addAttribute("error", "An error occurred. Please try again.");
            model.addAttribute("mobile", mobile);
            return "otp";
        }
    }

    /**
     * Logout endpoint
     */
    @GetMapping("/logout")
    public String logout(HttpSession session) {
        Long voterId = (Long) session.getAttribute("voterId");
        session.invalidate();
        log.info("✓ Logout - Voter ID: {}", voterId);
        return "redirect:/voter/login";
    }
}