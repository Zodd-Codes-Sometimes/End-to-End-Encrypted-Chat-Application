package utils;

import javax.crypto.Cipher;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.Base64;

public class RSAUtils {
    // Function to generate public/private key pair for the user, the key size indicates the number of bits of the key
    public static KeyPair generateRSAKeyPair(int keySize) throws NoSuchAlgorithmException {
        KeyPairGenerator keyPairGen = KeyPairGenerator.getInstance("RSA");
        keyPairGen.initialize(keySize);
        KeyPair pair = keyPairGen.generateKeyPair();
        return pair;
    }

    // Function to encrypt data via RSA
    public static String encrypt(String data, PublicKey publicKey) throws Exception {
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);
        byte[] encBytes = cipher.doFinal(data.getBytes(StandardCharsets.UTF_8));
        String enc = Base64.getEncoder().encodeToString(encBytes);
        return enc;
    }

    // Function to decrypt encrypted RSA data
    public static String decrypt(String data, PrivateKey privateKey) throws Exception {
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.DECRYPT_MODE, privateKey);
        byte[] enc = Base64.getDecoder().decode(data);
        byte[] decBytes = cipher.doFinal(enc);
        String dec = new String(decBytes, StandardCharsets.UTF_8);
        return dec;
    }
}