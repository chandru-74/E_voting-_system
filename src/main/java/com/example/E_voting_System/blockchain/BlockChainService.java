package com.example.E_voting_System.blockchain;

import com.example.E_voting_System.service.DigitalSignatureService;
import com.example.E_voting_System.service.HashService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class BlockChainService {

    private static final int DIFFICULTY = 2;
    private static final String GENESIS_DATA = "GENESIS_BLOCK";

    private final BlockRepository          blockRepository;
    private final HashService              hashService;
    private final DigitalSignatureService  digitalSignatureService;

    // ── Lock guarding the "read last block → compute next index/hash → save" sequence.
    // Without this, two votes arriving at nearly the same time could both read the
    // same count()/previousHash before either save() completes, producing two blocks
    // that claim the same index and previous hash — silently corrupting the chain.
    private final Object chainLock = new Object();

    @PostConstruct
    public void initGenesisBlock() {
        if (blockRepository.count() > 0) {
            log.info("Genesis block already exists — chain has {} block(s)", blockRepository.count());
            return;
        }

        Block genesis = new Block(0, "0".repeat(64), GENESIS_DATA);
        mineBlock(genesis);
        genesis.setSignature(digitalSignatureService.sign(signableContent(genesis)));
        blockRepository.save(genesis);
        log.info("Genesis block created — hash: {}...", genesis.getHash().substring(0, 8));
    }

    @Transactional
    public Block addBlock(String data) {
        synchronized (chainLock) {
            String previousHash = blockRepository.findTopByOrderByBlockIndexDesc()
                    .map(Block::getHash)
                    .orElse("0".repeat(64)); // fallback only; genesis should always exist by now

            int nextIndex = (int) blockRepository.count();
            Block block   = new Block(nextIndex, previousHash, data);

            mineBlock(block);
            block.setSignature(digitalSignatureService.sign(signableContent(block)));

            Block saved = blockRepository.save(block);
            log.info("Block #{} mined, signed, and saved — hash: {}...",
                    saved.getBlockIndex(), saved.getHash().substring(0, 8));

            if (!isChainValid()) {
                log.error("CHAIN INTEGRITY ALERT: chain failed validation immediately " +
                        "after adding block #{}", saved.getBlockIndex());
            }

            return saved;
        }
    }

    public List<Block> getChain() {
        return blockRepository.findAll();
    }

    public boolean isChainValid() {
        List<Block> chain = getChain();
        for (int i = 0; i < chain.size(); i++) {
            Block current = chain.get(i);

            String recalculated = calculateHash(current);
            if (!current.getHash().equals(recalculated)) {
                log.warn("Chain invalid at block #{} — hash mismatch", current.getBlockIndex());
                return false;
            }

            if (current.getSignature() == null ||
                    !digitalSignatureService.verify(signableContent(current), current.getSignature())) {
                log.warn("Chain invalid at block #{} — signature mismatch or missing",
                        current.getBlockIndex());
                return false;
            }

            if (i > 0) {
                Block previous = chain.get(i - 1);
                if (!current.getPreviousHash().equals(previous.getHash())) {
                    log.warn("Chain invalid at block #{} — previous hash mismatch",
                            current.getBlockIndex());
                    return false;
                }
            }
        }
        return true;
    }

    // ── Private helpers ───────────────────────────────────────

    private void mineBlock(Block block) {
        String target = "0".repeat(DIFFICULTY);
        String hash;
        do {
            block.setNonce(block.getNonce() + 1);
            hash = calculateHash(block);
        } while (!hash.startsWith(target));
        block.setHash(hash);
    }

    private String calculateHash(Block block) {
        String input = block.getBlockIndex()
                + block.getPreviousHash()
                + block.getData()
                + block.getNonce()
                + block.getTimestamp();
        return hashService.sha256Hex(input);
    }

    private String signableContent(Block block) {
        return block.getBlockIndex()
                + block.getPreviousHash()
                + block.getData()
                + block.getNonce()
                + block.getTimestamp()
                + block.getHash();
    }
}