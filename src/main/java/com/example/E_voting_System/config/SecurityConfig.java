package com.example.E_voting_System.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.XorCsrfTokenRequestAttributeHandler;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/",
                                "/home",
                                "/evoting",
                                "/instructions",
                                "/instructions/candidate",
                                "/instructions/voter",
                                "/voter/login",
                                "/voter/verify",
                                "/voter/verify-otp",
                                "/voter/resend-otp",
                                "/voter/register",
                                "/voter/verify-aadhaar",
                                "/candidate/register",
                                "/css/**",
                                "/js/**",
                                "/webjars/**",
                                "/images/**",
                                "/static/**",
                                "/error",
                                "/election-results",
                                "/election-results/**",
                                "/live-results",
                                "/final-results",
                                "/candidates"
                        ).permitAll()

                        .requestMatchers("/admin/**").permitAll()

                        .requestMatchers("/api/blockchain/**", "/api/stream/**").permitAll()
                        .requestMatchers("/api/results/publish", "/api/results/published",
                                "/api/results/reset", "/api/results/full-reset").permitAll()

                        .requestMatchers(
                                "/voter",
                                "/voter/already-voted",
                                "/voter/results",
                                "/api/vote/**",
                                "/api/results"
                        ).permitAll()

                        .anyRequest().authenticated()
                )

                .formLogin(form -> form.disable())

                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) ->
                                response.sendRedirect("/voter/login"))
                )

                .logout(logout -> logout
                        .logoutUrl("/voter/logout")
                        .logoutSuccessUrl("/voter/login?logout=true")
                        .invalidateHttpSession(true)
                        .clearAuthentication(true)
                        .permitAll()
                )

                .csrf(csrf -> csrf
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        .csrfTokenRequestHandler(new XorCsrfTokenRequestAttributeHandler())
                        .ignoringRequestMatchers(
                                "/api/stream/**",
                                "/api/vote/**",
                                "/api/results/**",
                                "/api/blockchain/**",
                                "/voter/login",
                                "/voter/register",
                                "/voter/verify",
                                "/voter/verify-otp",
                                "/voter/resend-otp",
                                "/candidate/register"
                        )
                )

                .addFilterAfter(new CsrfCookieFilter(), BasicAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}