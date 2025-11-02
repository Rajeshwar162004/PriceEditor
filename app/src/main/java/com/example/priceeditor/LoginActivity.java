package com.example.priceeditor;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class LoginActivity extends AppCompatActivity {

    private static final String CORRECT_PASSWORD = "1234";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        EditText etPassword = findViewById(R.id.etPassword);
        Button btnLogin = findViewById(R.id.btnLogin);

        btnLogin.setOnClickListener(v -> {
            String enteredPassword = etPassword.getText().toString().trim();

            if (enteredPassword.equals(CORRECT_PASSWORD)) {
                Intent intent = new Intent(LoginActivity.this, MenuActivity.class);
                startActivity(intent);
                finish();
            } else {
                Toast.makeText(this, "Incorrect Password", Toast.LENGTH_SHORT).show();
                etPassword.setText("");
            }
        });
    }
}
