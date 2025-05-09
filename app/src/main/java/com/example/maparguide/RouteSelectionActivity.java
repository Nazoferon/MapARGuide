package com.example.maparguide;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.MotionEvent;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.osmdroid.config.Configuration;
import org.osmdroid.events.MapEventsReceiver;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.MapEventsOverlay;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * Активність для вибору початкової та кінцевої точки маршруту.
 */
public class RouteSelectionActivity extends AppCompatActivity {
    private EditText startPointEditText, endPointEditText;
    private MapView mapView;
    private Button buildRouteButton;
    private FloatingActionButton myLocationButton;
    private FusedLocationProviderClient fusedLocationClient;
    private GeoPoint startPoint, endPoint;
    private Marker startMarker, endMarker;
    private MyLocationNewOverlay myLocationOverlay;
    private boolean isSelectingStart = false;
    private final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build();

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    getCurrentLocation();
                    enableMyLocationOverlay();
                } else {
                    Toast.makeText(this, "Дозвіл на геолокацію відхилено", Toast.LENGTH_LONG).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_route_selection);

        startPointEditText = findViewById(R.id.startPointEditText);
        endPointEditText = findViewById(R.id.endPointEditText);
        mapView = findViewById(R.id.mapView);
        buildRouteButton = findViewById(R.id.buildRouteButton);
        myLocationButton = findViewById(R.id.myLocationButton);
        Button selectStartButton = findViewById(R.id.selectStartButton);
        Button selectEndButton = findViewById(R.id.selectEndButton);

        // Налаштування osmdroid
        Configuration.getInstance().load(this, getPreferences(MODE_PRIVATE));
        mapView.setTileSource(org.osmdroid.tileprovider.tilesource.TileSourceFactory.MAPNIK);
        mapView.setMultiTouchControls(true);
        mapView.getController().setZoom(15.0);
        mapView.getController().setCenter(new GeoPoint(50.4501, 30.5234)); // Київ

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Ініціалізація маркерів
        startMarker = new Marker(mapView);
        startMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        startMarker.setTitle("Початкова точка");

        endMarker = new Marker(mapView);
        endMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        endMarker.setTitle("Кінцева точка");

        // Налаштування обробника натискань на карті
        MapEventsReceiver mapEventsReceiver = new MapEventsReceiver() {
            @Override
            public boolean singleTapConfirmedHelper(GeoPoint point) {
                if (isSelectingStart) {
                    setStartPoint(point);
                } else {
                    setEndPoint(point);
                }
                return true;
            }

            @Override
            public boolean longPressHelper(GeoPoint point) {
                return false;
            }
        };
        mapView.getOverlays().add(new MapEventsOverlay(mapEventsReceiver));

        // Кнопка "Моє місцезнаходження"
        myLocationButton.setOnClickListener(v -> {
            checkLocationPermission();
        });

        // Кнопки вибору точок
        selectStartButton.setOnClickListener(v -> {
            isSelectingStart = true;
            Toast.makeText(this, "Торкніться карти, щоб вибрати початкову точку", Toast.LENGTH_SHORT).show();
        });

        selectEndButton.setOnClickListener(v -> {
            isSelectingStart = false;
            Toast.makeText(this, "Торкніться карти, щоб вибрати кінцеву точку", Toast.LENGTH_SHORT).show();
        });

        // Кнопка побудови маршруту
        buildRouteButton.setOnClickListener(v -> {
            String start = startPointEditText.getText().toString().trim();
            String end = endPointEditText.getText().toString().trim();

            if (start.isEmpty() || end.isEmpty()) {
                Toast.makeText(this, "Введіть обидві точки", Toast.LENGTH_SHORT).show();
                return;
            }

            if (startPoint == null && start.equalsIgnoreCase("Поточне місце")) {
                getCurrentLocation();
            } else if (startPoint == null) {
                geocodeAddress(start, true);
            }

            if (endPoint == null) {
                geocodeAddress(end, false);
            } else if (startPoint != null) {
                buildRoute();
            }
        });

        // Перевіряємо дозвіл на доступ до місцезнаходження
        checkLocationPermission();
    }

    private void setStartPoint(GeoPoint point) {
        startPoint = point;
        startPointEditText.setText(String.format("%.6f, %.6f", point.getLatitude(), point.getLongitude()));

        // Оновлюємо маркер
        startMarker.setPosition(point);
        if (!mapView.getOverlays().contains(startMarker)) {
            mapView.getOverlays().add(startMarker);
        }
        mapView.invalidate();
    }

    private void setEndPoint(GeoPoint point) {
        endPoint = point;
        endPointEditText.setText(String.format("%.6f, %.6f", point.getLatitude(), point.getLongitude()));

        // Оновлюємо маркер
        endMarker.setPosition(point);
        if (!mapView.getOverlays().contains(endMarker)) {
            mapView.getOverlays().add(endMarker);
        }
        mapView.invalidate();
    }

    private void checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            getCurrentLocation();
            enableMyLocationOverlay();
        } else {
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
        }
    }

    private void enableMyLocationOverlay() {
        // Додаємо overlay для відображення поточного місцезнаходження
        if (myLocationOverlay == null) {
            myLocationOverlay = new MyLocationNewOverlay(new GpsMyLocationProvider(this), mapView);
            myLocationOverlay.enableMyLocation();
            mapView.getOverlays().add(myLocationOverlay);
        }
    }

    /**
     * Отримує поточне місцезнаходження через GPS.
     */
    private void getCurrentLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
                if (location != null) {
                    GeoPoint currentLocation = new GeoPoint(location.getLatitude(), location.getLongitude());
                    mapView.getController().animateTo(currentLocation);

                    // Якщо користувач хоче використати поточне місцезнаходження як початкову точку
                    if (isSelectingStart || startPointEditText.getText().toString().trim().equalsIgnoreCase("Поточне місце")) {
                        setStartPoint(currentLocation);
                    }
                } else {
                    Toast.makeText(this, "Не вдалося отримати місцезнаходження", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    /**
     * Перетворює адресу на координати через Nominatim API.
     */
    private void geocodeAddress(String address, boolean isStart) {
        // Перевіряємо, чи це не координати у форматі "широта, довгота"
        if (address.matches("[-+]?[0-9]*\\.?[0-9]+,\\s*[-+]?[0-9]*\\.?[0-9]+")) {
            try {
                String[] parts = address.split(",");
                double lat = Double.parseDouble(parts[0].trim());
                double lon = Double.parseDouble(parts[1].trim());
                GeoPoint point = new GeoPoint(lat, lon);

                if (isStart) {
                    setStartPoint(point);
                } else {
                    setEndPoint(point);
                }

                if (startPoint != null && endPoint != null) {
                    buildRoute();
                }
                return;
            } catch (Exception e) {
                // Якщо не вдалося розпарсити як координати, продовжуємо як з адресою
            }
        }

        new Thread(() -> {
            try {
                String encodedAddress = address.replace(" ", "+").replace(",", "%2C");
                String url = "https://nominatim.openstreetmap.org/search?q=" + encodedAddress +
                        "&format=json&limit=1";
                Request request = new Request.Builder()
                        .url(url)
                        .addHeader("User-Agent", "MapARGuide/1.0") // Додано User-Agent для Nominatim
                        .build();
                Response response = client.newCall(request).execute();

                if (!response.isSuccessful() || response.body() == null) {
                    throw new IOException("Сервер повернув помилку: " + response.code());
                }

                String json = response.body().string();
                JsonArray results = JsonParser.parseString(json).getAsJsonArray();

                if (results.size() == 0) {
                    runOnUiThread(() -> Toast.makeText(this, "Адресу '" + address + "' не знайдено", Toast.LENGTH_LONG).show());
                    return;
                }

                JsonObject result = results.get(0).getAsJsonObject();
                double lat = result.get("lat").getAsDouble();
                double lon = result.get("lon").getAsDouble();
                GeoPoint point = new GeoPoint(lat, lon);

                runOnUiThread(() -> {
                    if (isStart) {
                        setStartPoint(point);
                    } else {
                        setEndPoint(point);
                    }

                    // Анімуємо карту до знайденої точки
                    mapView.getController().animateTo(point);

                    if (startPoint != null && endPoint != null) {
                        buildRoute();
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this, "Помилка геокодування для '" + address + "': " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    /**
     * Передає маршрут до ARNavigationActivity.
     */
    private void buildRoute() {
        Intent intent = new Intent(this, ARNavigationActivity.class);
        intent.putExtra("startLat", startPoint.getLatitude());
        intent.putExtra("startLon", startPoint.getLongitude());
        intent.putExtra("endLat", endPoint.getLatitude());
        intent.putExtra("endLon", endPoint.getLongitude());
        startActivity(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
        if (myLocationOverlay != null) {
            myLocationOverlay.enableMyLocation();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
        if (myLocationOverlay != null) {
            myLocationOverlay.disableMyLocation();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapView.onDetach();
    }
}