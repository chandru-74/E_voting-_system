package com.example.E_voting_System.service;

import com.twilio.Twilio;
import com.twilio.rest.verify.v2.service.Verification;
import com.twilio.rest.verify.v2.service.VerificationCheck;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.logging.Logger;

/** [rescha@974,rcdo xlbp gpmq ijde, b30127d39e2d880a681e0d1d46e3a200 ]

 * Twilio Verify — dedicated OTP service (no phone number purchase needed).
 *
 * Setup (3 minutes):
 *   1. Twilio Console → left sidebar → "Verify" → "Services"
 *   2. Click "Create new Service" → name it "E-Voting OTP" → Create
 *   3. Copy the Service SID  (starts with "VA...")
 *   4. From Console home copy Account SID (AC...) and Auth Token
 *   5. Add verified test numbers:
 *      Console → Phone Numbers → Verified Caller IDs → Add your mobile
 *   6. Add to application.properties:
 *        twilio.account-sid=ACxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
 *        twilio.auth-token=your_auth_token_here
 *        twilio.verify-service-sid=VAxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
 *        twilio.enabled=true
 */
@Service
public class SmsService {

    private static final Logger log = Logger.getLogger(SmsService.class.getName());

    @Value("${twilio.account-sid:}")
    private String accountSid;

    @Value("${twilio.auth-token:}")
    private String authToken;

    @Value("${twilio.verify-service-sid:}")
    private String verifyServiceSid;

    @Value("${twilio.enabled:false}")
    private boolean enabled;

    @PostConstruct
    public void init() {
        if (enabled && !accountSid.isBlank() && !authToken.isBlank()) {
            Twilio.init(accountSid, authToken);
            log.info("Twilio Verify SMS service initialized");
        } else {
            log.info("Twilio disabled — OTP via email + console only");
        }
    }

    /**
     * Send OTP via Twilio Verify.
     * Twilio generates and sends the OTP itself — we don't pass the OTP code.
     * Returns true if dispatch succeeded.
     */
    public boolean sendOtp(String mobile, String otp) {
        if (!enabled || accountSid.isBlank() || verifyServiceSid.isBlank()) {
            log.info("[SMS disabled] OTP for " + maskMobile(mobile) + ": " + otp);
            return false;
        }

        try {
            String toNumber = mobile.startsWith("+") ? mobile : "+91" + mobile;

            Verification verification = Verification.creator(
                    verifyServiceSid,
                    toNumber,
                    "sms"   // channel: "sms" or "call" or "whatsapp"
            ).create();

            log.info("Twilio Verify OTP sent to " + maskMobile(mobile)
                    + " | Status: " + verification.getStatus());
            return true;

        } catch (Exception e) {
            log.severe("Twilio Verify error for " + maskMobile(mobile) + ": " + e.getMessage());
            return false;
        }
    }

    /**
     * Verify the OTP entered by the user against Twilio Verify.
     * Call this from VoterAuthService instead of your in-memory OTP store
     * when Twilio is enabled.
     */
    public boolean verifyCode(String mobile, String code) {
        if (!enabled || verifyServiceSid.isBlank()) return false;

        try {
            String toNumber = mobile.startsWith("+") ? mobile : "+91" + mobile;

            VerificationCheck check = VerificationCheck.creator(verifyServiceSid)
                    .setTo(toNumber)
                    .setCode(code)
                    .create();

            boolean approved = "approved".equals(check.getStatus());
            log.info("Twilio Verify check for " + maskMobile(mobile)
                    + " → " + check.getStatus());
            return approved;

        } catch (Exception e) {
            log.severe("Twilio Verify check error: " + e.getMessage());
            return false;
        }
    }

    public boolean isTwilioEnabled() {
        return enabled && !verifyServiceSid.isBlank();
    }

    private String maskMobile(String mobile) {
        if (mobile == null || mobile.length() < 4) return "****";
        return "******" + mobile.substring(mobile.length() - 4);
    }
}