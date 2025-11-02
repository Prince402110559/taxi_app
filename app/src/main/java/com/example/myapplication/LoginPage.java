package com.example.myapplication;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInOptionsExtension;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.Firebase;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class LoginPage extends AppCompatActivity {

    private EditText emailEnter, passwords;
    private Button btnLogin, btnRegister;
    private View progressOverlay;

    private FirebaseAuth auth;
    private ActivityResultLauncher<Intent> googleLauncher;
    private GoogleSignInClient gsc;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login_page);

        emailEnter = findViewById(R.id.enterEmail);
        passwords  = findViewById(R.id.enterPassword);
        btnLogin   = findViewById(R.id.btnLogin);
        btnRegister= findViewById(R.id.btnRegister);
        progressOverlay = findViewById(R.id.progressRoot);

        auth = FirebaseAuth.getInstance();

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        gsc = GoogleSignIn.getClient(this, gso);

        googleLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(result.getData());
                        try {
                            GoogleSignInAccount account = task.getResult(ApiException.class);
                            if (account == null) { showInfo("Google sign-in was canceled."); return; }
                            String idToken = account.getIdToken();
                            if (idToken == null) { showInfo("Google sign-in failed. Please try again."); return; }

                            showProgress(true);
                            AuthCredential cred = GoogleAuthProvider.getCredential(idToken, null);
                            auth.signInWithCredential(cred)
                                    .addOnSuccessListener(r -> {
                                        FirebaseUser user = auth.getCurrentUser();
                                        if (user == null) { showProgress(false); showInfo("Google sign-in failed."); return; }

                                        String uid = user.getUid();
                                        String fName = account.getGivenName() != null ? account.getGivenName() : "";
                                        String lName = account.getFamilyName() != null ? account.getFamilyName() : "";
                                        String email = user.getEmail();

                                        Map<String, Object> userData = new HashMap<>();
                                        userData.put("email", email);
                                        userData.put("firstName", fName);
                                        userData.put("lastName", lName);

                                        FirebaseFirestore.getInstance().collection("users").document(uid)
                                                .set(userData)
                                                .addOnSuccessListener(aVoid -> {
                                                    showProgress(false);
                                                    goHome();
                                                })
                                                .addOnFailureListener(e -> {
                                                    showProgress(false);
                                                    showInfo("Signed in, but couldn’t save your profile. You can continue using the app.");
                                                    goHome();
                                                });
                                    })
                                    .addOnFailureListener(e -> {
                                        showProgress(false);
                                        showInfo(mapAuthError(e));
                                    });
                        } catch (ApiException e) {
                            showInfo("Google sign-in was canceled.");
                        }
                    } else {
                        showInfo("Google sign-in was canceled.");
                    }
                });

        findViewById(R.id.btnGoogleLogin).setOnClickListener(v -> {
            showProgress(true);
            googleLauncher.launch(gsc.getSignInIntent());
        });

        btnLogin.setOnClickListener(v -> {
            String em = emailEnter.getText().toString().trim();
            String pw = passwords.getText().toString();

            String validation = validateCredentials(em, pw);
            if (validation != null) { showInfo(validation); return; }

            showProgress(true);
            auth.signInWithEmailAndPassword(em, pw)
                    .addOnSuccessListener(r -> {
                        showProgress(false);
                        FirebaseUser u = auth.getCurrentUser();
                            goHome();
                    })
                    .addOnFailureListener(e -> {
                        showProgress(false);
                        showInfo(mapAuthError(e));
                    });
        });

        btnRegister.setOnClickListener(v -> startActivity(new Intent(this, RegisterPage.class)));
    }

    private void goHome() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }

    // Human-friendly error messages
    private String mapAuthError(Exception e) {
        String msg = e != null ? e.getMessage() : "";
        if (msg == null) msg = "";
        String lower = msg.toLowerCase();

        if (lower.contains("blocked by device") || lower.contains("network")) {
            return "Cannot reach the server. Check your internet connection and try again.";
        }
        if (lower.contains("password is invalid")) {
            return "The email or password is incorrect. Please try again.";
        }
        if (lower.contains("no user record") || lower.contains("user record")) {
            return "No account found for this email. Check the email or create a new account.";
        }
        if (lower.contains("too many requests")) {
            return "Too many attempts. Please wait a moment and try again.";
        }
        if (lower.contains("requires recent login")) {
            return "For security, please sign in again and retry.";
        }
        if (lower.contains("timeout")) {
            return "The request timed out. Please try again.";
        }
        return "Something went wrong. Please try again.";
    }

    private String validateCredentials(String email, String pw) {
        if (email.isEmpty()) return "Please enter your email.";
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) return "Please enter a valid email address.";
        if (pw.isEmpty()) return "Please enter your password.";
        if (pw.length() < 6) return "Your password must be at least 6 characters.";
        return null;
    }

    private void showProgress(boolean show) {
        if (progressOverlay == null) return;
        progressOverlay.setVisibility(show ? View.VISIBLE : View.GONE);
        View root = findViewById(R.id.loginRoot);
        if (root != null) root.setEnabled(!show);
    }

    private void showInfo(String message) {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setMessage(message)
                .setPositiveButton("OK", (d,w) -> d.dismiss())
                .show();
    }
}