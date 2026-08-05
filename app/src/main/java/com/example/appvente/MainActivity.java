package com.example.appvente;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Écran principal : affiche la liste des vendeurs (lecture), permet
 * de lancer l'ajout, la modification et la suppression (bouton ou glisser
 * la carte vers la gauche), ainsi que la recherche par nom via la barre
 * de recherche dédiée.
 */
public class MainActivity extends AppCompatActivity implements VendeurAdapter.OnVendeurClickListener {

    private DatabaseHelper dbHelper;
    private VendeurAdapter adapter;
    private RecyclerView recyclerView;
    private View etatVide;
    private TextView txtListeVide;
    private TextView txtHeaderSalutation;
    private TextView txtHeaderCompte;
    private ImageView imgHeaderAvatar;
    private ImageButton btnLogout;
    private Toolbar toolbar;
    private ExtendedFloatingActionButton fabAjouter;
    private SessionManager sessionManager;
    private SearchView searchView;
    private TextView txtDateDebutFiltre;
    private TextView txtDateFinFiltre;
    private ImageView btnEffacerFiltre;

    /** Mémorise la recherche en cours, pour savoir quel message vide afficher. */
    private String rechercheEnCours = "";

    /** Bornes du filtre de date de naissance (au format "yyyy-MM-dd"). */
    private String filtreDateDebut = "";
    private String filtreDateFin = "";

    private static final SimpleDateFormat FORMAT_DATE_STOCKAGE = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    private static final SimpleDateFormat FORMAT_DATE_AFFICHAGE = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

    public static final String EXTRA_ID = "extra_id";
    public static final String EXTRA_NOM = "extra_nom";
    public static final String EXTRA_DATENAIS = "extra_datenais";
    public static final String EXTRA_PHOTO = "extra_photo";
    public static final int REQUEST_CODE_AJOUT = 1;
    public static final int REQUEST_CODE_MODIF = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sessionManager = new SessionManager(this);

        // Personne n'est connecté : retour à l'écran de connexion.
        if (!sessionManager.estConnecte()) {
            redirigerVersConnexion();
            return;
        }

        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        dbHelper = new DatabaseHelper(this);

        toolbar = findViewById(R.id.toolbarMain);
        setSupportActionBar(toolbar);

        recyclerView = findViewById(R.id.recyclerViewVendeurs);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setLayoutAnimation(AnimationUtils.loadLayoutAnimation(this, R.anim.anim_layout_items));
        configurerGlisserPourSupprimer();
        etatVide = findViewById(R.id.etatVide);
        txtListeVide = findViewById(R.id.txtListeVide);
        txtHeaderSalutation = findViewById(R.id.txtHeaderSalutation);
        txtHeaderCompte = findViewById(R.id.txtHeaderCompte);
        imgHeaderAvatar = findViewById(R.id.imgHeaderAvatar);
        btnLogout = findViewById(R.id.btnLogout);
        btnLogout.setOnClickListener(v -> seDeconnecter());

        initialiserEnTete();

        Animation animBanniere = AnimationUtils.loadAnimation(this, R.anim.anim_fade_in_up);
        animBanniere.setDuration(500);
        findViewById(R.id.headerBanner).startAnimation(animBanniere);

        View.OnClickListener ouvrirAjout = v -> {
            Intent intent = new Intent(MainActivity.this, AddEditVendeurActivity.class);
            startActivityForResult(intent, REQUEST_CODE_AJOUT);
            overridePendingTransition(R.anim.anim_slide_in_right, R.anim.anim_slide_out_left);
        };

        fabAjouter = findViewById(R.id.fabAjouter);
        fabAjouter.setOnClickListener(ouvrirAjout);
        findViewById(R.id.btnAjouterVide).setOnClickListener(ouvrirAjout);

        configurerRecherche();

