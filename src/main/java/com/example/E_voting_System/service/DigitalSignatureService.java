package com.example.E_voting_System.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * Signs and verifies blockchain block data using RSA.
 *
 * IMPORTANT: the key pair is generated ONCE and persisted to disk under
 * keys/blockchain-private.key and keys/blockchain-public.key. On every
 * subsequent startup, the existing keys are loaded instead of generating
 * new ones. Without this, a fresh key pair on every restart would make
 * every previously-signed block's signature permanently unverifiable —
 * the new key literally cannot check signatures made by the old one.
 *
 * The keys/ folder must be added to .gitignore — the private key must
 * never be committed to source control.
 */
@Slf4j
@Service
public class DigitalSignatureService {

    private static final String KEY_DIR         = "keys";
    private static final String PRIVATE_KEY_FILE = KEY_DIR + "/blockchain-private.key";
    private static final String PUBLIC_KEY_FILE  = KEY_DIR + "/blockchain-public.key";

    private PrivateKey privateKey;
    private PublicKey  publicKey;

    @PostConstruct
    public void init() {
        try {
            File dir = new File(KEY_DIR);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            File privFile = new File(PRIVATE_KEY_FILE);
            File pubFile  = new File(PUBLIC_KEY_FILE);

            if (privFile.exists() && pubFile.exists()) {
                loadKeys(privFile, pubFile);
                log.info("Loaded existing RSA key pair for digital signatures");
            } else {
                generateAndSaveKeys(privFile, pubFile);
                log.info("Generated NEW RSA key pair for digital signatures (first run)");
            }
        } catch (Exception e) {
            throw new IllegalStateException("Failed to initialize signing keys", e);
        }
    }

    private void generateAndSaveKeys(File privFile, File pubFile) throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair pair = generator.generateKeyPair();
        this.privateKey = pair.getPrivate();
        this.publicKey  = pair.getPublic();

        try (FileOutputStream out = new FileOutputStream(privFile)) {
            out.write(privateKey.getEncoded());
        }
        try (FileOutputStream out = new FileOutputStream(pubFile)) {
            out.write(publicKey.getEncoded());
        }
    }

    private void loadKeys(File privFile, File pubFile) throws Exception {
        byte[] privBytes = Files.readAllBytes(privFile.toPath());
        byte[] pubBytes  = Files.readAllBytes(pubFile.toPath());

        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        this.privateKey = keyFactory.generatePrivate(new PKCS8EncodedKeySpec(privBytes));
        this.publicKey  = keyFactory.generatePublic(new X509EncodedKeySpec(pubBytes));
    }

    public String sign(String data) {
        try {
            Signature signer = Signature.getInstance("SHA256withRSA");
            signer.initSign(privateKey);
            signer.update(data.getBytes());
            return Base64.getEncoder().encodeToString(signer.sign());
        } catch (Exception e) {
            throw new IllegalStateException("Failed to sign data", e);
        }
    }

    public boolean verify(String data, String signatureBase64) {
        try {
            Signature verifier = Signature.getInstance("SHA256withRSA");
            verifier.initVerify(publicKey);
            verifier.update(data.getBytes());
            byte[] signatureBytes = Base64.getDecoder().decode(signatureBase64);
            return verifier.verify(signatureBytes);
        } catch (Exception e) {
            log.warn("Signature verification failed: {}", e.getMessage());
            return false;
        }
    }
}