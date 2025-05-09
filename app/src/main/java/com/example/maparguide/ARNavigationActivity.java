package com.example.maparguide;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.osmdroid.config.Configuration;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Polyline;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * Активність для AR-навігації з реальним маршрутом.
 */
public class ARNavigationActivity extends AppCompatActivity {
    private PreviewView cameraPreview;
    private MapView mapView;
    private NavigationOverlayView navigationOverlay;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private SensorManager sensorManager;
    private TextToSpeech tts;
    private GeoPoint currentLocation;
    private GeoPoint endPoint;
    private List<GeoPoint> routePoints = new ArrayList<>();
    private List<String> instructions = new ArrayList<>();
    private int currentInstructionIndex = 0;
    private float[] gravity = new float[3];
    private float[] geomagnetic = new float[3];
    private SensorEventListener sensorListener;
    private MyLocationNewOverlay myLocationOverlay;
    private final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build();

    private final ActivityResultLauncher<String[]> requestPermissionsLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), permissions -> {
                boolean cameraGranted = permissions.getOrDefault(Manifest.permission.CAMERA, false);
                boolean locationGranted = permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false);
                if (cameraGranted && locationGranted) {
                    startARNavigation();
                } else {
                    StringBuilder errorMessage = new StringBuilder("Відхилено дозволи: ");
                    if (!cameraGranted) errorMessage.append("Камера ");
                    if (!locationGranted) errorMessage.append("Геолокація");
                    Toast.makeText(this, errorMessage.toString(), Toast.LENGTH_LONG).show();
                    finish();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ar_navigation);

        cameraPreview = findViewById(R.id.cameraPreview);
        mapView = findViewById(R.id.mapView);
        navigationOverlay = findViewById(R.id.navigationOverlay);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        // Ініціалізація TextToSpeech
        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                tts.setLanguage(new Locale("uk"));
            } else {
                Toast.makeText(this, "Помилка ініціалізації TextToSpeech", Toast.LENGTH_SHORT).show();
            }
        });

        // Налаштування osmdroid
        Configuration.getInstance().load(this, getPreferences(MODE_PRIVATE));

        // Отримання координат із RouteSelectionActivity
        double startLat = getIntent().getDoubleExtra("startLat", 50.4501);
        double startLon = getIntent().getDoubleExtra("startLon", 30.5234);
        double endLat = getIntent().getDoubleExtra("endLat", 50.4600);
        double endLon = getIntent().getDoubleExtra("endLon", 30.5400);
        currentLocation = new GeoPoint(startLat, startLon);
        endPoint = new GeoPoint(endLat, endLon);

        // Налаштування LocationCallback
        setupLocationCallback();

        // Перевірка дозволів
        String[] requiredPermissions = {
                Manifest.permission.CAMERA,
                Manifest.permission.ACCESS_FINE_LOCATION
        };
        boolean allPermissionsGranted = true;
        for (String permission : requiredPermissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                allPermissionsGranted = false;
                break;
            }
        }

        if (allPermissionsGranted) {
            startARNavigation();
        } else {
            requestPermissionsLauncher.launch(requiredPermissions);
        }
    }

    private void setupLocationCallback() {
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    return;
                }
                for (android.location.Location location : locationResult.getLocations()) {
                    if (location != null) {
                        currentLocation = new GeoPoint(location.getLatitude(), location.getLongitude());
                        mapView.getController().setCenter(currentLocation);
                        checkRouteDeviation();
                    }
                }
            }
        };
    }

    private void startARNavigation() {
        Toast.makeText(this, "AR-навігація запущена!", Toast.LENGTH_SHORT).show();

        // Ініціалізація CameraX
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(cameraPreview.getSurfaceProvider());
                CameraSelector cameraSelector = new CameraSelector.Builder()
                        .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                        .build();
                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(this, cameraSelector, preview);
            } catch (ExecutionException | InterruptedException e) {
                Toast.makeText(this, "Помилка запуску камери", Toast.LENGTH_SHORT).show();
                e.printStackTrace();
            }
        }, ContextCompat.getMainExecutor(this));

        // Ініціалізація карти та маршруту
        initMap();
        fetchRoute();

        // Ініціалізація компаса
        initCompass();

        // Відстеження позиції
        startLocationUpdates();
    }

    private void initMap() {
        mapView.setTileSource(org.osmdroid.tileprovider.tilesource.TileSourceFactory.MAPNIK);
        mapView.setMultiTouchControls(true);
        mapView.getController().setZoom(15.0);
        mapView.getController().setCenter(currentLocation);

        // Додавання маркера поточної позиції
        myLocationOverlay = new MyLocationNewOverlay(mapView);
        myLocationOverlay.enableMyLocation();
        mapView.getOverlays().add(myLocationOverlay);
    }

    /**
     * Отримує маршрут через GraphHopper API.
     */
    private void fetchRoute() {
        new Thread(() -> {
            try {
                String url = "https://graphhopper.com/api/1/route?point=" + currentLocation.getLatitude() + "," +
                        currentLocation.getLongitude() + "&point=" + endPoint.getLatitude() + "," +
                        endPoint.getLongitude() + "&vehicle=foot&locale=uk&key=b40da758-a6bb-47de-8f35-8eb1ef901cc7";
                Request request = new Request.Builder().url(url).build();
                Response response = client.newCall(request).execute();

                if (!response.isSuccessful() || response.body() == null) {
                    runOnUiThread(() -> Toast.makeText(this, "Сервер GraphHopper повернув помилку: " + response.code(), Toast.LENGTH_LONG).show());
                    return;
                }

                String json = response.body().string();
                JsonObject result = JsonParser.parseString(json).getAsJsonObject();

                if (result.has("message")) {
                    String errorMessage = result.get("message").getAsString();
                    runOnUiThread(() -> Toast.makeText(this, "Помилка GraphHopper: " + errorMessage, Toast.LENGTH_LONG).show());
                    return;
                }

                if (!result.has("paths") || !result.get("paths").isJsonArray()) {
                    runOnUiThread(() -> Toast.makeText(this, "Помилка: відповідь GraphHopper не містить маршрутів", Toast.LENGTH_LONG).show());
                    return;
                }

                JsonArray paths = result.getAsJsonArray("paths");
                if (paths.size() == 0) {
                    runOnUiThread(() -> Toast.makeText(this, "Маршрут не знайдено", Toast.LENGTH_LONG).show());
                    return;
                }

                JsonObject path = paths.get(0).getAsJsonObject();
                String encodedPoints = path.get("points").getAsString();
                routePoints.clear();
                routePoints.addAll(decodePolyline(encodedPoints));

                if (path.has("instructions") && path.get("instructions").isJsonArray()) {
                    JsonArray instructionsArray = path.getAsJsonArray("instructions");
                    instructions.clear();
                    for (int i = 0; i < instructionsArray.size(); i++) {
                        JsonObject instructionObj = instructionsArray.get(i).getAsJsonObject();
                        if (instructionObj.has("text")) {
                            instructions.add(instructionObj.get("text").getAsString());
                        }
                    }
                    currentInstructionIndex = 0;
                }

                runOnUiThread(() -> {
                    updateMap();
                    updateNavigationOverlay();
                });
            } catch (IOException e) {
                runOnUiThread(() -> Toast.makeText(this, "Помилка маршрутизації: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }).start();
    }


    /**
     * Декодує Polyline-рядок у список GeoPoint.
     */
    private List<GeoPoint> decodePolyline(String encoded) {
        List<GeoPoint> poly = new ArrayList<>();
        int index = 0, len = encoded.length();
        int lat = 0, lng = 0;

        while (index < len) {
            int b, shift = 0, result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lat += dlat;

            shift = 0;
            result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lng += dlng;

            GeoPoint p = new GeoPoint(((double) lat / 1E5), ((double) lng / 1E5));
            poly.add(p);
        }

        return poly;
    }

    private void updateMap() {
        mapView.getOverlays().clear();

        // Додавання маркера поточної позиції назад після очищення
        mapView.getOverlays().add(myLocationOverlay);

        Polyline route = new Polyline();
        route.setColor(0xFF0000FF);
        route.setWidth(10f);
        route.setPoints(routePoints);
        mapView.getOverlays().add(route);
        mapView.invalidate();
    }

    private void updateNavigationOverlay() {
        if (!instructions.isEmpty() && currentInstructionIndex < instructions.size()) {
            String instruction = instructions.get(currentInstructionIndex);
            navigationOverlay.setInstruction(instruction);

            // Перевірка чи TTS готовий і чи не порожня інструкція
            if (tts != null && !instruction.isEmpty()) {
                tts.speak(instruction, TextToSpeech.QUEUE_FLUSH, null, "instruction");
            }
        }
    }

    /**
     * Ініціалізує компас для орієнтації стрілки.
     */
    private void initCompass() {
        Sensor accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        Sensor magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

        if (accelerometer == null || magnetometer == null) {
            Toast.makeText(this, "Потрібні сенсори не доступні на пристрої", Toast.LENGTH_LONG).show();
            return;
        }

        sensorListener = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent event) {
                if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                    gravity = event.values.clone();
                } else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
                    geomagnetic = event.values.clone();
                }

                if (gravity != null && geomagnetic != null) {
                    float[] rotationMatrix = new float[9];
                    float[] orientation = new float[3];

                    boolean success = SensorManager.getRotationMatrix(rotationMatrix, null, gravity, geomagnetic);
                    if (success) {
                        SensorManager.getOrientation(rotationMatrix, orientation);
                        float azimuth = (float) Math.toDegrees(orientation[0]);
                        navigationOverlay.setArrowAngle(azimuth);
                    }
                }
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {}
        };

        sensorManager.registerListener(sensorListener, accelerometer, SensorManager.SENSOR_DELAY_UI);
        sensorManager.registerListener(sensorListener, magnetometer, SensorManager.SENSOR_DELAY_UI);
    }

    /**
     * Відстежує поточну позицію та переналаштовує маршрут.
     */
    private void startLocationUpdates() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {

            LocationRequest locationRequest = new LocationRequest.Builder(5000)
                    .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
                    .build();

            fusedLocationClient.requestLocationUpdates(locationRequest,
                    locationCallback,
                    null /* Looper */);

            // Отримуємо останню відому локацію для початкової позиції
            fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
                if (location != null) {
                    currentLocation = new GeoPoint(location.getLatitude(), location.getLongitude());
                    mapView.getController().setCenter(currentLocation);
                }
            });
        }
    }

    private void checkRouteDeviation() {
        if (routePoints.isEmpty()) return;

        GeoPoint closestPoint = routePoints.get(0);
        double minDistance = currentLocation.distanceToAsDouble(closestPoint);
        int closestPointIndex = 0;

        // Знаходимо найближчу точку маршруту та її індекс
        for (int i = 0; i < routePoints.size(); i++) {
            GeoPoint point = routePoints.get(i);
            double distance = currentLocation.distanceToAsDouble(point);
            if (distance < minDistance) {
                minDistance = distance;
                closestPoint = point;
                closestPointIndex = i;
            }
        }

        // Якщо значне відхилення від маршруту
        if (minDistance > 20) { // Відхилення більше 20 м
            Toast.makeText(this, "Маршрут переналаштовано", Toast.LENGTH_SHORT).show();
            if (tts != null) {
                tts.speak("Маршрут переналаштовано", TextToSpeech.QUEUE_FLUSH, null, "reroute");
            }
            fetchRoute();
        }
        // Перевіряємо, чи досягнуто точки наступної інструкції
        else if (currentInstructionIndex < instructions.size() - 1) {
            // Якщо ми пройшли точку, пов'язану з поточною інструкцією, переходимо до наступної
            // Припускаємо, що інструкції приблизно відповідають ключовим точкам маршруту
            int nextInstructionPointIndex = (int)(closestPointIndex + routePoints.size() / instructions.size());
            if (nextInstructionPointIndex < routePoints.size() &&
                    currentLocation.distanceToAsDouble(routePoints.get(nextInstructionPointIndex)) < 10) {
                currentInstructionIndex++;
                updateNavigationOverlay();
            }

            // Перевіряємо, чи досягли кінцевої точки
            if (currentLocation.distanceToAsDouble(endPoint) < 15) {
                Toast.makeText(this, "Ви прибули до пункту призначення!", Toast.LENGTH_LONG).show();
                if (tts != null) {
                    tts.speak("Ви прибули до пункту призначення!", TextToSpeech.QUEUE_FLUSH, null, "arrival");
                }
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();

        // Відновлюємо моніторинг сенсорів
        if (sensorListener != null && sensorManager != null) {
            Sensor accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            Sensor magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

            if (accelerometer != null && magnetometer != null) {
                sensorManager.registerListener(sensorListener, accelerometer, SensorManager.SENSOR_DELAY_UI);
                sensorManager.registerListener(sensorListener, magnetometer, SensorManager.SENSOR_DELAY_UI);
            }
        }

        // Відновлюємо оновлення локації
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            startLocationUpdates();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();

        // Зупиняємо оновлення локації та моніторинг сенсорів
        fusedLocationClient.removeLocationUpdates(locationCallback);

        if (sensorListener != null && sensorManager != null) {
            sensorManager.unregisterListener(sensorListener);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapView.onDetach();

        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }

        if (sensorListener != null && sensorManager != null) {
            sensorManager.unregisterListener(sensorListener);
        }

        if (fusedLocationClient != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
    }
}