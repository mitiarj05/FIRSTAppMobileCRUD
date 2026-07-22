package com.example.appvente;

import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class VendeurAdapter extends RecyclerView.Adapter<VendeurAdapter.VendeurViewHolder> {

    public interface OnVendeurClickListener {
        void onModifier(Vendeur vendeur);
        void onSupprimer(Vendeur vendeur);
    }

    private static final SimpleDateFormat FORMAT_STOCKAGE = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    private static final SimpleDateFormat FORMAT_AFFICHAGE = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
    private static final int TAILLE_AVATAR_DP = 58;

    private List<Vendeur> listeVendeurs;
    private final OnVendeurClickListener listener;

    public VendeurAdapter(List<Vendeur> listeVendeurs, OnVendeurClickListener listener) {
        this.listeVendeurs = listeVendeurs;
        this.listener = listener;
    }

    public void setListe(List<Vendeur> nouvelleListe) {
        this.listeVendeurs = nouvelleListe;
        notifyDataSetChanged();
    }

    /** Renvoie le vendeur affiché à une position donnée (utilisé pour le glisser-pour-supprimer). */
    public Vendeur getVendeurAt(int position) {
        return listeVendeurs.get(position);
    }

    @NonNull
    @Override
    public VendeurViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_vendeur, parent, false);
        return new VendeurViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VendeurViewHolder holder, int position) {
        Vendeur vendeur = listeVendeurs.get(position);
        holder.txtNom.setText(vendeur.getNom());
        holder.txtDatenais.setText(formaterDateAffichage(vendeur.getDatenais()));

        if (vendeur.getPhoto() != null && !vendeur.getPhoto().isEmpty()) {
            try {
                holder.imgPhoto.setImageURI(Uri.parse(vendeur.getPhoto()));
            } catch (Exception e) {
                afficherAvatarInitiales(holder, vendeur.getNom());
            }
        } else {
            // Pas de photo : on affiche un avatar coloré avec les initiales, plus soigné
            // qu'un simple pictogramme générique identique pour tout le monde.
            afficherAvatarInitiales(holder, vendeur.getNom());
        }

        holder.btnModifier.setOnClickListener(v -> listener.onModifier(vendeur));
        holder.btnSupprimer.setOnClickListener(v -> listener.onSupprimer(vendeur));
        // Toute la carte est cliquable : un tap en dehors des icônes ouvre la modification.
        holder.itemView.setOnClickListener(v -> listener.onModifier(vendeur));
    }

    private void afficherAvatarInitiales(VendeurViewHolder holder, String nom) {
        float densite = holder.itemView.getResources().getDisplayMetrics().density;
        int taillePx = Math.round(TAILLE_AVATAR_DP * densite);
        holder.imgPhoto.setImageBitmap(AvatarUtils.creerAvatarInitiales(nom, taillePx));
    }

    private String formaterDateAffichage(String dateStockage) {
        if (dateStockage == null || dateStockage.isEmpty()) {
            return "";
        }
        try {
            Date date = FORMAT_STOCKAGE.parse(dateStockage);
            if (date != null) {
                return "Né(e) le " + FORMAT_AFFICHAGE.format(date);
            }
        } catch (ParseException ignored) {
        }
        return dateStockage;
    }

    @Override
    public int getItemCount() {
        return listeVendeurs.size();
    }

    static class VendeurViewHolder extends RecyclerView.ViewHolder {
        ImageView imgPhoto;
        TextView txtNom, txtDatenais;
        View btnModifier, btnSupprimer;

        public VendeurViewHolder(@NonNull View itemView) {
            super(itemView);
            imgPhoto = itemView.findViewById(R.id.imgPhoto);
            txtNom = itemView.findViewById(R.id.txtNom);
            txtDatenais = itemView.findViewById(R.id.txtDatenais);
            btnModifier = itemView.findViewById(R.id.btnModifier);
            btnSupprimer = itemView.findViewById(R.id.btnSupprimer);
        }
    }
}