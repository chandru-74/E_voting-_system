package com.example.E_voting_System.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class InstructionsController {

    // "from" tells the template where the visitor came from, so the
    // back button can return them to the right place and hide "Home"
    // when they arrived from /evoting. Defaults to "home" so a direct
    // hit with no query param behaves like before.
    @GetMapping("/instructions/candidate")
    public String candidateInstructions(
            @RequestParam(defaultValue = "home") String from,
            Model model) {
        model.addAttribute("from", from);
        return "candidateinstructions"; // → templates/candidateinstructions.html
    }

    @GetMapping("/instructions/voter")
    public String voterInstructions(
            @RequestParam(defaultValue = "home") String from,
            Model model) {
        model.addAttribute("from", from);
        return "voterinstructions"; // → templates/voterinstructions.html
    }
}