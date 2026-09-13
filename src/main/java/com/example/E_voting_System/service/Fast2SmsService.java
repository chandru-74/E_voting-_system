package com.example.E_voting_System.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import jakarta.annotation.PostConstruct;
import java.util.logging.Logger;

/**
 * Fast2SMS — OTP Route integration.
 * Sends OTP SMS to Indian numbers without requiring DLT template
 * pre-registration, unlike general-purpose providers such as Twilio.
 */
@Service
public class Fast2SmsService {

    private static final Logger log = Logger.getLogger(Fast2SmsService.class.getName());
    private static final String API_URL = "https://www.fast2sms.com/dev/bulkV2";

    @Value("${fast2sms.api-key:}")
    private String apiKey;

    @Value("${fast2sms.enabled:false}")
    private boolean enabled;

    private final RestTemplate restTemplate = new RestTemplate();

    @PostConstruct
    public void init() {
        if (enabled && !apiKey.isBlank()) {
            log.info("Fast2SMS OTP service initialized");
        } else {
            log.info("Fast2SMS disabled");
        }
    }

    public boolean sendOtp(String mobile, String otp) {
        if (!enabled || apiKey.isBlank()) {
            log.info("[Fast2SMS disabled] OTP for " + maskMobile(mobile) + ": " + otp);
            return false;
        }

        try {
            String cleanMobile = mobile.replaceFirst("^\\+91", "");

            String url = UriComponentsBuilder.fromHttpUrl(API_URL)
                    .queryParam("variables_values", otp)
                    .queryParam("route", "otp")
                    .queryParam("numbers", cleanMobile)
                    .toUriString();

            HttpHeaders headers = new HttpHeaders();
            headers.set("authorization", apiKey);
            headers.set("Content-Type", "application/x-www-form-urlencoded");

            HttpEntity<Void> request = new HttpEntity<>(headers);
            ResponseEntity<String> response =
                    restTemplate.exchange(url, HttpMethod.GET, request, String.class);

            boolean success = response.getStatusCode().is2xxSuccessful();
            log.info("Fast2SMS OTP send to " + maskMobile(mobile)
                    + " | HTTP " + response.getStatusCode() + " | success=" + success);
            return success;

        } catch (Exception e) {
            log.severe("Fast2SMS error for " + maskMobile(mobile) + ": " + e.getMessage());
            return false;
        }
    }

    public boolean isFast2SmsEnabled() {
        return enabled && !apiKey.isBlank();
    }

    private String maskMobile(String mobile) {
        if (mobile == null || mobile.length() < 4) return "****";
        return "******" + mobile.substring(mobile.length() - 4);
    }
}