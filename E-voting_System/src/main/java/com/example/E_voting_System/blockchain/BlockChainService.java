package com.example.E_voting_System.blockchain;

import com.example.E_voting_System.dto.VotePayLoad;
import com.example.E_voting_System.service.DigitalSignatureService;
import com.example.E_voting_System.service.HashService;
import com.example.E_voting_System.service.MerkleTreeService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class BlockChainService {

    private final BlockRepository blockRepository;
    private final HashService hashService;
    private final DigitalSignatureService digitalSignatureService;
    private final MerkleTreeService merkleTreeService;
    private final ObjectMapper objectMapper;

    @Value("${blockchain.mining.difficulty}")
    private String prefix;

    @Value("${blockchain.mining.max-attempts}")
    private int maxAttempts;

    private static final String GENESIS_DATA = "GENESIS_BLOCK";

    @PostConstruct
    public void init() {
        initGenesisBlock();
    }

    public void initGenesisBlock() {
        if (blockRepository.count() == 0) {
            log.info("Creating Genesis Block...");

            Block genesis = new Block();
            genesis.setBlockId("GENESIS_BLOCK");
            genesis.setData(GENESIS_DATA);
            genesis.setPreviousHash("0");
            genesis.setTimestamp(Instant.now());
            genesis.setNonce(0);
            genesis.setMerkleRoot("0");

            mineBlock(genesis);
            genesis.setSignature(digitalSignatureService.sign(genesis.getHash()));

            blockRepository.save(genesis);
        }
    }

    public Block getLastBlock() {
        return Optional.ofNullable(blockRepository.findTopByOrderByIdDesc())
                .orElseThrow(() -> new RuntimeException("Genesis block missing"));
    }

    @Transactional
    public String addBlock(String data) {

        Block prev = getLastBlock();

        Block block = new Block();
        Instant now = Instant.now();

        block.setBlockId("block-" + now.toEpochMilli());
        block.setTimestamp(now);
        block.setPreviousHash(prev.getHash());
        block.setData(data);
        block.setNonce(0);

        block.setMerkleRoot(merkleTreeService.getMerkleRoot(List.of(data)));

        mineBlock(block);
        block.setSignature(digitalSignatureService.sign(block.getHash()));

        blockRepository.save(block);

        return "Block mined successfully";
    }

    private void mineBlock(Block block) {

        while (block.getNonce() < maxAttempts) {

            String hash = hashService.sha256Hex(buildHash(block));

            if (hash.startsWith(prefix)) {
                block.setHash(hash);
                return;
            }

            block.setNonce(block.getNonce() + 1);
        }

        throw new RuntimeException("Mining failed");
    }

    private String buildHash(Block block) {
        return block.getTimestamp().toEpochMilli() +
                block.getBlockId() +
                block.getData() +
                block.getMerkleRoot() +
                block.getPreviousHash() +
                block.getNonce();
    }

    private String recalculateHash(Block block) {
        return hashService.sha256Hex(buildHash(block));
    }

    public List<Block> getChain() {
        return blockRepository.findAllByOrderByTimestampAsc();
    }

    public boolean isChainValid() {

        List<Block> blocks = getChain();

        for (int i = 0; i < blocks.size(); i++) {

            Block current = blocks.get(i);

            if (!current.getHash().equals(recalculateHash(current))) return false;

            if (!digitalSignatureService.verify(current.getHash(), current.getSignature())) return false;

            if (i > 0) {
                Block prev = blocks.get(i - 1);
                if (!current.getPreviousHash().equals(prev.getHash())) return false;
            }
        }

        return true;
    }

    public List<Long> getTamperedBlocks() {

        List<Block> blocks = getChain();
        List<Long> tampered = new ArrayList<>();

        for (int i = 0; i < blocks.size(); i++) {

            Block current = blocks.get(i);

            if (!current.getHash().equals(recalculateHash(current))) {
                tampered.add(current.getId());
            }

            if (i > 0) {
                Block prev = blocks.get(i - 1);
                if (!current.getPreviousHash().equals(prev.getHash())) {
                    tampered.add(current.getId());
                }
            }
        }

        return tampered;
    }

    public Map<String, Long> getElectionResults() {

        Map<String, Long> results = new HashMap<>();

        for (Block block : getChain()) {

            if (GENESIS_DATA.equals(block.getData())) continue;

            try {
                VotePayLoad payload = objectMapper.readValue(block.getData(), VotePayLoad.class);
                String key = String.valueOf(payload.getCandidateId());
                results.put(key, results.getOrDefault(key, 0L) + 1);
            } catch (Exception ignored) {}
        }

        return results;
    }

    public Map<String, Object> getBlockchainStatus() {
        return Map.of(
                "valid", isChainValid(),
                "totalBlocks", getChain().size()
        );
    }

    public Map<String, Object> getExplorerData() {
        List<Block> blocks = getChain();

        return Map.of(
                "totalBlocks", blocks.size(),
                "blocks", blocks,
                "lastBlockHash", blocks.isEmpty() ? null : blocks.get(blocks.size() - 1).getHash()
        );
    }
}