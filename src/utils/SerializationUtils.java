package utils;

import java.io.*;
import java.util.Base64;
import java.util.Map;

public class SerializationUtils {
    // Function for serialization
    public static String serialize(Map<String, String> map) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream(bos);
        out.writeObject(map);
        out.flush();
        String serialized = Base64.getEncoder().encodeToString(bos.toByteArray());
        return serialized;
    }

    // Function for deserialization
    public static Map<String, String> deserialize(String enc) throws IOException, ClassNotFoundException {
        byte[] data = Base64.getDecoder().decode(enc);
        ByteArrayInputStream bis = new ByteArrayInputStream(data);
        ObjectInputStream in = new ObjectInputStream(bis);
        return (Map<String, String>) in.readObject();
    }
}
