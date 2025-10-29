package com.example.myapplication;


import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.myapplication.databinding.ActivityEditProfileBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class EditProfileActivity extends AppCompatActivity {


    private Button saveButton;
    private ActivityEditProfileBinding binding;
    private EditText etName,etSurname;

    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Inflate the layout using ViewBinding
        binding = ActivityEditProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());


        saveButton = findViewById(R.id.btnSave);
        etName = findViewById(R.id.etName);
        etSurname = findViewById(R.id.etSurname);


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
    if (user == null) return;

    String userId = user.getUid();
    String firstName = etName.getText().toString().trim();
    String lastName = etSurname.getText().toString().trim();

    if (firstName.isEmpty() || lastName.isEmpty()) {
        Toast.makeText(this, "Please enter first and last name", Toast.LENGTH_SHORT).show();
        return;
    }

        updateUserDocument(userId, firstName, lastName);
    }


private void updateUserDocument(String userId, String firstName, String lastName ) {
    Map<String, Object> updates = new HashMap<>();
    updates.put("firstName", firstName);
    updates.put("lastName", lastName);


    db.collection("users").document(userId)
            .update(updates)
            .addOnSuccessListener(aVoid -> Toast.makeText(this, "Profile updated", Toast.LENGTH_SHORT).show())
            .addOnFailureListener(e -> Toast.makeText(this, "Failed to update profile", Toast.LENGTH_SHORT).show());
}

}


