package bg.radidobrev.izpitenmarshrut;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import org.json.JSONArray;
import org.json.JSONObject;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polyline;
import org.osmdroid.views.overlay.MapEventsOverlay;
import org.osmdroid.events.MapEventsReceiver;
import java.net.HttpURLConnection;
import java.net.URL;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;

public class MainActivity extends Activity {
    private MapView map;
    private TextView status;
    private LocationManager locationManager;
    private LocationListener locationListener;
    private GeoPoint currentPosition;
    private Marker carMarker, destinationMarker;
    private Polyline routeLine;
    private final GeoPoint kazanlak = new GeoPoint(42.6194, 25.3930);
    private static final int GPS_PERMISSION = 42;

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Configuration.getInstance().setUserAgentValue(getPackageName());

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(10, 24, 10, 10);
        root.setBackgroundColor(Color.rgb(15, 23, 42));

        TextView heading = new TextView(this);
        heading.setText("ИЗПИТЕН МАРШРУТ – РАДИ ДОБРЕВ");
        heading.setTextSize(19);
        heading.setTextColor(Color.WHITE);
        root.addView(heading);

        status = new TextView(this);
        status.setText("Задръж върху картата, за да избереш цел.");
        status.setTextColor(Color.WHITE);
        status.setTextSize(15);
        root.addView(status);

        map = new MapView(this);
        map.setTileSource(TileSourceFactory.MAPNIK);
        map.setMultiTouchControls(true);
        map.getController().setZoom(15.0);
        map.getController().setCenter(kazanlak);
        root.addView(map, new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        map.getOverlays().add(new MapEventsOverlay(new MapEventsReceiver() {
            @Override public boolean singleTapConfirmedHelper(GeoPoint p) {
                return false;
            }
            @Override public boolean longPressHelper(GeoPoint p) {
                calculateRoute(p);
                return true;
            }
        }));

        addButton(root, "МОЕТО МЕСТОПОЛОЖЕНИЕ", () -> startGps());
        addButton(root, "ПОКАЖИ КАЗАНЛЪК", () -> {
            map.getController().setCenter(kazanlak);
            map.getController().setZoom(15.0);
        });
        addButton(root, "ИЗЧИСТИ МАРШРУТА", () -> clearRoute());
        setContentView(root);
    }

    private void addButton(LinearLayout root, String label, Runnable action) {
        Button b = new Button(this);
        b.setText(label);
        b.setOnClickListener(v -> action.run());
        root.addView(b);
    }

    private void message(String text) {
        runOnUiThread(() -> status.setText(text));
    }

    private boolean hasPermission() {
        return checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED ||
               checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void startGps() {
        if (!hasPermission()) {
            requestPermissions(new String[] {
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            }, GPS_PERMISSION);
            return;
        }
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        if (locationManager == null ||
                !locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            message("Включи GPS от настройките на телефона.");
            return;
        }
        if (locationListener != null) locationManager.removeUpdates(locationListener);
        locationListener = new LocationListener() {
            @Override public void onLocationChanged(Location location) {
                currentPosition = new GeoPoint(
                    location.getLatitude(), location.getLongitude());
                if (carMarker == null) {
                    carMarker = new Marker(map);
                    carMarker.setTitle("Моят автомобил");
                    map.getOverlays().add(carMarker);
                }
                carMarker.setPosition(currentPosition);
                map.getController().animateTo(currentPosition);
                map.invalidate();
                message("GPS работи. Задръж върху картата за маршрут.");
            }
        };
        try {
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER, 2000L, 2f, locationListener);
            message("Изчакване на GPS сигнал...");
        } catch (SecurityException e) {
            message("Няма разрешение за GPS.");
        }
    }

    private void calculateRoute(GeoPoint destination) {
        if (currentPosition == null) {
            message("Първо натисни МОЕТО МЕСТОПОЛОЖЕНИЕ и изчакай GPS.");
            return;
        }
        message("Изчисляване на маршрута...");
        new Thread(() -> {
            HttpURLConnection connection = null;
            try {
                String coords = currentPosition.getLongitude() + "," +
                    currentPosition.getLatitude() + ";" +
                    destination.getLongitude() + "," +
                    destination.getLatitude();
                String url = "https://router.project-osrm.org/route/v1/driving/" +
                    coords + "?overview=full&geometries=geojson";
                connection = (HttpURLConnection) new URL(url).openConnection();
                connection.setConnectTimeout(12000);
                connection.setReadTimeout(12000);
                if (connection.getResponseCode() != 200)
                    throw new Exception("Сървърът не отговори успешно.");
                BufferedReader reader = new BufferedReader(
                    new InputStreamReader(connection.getInputStream()));
                StringBuilder json = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) json.append(line);
                reader.close();

                JSONObject data = new JSONObject(json.toString());
                JSONArray routes = data.getJSONArray("routes");
                if (routes.length() == 0) throw new Exception("Няма намерен маршрут.");
                JSONObject route = routes.getJSONObject(0);
                JSONArray points = route.getJSONObject("geometry")
                    .getJSONArray("coordinates");
                ArrayList<GeoPoint> path = new ArrayList<>();
                for (int i = 0; i < points.length(); i++) {
                    JSONArray pair = points.getJSONArray(i);
                    path.add(new GeoPoint(pair.getDouble(1), pair.getDouble(0)));
                }
                double km = route.getDouble("distance") / 1000.0;
                runOnUiThread(() -> {
                    clearRoute();
                    routeLine = new Polyline();
                    routeLine.setPoints(path);
                    routeLine.getOutlinePaint().setColor(Color.rgb(30, 110, 255));
                    routeLine.getOutlinePaint().setStrokeWidth(13f);
                    map.getOverlays().add(routeLine);
                    destinationMarker = new Marker(map);
                    destinationMarker.setPosition(destination);
                    destinationMarker.setTitle("Край на маршрута");
                    map.getOverlays().add(destinationMarker);
                    map.zoomToBoundingBox(routeLine.getBounds(), true, 80);
                    map.invalidate();
                    message(String.format(java.util.Locale.getDefault(),
                        "Маршрут: %.1f км. Синята линия показва пътя.", km));
                });
            } catch (Exception e) {
                message("Маршрутът не се зареди. Провери интернет или избери друга точка.");
            } finally {
                if (connection != null) connection.disconnect();
            }
        }).start();
    }

    private void clearRoute() {
        if (routeLine != null) map.getOverlays().remove(routeLine);
        if (destinationMarker != null) map.getOverlays().remove(destinationMarker);
        routeLine = null;
        destinationMarker = null;
        map.invalidate();
    }

    @Override public void onRequestPermissionsResult(
        int requestCode, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(requestCode, permissions, results);
        if (requestCode == GPS_PERMISSION) {
            if (hasPermission()) startGps();
            else message("Разрешението за GPS е отказано.");
        }
    }

    @Override protected void onResume() {
        super.onResume();
        if (map != null) map.onResume();
    }
    @Override protected void onPause() {
        if (locationManager != null && locationListener != null)
            locationManager.removeUpdates(locationListener);
        if (map != null) map.onPause();
        super.onPause();
    }
    @Override protected void onDestroy() {
        if (map != null) map.onDetach();
        super.onDestroy();
    }
}
