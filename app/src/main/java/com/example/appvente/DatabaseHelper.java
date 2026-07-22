package com.example.appvente;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "appvente.db";
    private static final int DATABASE_VERSION = 1;

    public static final String TABLE_VENDEUR = "VENDEUR";
    public static final String COL_ID = "idvend";
    public static final String COL_NOM = "nom";
    public static final String COL_DATENAIS = "datenais";
    public static final String COL_PHOTO = "photo";

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
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_VENDEUR);
        onCreate(db);
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
        List<Vendeur> liste = new ArrayList<>();
        String query = "SELECT * FROM " + TABLE_VENDEUR + " WHERE " + COL_NOM + " LIKE ? ORDER BY " + COL_NOM + " ASC";
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(query, new String[]{"%" + motCle + "%"});

        if (cursor.moveToFirst()) {
            do {
                liste.add(mapCursorVersVendeur(cursor));
            } while (cursor.moveToNext());
        }
        cursor.close();
        return liste;
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

    private Vendeur mapCursorVersVendeur(Cursor cursor) {
        return new Vendeur(
                cursor.getInt(cursor.getColumnIndexOrThrow(COL_ID)),
                cursor.getString(cursor.getColumnIndexOrThrow(COL_NOM)),
                cursor.getString(cursor.getColumnIndexOrThrow(COL_DATENAIS)),
                cursor.getString(cursor.getColumnIndexOrThrow(COL_PHOTO))
        );
    }
}