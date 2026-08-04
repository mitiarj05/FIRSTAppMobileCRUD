package com.example.appvente;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

/**
 * Écran d'inscription : crée un compte (nom, email, mot de passe) dans la base
 * puis renvoie vers l'écran de connexion. Vérifie aussi que l'email n'est pas
 * déjà utilisé et que le mot de passe est saisi deux fois à l'identique.
 */
public class SignupActivity extends AppCompatActivity {

    private TextInputLayout layoutNom;
    private TextInputLayout layoutEmail;
    private TextInputLayout layoutMdp;
    private TextInputLayout layoutConfirmationMdp;
    private TextInputEditText editNom;
    private TextInputEditText editEmail;
    private TextInputEditText editMdp;
    private TextInputEditText editConfirmationMdp;
    private MaterialButton btnInscription;

    private DatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.signupRoot), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        dbHelper = new DatabaseHelper(this);

        Toolbar toolbar = findViewById(R.id.toolbarSignup);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> quitter());

        layoutNom = findViewById(R.id.layoutNom);
        layoutEmail = findViewById(R.id.layoutEmail);
        layoutMdp = findViewById(R.id.layoutMdp);
        layoutConfirmationMdp = findViewById(R.id.layoutConfirmationMdp);
        editNom = findViewById(R.id.editNom);
        editEmail = findViewById(R.id.editEmail);
        editMdp = findViewById(R.id.editMdp);
        editConfirmationMdp = findViewById(R.id.editConfirmationMdp);
        btnInscription = findViewById(R.id.btnInscription);

        jouerAnimationsEntree();

        btnInscription.setOnClickListener(v -> sInscrire());

        findViewById(R.id.txtConnexion).setOnClickListener(v -> quitter());

        // Retour système : même transition animée que la flèche de la toolbar.
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                quitter();
            }
        });
    }

    /** Anime l'apparition du sous-titre puis de la carte d'inscription. */
    private void jouerAnimationsEntree() {
        Animation sousTitre = AnimationUtils.loadAnimation(this, R.anim.anim_fade_in_up);
        findViewById(R.id.txtSousTitre).startAnimation(sousTitre);

        Animation carte = AnimationUtils.loadAnimation(this, R.anim.anim_fade_in_up);
        carte.setStartOffset(140);
        findViewById(R.id.cardInscription).startAnimation(carte);
    }

    /** Ferme l'écran avec la transition de retour animée. */
    private void quitter() {
        finish();
        overridePendingTransition(R.anim.anim_slide_in_left, R.anim.anim_slide_out_right);
    }

    private void sInscrire() {
        String nom = texte(editNom).trim();
        String email = texte(editEmail).trim();
        String motDePasse = texte(editMdp);
        String confirmation = texte(editConfirmationMdp);

        boolean valide = true;

        if (TextUtils.isEmpty(nom)) {
            layoutNom.setError(getString(R.string.champ_obligatoire));
            valide = false;
        } else if (nom.length() < 2) {
            layoutNom.setError(getString(R.string.nom_trop_court));
            valide = false;
        }

        if (TextUtils.isEmpty(email)) {
            layoutEmail.setError(getString(R.string.champ_obligatoire));
            valide = false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            layoutEmail.setError(getString(R.string.email_invalide));
            valide = false;
        } else if (dbHelper.emailExiste(email)) {
            layoutEmail.setError(getString(R.string.email_deja_utilise));
            valide = false;
        }

        if (TextUtils.isEmpty(motDePasse)) {
            layoutMdp.setError(getString(R.string.champ_obligatoire));
            valide = false;
        } else if (motDePasse.length() < 6) {
            layoutMdp.setError(getString(R.string.mot_de_passe_court));
            valide = false;
        }

        if (TextUtils.isEmpty(confirmation)) {
            layoutConfirmationMdp.setError(getString(R.string.champ_obligatoire));
            valide = false;
        } else if (!confirmation.equals(motDePasse)) {
            layoutConfirmationMdp.setError(getString(R.string.mdp_non_conforme));
            valide = false;
        }

        if (!valide) {
            return;
        }

        dbHelper.ajouterUtilisateur(nom, email, motDePasse);
        Toast.makeText(this, R.string.compte_cree, Toast.LENGTH_SHORT).show();

        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
        overridePendingTransition(R.anim.anim_slide_in_left, R.anim.anim_slide_out_right);
    }

    private String texte(TextInputEditText champ) {
        return champ.getText() == null ? "" : champ.getText().toString();
    }
}
