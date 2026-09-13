package com.example.E_voting_System.controller;

import com.example.E_voting_System.common.ApiResponse;
import com.example.E_voting_System.service.ElectionResultService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/results")
@RequiredArgsConstructor
public class ElectionResultController {

    private final ElectionResultService electionResultService;

    @GetMapping("/blockchain")
    public ApiResponse<Map<String, Long>> getBlockchainResults() {
        return ApiResponse.success(electionResultService.getElectionResults());
    }
}