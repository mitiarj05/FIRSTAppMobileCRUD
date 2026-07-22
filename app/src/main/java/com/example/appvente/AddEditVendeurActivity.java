package com.example.appvente;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;

/**
 * Écran unique servant à la fois pour AJOUTER et MODIFIER un vendeur.
 * Si un idvend est reçu en extra (via l'Intent), on est en mode "modification" :
 * les champs sont pré-remplis et l'enregistrement fait un UPDATE.
 * Sinon, on est en mode "ajout" et l'enregistrement fait un INSERT.
 */
public class AddEditVendeurActivity extends AppCompatActivity {

    // Clés utilisées pour sauvegarder/restaurer l'état lors d'une rotation d'écran.
    private static final String KEY_IDVEND = "key_idvend";
    private static final String KEY_NOM = "key_nom";
    private static final String KEY_DATENAIS = "key_datenais";
    private static final String KEY_PHOTO_URI = "key_photo_uri";
    private static final String KEY_INITIAL_NOM = "key_initial_nom";
    private static final String KEY_INITIAL_DATENAIS = "key_initial_datenais";
    private static final String KEY_INITIAL_PHOTO_URI = "key_initial_photo_uri";

    // Constantes pour la validation de l'âge
    private static final int AGE_MINIMUM = 18;
    private static final int AGE_MAXIMUM = 100;

    private TextInputEditText editNom;
    private TextView txtDatenais;
    private ImageView imgPhoto;
    private TextView txtChoisirPhotoLabel;
    private MaterialButton btnEnregistrer;
    private ProgressBar progressBar;

    private String datenaisChoisie = "";
    private Uri photoUri = null;

    private int idvend = 0; // 0 = nouveau vendeur (pas encore en base)
    private DatabaseHelper dbHelper;

    // Valeurs initiales du formulaire, utilisées pour détecter s'il y a des
    // modifications non enregistrées (pour la confirmation de sortie).
    private String initialNom = "";
    private String initialDatenais = "";
    private String initialPhotoUriStr = "";

