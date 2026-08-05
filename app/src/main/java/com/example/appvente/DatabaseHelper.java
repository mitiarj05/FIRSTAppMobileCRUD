package com.example.appvente;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.Nullable;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "appvente.db";
    private static final int DATABASE_VERSION = 2;

    public static final String TABLE_VENDEUR = "VENDEUR";
    public static final String COL_ID = "idvend";
    public static final String COL_NOM = "nom";
    public static final String COL_DATENAIS = "datenais";
    public static final String COL_PHOTO = "photo";

    public static final String TABLE_UTILISATEUR = "UTILISATEUR";
    public static final String COL_ID_USER = "id_user";
    public static final String COL_EMAIL = "email";
    public static final String COL_MOT_DE_PASSE = "mot_de_passe";

    public DatabaseHelper(@Nullable Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createTable = "CREATE TABLE " + TABLE_VENDEUR + " (" +
                COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_NOM + " TEXT NOT NULL, " +
                COL_DATENAIS + " TEXT, " +
                COL_PHOTO + " TEXT" +
                ")";
        db.execSQL(createTable);

        String createTableUtilisateur = "CREATE TABLE " + TABLE_UTILISATEUR + " (" +
                COL_ID_USER + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_NOM + " TEXT NOT NULL, " +
                COL_EMAIL + " TEXT NOT NULL UNIQUE, " +
                COL_MOT_DE_PASSE + " TEXT NOT NULL" +
                ")";
        db.execSQL(createTableUtilisateur);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            String createTableUtilisateur = "CREATE TABLE IF NOT EXISTS " + TABLE_UTILISATEUR + " (" +
                    COL_ID_USER + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COL_NOM + " TEXT NOT NULL, " +
                    COL_EMAIL + " TEXT NOT NULL UNIQUE, " +
                    COL_MOT_DE_PASSE + " TEXT NOT NULL" +
                    ")";
            db.execSQL(createTableUtilisateur);
        }
    }

    public long ajouterVendeur(Vendeur vendeur) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_NOM, vendeur.getNom());
        values.put(COL_DATENAIS, vendeur.getDatenais());
        values.put(COL_PHOTO, vendeur.getPhoto());
        return db.insert(TABLE_VENDEUR, null, values);
    }

    public List<Vendeur> getTousLesVendeurs() {
        List<Vendeur> liste = new ArrayList<>();
        String query = "SELECT * FROM " + TABLE_VENDEUR + " ORDER BY " + COL_NOM + " ASC";
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(query, null);

        if (cursor.moveToFirst()) {
            do {
                liste.add(mapCursorVersVendeur(cursor));
            } while (cursor.moveToNext());
        }
        cursor.close();
        return liste;
    }

    public List<Vendeur> rechercherVendeurParNom(String motCle) {
        return rechercherVendeurs(motCle, "", "");
    }

    /**
     * Recherche les vendeurs en combinant un mot-clé sur le nom OU la date de
     * naissance, ainsi qu'une plage de dates de naissance (au format "yyyy-MM-dd",
     * comparaison lexicographique fiable car les dates sont stockées en toutes lettres).
     */
    public List<Vendeur> rechercherVendeurs(String motCle, String dateDebut, String dateFin) {
        List<Vendeur> liste = new ArrayList<>();
        StringBuilder query = new StringBuilder("SELECT * FROM " + TABLE_VENDEUR + " WHERE 1=1");
        List<String> arguments = new ArrayList<>();

        if (motCle != null && !motCle.trim().isEmpty()) {
            String cle = motCle.trim();
            // Le mot-clé peut être un nom, une année ("1990") ou une date de naissance.
            query.append(" AND (").append(COL_NOM).append(" LIKE ? OR ").append(COL_DATENAIS).append(" LIKE ?");
            arguments.add("%" + cle + "%");
            arguments.add("%" + cle + "%");

            // Recherche par date complète au format "jj/mm/aaaa".
            String dateStockage = convertirEnDateStockage(cle);
            if (dateStockage != null) {
                query.append(" OR ").append(COL_DATENAIS).append(" = ?");
                arguments.add(dateStockage);
            }
            query.append(")");
        }
        if (dateDebut != null && !dateDebut.isEmpty()) {
            query.append(" AND ").append(COL_DATENAIS).append(" >= ?");
            arguments.add(dateDebut);
        }
        if (dateFin != null && !dateFin.isEmpty()) {
            query.append(" AND ").append(COL_DATENAIS).append(" <= ?");
            arguments.add(dateFin);
        }

        query.append(" ORDER BY ").append(COL_NOM).append(" ASC");

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(query.toString(), arguments.toArray(new String[0]));

        if (cursor.moveToFirst()) {
            do {
                liste.add(mapCursorVersVendeur(cursor));
            } while (cursor.moveToNext());
        }
        cursor.close();
        return liste;
    }

    /** Convertit une date saisie en "jj/mm/aaaa" vers le format de stockage "aaaa-mm-jj". */
    private String convertirEnDateStockage(String valeur) {
        try {
            SimpleDateFormat formatSaisie = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            formatSaisie.setLenient(false);
            Date date = formatSaisie.parse(valeur);
            if (date != null) {
                return new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(date);
            }
        } catch (ParseException ignored) {
        }
        return null;
    }

    public int modifierVendeur(Vendeur vendeur) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_NOM, vendeur.getNom());
        values.put(COL_DATENAIS, vendeur.getDatenais());
        values.put(COL_PHOTO, vendeur.getPhoto());
        return db.update(TABLE_VENDEUR, values, COL_ID + " = ?",
                new String[]{String.valueOf(vendeur.getIdvend())});
    }

    public void supprimerVendeur(int idvend) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_VENDEUR, COL_ID + " = ?", new String[]{String.valueOf(idvend)});
    }

    /** Ajoute un nouveau compte utilisateur. Retourne l'id créé, ou -1 si l'email existe déjà. */
    public long ajouterUtilisateur(String nom, String email, String motDePasse) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_NOM, nom);
        values.put(COL_EMAIL, email);
        values.put(COL_MOT_DE_PASSE, PasswordUtils.hashPassword(motDePasse));
        return db.insert(TABLE_UTILISATEUR, null, values);
    }

    /** Vrai si un compte utilise déjà cet email. */
    public boolean emailExiste(String email) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT 1 FROM " + TABLE_UTILISATEUR +
                " WHERE " + COL_EMAIL + " = ?", new String[]{email});
        boolean existe = cursor.moveToFirst();
        cursor.close();
        return existe;
    }

    /** Vérifie les identifiants et retourne l'utilisateur si le couple email/mot de passe est valide. */
    public Utilisateur verifierIdentifiants(String email, String motDePasse) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_UTILISATEUR +
                        " WHERE " + COL_EMAIL + " = ? AND " + COL_MOT_DE_PASSE + " = ?",
                new String[]{email, PasswordUtils.hashPassword(motDePasse)});

        Utilisateur utilisateur = null;
        if (cursor.moveToFirst()) {
            utilisateur = new Utilisateur(
                    cursor.getInt(cursor.getColumnIndexOrThrow(COL_ID_USER)),
                    cursor.getString(cursor.getColumnIndexOrThrow(COL_NOM)),
                    cursor.getString(cursor.getColumnIndexOrThrow(COL_EMAIL))
            );
        }
        cursor.close();
        return utilisateur;
    }

    private Vendeur mapCursorVersVendeur(Cursor cursor) {
        return new Vendeur(
                cursor.getInt(cursor.getColumnIndexOrThrow(COL_ID)),
                cursor.getString(cursor.getColumnIndexOrThrow(COL_NOM)),
                cursor.getString(cursor.getColumnIndexOrThrow(COL_DATENAIS)),
                cursor.getString(cursor.getColumnIndexOrThrow(COL_PHOTO))
        );
    }
}