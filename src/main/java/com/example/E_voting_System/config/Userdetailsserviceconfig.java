package com.example.E_voting_System.config;

import com.example.E_voting_System.entity.Voter;
import com.example.E_voting_System.repository.VoterRepository;
import com.example.E_voting_System.service.HashService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.List;

/**
 * Loads voter by Aadhaar hash so Spring Security can authenticate them.
 * The "username" passed in formLogin is the raw Aadhaar — we hash it here
 * before looking up the DB, so plain Aadhaar is never stored.
 */
@Configuration
@RequiredArgsConstructor
public class Userdetailsserviceconfig {

    private final VoterRepository voterRepository;
    private final HashService     hashService;

    @Bean
    public UserDetailsService userDetailsService() {
        return (aadhaar) -> {
            String hash = hashService.sha256Hex(aadhaar);
            Voter voter = voterRepository.findByAadhaarHash(hash)
                    .orElseThrow(() ->
                            new UsernameNotFoundException("Voter not found"));

            return User.builder()
                    .username(String.valueOf(voter.getId()))
                    .password("{noop}OTP_HANDLED_SEPARATELY") // password check skipped — OTP flow
                    .authorities(List.of(new SimpleGrantedAuthority("ROLE_VOTER")))
                    .build();
        };
    }
}