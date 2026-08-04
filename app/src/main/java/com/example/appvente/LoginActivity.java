package com.example.appvente;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

/**
 * Écran de connexion : vérifie les identifiants en base puis ouvre l'écran
 * principal. Si aucun compte n'existe, un lien mène à l'écran d'inscription.
 */
public class LoginActivity extends AppCompatActivity {

    private TextInputLayout layoutEmail;
    private TextInputLayout layoutMdp;
    private TextInputEditText editEmail;
    private TextInputEditText editMdp;
    private MaterialButton btnConnexion;

    private DatabaseHelper dbHelper;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sessionManager = new SessionManager(this);
        dbHelper = new DatabaseHelper(this);

        // Déjà connecté : on va directement à l'écran principal.
        if (sessionManager.estConnecte()) {
            ouvrirPrincipal();
            return;
        }

        setContentView(R.layout.activity_login);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.loginRoot), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        layoutEmail = findViewById(R.id.layoutEmail);
        layoutMdp = findViewById(R.id.layoutMdp);
        editEmail = findViewById(R.id.editEmail);
        editMdp = findViewById(R.id.editMdp);
        btnConnexion = findViewById(R.id.btnConnexion);

        jouerAnimationsEntree();

        btnConnexion.setOnClickListener(v -> seConnecter());

        findViewById(R.id.txtInscription).setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, SignupActivity.class));
            overridePendingTransition(R.anim.anim_slide_in_right, R.anim.anim_slide_out_left);
        });
    }

    /** Anime l'apparition successive du logo, du titre et de la carte de connexion. */
    private void jouerAnimationsEntree() {
        Animation fadeUp = AnimationUtils.loadAnimation(this, R.anim.anim_fade_in_up);
        findViewById(R.id.imgLogo).startAnimation(fadeUp);

        Animation titre = AnimationUtils.loadAnimation(this, R.anim.anim_fade_in_up);
        titre.setStartOffset(110);
        findViewById(R.id.txtTitre).startAnimation(titre);

        Animation sousTitre = AnimationUtils.loadAnimation(this, R.anim.anim_fade_in_up);
        sousTitre.setStartOffset(200);
        findViewById(R.id.txtSousTitre).startAnimation(sousTitre);

        Animation carte = AnimationUtils.loadAnimation(this, R.anim.anim_scale_in);
        carte.setStartOffset(280);
        findViewById(R.id.cardConnexion).startAnimation(carte);
    }

    private void seConnecter() {
        String email = texte(editEmail).trim();
        String motDePasse = texte(editMdp);

        boolean valide = true;
        if (TextUtils.isEmpty(email)) {
            layoutEmail.setError(getString(R.string.champ_obligatoire));
            valide = false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            layoutEmail.setError(getString(R.string.email_invalide));
            valide = false;
        }

        if (TextUtils.isEmpty(motDePasse)) {
            layoutMdp.setError(getString(R.string.champ_obligatoire));
            valide = false;
        }

        if (!valide) {
            return;
        }

        Utilisateur utilisateur = dbHelper.verifierIdentifiants(email, motDePasse);
        if (utilisateur == null) {
            Toast.makeText(this, R.string.identifiants_incorrects, Toast.LENGTH_SHORT).show();
            return;
        }

        sessionManager.sauvegarderSession(utilisateur);
        Toast.makeText(this, getString(R.string.connexion_reussie, utilisateur.getNom()), Toast.LENGTH_SHORT).show();
        ouvrirPrincipal();
    }

    private void ouvrirPrincipal() {
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
        overridePendingTransition(R.anim.anim_slide_in_right, R.anim.anim_slide_out_left);
    }

    private String texte(TextInputEditText champ) {
        return champ.getText() == null ? "" : champ.getText().toString();
    }
}
