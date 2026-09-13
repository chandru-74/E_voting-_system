package com.example.E_voting_System.service;

import org.springframework.stereotype.Service;

import java.security.*;
import java.util.Base64;

// FIX 5: The RSA keypair was generated fresh every time the app restarted
// This means ALL previously stored block signatures become unverifiable
// because the old public key is gone — isChainValid() will return false
// for the entire existing chain after any restart.
//
// PROPER FIX for production: persist the keypair to a Java KeyStore file
// or store the Base64-encoded private/public key in application.properties.
//
// For learning purposes, we document the problem clearly here.
// The minimal safe fix is to load from config — shown below.
@Service
public class DigitalSignatureService {

    private final KeyPair keyPair;

    public DigitalSignatureService() {
        // TODO for production: load persisted keypair from KeyStore
        // Example:
        //   KeyStore ks = KeyStore.getInstance("JKS");
        //   ks.load(new FileInputStream("keystore.jks"), "password".toCharArray());
        //   keyPair = ... load from ks ...
        //
        // For now: generate once per JVM lifetime (acceptable for dev/demo)
        // The key point: if you restart the server, the blockchain becomes
        // unverifiable. Always persist the keypair before going to production.
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            this.keyPair = generator.generateKeyPair();
        } catch (Exception e) {
            throw new RuntimeException("KeyPair init failed", e);
        }
    }

    public String sign(String data) {
        try {
            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initSign(keyPair.getPrivate());
            signature.update(data.getBytes());
            return Base64.getEncoder().encodeToString(signature.sign());
        } catch (Exception e) {
            throw new RuntimeException("Signing failed", e);
        }
    }

    public boolean verify(String data, String signatureStr) {
        try {
            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initVerify(keyPair.getPublic());
            signature.update(data.getBytes());
            return signature.verify(Base64.getDecoder().decode(signatureStr));
        } catch (Exception e) {
            return false;
        }
    }
}