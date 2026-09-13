package com.example.E_voting_System.blockchain;

import com.example.E_voting_System.common.ApiResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * ✅ FIXED BLOCKCHAIN CONTROLLER
 *
 * Changes:
 * 1. Added input validation (maxlength check)
 * 2. Proper error handling with ApiResponse
 * 3. Consistent HTTP status codes
 * 4. Added logging
 * 5. Request size limits
 */
@Slf4j
@RestController
@RequestMapping("/api/blockchain")
@RequiredArgsConstructor
public class BlockChainController {

    private final BlockChainService blockChainService;

    private static final int MAX_DATA_SIZE = 10000;  // 10KB max per block

    /**
     * ✅ FIXED: Added input validation
     */
    @PostMapping("/add")
    public ResponseEntity<ApiResponse<String>> addBlock(@RequestBody String data) {

        log.info("Block addition request received - Data size: {}",
                data != null ? data.length() : 0);

        // ✅ Validation 1: Null check
        if (data == null || data.isBlank()) {
            log.warn("Empty data provided for block");
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("Data cannot be empty"));
        }

        // ✅ Validation 2: Size limit
        if (data.length() > MAX_DATA_SIZE) {
            log.warn("Data too large - Size: {} bytes (max: {})", data.length(), MAX_DATA_SIZE);
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(
                            "Data too large. Maximum size: " + MAX_DATA_SIZE + " characters"));
        }

        try {
            String result = blockChainService.addBlock(data);
            log.info("Block added successfully");
            return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body(ApiResponse.success(result));

        } catch (RuntimeException e) {
            log.error("Invalid block data: {}", e.getMessage());
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("Invalid block data: " + e.getMessage()));

        } catch (Exception e) {
            log.error("Unexpected error while adding block", e);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Error adding block: " + e.getMessage()));
        }
    }

    /**
     * Get blockchain explorer data
     */
    @GetMapping("/explorer")
    public ResponseEntity<ApiResponse<Map<String, Object>>> explorer() {
        try {
            log.info("Blockchain explorer request");
            Map<String, Object> data = blockChainService.getExplorerData();
            return ResponseEntity.ok(ApiResponse.success(data));

        } catch (Exception e) {
            log.error("Error fetching explorer data", e);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Error fetching explorer data"));
        }
    }

    /**
     * Get blockchain info
     */
    @GetMapping("/info")
    public ResponseEntity<ApiResponse<Map<String, Object>>> info() {
        try {
            log.info("Blockchain info request");

            boolean valid = blockChainService.isChainValid();
            int totalBlocks = blockChainService.getChain().size();

            Map<String, Object> infoMap = Map.of(
                    "totalBlocks", totalBlocks,
                    "valid", valid
            );

            return ResponseEntity.ok(ApiResponse.success(infoMap));

        } catch (Exception e) {
            log.error("Error fetching blockchain info", e);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Error fetching blockchain info"));
        }
    }

    /**
     * Get blockchain status
     */
    @GetMapping("/status")
    public ResponseEntity<ApiResponse<Map<String, Object>>> blockchainStatus() {
        try {
            log.info("Blockchain status request");
            Map<String, Object> status = blockChainService.getBlockchainStatus();
            return ResponseEntity.ok(ApiResponse.success(status));

        } catch (Exception e) {
            log.error("Error fetching blockchain status", e);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Error fetching status"));
        }
    }

    /**
     * Detect tampered blocks
     */
    @GetMapping("/tampered-blocks")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getTamperedBlocks() {
        try {
            log.info("Tampered blocks detection request");

            var tamperedList = blockChainService.getTamperedBlocks();
            Map<String, Object> response = Map.of(
                    "tamperedBlocks", tamperedList,
                    "count", tamperedList.size()
            );

            return ResponseEntity.ok(ApiResponse.success(response));

        } catch (Exception e) {
            log.error("Error detecting tampered blocks", e);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Error detecting tampered blocks"));
        }
    }

    /**
     * Get election results
     */
    @GetMapping("/results")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getResults() {
        try {
            log.info("Election results request");

            Map<String, Long> results = blockChainService.getElectionResults();
            Map<String, Object> response = Map.of(
                    "results", results,
                    "totalVotes", results.values().stream()
                            .mapToLong(Long::longValue)
                            .sum()
            );

            return ResponseEntity.ok(ApiResponse.success(response));

        } catch (Exception e) {
            log.error("Error fetching election results", e);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Error fetching results"));
        }
    }

    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<ApiResponse<String>> health() {
        return ResponseEntity.ok(ApiResponse.success("Blockchain service is healthy"));
    }
}