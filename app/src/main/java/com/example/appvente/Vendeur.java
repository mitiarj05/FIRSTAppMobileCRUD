package com.example.appvente;

public class Vendeur {
    private int idvend;
    private String nom;
    private String datenais;
    private String photo;

    public Vendeur(int idvend, String nom, String datenais, String photo) {
        this.idvend = idvend;
        this.nom = nom;
        this.datenais = datenais;
        this.photo = photo;
    }

    public Vendeur(String nom, String datenais, String photo) {
        this(0, nom, datenais, photo);
    }

    public int getIdvend() {
        return idvend;
    }

    public void setIdvend(int idvend) {
        this.idvend = idvend;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public String getDatenais() {
        return datenais;
    }

    public void setDatenais(String datenais) {
        this.datenais = datenais;
    }

    public String getPhoto() {
        return photo;
    }

    public void setPhoto(String photo) {
        this.photo = photo;
    }
}