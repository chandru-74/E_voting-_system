package com.example.E_voting_System.controller;

import com.example.E_voting_System.dto.CandidateRegisterRequest;
import com.example.E_voting_System.service.CandidateService;
import com.example.E_voting_System.service.ElectionSetupService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@Slf4j
@Controller
@RequestMapping("/candidate")
@RequiredArgsConstructor
public class CandidateRegisterController {

    private final CandidateService candidateService;
    private final ElectionSetupService electionSetupService;

    @GetMapping("/register")
    public String registerForm(Model model) {
        if (!model.containsAttribute("request")) {
            model.addAttribute("request", new CandidateRegisterRequest());
        }
        return "candidate-register";
    }

    @PostMapping("/register")
    public String register(@Valid @ModelAttribute("request") CandidateRegisterRequest request,
                           BindingResult bindingResult, Model model) {

        if (bindingResult.hasErrors()) {
            return "candidate-register";
        }

        if (electionSetupService.isCandidatesLocked()) {
            model.addAttribute("error",
                    "Candidate nominations are closed — the final ballot has been locked by the Election Commission.");
            return "candidate-register";
        }

        try {
            candidateService.registerCandidate(request);
            model.addAttribute("success",
                    "✅ Application submitted! Your candidacy for " + request.getPartyName() +
                            " in " + request.getWardName() +
                            " is now pending Election Commission approval.");
            model.addAttribute("request", new CandidateRegisterRequest());
        } catch (IllegalStateException | IllegalArgumentException e) {
            log.warn("Candidate registration rejected: {}", e.getMessage());
            model.addAttribute("error", e.getMessage());
        }

        return "candidate-register";
    }
}