    // Lance le sélecteur de fichiers du système pour choisir une image dans la galerie.
    private final ActivityResultLauncher<String[]> selectionPhotoLauncher =
            registerForActivityResult(new ActivityResultContracts.OpenDocument(), uri -> {
                if (uri != null) {
                    try {
                        // Garde l'accès à l'image même après la fermeture de l'app.
                        getContentResolver().takePersistableUriPermission(
                                uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    } catch (SecurityException ignored) {
                    }
                    photoUri = uri;
                    imgPhoto.setImageURI(photoUri);
                    txtChoisirPhotoLabel.setText(R.string.changer_photo);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_edit_vendeur);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.addEditRoot), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        dbHelper = new DatabaseHelper(this);

        // Le thème de l'app est en "NoActionBar" : on utilise donc une Toolbar
        // définie dans le layout, avec une flèche de retour (icône de fermeture).
        Toolbar toolbar = findViewById(R.id.toolbarAddEdit);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> confirmerFermeture());

        editNom = findViewById(R.id.editNom);
        txtDatenais = findViewById(R.id.txtDatenais);
        imgPhoto = findViewById(R.id.imgPhotoEdit);
        txtChoisirPhotoLabel = findViewById(R.id.txtChoisirPhotoLabel);
        btnEnregistrer = findViewById(R.id.btnEnregistrer);
        progressBar = findViewById(R.id.progressBarEnregistrement);

        // La photo se change aussi bien en touchant l'image, le petit bouton
        // caméra superposé, ou le libellé texte en dessous.
        View.OnClickListener choisirPhotoListener = v ->
                selectionPhotoLauncher.launch(new String[]{"image/*"});
        findViewById(R.id.btnChoisirPhoto).setOnClickListener(choisirPhotoListener);
        txtChoisirPhotoLabel.setOnClickListener(choisirPhotoListener);
        imgPhoto.setOnClickListener(choisirPhotoListener);

        txtDatenais.setOnClickListener(v -> afficherDatePicker());

        if (savedInstanceState != null) {
            // ---- Écran recréé après une rotation : on restaure l'état en cours ----
            restaurerEtat(savedInstanceState);
        } else {
            initialiserFormulaire();
        }

        // Bouton retour du système (geste ou touche) : on vérifie aussi les modifications non enregistrées.
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                confirmerFermeture();
            }
        });

        btnEnregistrer.setOnClickListener(v -> enregistrer());
    }

    /** Premier lancement de l'écran (pas une rotation) : pré-remplissage depuis l'Intent si modification. */
    private void initialiserFormulaire() {
        Intent intent = getIntent();
        if (intent.hasExtra(MainActivity.EXTRA_ID)) {
            // ---- Mode modification : on pré-remplit avec les données existantes ----
            idvend = intent.getIntExtra(MainActivity.EXTRA_ID, 0);
            editNom.setText(intent.getStringExtra(MainActivity.EXTRA_NOM));
            datenaisChoisie = intent.getStringExtra(MainActivity.EXTRA_DATENAIS);
            txtDatenais.setText(datenaisChoisie);
            String photoExistante = intent.getStringExtra(MainActivity.EXTRA_PHOTO);
            if (photoExistante != null && !photoExistante.isEmpty()) {
                photoUri = Uri.parse(photoExistante);
                try {
                    imgPhoto.setImageURI(photoUri);
                } catch (Exception ignored) {
                }
                txtChoisirPhotoLabel.setText(R.string.changer_photo);
            }
            setTitle(R.string.modifier_vendeur);
        } else {
            // ---- Mode ajout : formulaire vide ----
            setTitle(R.string.ajouter_vendeur);
        }

        // On mémorise l'état de départ pour pouvoir détecter des modifications plus tard.
        initialNom = texteEditNom();
        initialDatenais = datenaisChoisie == null ? "" : datenaisChoisie;
        initialPhotoUriStr = photoUri != null ? photoUri.toString() : "";
    }

    /** Restaure l'écran après une rotation, à partir du Bundle sauvegardé. */
    private void restaurerEtat(@NonNull Bundle savedInstanceState) {
        idvend = savedInstanceState.getInt(KEY_IDVEND, 0);
        setTitle(idvend == 0 ? R.string.ajouter_vendeur : R.string.modifier_vendeur);

        editNom.setText(savedInstanceState.getString(KEY_NOM, ""));

        datenaisChoisie = savedInstanceState.getString(KEY_DATENAIS, "");
        txtDatenais.setText(datenaisChoisie);

        String photoTexte = savedInstanceState.getString(KEY_PHOTO_URI, "");
        if (!photoTexte.isEmpty()) {
            photoUri = Uri.parse(photoTexte);
            try {
                imgPhoto.setImageURI(photoUri);
            } catch (Exception ignored) {
            }
            txtChoisirPhotoLabel.setText(R.string.changer_photo);
        }

        initialNom = savedInstanceState.getString(KEY_INITIAL_NOM, "");
        initialDatenais = savedInstanceState.getString(KEY_INITIAL_DATENAIS, "");
        initialPhotoUriStr = savedInstanceState.getString(KEY_INITIAL_PHOTO_URI, "");
    }

    /** Sauvegarde l'état en cours du formulaire avant une rotation d'écran (ou mise en arrière-plan). */
    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(KEY_IDVEND, idvend);
        outState.putString(KEY_NOM, texteEditNom());
        outState.putString(KEY_DATENAIS, datenaisChoisie);
        outState.putString(KEY_PHOTO_URI, photoUri != null ? photoUri.toString() : "");
        outState.putString(KEY_INITIAL_NOM, initialNom);
        outState.putString(KEY_INITIAL_DATENAIS, initialDatenais);
        outState.putString(KEY_INITIAL_PHOTO_URI, initialPhotoUriStr);
    }

    private String texteEditNom() {
        return editNom.getText() == null ? "" : editNom.getText().toString();
    }

    /** Vrai si le formulaire contient des changements non enregistrés par rapport à l'état de départ. */
    private boolean formulaireModifie() {
        String photoActuelle = photoUri != null ? photoUri.toString() : "";
        return !Objects.equals(texteEditNom().trim(), initialNom.trim())
                || !Objects.equals(datenaisChoisie, initialDatenais)
                || !Objects.equals(photoActuelle, initialPhotoUriStr);
    }

    /** Ferme l'écran, en demandant confirmation si des données n'ont pas été enregistrées. */
    private void confirmerFermeture() {
        if (formulaireModifie()) {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.quitter_titre)
                    .setMessage(R.string.quitter_message)
                    .setPositiveButton(R.string.quitter, (dialog, which) -> finish())
                    .setNegativeButton(R.string.annuler, null)
                    .show();
        } else {
            finish();
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        confirmerFermeture();
        return true;
    }

    /** Ouvre un calendrier pour choisir la date de naissance. */
    private void afficherDatePicker() {
        Calendar calendrier = Calendar.getInstance();
        int annee = calendrier.get(Calendar.YEAR);
        int mois = calendrier.get(Calendar.MONTH);
        int jour = calendrier.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog dialog = new DatePickerDialog(this,
                (view, selYear, selMonth, selDay) -> {
                    datenaisChoisie = String.format(Locale.getDefault(),
                            "%04d-%02d-%02d", selYear, selMonth + 1, selDay);
                    txtDatenais.setText(datenaisChoisie);
                    txtDatenais.setError(null);
                }, annee, mois, jour);

        // Empêche physiquement de sélectionner une date future dans le calendrier.
        dialog.getDatePicker().setMaxDate(System.currentTimeMillis());

        // Calcule la date minimum (il y a AGE_MAXIMUM ans)
        Calendar calMin = Calendar.getInstance();
        calMin.add(Calendar.YEAR, -AGE_MAXIMUM);
        dialog.getDatePicker().setMinDate(calMin.getTimeInMillis());

        dialog.show();
    }

    /**
     * Vérifie si une date au format "yyyy-MM-dd" est postérieure à aujourd'hui.
     * En cas de format invalide, on considère prudemment que ce n'est pas une date future
     * (le DatePicker garantit normalement un format correct).
     */
    private boolean dateEstDansLeFutur(String dateTexte) {
        try {
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Date date = format.parse(dateTexte);
            return date != null && date.after(new Date());
        } catch (ParseException e) {
            return false;
        }
    }

    /**
     * Vérifie si la personne a au moins l'âge minimum spécifié (18 ans).
     */
    private boolean verifierAgeMinimum(String dateTexte) {
        try {
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Date dateNaissance = format.parse(dateTexte);
            Calendar calNaissance = Calendar.getInstance();
            calNaissance.setTime(dateNaissance);

            Calendar calAujourdhui = Calendar.getInstance();
            int age = calAujourdhui.get(Calendar.YEAR) - calNaissance.get(Calendar.YEAR);

            // Vérifier si l'anniversaire est déjà passé cette année
            if (calAujourdhui.get(Calendar.MONTH) < calNaissance.get(Calendar.MONTH) ||
                    (calAujourdhui.get(Calendar.MONTH) == calNaissance.get(Calendar.MONTH) &&
                            calAujourdhui.get(Calendar.DAY_OF_MONTH) < calNaissance.get(Calendar.DAY_OF_MONTH))) {
                age--;
            }

            return age >= AGE_MINIMUM;
        } catch (ParseException e) {
            return false;
        }
    }

    /**
     * Vérifie si la personne a au maximum l'âge spécifié (100 ans).
     */
    private boolean verifierAgeMaximum(String dateTexte) {
        try {
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Date dateNaissance = format.parse(dateTexte);
            Calendar calNaissance = Calendar.getInstance();
            calNaissance.setTime(dateNaissance);

            Calendar calAujourdhui = Calendar.getInstance();
            int age = calAujourdhui.get(Calendar.YEAR) - calNaissance.get(Calendar.YEAR);

            // Vérifier si l'anniversaire est déjà passé cette année
            if (calAujourdhui.get(Calendar.MONTH) < calNaissance.get(Calendar.MONTH) ||
                    (calAujourdhui.get(Calendar.MONTH) == calNaissance.get(Calendar.MONTH) &&
                            calAujourdhui.get(Calendar.DAY_OF_MONTH) < calNaissance.get(Calendar.DAY_OF_MONTH))) {
                age--;
            }

            return age <= AGE_MAXIMUM;
        } catch (ParseException e) {
            return false;
        }
    }

    /**
     * Valide le formulaire puis enregistre (ajout ou modification) en base.
     * Un indicateur de chargement (ProgressBar) s'affiche pendant l'opération et
     * tous les champs sont désactivés pour éviter une double saisie/un double-clic.
     */
    private void enregistrer() {
        String nom = texteEditNom().trim();

        boolean formulaireValide = true;

        if (TextUtils.isEmpty(nom)) {
            editNom.setError(getString(R.string.champ_obligatoire));
            formulaireValide = false;
        } else if (nom.length() < 2) {
            editNom.setError(getString(R.string.nom_trop_court));
            formulaireValide = false;
        }

        if (TextUtils.isEmpty(datenaisChoisie)) {
            txtDatenais.setError(getString(R.string.date_obligatoire));
            Toast.makeText(this, R.string.date_obligatoire, Toast.LENGTH_SHORT).show();
            formulaireValide = false;
        } else if (dateEstDansLeFutur(datenaisChoisie)) {
            txtDatenais.setError(getString(R.string.date_future));
            Toast.makeText(this, R.string.date_future, Toast.LENGTH_SHORT).show();
            formulaireValide = false;
        } else if (!verifierAgeMinimum(datenaisChoisie)) {
            txtDatenais.setError(getString(R.string.age_minimum, AGE_MINIMUM));
            Toast.makeText(this, getString(R.string.age_minimum, AGE_MINIMUM), Toast.LENGTH_SHORT).show();
            formulaireValide = false;
        } else if (!verifierAgeMaximum(datenaisChoisie)) {
            txtDatenais.setError(getString(R.string.age_maximum, AGE_MAXIMUM));
            Toast.makeText(this, getString(R.string.age_maximum, AGE_MAXIMUM), Toast.LENGTH_SHORT).show();
            formulaireValide = false;
        }

        if (!formulaireValide) {
            return;
        }

        activerModeChargement(true);

        // La base SQLite locale est quasi instantanée : on ajoute un très léger délai
        // uniquement pour que l'indicateur de chargement reste visible et perceptible
        // (bonne pratique à montrer, même si l'opération réelle est très rapide).
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            String photo = photoUri != null ? photoUri.toString() : null;
            Vendeur vendeur = new Vendeur(idvend, nom, datenaisChoisie, photo);

            if (idvend == 0) {
                dbHelper.ajouterVendeur(vendeur);
                Toast.makeText(this, R.string.vendeur_ajoute, Toast.LENGTH_SHORT).show();
            } else {
                dbHelper.modifierVendeur(vendeur);
                Toast.makeText(this, R.string.vendeur_modifie, Toast.LENGTH_SHORT).show();
            }

            setResult(RESULT_OK);
            finish();
        }, 400);
    }

    /** Affiche/masque l'indicateur de chargement et active/désactive les champs du formulaire. */
    private void activerModeChargement(boolean enCours) {
        progressBar.setVisibility(enCours ? View.VISIBLE : View.GONE);
        btnEnregistrer.setEnabled(!enCours);
        editNom.setEnabled(!enCours);
        txtDatenais.setEnabled(!enCours);
        imgPhoto.setEnabled(!enCours);
        txtChoisirPhotoLabel.setEnabled(!enCours);
        findViewById(R.id.btnChoisirPhoto).setEnabled(!enCours);
    }
}