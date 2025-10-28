package com.example.priceeditor;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class MenuActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu);

        Button btnSeedRatePQT = findViewById(R.id.btnSeedRatePQT);
        Button btnSeedRatePMT = findViewById(R.id.btnSeedRatePMT);
        Button btnSolventOil = findViewById(R.id.btnSolventOil);

        btnSeedRatePQT.setOnClickListener(v -> {
            Intent intent = new Intent(MenuActivity.this, SeedRatePQTActivity.class);
            startActivity(intent);
        });

        btnSeedRatePMT.setOnClickListener(v -> {
            Intent intent = new Intent(MenuActivity.this, SeedRatePMTActivity.class);
            startActivity(intent);
        });

        btnSolventOil.setOnClickListener(v -> {
            Intent intent = new Intent(MenuActivity.this, SolventOilActivity.class);
            startActivity(intent);
        });
    }
}
