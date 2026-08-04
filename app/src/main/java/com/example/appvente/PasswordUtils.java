package com.example.appvente;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/** Utilitaire pour hacher les mots de passe avant stockage en base. */
public class PasswordUtils {

    private PasswordUtils() {
    }

    /** Retourne le mot de passe haché en SHA-256 (hexadécimal). */
    public static String hashPassword(String motDePasse) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(motDePasse.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            return motDePasse;
        }
    }
}
