package utils;

import javax.crypto.Cipher;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

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

        byte[] inBytes = data.getBytes(StandardCharsets.UTF_8);
        int keySizeBytes = ((RSAPublicKey) publicKey).getModulus().bitLength() / 8;
        // RSA uses PKCS#1 v1.5 padding which takes 11 bytes so the maximum chunk size will be => key size (in bytes) - 11
        int maxChunkSize = keySizeBytes - 11;
        /*
        The number of bytes that RSA can encrypt depends on the key size. A key of 2048 bits / 256 bytes will encrypt maximum 245 bytes (11 bytes are for the padding)
        Since the data to encrypt can be longer than 245 bytes, we break down the encryption into chunks and then encrypt the chunks separately
         */
        List<byte[]> encChunks = new ArrayList<>();

        for(int i = 0; i <= inBytes.length; i += maxChunkSize){
            int len = Math.min(maxChunkSize, inBytes.length - i);
            byte[] chunk = new byte[len];
            System.arraycopy(inBytes, i, chunk, 0, len);
            encChunks.add(cipher.doFinal(chunk));
        }

        byte[] encryptedBytes = joinChunks(encChunks);
        String enc = Base64.getEncoder().encodeToString(encryptedBytes);
        return enc;
    }

    // Function to decrypt encrypted RSA data
    public static String decrypt(String data, PrivateKey privateKey) throws Exception {
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.DECRYPT_MODE, privateKey);
        byte[] encBytes = Base64.getDecoder().decode(data);
        int keySizeBytes = ((RSAPrivateKey) privateKey).getModulus().bitLength() / 8;
        // Decrypt the data in chunks then join the chunks
        List<byte[]> decChunks = new ArrayList<>();

        for(int i = 0; i < encBytes.length; i += keySizeBytes){
            int len = Math.min(keySizeBytes, encBytes.length - i);
            byte[] chunk = new byte[len];
            System.arraycopy(encBytes, i, chunk, 0, len);
            decChunks.add(cipher.doFinal(chunk));
        }

        byte[] decBytes = joinChunks(decChunks);
        String dec = new String(decBytes, StandardCharsets.UTF_8);
        return dec;
    }

    // Helper Function to join the chunks
    public static byte[] joinChunks(List<byte[]> chunks){
        int length = 0;
        for(byte[] chunk: chunks){
            length += chunk.length;
        }
        byte[] joinedChunks = new byte[length];
        int currentPosition = 0;
        for(byte[] chunk: chunks) {
            System.arraycopy(chunk, 0, joinedChunks, currentPosition, chunk.length);
            currentPosition += chunk.length;
        }
        return joinedChunks;
    }
}