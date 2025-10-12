package com.jivs.platform.service.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.security.*;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * Service for encrypting and decrypting data
 * Supports AES-256-GCM encryption with key rotation
 */
@Service
public class EncryptionService {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(EncryptionService.class);

    private static final String ALGORITHM = "AES";
    private static final String CIPHER_ALGORITHM = "AES/GCM/NoPadding";
    private static final int KEY_SIZE = 256;
    private static final int GCM_TAG_LENGTH = 128;
    private static final int GCM_IV_LENGTH = 12;

    @Value("${encryption.master-key:#{null}}")
    private String masterKeyBase64;

    @Value("${encryption.key-rotation-enabled:true}")
    private boolean keyRotationEnabled;

    private SecretKey masterKey;
    private final Map<String, SecretKey> dataKeys = new HashMap<>();

    /**
     * Initialize encryption service
     */
    public void initialize() throws Exception {
        if (masterKeyBase64 != null && !masterKeyBase64.isEmpty()) {
            byte[] keyBytes = Base64.getDecoder().decode(masterKeyBase64);
            masterKey = new SecretKeySpec(keyBytes, ALGORITHM);
            log.info("Loaded master key from configuration");
        } else {
            masterKey = generateKey();
            log.info("Generated new master key");
        }
    }

    /**
     * Encrypt data
     */
    public byte[] encrypt(byte[] data) throws Exception {
        return encrypt(data, null);
    }

