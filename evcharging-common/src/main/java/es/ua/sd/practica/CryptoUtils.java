package es.ua.sd.practica;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

public class CryptoUtils {

    private static final String ALGORITMO = "AES";

    //Lo usará la central
    public static String generarClave() throws Exception {
        KeyGenerator keyGen = KeyGenerator.getInstance(ALGORITMO);
        keyGen.init(128); 
        SecretKey secretKey = keyGen.generateKey();
        return Base64.getEncoder().encodeToString(secretKey.getEncoded());
    }

    //Lo usará el CP al recibir la clave via api rest
    public static SecretKey stringToKey(String claveBase64) {
        byte[] decodedKey = Base64.getDecoder().decode(claveBase64);
        return new SecretKeySpec(decodedKey, ALGORITMO);
    }

    public static String cifrar(String mensaje, SecretKey clave) throws Exception {
        Cipher cipher = Cipher.getInstance(ALGORITMO);
        cipher.init(Cipher.ENCRYPT_MODE, clave);
        byte[] mensajeCifrado = cipher.doFinal(mensaje.getBytes());
        return Base64.getEncoder().encodeToString(mensajeCifrado);
    }

    public static String descifrar(String mensajeCifradoBase64, SecretKey clave) throws Exception {
        Cipher cipher = Cipher.getInstance(ALGORITMO);
        cipher.init(Cipher.DECRYPT_MODE, clave);
        byte[] mensajeBytes = Base64.getDecoder().decode(mensajeCifradoBase64);
        byte[] mensajeDescifrado = cipher.doFinal(mensajeBytes);
        return new String(mensajeDescifrado);
    }
}
