package com.example.E_voting_System.blockchain;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Collections;
import java.util.List;

@Controller
@RequiredArgsConstructor
@RequestMapping("/blockchain")
public class BlockChainViewController {

    private final BlockChainService blockChainService;

    @GetMapping
    public String viewBlockchain(Model model) {

        List<Block> blocks;

        try {
            blocks = blockChainService.getChain();
        } catch (Exception e) {
            System.out.println("Error fetching chain: " + e.getMessage());
            blocks = Collections.emptyList();
        }

        model.addAttribute("blocks", blocks);
        model.addAttribute("totalBlocks", blocks.size());

        boolean valid = false;
        String validationError = null;

        try {
            valid = blockChainService.isChainValid();
        } catch (Exception e) {
            validationError = e.getMessage();
            System.out.println("Validation Error: " + e.getMessage());
        }

        if (!valid && validationError == null) {
            validationError = "Blockchain validation failed";
        }

        model.addAttribute("valid", valid);
        model.addAttribute("validationError", validationError);

        List<Long> tamperedIds = Collections.emptyList();

        try {
            tamperedIds = blockChainService.getTamperedBlocks();
        } catch (Exception e) {
            System.out.println("Tamper detection error: " + e.getMessage());
        }

        model.addAttribute("tamperedIds", tamperedIds);

        return "blockchain";
    }
}