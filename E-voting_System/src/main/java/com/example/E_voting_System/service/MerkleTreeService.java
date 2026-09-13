package com.example.E_voting_System.service;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class MerkleTreeService {

    public String getMerkleRoot(List<String> hashes) {

        if (hashes.isEmpty()) return "0";

        List<String> tempList = new ArrayList<>(hashes);

        while (tempList.size() > 1) {

            List<String> newList = new ArrayList<>();

            for (int i = 0; i < tempList.size(); i += 2) {

                if (i + 1 < tempList.size()) {
                    newList.add(hash(tempList.get(i) + tempList.get(i + 1)));
                } else {
                    newList.add(hash(tempList.get(i) + tempList.get(i)));
                }

            }

            tempList = newList;
        }

        return tempList.get(0);
    }

    private String hash(String data) {
        return org.apache.commons.codec.digest.DigestUtils.sha256Hex(data);
    }
}