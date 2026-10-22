package pe.utp.sigecapjv.util;

import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/** Hash versionado, con sal independiente por contraseña. */
public final class Contrasenas {
    private static final int ITERACIONES = 600_000;
    private static final SecureRandom AZAR = new SecureRandom();
    private Contrasenas() { }

    public static String crearHash(char[] clave) {
        if (clave == null || clave.length < 12 || clave.length > 128)
            throw new IllegalArgumentException("La contraseña debe tener entre 12 y 128 caracteres.");
        byte[] sal = new byte[16];
        AZAR.nextBytes(sal);
        byte[] hash = derivar(clave, sal);
        try {
            return "pbkdf2-sha256$" + ITERACIONES + "$"
                    + Base64.getEncoder().encodeToString(sal) + "$"
                    + Base64.getEncoder().encodeToString(hash);
        } finally { Arrays.fill(hash, (byte) 0); }
    }

    public static boolean verificar(char[] clave, String almacenado) {
        if (clave == null || clave.length > 128 || almacenado == null) return false;
        String[] partes = almacenado.split("\\$", -1);
        if (partes.length != 4 || !partes[0].equals("pbkdf2-sha256")
                || !partes[1].equals(String.valueOf(ITERACIONES))) return false;
        byte[] sal;
        byte[] esperado;
        try {
            sal = Base64.getDecoder().decode(partes[2]);
            esperado = Base64.getDecoder().decode(partes[3]);
        } catch (IllegalArgumentException ex) { return false; }
        if (sal.length != 16 || esperado.length != 32) return false;
        byte[] obtenido = derivar(clave, sal);
        try { return MessageDigest.isEqual(esperado, obtenido); }
        finally { Arrays.fill(obtenido, (byte) 0); Arrays.fill(esperado, (byte) 0); }
    }

    private static byte[] derivar(char[] clave, byte[] sal) {
        PBEKeySpec spec = new PBEKeySpec(clave, sal, ITERACIONES, 256);
        try { return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded(); }
        catch (GeneralSecurityException ex) { throw new IllegalStateException("No se pudo verificar la contraseña.", ex); }
        finally { spec.clearPassword(); }
    }
}
