package com.example.myapplication;


import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;
import com.example.myapplication.databinding.ActivityEditProfileBinding;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class EditProfileActivity extends AppCompatActivity {


    private EditText etName,etSurname;
    private View progressOverlay;

    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Inflate the layout using ViewBinding
        ActivityEditProfileBinding binding = ActivityEditProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());


        Button saveButton = findViewById(R.id.btnSave);
        etName = findViewById(R.id.etName);
        etSurname = findViewById(R.id.etSurname);

        progressOverlay = findViewById(R.id.progressRoot); // from progress_overlay include
        Button btnChangePassword = findViewById(R.id.btnChangePassword);

        btnChangePassword.setOnClickListener(v -> showChangePasswordDialog());


        db = FirebaseFirestore.getInstance();

        loadCurrentUserProfile();
        // Optional: set title of the page
        setTitle("Edit Profile");

        saveButton.setOnClickListener(v -> saveUserProfile());
    }


private void loadCurrentUserProfile() {
    FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
    if (user == null) return;

    String userId = user.getUid();
    db.collection("users").document(userId)
            .get()
            .addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    String firstName = documentSnapshot.getString("firstName");
                    String lastName = documentSnapshot.getString("lastName");

                    etName.setText(firstName);
                    etSurname.setText(lastName);

                }
            });

    }

    private void saveUserProfile() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) { showInfo("Please sign in again."); return; }

        String userId = user.getUid();
        String firstName = etName.getText().toString().trim();
        String lastName  = etSurname.getText().toString().trim();

        if (firstName.isEmpty() || lastName.isEmpty()) {
            showInfo("Please enter first and last name.");
            return;
        }

        showProgress(true);
        updateUserDocument(userId, firstName, lastName);
    }

    private void updateUserDocument(String userId, String firstName, String lastName) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("firstName", firstName);
        updates.put("lastName", lastName);

        db.collection("users").document(userId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    showProgress(false);
                    showInfo("Profile updated.");
                })
                .addOnFailureListener(e -> {
                    showProgress(false);
                    showInfo("Failed to update profile. Please try again.");
                });
    }

    private void showChangePasswordDialog() {
        View v = getLayoutInflater().inflate(R.layout.dialog_change_password, null);
        EditText etCurrent = v.findViewById(R.id.etCurrentPassword);
        EditText etNew     = v.findViewById(R.id.etNewPassword);
        EditText etConfirm = v.findViewById(R.id.etConfirmPassword);

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Change Password")
                .setView(v)
                .setNegativeButton("Cancel", (d,w) -> d.dismiss())
                .setPositiveButton("Update", (d,w) -> {
                    String curr = etCurrent.getText().toString();
                    String npw  = etNew.getText().toString();
                    String cpw  = etConfirm.getText().toString();

                    String validation = validatePasswordInputs(curr, npw, cpw);
                    if (validation != null) { showInfo(validation); return; }

                    doChangePassword(curr, npw);
                })
                .show();
    }

    private void doChangePassword(String currentPassword, String newPassword) {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        FirebaseUser user = auth.getCurrentUser();
        if (user == null || user.getEmail() == null) {
            showInfo("You need to be signed in to change your password.");
            return;
        }

        showProgress(true);
        // Re-authenticate with current email/password
        AuthCredential cred = com.google.firebase.auth.EmailAuthProvider
                .getCredential(user.getEmail(), currentPassword);

        user.reauthenticate(cred).addOnCompleteListener(reauthTask -> {
            if (!reauthTask.isSuccessful()) {
                showProgress(false);
                showInfo("Current password is incorrect. Please try again.");
                return;
            }
            user.updatePassword(newPassword).addOnCompleteListener(updateTask -> {
                showProgress(false);
                if (updateTask.isSuccessful()) {
                    showInfo("Password updated successfully.");
                } else {
                    showInfo(mapAuthError(updateTask.getException()));
                }
            });
        });
    }

    private String validatePasswordInputs(String current, String npw, String cpw) {
        if (current.isEmpty()) return "Please enter your current password.";
        if (npw.isEmpty()) return "Please enter a new password.";
        if (npw.length() < 6) return "New password must be at least 6 characters.";
        if (!npw.equals(cpw)) return "New passwords do not match.";
        if (npw.equals(current)) return "New password must be different from the current one.";
        return null;
    }

    private void showProgress(boolean show) {
        if (progressOverlay == null) return;
        progressOverlay.setVisibility(show ? View.VISIBLE : View.GONE);
        View root = findViewById(android.R.id.content);
        if (root != null) root.setEnabled(!show);
    }

    private void showInfo(String message) {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setMessage(message)
                .setPositiveButton("OK", (d,w) -> d.dismiss())
                .show();
    }

    private String mapAuthError(Exception e) {
        String msg = (e != null && e.getMessage() != null) ? e.getMessage() : "";
        String lower = msg.toLowerCase();
        if (lower.contains("requires recent login")) return "For security, please sign in again and try updating the password.";
        if (lower.contains("weak-password")) return "That password is too weak. Try a stronger one.";
        if (lower.contains("network")) return "Cannot reach the server. Check your internet and try again.";
        return "Couldn’t update your password. Please try again.";
    }
}





