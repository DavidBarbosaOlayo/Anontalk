package security.encryption;

import java.security.PrivateKey;
import java.security.PublicKey;

/** Guarda en memoria tu par de claves tras el login. */
public class KeyManager {
    private static PrivateKey privateKey;
    private static PublicKey  publicKey;

    public static void setPrivateKey(PrivateKey pk) { privateKey = pk; }
    public static PrivateKey getPrivateKey() { return privateKey; }

    public static void setPublicKey(PublicKey pk) { publicKey = pk; }
    public static PublicKey getPublicKey() { return publicKey; }
}
