package com.example.myapplication;

import android.os.Bundle;
import android.widget.Button;

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

        Button logout = findViewById(R.id.logoutBtn);

        logout.setOnClickListener(v ->{

            // TODO: 2025/10/17 we need to add a logout function 
        });

        // Example: if you have a switch for notifications
        // binding.switchNotifications.setOnCheckedChangeListener((buttonView, isChecked) -> {
        //     // Save settings here
        // });
    }
}
