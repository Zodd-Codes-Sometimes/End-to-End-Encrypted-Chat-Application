import utils.RSAUtils;
import utils.SerializationUtils;
import java.security.*;
import java.security.interfaces.RSAPublicKey;
import java.util.HashMap;
import java.util.Map;

// This is for testing purposes and won't be used in production

public class Test {
    public static void main(String[] args) {

        try {
            KeyPair user1_pair = RSAUtils.generateRSAKeyPair(2048);
            KeyPair user2_pair = RSAUtils.generateRSAKeyPair(2048);
            String enc = RSAUtils.encrypt("Hello", user1_pair.getPublic());
            String enc2 = RSAUtils.encrypt("Hello", user2_pair.getPublic());
//            System.out.println("Encrypted: " + enc);
            Map<String, String> dict = new HashMap<>();
            dict.put("user1", enc);
            dict.put("user2", enc2);
            String serialized = SerializationUtils.serialize(dict);
            System.out.println("Serialized: " + serialized);
            Map<String,String> deserialzed = SerializationUtils.deserialize(serialized);
            System.out.println("Deserialized: " + deserialzed);
            String dec = RSAUtils.decrypt(enc, user1_pair.getPrivate());
            String dec2 = RSAUtils.decrypt(enc2, user2_pair.getPrivate());
            System.out.println("Decrypted for user 1: " + dec);
            System.out.println("Decrypted for user 2: " + dec2);
        } catch (Exception e) {
            System.out.println(e);
        }
    }
}
