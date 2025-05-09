# MapARGuide - AR Navigation with Offline Support

MapARGuide is an Android application that provides Augmented Reality (AR) navigation capabilities with the option for offline navigation. It allows users to select start and end points for a route, either by typing an address or selecting a point on the map, and then navigates them using AR.

## Features

* **Geolocation:** Retrieve the user's current location for navigation.
* **Map Interactivity:** Select start and end points on the map via simple taps.
* **Route Building:** Build routes between two points, either by address or geolocation.
* **Augmented Reality Navigation:** After selecting the route, users can follow the navigation via AR in a separate screen.
* **Offline Support:** Once a route is built, it can be used offline.

## Requirements

* Android 8.0 (Oreo) or higher.
* Internet connection for address search (Nominatim API) and initial route setup.
* **Location Permission:** The app requires access to the device's location to show your current position on the map.

## Installation

### 1. Clone the repository

```bash
git clone https://github.com/your-username/MapARGuide.git
cd MapARGuide
```

### 2. Open in Android Studio

1. Open Android Studio.
2. Select **Open an existing project**.
3. Navigate to the `MapARGuide` directory and open it.

### 3. Gradle Build

Once the project is opened in Android Studio, ensure Gradle syncs automatically. If it doesn't, sync manually by going to `File > Sync Project with Gradle Files`.

### 4. Permissions

Make sure the following permissions are declared in the `AndroidManifest.xml`:

```xml
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.INTERNET" />
```

Also, make sure to request location permission at runtime in the app.

## Usage

### Route Selection

1. **Start and End Points:**

   * You can enter a starting and ending point manually in the `EditText` fields or select them by tapping on the map.
   * To select a start or end point, tap the corresponding button (`Select Start` or `Select End`) and touch the map where you want to place the marker.

2. **Geolocation:**

   * Tap the "My Location" button to center the map on your current location. You can also use this as your starting point by selecting "Use as Start Location" after tapping the button.

3. **Building the Route:**

   * Enter addresses or use your location as the start or end point.
   * Press "Build Route" to start the AR navigation after the route is selected.

4. **Route Display:**

   * Once a route is built, the app will switch to the AR navigation screen, displaying the directions and guiding you with augmented reality.

### Map Interaction

* **Tap to Set Points:** Tap anywhere on the map to set your start and end points based on your selection.
* **Zoom and Pan:** Use pinch to zoom and swipe to pan the map to explore different areas.

### AR Navigation

* Once a route is built, the application will guide you using AR, allowing you to visualize directions in real-time.

## Troubleshooting

* **Location Permission:** Ensure the app has location permissions to access GPS features.
* **No Route Found:** Make sure both the start and end locations are valid addresses or coordinates.
* **No Network:** If you're offline, the app can still use previously cached routes, but real-time address searches and location features require an internet connection.

## Libraries Used

* **osmdroid:** OpenStreetMap-based library for map rendering and interaction.
* **OkHttp:** HTTP client for making network requests.
* **Gson:** JSON parsing for handling Nominatim geocoding results.
* **FusedLocationProviderClient:** For getting the current device location.

## License

MapARGuide is open-source software released under the MIT License. See [LICENSE](LICENSE) for more information.
