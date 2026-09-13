package com.example.E_voting_System.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MerkleTreeService {

    private final HashService hashService;

    public String computeRoot(List<String> hashes) {
        if (hashes == null || hashes.isEmpty()) return "0".repeat(64);

        List<String> current = new ArrayList<>(hashes);
        while (current.size() > 1) {
            List<String> next = new ArrayList<>();
            for (int i = 0; i < current.size(); i += 2) {
                String left  = current.get(i);
                String right = (i + 1 < current.size()) ? current.get(i + 1) : left;
                next.add(hashService.sha256Hex(left + right));
            }
            current = next;
        }
        return current.get(0);
    }
}