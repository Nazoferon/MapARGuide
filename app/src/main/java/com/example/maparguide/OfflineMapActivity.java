package com.example.maparguide;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class OfflineMapActivity extends AppCompatActivity {
    private Spinner regionSpinner;
    private ProgressBar downloadProgress;
    private Button downloadButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_offline_map);

        regionSpinner = findViewById(R.id.regionSpinner);
        downloadButton = findViewById(R.id.downloadButton);
        downloadProgress = findViewById(R.id.downloadProgress);

        // Налаштування спіннера з регіонами
        String[] regions = {"Kyiv", "Lviv", "Odessa"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, regions);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        regionSpinner.setAdapter(adapter);

        downloadButton.setOnClickListener(v -> {
            String selectedRegion = regionSpinner.getSelectedItem().toString();
            downloadProgress.setVisibility(View.VISIBLE);
            downloadButton.setEnabled(false);

            // Імітація завантаження (3 секунди)
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                downloadProgress.setVisibility(View.GONE);
                downloadButton.setEnabled(true);
                Toast.makeText(this, "Карта для " + selectedRegion + " завантажена!", Toast.LENGTH_SHORT).show();
            }, 3000);
        });
    }
}
