package com.example.myapplication;

import static android.app.PendingIntent.getActivity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.databinding.ActivitySettingsBinding;

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

            // TODO: 2025/10/17 we need to add a logout function
            Intent intent = new Intent(SettingsActivity.this, WelcomePage.class);
            Toast.makeText(SettingsActivity.this, "logout button preseddd", Toast.LENGTH_SHORT).show();
            startActivity(intent);
        });


    }
}
