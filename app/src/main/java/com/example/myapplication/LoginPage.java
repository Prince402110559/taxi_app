package com.example.myapplication;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class LoginPage extends AppCompatActivity {
    private TextView title;
    private TextView subTitle;
    private TextView subText;
    private EditText emailEnter;
    private EditText passwords;
    private Button btnLogin;
    private Button btnGoogle;
    private Button btnApple;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login_page);
        title = findViewById(R.id.Title);
        subTitle = findViewById(R.id.subTitle);
        subText = findViewById(R.id.subtext);
        emailEnter = findViewById(R.id.emailEnter);
        passwords = findViewById(R.id.passwords);
        btnLogin = findViewById(R.id.btnLogin);
        btnGoogle = findViewById(R.id.Google);
        btnApple = findViewById(R.id.Apple);

        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(LoginPage.this, "Login button pressed!", Toast.LENGTH_SHORT).show();
            }
        });
        btnGoogle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(LoginPage.this, "Google button pressed!", Toast.LENGTH_SHORT).show();
            }
        });

        btnApple.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(LoginPage.this, "Apple button pressed!", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
