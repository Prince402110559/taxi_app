package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class WelcomePage extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.welcome_page); // replace with your XML file name

        // Initialize views
        TextView tvTitle = findViewById(R.id.tvTitle);
        ImageView ivTaxi = findViewById(R.id.ivTaxi);
        TextView tvSubTitle = findViewById(R.id.tvSubTitle);
        TextView tvSlogan1 = findViewById(R.id.tvSlogan1);
        TextView tvSlogan2 = findViewById(R.id.tvSlogan2);
        Button btnRegister = findViewById(R.id.btnRegister);
        Button btnLogin = findViewById(R.id.btnLogin);

        btnRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(WelcomePage.this, RegisterPage.class);
                startActivity(intent);
                 }
        });
        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(WelcomePage.this, LoginPage.class);
                startActivity(intent);
            }
        });
}

}