        chargerListe();
    }

    /** Efface la session et ouvre l'écran de connexion. */
    private void redirigerVersConnexion() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
        overridePendingTransition(R.anim.anim_fade_in, R.anim.anim_fade_out);
    }

    /** Remplit la bannière d'accueil avec le nom et l'avatar de l'utilisateur connecté. */
    private void initialiserEnTete() {
        String nom = sessionManager.getNom();
        txtHeaderSalutation.setText(getString(R.string.bonjour, nom.isEmpty() ? getString(R.string.app_name) : nom));

        float densite = getResources().getDisplayMetrics().density;
        int taillePx = Math.round(54 * densite);
        imgHeaderAvatar.setImageBitmap(AvatarUtils.creerAvatarInitiales(nom.isEmpty() ? "App" : nom, taillePx));
    }

    /** Recharge la liste complète depuis la base (utilisé au démarrage et après CRUD). */
    private void chargerListe() {
        rechercheEnCours = "";
        filtreDateDebut = "";
        filtreDateFin = "";
        searchView.setQuery("", false);
        miseAJourAffichageFiltres();
        appliquerFiltres();
        // Anime l'apparition des cartes (sauf pendant une recherche en direct).
        recyclerView.scheduleLayoutAnimation();
    }

    /** Met à jour le RecyclerView, le compteur, et l'état vide éventuel. */
    private void afficherResultats(List<Vendeur> vendeurs) {
        if (adapter == null) {
            adapter = new VendeurAdapter(vendeurs, this);
            recyclerView.setAdapter(adapter);
        } else {
            adapter.setListe(vendeurs);
        }

        txtHeaderCompte.setText(getResources().getQuantityString(
                R.plurals.compte_vendeurs, vendeurs.size(), vendeurs.size()));

        boolean vide = vendeurs.isEmpty();
        if (vide) {
            etatVide.setVisibility(View.VISIBLE);
            etatVide.startAnimation(AnimationUtils.loadAnimation(this, R.anim.anim_fade_in));
        } else {
            etatVide.setVisibility(View.GONE);
        }
        recyclerView.setVisibility(vide ? View.GONE : View.VISIBLE);

        // Le bouton flottant "Ajouter" ne s'affiche que si la liste contient déjà
        // des vendeurs, pour éviter d'avoir deux boutons "Ajouter" redondants
        // en même temps que celui de l'état vide.
        if (vide) {
            fabAjouter.hide();
        } else {
            fabAjouter.show();
        }

        if (vide) {
            if (rechercheEnCours.isEmpty() && !filtreDateActif()) {
                txtListeVide.setText(R.string.aucun_vendeur);
            } else if (rechercheEnCours.isEmpty()) {
                txtListeVide.setText(R.string.aucun_resultat_filtre);
            } else {
                txtListeVide.setText(getString(R.string.aucun_resultat, rechercheEnCours));
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // On revient toujours à la liste complète après un ajout/modif/suppr.
        chargerListe();
    }

    /** Initialise la barre de recherche et ses couleurs pour rester lisible sur fond blanc. */
    private void configurerRecherche() {
        searchView = findViewById(R.id.searchBar);
        searchView.setIconifiedByDefault(false);
        searchView.setMaxWidth(Integer.MAX_VALUE);
        searchView.setQueryHint(getString(R.string.rechercher_hint));

        EditText champRecherche = searchView.findViewById(androidx.appcompat.R.id.search_src_text);
        champRecherche.setTextColor(ContextCompat.getColor(this, R.color.colorTextPrimary));
        champRecherche.setHintTextColor(ContextCompat.getColor(this, R.color.colorTextSecondary));
        champRecherche.setTextSize(16);

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                rechercher(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                rechercher(newText);
                return true;
            }
        });

        configurerFiltreDates();
    }

    /** Filtre la liste par date de naissance : deux bornes (début et fin) via un calendrier. */
    private void configurerFiltreDates() {
        txtDateDebutFiltre = findViewById(R.id.txtDateDebutFiltre);
        txtDateFinFiltre = findViewById(R.id.txtDateFinFiltre);
        btnEffacerFiltre = findViewById(R.id.btnEffacerFiltre);

        txtDateDebutFiltre.setOnClickListener(v -> afficherDatePickerFiltre(true));
        txtDateFinFiltre.setOnClickListener(v -> afficherDatePickerFiltre(false));
        btnEffacerFiltre.setOnClickListener(v -> {
            filtreDateDebut = "";
            filtreDateFin = "";
            miseAJourAffichageFiltres();
            appliquerFiltres();
        });
    }

    /** Ouvre le calendrier pour choisir la borne de début (estDebut) ou de fin du filtre. */
    private void afficherDatePickerFiltre(boolean estDebut) {
        Calendar calendrier = Calendar.getInstance();
        String dateActuelle = estDebut ? filtreDateDebut : filtreDateFin;
        if (!dateActuelle.isEmpty()) {
            try {
                Date date = FORMAT_DATE_STOCKAGE.parse(dateActuelle);
                if (date != null) {
                    calendrier.setTime(date);
                }
            } catch (ParseException ignored) {
            }
        }

        DatePickerDialog dialog = new DatePickerDialog(this,
                (view, selYear, selMonth, selDay) -> {
                    String date = String.format(Locale.getDefault(),
                            "%04d-%02d-%02d", selYear, selMonth + 1, selDay);
                    if (estDebut) {
                        filtreDateDebut = date;
                    } else {
                        filtreDateFin = date;
                    }
                    // Si les deux bornes sont définies dans le mauvais ordre, on les réajuste.
                    if (!filtreDateDebut.isEmpty() && !filtreDateFin.isEmpty()
                            && filtreDateDebut.compareTo(filtreDateFin) > 0) {
                        if (estDebut) {
                            filtreDateFin = filtreDateDebut;
                        } else {
                            filtreDateDebut = filtreDateFin;
                        }
                    }
                    miseAJourAffichageFiltres();
                    appliquerFiltres();
                },
                calendrier.get(Calendar.YEAR),
                calendrier.get(Calendar.MONTH),
                calendrier.get(Calendar.DAY_OF_MONTH));

        // Une date de naissance ne peut pas être dans le futur.
        dialog.getDatePicker().setMaxDate(System.currentTimeMillis());
        dialog.show();
    }

    /** Affiche les bornes choisies dans les champs (ou le libellé "Du/Au" si vide). */
    private void miseAJourAffichageFiltres() {
        txtDateDebutFiltre.setText(filtreDateDebut.isEmpty()
                ? getString(R.string.date_debut_filtre)
                : formaterDateFiltre(filtreDateDebut));
        txtDateFinFiltre.setText(filtreDateFin.isEmpty()
                ? getString(R.string.date_fin_filtre)
                : formaterDateFiltre(filtreDateFin));
    }

    private boolean filtreDateActif() {
        return !filtreDateDebut.isEmpty() || !filtreDateFin.isEmpty();
    }

    private String formaterDateFiltre(String dateStockage) {
        try {
            Date date = FORMAT_DATE_STOCKAGE.parse(dateStockage);
            if (date != null) {
                return FORMAT_DATE_AFFICHAGE.format(date);
            }
        } catch (ParseException ignored) {
        }
        return dateStockage;
    }

    /** Déconnecte l'utilisateur, après confirmation, puis revient à l'écran de connexion. */
    private void seDeconnecter() {
        String nom = sessionManager.getNom();
        String message = getString(R.string.deconnexion_confirmer_message,
                nom.isEmpty() ? getString(R.string.app_name) : nom);

        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.deconnexion_confirmer_titre)
                .setMessage(message)
                .setPositiveButton(R.string.se_deconnecter, (d, which) -> {
                    sessionManager.deconnecter();
                    Toast.makeText(this, R.string.deconnexion_reussie, Toast.LENGTH_SHORT).show();
                    redirigerVersConnexion();
                })
                .setNegativeButton(R.string.annuler, null)
                .create();

        dialog.setOnShowListener(d -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                    .setTextColor(ContextCompat.getColor(this, R.color.colorError));
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE)
                    .setTextColor(ContextCompat.getColor(this, R.color.colorTextSecondary));
        });
        dialog.show();
    }

    /**
     * Filtre la liste par nom (recherche en direct, insensible à la casse)
     * et par plage de dates de naissance.
     */
    private void rechercher(String motCle) {
        rechercheEnCours = motCle == null ? "" : motCle.trim();
        appliquerFiltres();
    }

    /** Applique les critères actifs (nom/date + plage de naissance) et met à jour la liste. */
    private void appliquerFiltres() {
        List<Vendeur> resultats = dbHelper.rechercherVendeurs(
                rechercheEnCours, filtreDateDebut, filtreDateFin);
        afficherResultats(resultats);
    }

    @Override
    public void onModifier(Vendeur vendeur) {
        Intent intent = new Intent(this, AddEditVendeurActivity.class);
        intent.putExtra(EXTRA_ID, vendeur.getIdvend());
        intent.putExtra(EXTRA_NOM, vendeur.getNom());
        intent.putExtra(EXTRA_DATENAIS, vendeur.getDatenais());
        intent.putExtra(EXTRA_PHOTO, vendeur.getPhoto());
        startActivityForResult(intent, REQUEST_CODE_MODIF);
        overridePendingTransition(R.anim.anim_slide_in_right, R.anim.anim_slide_out_left);
    }

    @Override
    public void onSupprimer(Vendeur vendeur) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.supprimer_titre)
                .setMessage(getString(R.string.supprimer_message, vendeur.getNom()))
                .setPositiveButton(R.string.oui, (dialog, which) -> {
                    dbHelper.supprimerVendeur(vendeur.getIdvend());
                    Toast.makeText(this, R.string.vendeur_supprime, Toast.LENGTH_SHORT).show();
                    chargerListe();
                })
                .setNegativeButton(R.string.non, null)
                .show();
    }

    /**
     * Active le glisser-pour-supprimer : faire glisser une carte de la liste vers
     * la gauche déclenche la même confirmation de suppression que le bouton poubelle.
     * Si l'utilisateur annule, la carte reprend sa place initiale.
     */
    private void configurerGlisserPourSupprimer() {
        ItemTouchHelper.SimpleCallback callback = new ItemTouchHelper.SimpleCallback(
                0, ItemTouchHelper.LEFT) {

            @Override
            public boolean onMove(@NonNull RecyclerView rv, @NonNull RecyclerView.ViewHolder vh,
                                  @NonNull RecyclerView.ViewHolder target) {
                return false; // Pas de réorganisation par glisser-déposer, seulement la suppression.
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                Vendeur vendeur = adapter.getVendeurAt(position);

                new AlertDialog.Builder(MainActivity.this)
                        .setTitle(R.string.supprimer_titre)
                        .setMessage(getString(R.string.supprimer_message, vendeur.getNom()))
                        .setPositiveButton(R.string.oui, (dialog, which) -> {
                            dbHelper.supprimerVendeur(vendeur.getIdvend());
                            Toast.makeText(MainActivity.this, R.string.vendeur_supprime, Toast.LENGTH_SHORT).show();
                            chargerListe();
                        })
                        // Annulé : on remet la carte à sa place (sinon elle resterait visuellement glissée).
                        .setNegativeButton(R.string.non, (dialog, which) -> adapter.notifyItemChanged(position))
                        .setOnCancelListener(dialog -> adapter.notifyItemChanged(position))
                        .show();
            }

            @Override
            public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView,
                                    @NonNull RecyclerView.ViewHolder viewHolder,
                                    float dX, float dY, int actionState, boolean isCurrentlyActive) {
                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);

                if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE && dX < 0) {
                    View itemView = viewHolder.itemView;

                    // Fond rouge derrière la carte pendant le glissement.
                    Paint paint = new Paint();
                    paint.setColor(ContextCompat.getColor(MainActivity.this, R.color.colorDelete));
                    RectF fond = new RectF(itemView.getRight() + dX, itemView.getTop(),
                            itemView.getRight(), itemView.getBottom());
                    c.drawRoundRect(fond, 16f, 16f, paint);

                    // Icône poubelle blanche, alignée à droite.
                    Drawable icone = ContextCompat.getDrawable(MainActivity.this, android.R.drawable.ic_menu_delete);
                    if (icone != null) {
                        int taille = itemView.getHeight() / 3;
                        int haut = itemView.getTop() + (itemView.getHeight() - taille) / 2;
                        int marge = 32;
                        icone.setBounds(itemView.getRight() - taille - marge, haut,
                                itemView.getRight() - marge, haut + taille);
                        icone.setTint(Color.WHITE);
                        icone.draw(c);
                    }
                }
            }
        };
        new ItemTouchHelper(callback).attachToRecyclerView(recyclerView);
    }
}