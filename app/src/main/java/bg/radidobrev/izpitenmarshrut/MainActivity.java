
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

    private static final int GPS_REQUEST = 60;

    private MapView map;
    private TextView status;
    private Button modeButton;

    private LocationManager locationManager;
    private LocationListener locationListener;

    private GeoPoint lastPosition;
    private GeoPoint routeStart;
    private GeoPoint routeEnd;

    private Marker carMarker;
    private Marker startMarker;
    private Marker endMarker;
    private Polyline routeLine;

    private boolean followCar = true;
    private boolean selectingRoute = false;

    private int routeRequestId = 0;

    private final GeoPoint kazanlak =
            new GeoPoint(42.6194, 25.3930);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Configuration.getInstance()
                .setUserAgentValue(getPackageName());

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(10, 20, 10, 10);
        layout.setBackgroundColor(
                Color.rgb(15, 23, 42)
        );

        TextView title = new TextView(this);
        title.setText("ИЗПИТЕН МАРШРУТ – РАДИ ДОБРЕВ");
        title.setTextColor(Color.WHITE);
        title.setTextSize(18);
        layout.addView(title);

        status = new TextView(this);
        status.setText("Версия 7.0 | Избери маршрут");
        status.setTextColor(Color.GREEN);
        status.setTextSize(15);
        layout.addView(status);

        map = new MapView(this);
        map.setTileSource(TileSourceFactory.MAPNIK);
        map.setMultiTouchControls(true);
        map.getController().setZoom(15.0);
        map.getController().setCenter(kazanlak);

        layout.addView(
                map,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        0,
                        1f
                )
        );

        MapEventsReceiver events = new MapEventsReceiver() {
            @Override
            public boolean singleTapConfirmedHelper(GeoPoint point) {
                return false;
            }

            @Override
            public boolean longPressHelper(GeoPoint point) {
                if (!selectingRoute) {
                    return false;
                }

                if (routeStart == null) {
                    routeStart = point;
                    placeStartMarker(point);
                    showStatus(
                            "Началото е избрано. Задръж за крайна точка."
                    );
                } else {
                    routeEnd = point;
                    placeEndMarker(point);
                    selectingRoute = false;
                    calculateRoute(routeStart, routeEnd);
                }

                return true;
            }
        };

        map.getOverlays().add(
                new MapEventsOverlay(events)
        );

        Button readyButton = new Button(this);
        readyButton.setText("ГОТОВ МАРШРУТ 1");

        readyButton.setOnClickListener(v -> {
            clearRoute();
            followCar = false;
            updateModeButton();

            // Примерни точки в Казанлък.
            routeStart = new GeoPoint(
                    42.6194, 25.3930
            );
            routeEnd = new GeoPoint(
                    42.6280, 25.4070
            );

            placeStartMarker(routeStart);
            placeEndMarker(routeEnd);

            calculateRoute(routeStart, routeEnd);
        });

        layout.addView(readyButton);

        Button customButton = new Button(this);
        customButton.setText("СВОБОДЕН МАРШРУТ");

        customButton.setOnClickListener(v -> {
            clearRoute();
            selectingRoute = true;
            followCar = false;
            updateModeButton();

            showStatus(
                    "Задръж върху картата за начална точка"
            );
        });

        layout.addView(customButton);

        Button gpsButton = new Button(this);
        gpsButton.setText("МОЕТО МЕСТОПОЛОЖЕНИЕ / GPS");

        gpsButton.setOnClickListener(v -> {
            startGps();

            if (lastPosition != null) {
                map.getController()
                        .animateTo(lastPosition);
                map.getController().setZoom(17.0);
            }
        });

        layout.addView(gpsButton);

        modeButton = new Button(this);
        updateModeButton();

        modeButton.setOnClickListener(v -> {
            followCar = !followCar;
            updateModeButton();

            if (followCar && lastPosition != null) {
                map.getController()
                        .animateTo(lastPosition);
            }

            showStatus(
                    followCar
                    ? "Автоматично следване: ВКЛ."
                    : "Ръчен режим: ВКЛ."
            );
        });

        layout.addView(modeButton);

        Button clearButton = new Button(this);
        clearButton.setText("ИЗЧИСТИ МАРШРУТА");

        clearButton.setOnClickListener(v -> {
            clearRoute();
            showStatus("Маршрутът е изчистен");
        });

        layout.addView(clearButton);

        setContentView(layout);
    }

    private void updateModeButton() {
        modeButton.setText(
                followCar
                ? "СЛЕДВАНЕ: ВКЛ."
                : "РЪЧЕН РЕЖИМ: ВКЛ."
        );
    }

    private void showStatus(String message) {
        runOnUiThread(
                () -> status.setText(message)
        );
    }

    private void placeStartMarker(GeoPoint point) {
        if (startMarker != null) {
            map.getOverlays().remove(startMarker);
        }

        startMarker = new Marker(map);
        startMarker.setPosition(point);
        startMarker.setTitle("Начало");
        map.getOverlays().add(startMarker);
        map.invalidate();
    }

    private void placeEndMarker(GeoPoint point) {
        if (endMarker != null) {
            map.getOverlays().remove(endMarker);
        }

        endMarker = new Marker(map);
        endMarker.setPosition(point);
        endMarker.setTitle("Край");
        map.getOverlays().add(endMarker);
        map.invalidate();
    }

    private void clearRoute() {
        routeRequestId++;

        selectingRoute = false;
        routeStart = null;
        routeEnd = null;

        if (routeLine != null) {
            map.getOverlays().remove(routeLine);
            routeLine = null;
        }

        if (startMarker != null) {
            map.getOverlays().remove(startMarker);
            startMarker = null;
        }

        if (endMarker != null) {
            map.getOverlays().remove(endMarker);
            endMarker = null;
        }

        map.invalidate();
    }

    private void calculateRoute(
            GeoPoint start,
            GeoPoint end
    ) {
        showStatus("Изчисляване на маршрута...");

        final int requestId = ++routeRequestId;

        new Thread(() -> {
            HttpURLConnection connection = null;

            try {
                String address =
                        "https://router.project-osrm.org/route/v1/driving/"
                        + start.getLongitude() + ","
                        + start.getLatitude() + ";"
                        + end.getLongitude() + ","
                        + end.getLatitude()
                        + "?overview=full&geometries=geojson";

                URL url = new URL(address);

                connection =
                        (HttpURLConnection) url.openConnection();

                connection.setConnectTimeout(12000);
                connection.setReadTimeout(12000);
                connection.setRequestMethod("GET");

                if (connection.getResponseCode() != 200) {
                    throw new Exception(
                            "HTTP " + connection.getResponseCode()
                    );
                }

                BufferedReader reader =
                        new BufferedReader(
                                new InputStreamReader(
                                        connection.getInputStream()
                                )
                        );

                StringBuilder response = new StringBuilder();
                String line;

                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }

                reader.close();

                JSONObject json =
                        new JSONObject(response.toString());

                if (!"Ok".equals(json.optString("code"))) {
                    throw new Exception(
                            "Не е намерен автомобилен маршрут"
                    );
                }

                JSONObject route =
                        json.getJSONArray("routes")
                                .getJSONObject(0);

                JSONArray coordinates =
                        route.getJSONObject("geometry")
                                .getJSONArray("coordinates");

                ArrayList<GeoPoint> points =
                        new ArrayList<>();

                for (int i = 0; i < coordinates.length(); i++) {
                    JSONArray coordinate =
                            coordinates.getJSONArray(i);

                    double lon = coordinate.getDouble(0);
                    double lat = coordinate.getDouble(1);

                    points.add(new GeoPoint(lat, lon));
                }

                double distanceKm =
                        route.getDouble("distance") / 1000.0;

                runOnUiThread(() -> {
                    if (requestId != routeRequestId) {
                        return;
                    }

                    if (routeLine != null) {
                        map.getOverlays().remove(routeLine);
                    }

                    routeLine = new Polyline();
                    routeLine.setPoints(points);
                    routeLine.getOutlinePaint()
                            .setColor(Color.rgb(0, 120, 255));
                    routeLine.getOutlinePaint()
                            .setStrokeWidth(12f);

                    map.getOverlays().add(routeLine);

                    if (!points.isEmpty()) {
                        map.zoomToBoundingBox(
                                routeLine.getBounds(),
                                true,
                                80
                        );
                    }

                    map.invalidate();

                    showStatus(
                            String.format(
                                    java.util.Locale.getDefault(),
                                    "Маршрут: %.1f км",
                                    distanceKm
                            )
                    );
                });

            } catch (Exception e) {
                runOnUiThread(() -> {
                    if (requestId == routeRequestId) {
                        showStatus(
                                "Грешка при маршрута: "
                                + e.getMessage()
                        );
                    }
                });

            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        }).start();
    }

    private boolean hasLocationPermission() {
        return checkSelfPermission(
                Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
                ||
                checkSelfPermission(
                        Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED;
    }

    private void startGps() {
        if (!hasLocationPermission()) {
            requestPermissions(
                    new String[]{
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                    },
                    GPS_REQUEST
            );
            return;
        }

        locationManager =
                (LocationManager) getSystemService(
                        LOCATION_SERVICE
                );

        if (locationManager == null) {
            showStatus("GPS услугата не е достъпна");
            return;
        }

        if (!locationManager.isProviderEnabled(
                LocationManager.GPS_PROVIDER
        )) {
            showStatus(
                    "Включи местоположението на телефона"
            );
            return;
        }

        if (locationListener != null) {
            locationManager.removeUpdates(
                    locationListener
            );
        }

        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(
                    Location location
            ) {
                lastPosition = new GeoPoint(
                        location.getLatitude(),
                        location.getLongitude()
                );

                if (carMarker == null) {
                    carMarker = new Marker(map);
                    carMarker.setTitle("Моят автомобил");
                    carMarker.setAnchor(
                            Marker.ANCHOR_CENTER,
                            Marker.ANCHOR_BOTTOM
                    );
                    map.getOverlays().add(carMarker);
                }

                carMarker.setPosition(lastPosition);

                if (followCar) {
                    map.getController()
                            .animateTo(lastPosition);
                    map.getController().setZoom(17.0);
                }

                map.invalidate();

                showStatus(
                        followCar
                        ? "GPS работи | Следване: ВКЛ."
                        : "GPS работи | Ръчен режим"
                );
            }
        };

        try {
            locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    2000L,
                    2f,
                    locationListener
            );

            showStatus("Изчакване на GPS сигнал...");

        } catch (SecurityException e) {
            showStatus("Няма разрешение за GPS");
        } catch (Exception e) {
            showStatus(
                    "GPS грешка: "
                    + e.getClass().getSimpleName()
            );
        }
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            String[] permissions,
            int[] results
    ) {
        super.onRequestPermissionsResult(
                requestCode,
                permissions,
                results
        );

        if (requestCode == GPS_REQUEST) {
            if (hasLocationPermission()) {
                startGps();
            } else {
                showStatus(
                        "Разрешението за GPS е отказано"
                );
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (map != null) {
            map.onResume();
        }
    }

    @Override
    protected void onPause() {
        if (locationManager != null
                && locationListener != null) {
            locationManager.removeUpdates(
                    locationListener
            );
            locationListener = null;
        }

        if (map != null) {
            map.onPause();
        }

        super.onPause();
    }

    @Override
    protected void onDestroy() {
        if (map != null) {
            map.onDetach();
        }

        super.onDestroy();
    }
}
