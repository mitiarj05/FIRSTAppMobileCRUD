package com.example.appvente;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FacebookAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;

import java.util.Arrays;

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

    private FirebaseAuth firebaseAuth;
    private GoogleSignInClient googleSignInClient;
    private ActivityResultLauncher<Intent> googleSignInLauncher;
    private CallbackManager facebookCallbackManager;

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

        initialiserConnexionSociale();

        btnConnexion.setOnClickListener(v -> seConnecter());

        findViewById(R.id.btnGoogle).setOnClickListener(v -> seConnecterGoogle());
        findViewById(R.id.btnFacebook).setOnClickListener(v -> seConnecterFacebook());

        findViewById(R.id.txtInscription).setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, SignupActivity.class));
            overridePendingTransition(R.anim.anim_slide_in_right, R.anim.anim_slide_out_left);
        });

        findViewById(R.id.txtMotDePasseOublie).setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, ForgotPasswordActivity.class));
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

    /** Initialise Firebase Auth puis les clients Google et Facebook. */
    private void initialiserConnexionSociale() {
        try {
            if (FirebaseApp.getApps(this).isEmpty()) {
                FirebaseApp.initializeApp(this);
            }
            firebaseAuth = FirebaseAuth.getInstance();
        } catch (Exception e) {
            firebaseAuth = null;
        }
        configurerGoogle();
        configurerFacebook();
    }

    private void configurerGoogle() {
        try {
            int resId = getResources().getIdentifier("default_web_client_id", "string", getPackageName());
            if (resId == 0) {
                return;
            }
            String webClientId = getString(resId);
            if (webClientId == null || webClientId.startsWith("000000000000")) {
                return; // google-services.json de remplacement : pas encore configuré
            }
            GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestIdToken(webClientId)
                    .requestEmail()
                    .build();
            googleSignInClient = GoogleSignIn.getClient(this, gso);
            googleSignInLauncher = registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                            Task<GoogleSignInAccount> tache = GoogleSignIn.getSignedInAccountFromIntent(result.getData());
                            gererCompteGoogle(tache);
                        }
                    });
        } catch (Exception e) {
            googleSignInClient = null;
        }
    }

    private void configurerFacebook() {
        try {
            if (!FacebookSdk.isInitialized()) {
                return; // auto-initialisation via le manifeste : pas encore prête
            }
            facebookCallbackManager = CallbackManager.Factory.create();
            LoginManager.getInstance().registerCallback(facebookCallbackManager,
                    new FacebookCallback<LoginResult>() {
                        @Override
                        public void onSuccess(LoginResult loginResult) {
                            String jeton = loginResult.getAccessToken().getToken();
                            AuthCredential credential = FacebookAuthProvider.getCredential(jeton);
                            seConnecterAvecCredential(credential, null, null);
                        }

                        @Override
                        public void onCancel() {
                            // L'utilisateur a annulé la connexion : rien à faire.
                        }

                        @Override
                        public void onError(FacebookException erreur) {
                            Toast.makeText(LoginActivity.this, R.string.erreur_connexion_facebook, Toast.LENGTH_SHORT).show();
                        }
                    });
        } catch (Exception e) {
            facebookCallbackManager = null;
        }
    }

    private void seConnecterGoogle() {
        if (googleSignInClient == null || googleSignInLauncher == null) {
            Toast.makeText(this, R.string.social_non_config, Toast.LENGTH_SHORT).show();
            return;
        }
        googleSignInLauncher.launch(googleSignInClient.getSignInIntent());
    }

    private void seConnecterFacebook() {
        if (facebookCallbackManager == null) {
            Toast.makeText(this, R.string.social_non_config, Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            LoginManager.getInstance().logInWithReadPermissions(this, Arrays.asList("email", "public_profile"));
        } catch (Exception e) {
            Toast.makeText(this, R.string.erreur_connexion_facebook, Toast.LENGTH_SHORT).show();
        }
    }

    private void gererCompteGoogle(Task<GoogleSignInAccount> tache) {
        try {
            GoogleSignInAccount compte = tache.getResult(ApiException.class);
            String idToken = compte.getIdToken();
            if (idToken == null) {
                Toast.makeText(this, R.string.erreur_connexion_google, Toast.LENGTH_SHORT).show();
                return;
            }
            AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
            seConnecterAvecCredential(credential, compte.getDisplayName(), compte.getEmail());
        } catch (ApiException e) {
            Toast.makeText(this, R.string.erreur_connexion_google, Toast.LENGTH_SHORT).show();
        }
    }

    /** Connecte l'utilisateur via le credential Firebase, puis enregistre la session locale. */
    private void seConnecterAvecCredential(AuthCredential credential, String nom, String email) {
        if (firebaseAuth == null) {
            Toast.makeText(this, R.string.social_non_config, Toast.LENGTH_SHORT).show();
            return;
        }
        firebaseAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, tache -> {
                    if (tache.isSuccessful()) {
                        FirebaseUser utilisateur = firebaseAuth.getCurrentUser();
                        String nomFinal = nom != null ? nom
                                : (utilisateur != null && utilisateur.getDisplayName() != null
                                ? utilisateur.getDisplayName() : getString(R.string.utilisateur));
                        String emailFinal = email != null ? email
                                : (utilisateur != null && utilisateur.getEmail() != null
                                ? utilisateur.getEmail() : "");
                        int id = utilisateur != null && utilisateur.getUid() != null
                                ? (utilisateur.getUid().hashCode() & 0x7FFFFFFF) | 1 : 1;
                        sessionManager.sauvegarderSession(new Utilisateur(id, nomFinal, emailFinal));
                        Toast.makeText(this, getString(R.string.connexion_reussie, nomFinal), Toast.LENGTH_SHORT).show();
                        ouvrirPrincipal();
                    } else {
                        Toast.makeText(this, R.string.erreur_connexion_sociale, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (facebookCallbackManager != null) {
            facebookCallbackManager.onActivityResult(requestCode, resultCode, data);
        }
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
