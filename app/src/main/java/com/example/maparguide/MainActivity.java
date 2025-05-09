package com.example.maparguide;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_CODE = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button startNavigationButton = findViewById(R.id.startNavigationButton);
        Button offlineMapButton = findViewById(R.id.offlineMapButton);

        // Check and request permissions
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_VIDEO) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_AUDIO) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{
                            Manifest.permission.READ_MEDIA_IMAGES,
                            Manifest.permission.READ_MEDIA_VIDEO,
                            Manifest.permission.READ_MEDIA_AUDIO
                    },
                    REQUEST_CODE);
        }

        startNavigationButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, RouteSelectionActivity.class);
            startActivity(intent);
        });

        offlineMapButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, OfflineMapActivity.class);
            startActivity(intent);
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted
                Toast.makeText(this, "Permissions granted", Toast.LENGTH_SHORT).show();
            } else {
                // Permission denied
                Toast.makeText(this, "Permissions denied", Toast.LENGTH_SHORT).show();
            }
        }
    }
}