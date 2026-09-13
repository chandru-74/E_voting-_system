package com.example.E_voting_System.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    // Root URL goes straight to the voter-facing evoting page.
    @GetMapping("/")
    public String root() {
        return "evoting"; // → templates/evoting.html
    }

    // Full portal page — shows all cards (Voter Login, Voter Registration,
    // Candidate Registration, Candidate List, Candidate/Voter Instructions,
    // Admin Login, Results Centre).
    @GetMapping("/home")
    public String home() {
        return "home"; // → templates/home.html
    }

    // Voter-facing landing page.
    @GetMapping("/evoting")
    public String evoting() {
        return "evoting"; // → templates/evoting.html
    }
}