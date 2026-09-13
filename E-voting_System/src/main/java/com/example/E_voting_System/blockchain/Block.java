package com.example.E_voting_System.blockchain;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Block {

    private int index;
    private String previousHash;
    private String data;
    private long timestamp;
    private String hash;
}