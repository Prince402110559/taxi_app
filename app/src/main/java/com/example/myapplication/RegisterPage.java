package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class RegisterPage extends AppCompatActivity {

    private FirebaseAuth auth;
    private GoogleSignInClient gsc;
    private ActivityResultLauncher<Intent> googleLauncher;

    private View progressOverlay;

    private EditText firstName, lastName, emailEnter, passwords, confirmPassword;
    private Button btnRegister, btnLogin;

    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.register_page);

        firstName = findViewById(R.id.firstName);
        lastName = findViewById(R.id.lastName);
        emailEnter = findViewById(R.id.emailEnter);
        passwords = findViewById(R.id.passwords);
        confirmPassword = findViewById(R.id.confirmPassword);
        btnRegister = findViewById(R.id.btnRegister);
        btnLogin = findViewById(R.id.btnLogin);
        progressOverlay = findViewById(R.id.progressRoot);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

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
                            if (account == null || account.getIdToken() == null) {
                                showProgress(false);
                                showInfo("Google sign-up failed. Please try again.");
                                return;
                            }
                            AuthCredential cred = GoogleAuthProvider.getCredential(account.getIdToken(), null);
                            auth.signInWithCredential(cred)
                                    .addOnSuccessListener(r -> {
                                        FirebaseUser user = auth.getCurrentUser();
                                        if (user != null) {
                                            String uid = user.getUid();
                                            String fName = account.getGivenName() != null ? account.getGivenName() : "";
                                            String lName = account.getFamilyName() != null ? account.getFamilyName() : "";
                                            String email = account.getEmail();

                                            saveUserData(uid, fName, lName, email, () -> {
                                                showProgress(false);
                                                goHome();
                                            });
                                        } else {
                                            showProgress(false);
                                            showInfo("Google sign-up failed. Please try again.");
                                        }
                                    })
                                    .addOnFailureListener(e -> {
                                        showProgress(false);
                                        showInfo(mapAuthError(e));
                                    });
                        } catch (ApiException e) {
                            showProgress(false);
                            showInfo("Google sign-up was canceled.");
                        }
                    } else {
                        showProgress(false);
                        showInfo("Google sign-up was canceled.");
                    }
                });

        findViewById(R.id.Google).setOnClickListener(v -> {
            showProgress(true);
            googleLauncher.launch(gsc.getSignInIntent());
        });

        btnRegister.setOnClickListener(v -> {
            String fName = firstName.getText().toString().trim();
            String lName = lastName.getText().toString().trim();
            String em = emailEnter.getText().toString().trim();
            String pw = passwords.getText().toString();
            String cpw = confirmPassword.getText().toString();

            String validation = validateInputs(fName, lName, em, pw, cpw);
            if (validation != null) { showInfo(validation); return; }

            showProgress(true);
            auth.createUserWithEmailAndPassword(em, pw)
                    .addOnSuccessListener(authResult -> {
                        FirebaseUser user = authResult.getUser();
                        if (user == null) { showProgress(false); showInfo("Sign up failed. Please try again."); return; }

                        saveUserData(user.getUid(), fName, lName, em, () -> {
                            showProgress(false);
                            goHome();
                        });
                    })
                    .addOnFailureListener(e -> {
                        showProgress(false);
                        showInfo(mapAuthError(e));
                    });
        });

        btnLogin.setOnClickListener(v -> startActivity(new Intent(this, LoginPage.class)));
    }

    private void saveUserData(String userId, String firstName, String lastName, String email, Runnable onDone) {
        Map<String, Object> userData = new HashMap<>();
        userData.put("firstName", firstName);
        userData.put("lastName", lastName);
        userData.put("email", email);

        db.collection("users").document(userId)
                .set(userData)
                .addOnSuccessListener(aVoid -> onDone.run())
                .addOnFailureListener(e -> {
                    showInfo("Account created, but we couldn’t save your profile. You can continue.");
                    onDone.run();
                });
    }

    private void goHome() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }

    private void showProgress(boolean show) {
        if (progressOverlay == null) return;
        progressOverlay.setVisibility(show ? View.VISIBLE : View.GONE);
        View root = findViewById(R.id.registerRoot);
        if (root != null) root.setEnabled(!show);
    }

    private void showInfo(String message) {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setMessage(message)
                .setPositiveButton("OK", (d,w) -> d.dismiss())
                .show();
    }

    private String validateInputs(String fName, String lName, String email, String pw, String cpw) {
        if (fName.isEmpty()) return "Please enter your first name.";
        if (lName.isEmpty()) return "Please enter your last name.";
        if (email.isEmpty()) return "Please enter your email.";
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) return "Please enter a valid email address.";
        if (pw.isEmpty()) return "Please enter a password.";
        if (pw.length() < 6) return "Your password must be at least 6 characters.";
        if (!pw.equals(cpw)) return "Passwords do not match.";
        return null;
    }

    private String mapAuthError(Exception e) {
        String msg = (e != null && e.getMessage() != null) ? e.getMessage() : "";
        String lower = msg.toLowerCase();
        if (lower.contains("email address is already in use")) return "An account already exists with this email. Try logging in.";
        if (lower.contains("badly formatted")) return "That email address doesn’t look right.";
        if (lower.contains("network") || lower.contains("blocked by device")) return "Cannot reach the server. Check your internet and try again.";
        if (lower.contains("too many requests")) return "Too many attempts. Please wait a moment and try again.";
        return "Something went wrong. Please try again.";
    }
}
