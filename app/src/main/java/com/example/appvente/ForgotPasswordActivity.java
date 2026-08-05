package com.example.appvente;

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
 * Écran « Mot de passe oublié » : l'utilisateur vérifie son email en base,
 * choisit un nouveau mot de passe (saisi deux fois) qui est alors mis à jour.
 */
public class ForgotPasswordActivity extends AppCompatActivity {

    private TextInputLayout layoutEmail;
    private TextInputLayout layoutNouveauMdp;
    private TextInputLayout layoutConfirmation;
    private TextInputEditText editEmail;
    private TextInputEditText editNouveauMdp;
    private TextInputEditText editConfirmation;

    private DatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.forgotRoot), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        dbHelper = new DatabaseHelper(this);

        Toolbar toolbar = findViewById(R.id.toolbarForgot);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> quitter());

        layoutEmail = findViewById(R.id.layoutEmail);
        layoutNouveauMdp = findViewById(R.id.layoutNouveauMdp);
        layoutConfirmation = findViewById(R.id.layoutConfirmation);
        editEmail = findViewById(R.id.editEmail);
        editNouveauMdp = findViewById(R.id.editNouveauMdp);
        editConfirmation = findViewById(R.id.editConfirmation);

        jouerAnimationsEntree();

        findViewById(R.id.btnReinitialiser).setOnClickListener(v -> reinitialiser());

        // Retour système : même transition animée que la flèche de la toolbar.
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                quitter();
            }
        });
    }

    /** Anime l'apparition du sous-titre puis de la carte. */
    private void jouerAnimationsEntree() {
        Animation sousTitre = AnimationUtils.loadAnimation(this, R.anim.anim_fade_in_up);
        findViewById(R.id.txtSousTitre).startAnimation(sousTitre);

        Animation carte = AnimationUtils.loadAnimation(this, R.anim.anim_fade_in_up);
        carte.setStartOffset(140);
        findViewById(R.id.cardReinitialiser).startAnimation(carte);
    }

    private void reinitialiser() {
        String email = texte(editEmail).trim();
        String nouveauMdp = texte(editNouveauMdp);
        String confirmation = texte(editConfirmation);

        boolean valide = true;

        if (TextUtils.isEmpty(email)) {
            layoutEmail.setError(getString(R.string.champ_obligatoire));
            valide = false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            layoutEmail.setError(getString(R.string.email_invalide));
            valide = false;
        } else if (!dbHelper.emailExiste(email)) {
            layoutEmail.setError(getString(R.string.email_inexistant));
            valide = false;
        }

        if (TextUtils.isEmpty(nouveauMdp)) {
            layoutNouveauMdp.setError(getString(R.string.champ_obligatoire));
            valide = false;
        } else if (nouveauMdp.length() < 6) {
            layoutNouveauMdp.setError(getString(R.string.mot_de_passe_court));
            valide = false;
        }

        if (TextUtils.isEmpty(confirmation)) {
            layoutConfirmation.setError(getString(R.string.champ_obligatoire));
            valide = false;
        } else if (!confirmation.equals(nouveauMdp)) {
            layoutConfirmation.setError(getString(R.string.mdp_non_conforme));
            valide = false;
        }

        if (!valide) {
            return;
        }

        dbHelper.modifierMotDePasse(email, nouveauMdp);
        Toast.makeText(this, R.string.mot_de_passe_reinitialise, Toast.LENGTH_SHORT).show();
        quitter();
    }

    /** Ferme l'écran avec la transition de retour animée. */
    private void quitter() {
        finish();
        overridePendingTransition(R.anim.anim_slide_in_left, R.anim.anim_slide_out_right);
    }

    private String texte(TextInputEditText champ) {
        return champ.getText() == null ? "" : champ.getText().toString();
    }
}
