package com.example.E_voting_System.blockchain;

import com.example.E_voting_System.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/blockchain")
@RequiredArgsConstructor
public class BlockChainController {

    private final BlockChainService blockChainService;

    @GetMapping("/chain")
    public ResponseEntity<ApiResponse<List<Block>>> getChain() {
        return ResponseEntity.ok(ApiResponse.success(blockChainService.getChain()));
    }

    @GetMapping("/validate")
    public ResponseEntity<ApiResponse<Boolean>> validate() {
        boolean valid = blockChainService.isChainValid();
        return ResponseEntity.ok(ApiResponse.success(valid));
    }
}