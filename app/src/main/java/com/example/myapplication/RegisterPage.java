package com.example.myapplication;


import static androidx.core.content.ContextCompat.startActivity;

import android.content.Intent;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

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
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import com.google.android.gms.common.SignInButton;
import com.google.firebase.firestore.FirebaseFirestore;


import androidx.appcompat.app.AppCompatActivity;

import java.util.HashMap;
import java.util.Map;

public class RegisterPage extends AppCompatActivity{
    private FirebaseAuth auth;
    private GoogleSignInClient gsc;
    private ActivityResultLauncher<Intent> googleLauncher;

    private EditText firstName, lastName, emailEnter, passwords, confirmPassword;
    private Button  btnRegister, btnLogin;

     // Profile pic URI

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




        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        gsc = GoogleSignIn.getClient(this, gso);

        googleLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(result.getData());
                try {
                    GoogleSignInAccount account = task.getResult(ApiException.class);
                    AuthCredential cred = GoogleAuthProvider.getCredential(account.getIdToken(), null);
                    auth.signInWithCredential(cred)
                            .addOnSuccessListener(r -> {
                                toast("Registered with Google!");
                                startActivity(new Intent(this, MainActivity.class));
                                finish();
                            })
                            .addOnFailureListener(e -> toast("Google sign-in failed: " + e.getMessage()));
                } catch (ApiException e) { toast("Google sign-in canceled"); }
            }
        });

        findViewById(R.id.Google).setOnClickListener(v -> googleLauncher.launch(gsc.getSignInIntent()));

        btnRegister.setOnClickListener(v -> {
            String em = emailEnter.getText().toString().trim();
            String pw = passwords.getText().toString();
            String cpw = confirmPassword.getText().toString();
            String fName = firstName.getText().toString().trim();
            String lName = lastName.getText().toString().trim();
            if (em.isEmpty() || pw.length() < 6) {
                toast("Enter valid email and 6+ char password");
                return;
            }
            if (!pw.equals(cpw)) {
                toast("Passwords do not match");
                return;
            }
            if (fName.isEmpty() || lName.isEmpty()) {
                toast("Enter first and last name");
                return;
            }
            auth.createUserWithEmailAndPassword(em, pw)
                    .addOnSuccessListener(authResult -> {
                        FirebaseUser user = authResult.getUser();
                        if (user != null) {
                            saveUserData(user.getUid(), fName, lName, em);
                        }
                    })
                    .addOnFailureListener(e -> toast("Sign up failed: " + e.getMessage()));
        });

        // Go back to Login
        btnLogin.setOnClickListener(v ->
                startActivity(new Intent(this, LoginPage.class))
        );
    }

    private void saveUserData(String userId, String firstName, String lastName, String email) {
        Map<String, Object> userData = new HashMap<>();
        userData.put("firstName", firstName);
        userData.put("lastName", lastName);
        userData.put("email", email);

        db.collection("users").document(userId)
                .set(userData)
                .addOnSuccessListener(aVoid -> {
                    toast("Registration success!");
                    startActivity(new Intent(this, MainActivity.class));
                    finish();
                })
                .addOnFailureListener(e -> toast("Failed to save user data"));
    }
    private void toast(String s) { Toast.makeText(this, s, Toast.LENGTH_SHORT).show(); }



}
