package com.example.appvente;

import android.content.Context;
import android.content.SharedPreferences;

/** Gère la session de l'utilisateur connecté via SharedPreferences. */
public class SessionManager {

    private static final String PREF_NOM = "session_utilisateur";
    private static final String KEY_ID = "id_user";
    private static final String KEY_NOM = "nom";
    private static final String KEY_EMAIL = "email";

    private final SharedPreferences preferences;

    public SessionManager(Context context) {
        preferences = context.getSharedPreferences(PREF_NOM, Context.MODE_PRIVATE);
    }

    /** Enregistre la session après une connexion ou une inscription réussie. */
    public void sauvegarderSession(Utilisateur utilisateur) {
        preferences.edit()
                .putInt(KEY_ID, utilisateur.getId())
                .putString(KEY_NOM, utilisateur.getNom())
                .putString(KEY_EMAIL, utilisateur.getEmail())
                .apply();
    }

    /** Vrai si un utilisateur est actuellement connecté. */
    public boolean estConnecte() {
        return preferences.getInt(KEY_ID, -1) != -1;
    }

    public int getId() {
        return preferences.getInt(KEY_ID, -1);
    }

    public String getNom() {
        return preferences.getString(KEY_NOM, "");
    }

    public String getEmail() {
        return preferences.getString(KEY_EMAIL, "");
    }

    /** Déconnecte l'utilisateur et efface la session. */
    public void deconnecter() {
        preferences.edit().clear().apply();
    }
}
