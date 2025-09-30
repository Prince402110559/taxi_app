package com.example.myapplication;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class WelcomePage extends AppCompatActivity {
    private TextView tvTitle;
    private TextView tvSubTitle;
    private TextView tvSlogan1;
    private TextView tvSlogan2;
    private ImageView ivTaxi;
    private Button btnRegister;
    private Button btnLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.welcome_page); // replace with your XML file name

        // Initialize views
        tvTitle = findViewById(R.id.tvTitle);
        ivTaxi = findViewById(R.id.ivTaxi);
        tvSubTitle = findViewById(R.id.tvSubTitle);
        tvSlogan1 = findViewById(R.id.tvSlogan1);
        tvSlogan2 = findViewById(R.id.tvSlogan2);
        btnRegister = findViewById(R.id.btnRegister);
        btnLogin = findViewById(R.id.btnLogin);

        btnRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(WelcomePage.this, "Register button pressed!", Toast.LENGTH_SHORT).show();
            }
        });
        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(WelcomePage.this, "Login button pressed!", Toast.LENGTH_SHORT).show();
            }
        });
}

}
