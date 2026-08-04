package com.example.appvente;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;

/**
 * Génère un avatar circulaire avec les initiales du vendeur, coloré selon
 * un hash de son nom (même principe que Gmail ou Contacts Android).
 * Utilisé dans la liste quand le vendeur n'a pas de photo enregistrée,
 * ce qui donne un rendu plus soigné qu'une simple silhouette grise.
 */
public final class AvatarUtils {

    /** Palette de couleurs agréables, choisies pour bien contraster avec du texte blanc. */
    private static final int[] PALETTE = {
            0xFF4F46E5, 0xFF7C3AED, 0xFF0EA5E9, 0xFF10B981,
            0xFFF59E0B, 0xFFEC4899, 0xFF6366F1, 0xFF06B6D4,
            0xFF8B5CF6, 0xFF22C55E
    };

    private AvatarUtils() {
        // Classe utilitaire : pas d'instanciation.
    }

    /**
     * Crée un bitmap circulaire de taille donnée (en pixels réels) contenant
     * les initiales du nom, sur un fond coloré déterminé par ce nom.
     */
    public static Bitmap creerAvatarInitiales(String nom, int taillePx) {
        Bitmap bitmap = Bitmap.createBitmap(taillePx, taillePx, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        Paint fond = new Paint(Paint.ANTI_ALIAS_FLAG);
        fond.setColor(couleurPour(nom));
        canvas.drawCircle(taillePx / 2f, taillePx / 2f, taillePx / 2f, fond);

        Paint texte = new Paint(Paint.ANTI_ALIAS_FLAG);
        texte.setColor(Color.WHITE);
        texte.setTextSize(taillePx * 0.42f);
        texte.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        texte.setTextAlign(Paint.Align.CENTER);

        String initiales = initialesDe(nom);
        Paint.FontMetrics fm = texte.getFontMetrics();
        float y = taillePx / 2f - (fm.ascent + fm.descent) / 2f;
        canvas.drawText(initiales, taillePx / 2f, y, texte);

        return bitmap;
    }

    /** Renvoie 1 ou 2 lettres majuscules à partir du nom (ex: "Rakoto Jean" -> "RJ"). */
    private static String initialesDe(String nom) {
        if (nom == null || nom.trim().isEmpty()) {
            return "?";
        }
        String[] mots = nom.trim().split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < mots.length && i < 2; i++) {
            if (!mots[i].isEmpty()) {
                sb.append(Character.toUpperCase(mots[i].charAt(0)));
            }
        }
        return sb.length() > 0 ? sb.toString() : "?";
    }

    /** Choisit toujours la même couleur pour un même nom (répartition sur la palette via hashcode). */
    private static int couleurPour(String nom) {
        int hash = nom == null ? 0 : Math.abs(nom.hashCode());
        return PALETTE[hash % PALETTE.length];
    }
}