package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.example.myapplication.databinding.ActivitySettingsBinding;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class SettingsActivity extends AppCompatActivity {

    private final FirebaseAuth mAuth = FirebaseAuth.getInstance();
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivitySettingsBinding binding = ActivitySettingsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        Button themeSwitchButton = findViewById(R.id.switchDarkMode);
        TextView helpSupportHeader = findViewById(R.id.helpSupportHeader);
        Button deleteAccount = findViewById(R.id.deleteAccountBtn);
        final LinearLayout helpSupportDetails = findViewById(R.id.helpSupportDetails);
        TextView termsHeader = findViewById(R.id.termsHeader);
        LinearLayout termsDetails = findViewById(R.id.termsDetails);
        TextView termsDetailsText = findViewById(R.id.termsDetailsText);
        Button logout = findViewById(R.id.logoutBtnSettings);

        String termsText = "By downloading and using this application, " +
                "you agree to comply with its terms of use and applicable laws. " +
                "The app is provided for informational, booking, and convenience purposes; all schedules, fares, and route information are subject " +
                "to change without notice. Users are responsible for ensuring their account information remains accurate and secure. Unauthorized access," +
                " misuse, or distribution of app content is strictly prohibited. We strive to ensure " +
                "the accuracy and reliability of all data presented, but cannot guarantee the availability or uninterrupted service of the app at all times. You acknowledge that use of this app is at your own discretion and risk.\n\n"
                + "Furthermore, the application and its owners are not liable for any losses, delays, or damages resulting from reliance on information provided within the app, or from technical issues beyond our control. Users should report any errors, security issues, or concerns using the provided feedback channels. We reserve the right to modify or discontinue features without prior notice to users. Personal information collected is handled according to our privacy policy, available within the app. Continued use of the application constitutes acceptance of these terms and any updates published in the future.";

        setTitle("Settings");

        logout.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            Intent intent = new Intent(SettingsActivity.this, WelcomePage.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });

        themeSwitchButton.setOnClickListener(v -> {
            int currentNightMode = getResources().getConfiguration().uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK;
            if (currentNightMode == android.content.res.Configuration.UI_MODE_NIGHT_YES) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            }
        });

        helpSupportHeader.setOnClickListener(v ->
                helpSupportDetails.setVisibility(helpSupportDetails.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE)
        );

        deleteAccount.setOnClickListener(v -> showDeleteAccountPrompt());

        termsHeader.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (termsDetails.getVisibility() == View.VISIBLE) {
                    // Hide details
                    termsDetails.setVisibility(View.GONE);
                } else {
                    // Show details and set text
                    termsDetailsText.setText(termsText);
                    termsDetails.setVisibility(View.VISIBLE);
                }
            }
        });
    }

    private void showDeleteAccountPrompt() {
        new AlertDialog.Builder(this)
                .setTitle("Delete account?")
                .setMessage("This will permanently delete your account and associated data. This action cannot be undone.")
                .setNegativeButton("Cancel", (d, w) -> d.dismiss())
                .setPositiveButton("Delete", (d, w) -> confirmAndDeleteAccount())
                .setCancelable(true)
                .show();
    }

    private void confirmAndDeleteAccount() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            goToWelcome();
            return;
        }

        // Try direct delete first; Firebase may require recent login
        user.delete().addOnCompleteListener(delTask -> {
            if (delTask.isSuccessful()) {
                onAccountDeleted(user.getUid());
            } else {
                // Re-authenticate for email/password accounts
                showReauthDialog(user);
            }
        });
    }

    private void showReauthDialog(FirebaseUser user) {
        View v = getLayoutInflater().inflate(R.layout.dialog_reauth, null);
        TextInputEditText emailEt = v.findViewById(R.id.email);
        TextInputEditText passEt  = v.findViewById(R.id.password);
        if (emailEt != null) emailEt.setText(user.getEmail());

        new AlertDialog.Builder(this)
                .setTitle("Confirm your identity")
                .setView(v)
                .setNegativeButton("Cancel", (d,w) -> d.dismiss())
                .setPositiveButton("Continue", (d,w) -> {
                    String email = emailEt != null && emailEt.getText() != null ? emailEt.getText().toString().trim() : "";
                    String pass  = passEt  != null && passEt.getText()  != null ? passEt.getText().toString()         : "";

                    if (email.isEmpty() || pass.isEmpty()) {
                        Toast.makeText(this, "Enter your email and password.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    user.reauthenticate(EmailAuthProvider.getCredential(email, pass))
                            .addOnCompleteListener(reauthTask -> {
                                if (reauthTask.isSuccessful()) {
                                    user.delete().addOnCompleteListener(finalDelete -> {
                                        if (finalDelete.isSuccessful()) onAccountDeleted(user.getUid());
                                        else showInfo("Could not delete your account. Please try again.");
                                    });
                                } else {
                                    showInfo("Re-authentication failed. Check your credentials and try again.");
                                }
                            });
                })
                .show();
    }

    private void onAccountDeleted(String uid) {
        // Optional: remove Firestore user document if you keep one
        db.collection("users").document(uid).delete()
                .addOnCompleteListener(t -> {
                    showInfo("Your account has been deleted.");
                    goToWelcome();
                });
    }

    private void goToWelcome() {
        mAuth.signOut();
        Intent intent = new Intent(SettingsActivity.this, WelcomePage.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void showInfo(String msg) {
        new AlertDialog.Builder(this)
                .setMessage(msg)
                .setPositiveButton("OK", (d,w) -> d.dismiss())
                .show();
    }
}