    /**
     * Encrypt data with specific key ID
     */
    public byte[] encrypt(byte[] data, String keyId) throws Exception {
        if (data == null || data.length == 0) {
            throw new IllegalArgumentException("Data cannot be null or empty");
        }

        try {
            // Get or generate data key
            SecretKey dataKey = getOrGenerateDataKey(keyId);

            // Generate IV
            byte[] iv = generateIV();

            // Initialize cipher
            Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, dataKey, spec);

            // Encrypt data
            byte[] encryptedData = cipher.doFinal(data);

            // Combine IV and encrypted data
            ByteBuffer byteBuffer = ByteBuffer.allocate(1 + iv.length + encryptedData.length);
            byteBuffer.put((byte) iv.length);
            byteBuffer.put(iv);
            byteBuffer.put(encryptedData);

            byte[] result = byteBuffer.array();
            log.debug("Encrypted {} bytes to {} bytes", data.length, result.length);

            return result;

        } catch (Exception e) {
            log.error("Encryption failed: {}", e.getMessage(), e);
            throw new Exception("Failed to encrypt data", e);
        }
    }

    /**
     * Decrypt data
     */
    public byte[] decrypt(byte[] encryptedData) throws Exception {
        return decrypt(encryptedData, null);
    }

    /**
     * Decrypt data with specific key ID
     */
    public byte[] decrypt(byte[] encryptedData, String keyId) throws Exception {
        if (encryptedData == null || encryptedData.length == 0) {
            throw new IllegalArgumentException("Encrypted data cannot be null or empty");
        }

        try {
            // Extract IV and ciphertext
            ByteBuffer byteBuffer = ByteBuffer.wrap(encryptedData);
            int ivLength = byteBuffer.get();
            byte[] iv = new byte[ivLength];
            byteBuffer.get(iv);
            byte[] ciphertext = new byte[byteBuffer.remaining()];
            byteBuffer.get(ciphertext);

            // Get data key
            SecretKey dataKey = getOrGenerateDataKey(keyId);

            // Initialize cipher
            Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, dataKey, spec);

            // Decrypt data
            byte[] decryptedData = cipher.doFinal(ciphertext);
            log.debug("Decrypted {} bytes to {} bytes", encryptedData.length, decryptedData.length);

            return decryptedData;

        } catch (Exception e) {
            log.error("Decryption failed: {}", e.getMessage(), e);
            throw new Exception("Failed to decrypt data", e);
        }
    }

    /**
     * Encrypt string
     */
    public String encryptString(String plaintext) throws Exception {
        byte[] encrypted = encrypt(plaintext.getBytes());
        return Base64.getEncoder().encodeToString(encrypted);
    }

    /**
     * Decrypt string
     */
    public String decryptString(String encryptedBase64) throws Exception {
        byte[] encrypted = Base64.getDecoder().decode(encryptedBase64);
        byte[] decrypted = decrypt(encrypted);
        return new String(decrypted);
    }

    /**
     * Hash data with SHA-256
     */
    public String hash(String data) throws Exception {
        return hash(data.getBytes());
    }

    /**
     * Hash byte array with SHA-256
     */
    public String hash(byte[] data) throws Exception {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data);
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            log.error("Hashing failed: {}", e.getMessage(), e);
            throw new Exception("Failed to hash data", e);
        }
    }

    /**
     * Hash password with salt
     */
    public PasswordHash hashPassword(String password) throws Exception {
        try {
            // Generate salt
            SecureRandom random = new SecureRandom();
            byte[] salt = new byte[16];
            random.nextBytes(salt);

            // Hash password with salt
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(salt);
            byte[] hash = digest.digest(password.getBytes());

            PasswordHash result = new PasswordHash();
            result.setHash(Base64.getEncoder().encodeToString(hash));
            result.setSalt(Base64.getEncoder().encodeToString(salt));

            return result;

        } catch (Exception e) {
            log.error("Password hashing failed: {}", e.getMessage(), e);
            throw new Exception("Failed to hash password", e);
        }
    }

    /**
     * Verify password
     */
    public boolean verifyPassword(String password, String hashBase64, String saltBase64) throws Exception {
        try {
            byte[] salt = Base64.getDecoder().decode(saltBase64);

            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(salt);
            byte[] hash = digest.digest(password.getBytes());

            String calculatedHash = Base64.getEncoder().encodeToString(hash);
            return calculatedHash.equals(hashBase64);

        } catch (Exception e) {
            log.error("Password verification failed: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Generate data encryption key
     */
    public SecretKey generateKey() throws Exception {
        try {
            KeyGenerator keyGen = KeyGenerator.getInstance(ALGORITHM);
            keyGen.init(KEY_SIZE);
            return keyGen.generateKey();
        } catch (Exception e) {
            log.error("Key generation failed: {}", e.getMessage(), e);
            throw new Exception("Failed to generate key", e);
        }
    }

    /**
     * Generate initialization vector
     */
    private byte[] generateIV() {
        byte[] iv = new byte[GCM_IV_LENGTH];
        SecureRandom random = new SecureRandom();
        random.nextBytes(iv);
        return iv;
    }

    /**
     * Get or generate data key
     */
    private SecretKey getOrGenerateDataKey(String keyId) throws Exception {
        if (keyId == null) {
            return masterKey;
        }

        synchronized (dataKeys) {
            if (!dataKeys.containsKey(keyId)) {
                SecretKey dataKey = generateKey();
                dataKeys.put(keyId, dataKey);
                log.info("Generated new data key: {}", keyId);
            }
            return dataKeys.get(keyId);
        }
    }

    /**
     * Rotate encryption key
     */
    public RotationResult rotateKey(String oldKeyId, String newKeyId) throws Exception {
        log.info("Rotating key from {} to {}", oldKeyId, newKeyId);

        RotationResult result = new RotationResult();
        result.setOldKeyId(oldKeyId);
        result.setNewKeyId(newKeyId);
        result.setSuccess(false);

        try {
            // Generate new key
            SecretKey newKey = generateKey();
            dataKeys.put(newKeyId, newKey);

            // Mark old key for retirement
            // In production, would re-encrypt all data with new key

            result.setSuccess(true);
            log.info("Key rotation completed successfully");

        } catch (Exception e) {
            log.error("Key rotation failed: {}", e.getMessage(), e);
            throw new Exception("Failed to rotate key", e);
        }

        return result;
    }

    /**
     * Export key (encrypted with master key)
     */
    public String exportKey(String keyId) throws Exception {
        SecretKey dataKey = dataKeys.get(keyId);
        if (dataKey == null) {
            throw new IllegalArgumentException("Key not found: " + keyId);
        }

        byte[] keyBytes = dataKey.getEncoded();
        byte[] encryptedKey = encrypt(keyBytes, null);
        return Base64.getEncoder().encodeToString(encryptedKey);
    }

    /**
     * Import key (decrypt with master key)
     */
    public void importKey(String keyId, String encryptedKeyBase64) throws Exception {
        byte[] encryptedKey = Base64.getDecoder().decode(encryptedKeyBase64);
        byte[] keyBytes = decrypt(encryptedKey, null);
        SecretKey dataKey = new SecretKeySpec(keyBytes, ALGORITHM);
        dataKeys.put(keyId, dataKey);
        log.info("Imported key: {}", keyId);
    }

    /**
     * Generate random token
     */
    public String generateToken(int length) {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[length];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /**
     * Generate secure random bytes
     */
    public byte[] generateRandomBytes(int length) {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[length];
        random.nextBytes(bytes);
        return bytes;
    }
}

/**
 * Password hash result
 */
class PasswordHash {
    private String hash;
    private String salt;

    public String getHash() { return hash; }
    public void setHash(String hash) { this.hash = hash; }
    public String getSalt() { return salt; }
    public void setSalt(String salt) { this.salt = salt; }
}

/**
 * Key rotation result
 */
class RotationResult {
    private String oldKeyId;
    private String newKeyId;
    private boolean success;
    private String message;

    public String getOldKeyId() { return oldKeyId; }
    public void setOldKeyId(String oldKeyId) { this.oldKeyId = oldKeyId; }
    public String getNewKeyId() { return newKeyId; }
    public void setNewKeyId(String newKeyId) { this.newKeyId = newKeyId; }
    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
