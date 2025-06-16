import utils.RSAUtils;
import java.security.*;

// This is for testing purposes and won't be used in production

public class Test {
    public static void main(String[] args) {

        try {
            KeyPair pair = RSAUtils.generateRSAKeyPair(2048);
            String enc = RSAUtils.encrypt("Encryption/Decryption Test", pair.getPublic());
            System.out.println("Encrypted: " + enc);
            String dec = RSAUtils.decrypt(enc, pair.getPrivate());
            System.out.println("Decrypted: " + dec);
        } catch (Exception e) {
            System.out.println(e);
        }
    }
}
