package com.example.myapplication;

import static android.app.PendingIntent.getActivity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.databinding.ActivitySettingsBinding;
import com.google.firebase.Firebase;
import com.google.firebase.auth.FirebaseAuth;

public class SettingsActivity extends AppCompatActivity {

    private ActivitySettingsBinding binding;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivitySettingsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setTitle("Settings");

        Button logout = findViewById(R.id.logoutBtnSettings);

        logout.setOnClickListener(v ->{
            FirebaseAuth.getInstance().signOut();
            Intent intent = new Intent(SettingsActivity.this, WelcomePage.class);
            finish();
        });


    }
}
