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
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignIn;


public class LoginPage extends AppCompatActivity {

    private EditText emailEnter;
    private EditText passwords;
    private Button btnLogin;
    private Button btnRegister;

    private FirebaseAuth auth;
    private ActivityResultLauncher<Intent> googleLauncher;
    private GoogleSignInClient gsc;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login_page);

         emailEnter = findViewById(R.id.enterEmail);
         passwords = findViewById(R.id.enterPassword);
         btnLogin = findViewById(R.id.btnLogin);
         btnRegister = findViewById(R.id.btnRegister);

        auth = FirebaseAuth.getInstance();

         GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                 .requestIdToken(getString(R.string.default_web_client_id))
                 .requestEmail()
                 .build();
         gsc = GoogleSignIn.getClient(this, gso);
         googleLauncher= registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),result -> {
             if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                 Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(result.getData());
                 try {
                     GoogleSignInAccount account = task.getResult(ApiException.class);
                     AuthCredential cred = GoogleAuthProvider.getCredential(account.getIdToken(), null);
                     auth.signInWithCredential(cred)
                             .addOnSuccessListener(r -> {
                                 toast("Signed in with Google!");
                                 startActivity(new Intent(this, MainActivity.class));
                                 finish();
                             })
                             .addOnFailureListener(e -> toast("Google sign-in failed: " + e.getMessage()));
                 } catch (ApiException e) { toast("Google sign-in canceled"); }
             }
         });

         findViewById(R.id.btnGoogleLogin).setOnClickListener(v -> googleLauncher.launch(gsc.getSignInIntent()));


        btnLogin.setOnClickListener(v -> {
            String em = emailEnter.getText().toString().trim();
            String pw = passwords.getText().toString();
            if (em.isEmpty() || pw.length() < 6) { toast("Enter valid email and 6+ char password"); return; }
            auth.signInWithEmailAndPassword(em, pw)
                    .addOnSuccessListener(r -> goHome())
                    .addOnFailureListener(e -> toast("Login failed: " + e.getMessage()));
        });

        btnRegister.setOnClickListener(v -> startActivity(new Intent(this, RegisterPage.class)));


    }
    private void goHome() { startActivity(new Intent(this, MainActivity.class)); finish(); }
    private void toast(String s) { Toast.makeText(this, s, Toast.LENGTH_SHORT).show(); }

}
