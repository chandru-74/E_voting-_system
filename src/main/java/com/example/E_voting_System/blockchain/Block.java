package com.example.E_voting_System.blockchain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "blocks")
@Getter
@Setter
@NoArgsConstructor
public class Block {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private int blockIndex;

    @Column(length = 512)
    private String previousHash;

    @Column(length = 2000)
    private String data;

    @Column(length = 512)
    private String hash;

    private long nonce;

    private long timestamp;

    // NEW: RSA signature over (blockIndex + previousHash + data + nonce +
    // timestamp + hash) — proves this exact block was produced by this
    // server's private key, not just that its hash chain is internally
    // consistent. See DigitalSignatureService.
    @Column(length = 512)
    private String signature;

    public Block(int blockIndex, String previousHash, String data) {
        this.blockIndex = blockIndex;
        this.previousHash = previousHash;
        this.data = data;
        this.timestamp = System.currentTimeMillis();
        this.nonce = 0;
    }
}