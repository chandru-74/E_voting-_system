package com.example.E_voting_System.controller;

import com.example.E_voting_System.entity.Candidate;
import com.example.E_voting_System.service.CandidateService;
import com.example.E_voting_System.service.ElectionSetupService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
public class PublicCandidateController {

    private final CandidateService candidateService;
    private final ElectionSetupService electionSetupService;

    @GetMapping("/candidates")
    public String listCandidates(Model model) {
        List<Candidate> named = candidateService.getAllCandidates().stream()
                .filter(c -> c.getCandidateName() != null && !c.getCandidateName().isBlank())
                .sorted(Comparator.comparing(Candidate::getWardName)
                        .thenComparing(Candidate::getPartyName))
                .toList();

        Map<String, List<Candidate>> byWard = named.stream()
                .collect(Collectors.groupingBy(
                        Candidate::getWardName, LinkedHashMap::new, Collectors.toList()));

        model.addAttribute("wardGroups", byWard);
        model.addAttribute("totalCandidates", named.size());
        model.addAttribute("totalWards", byWard.size());
        model.addAttribute("locked", electionSetupService.isCandidatesLocked());
        return "candidates"; // → templates/candidates.html
    }
}