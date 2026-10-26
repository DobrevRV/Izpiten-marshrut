
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

import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

public class MainActivity extends Activity {

    private static final int GPS_REQUEST = 60;

    private final GeoPoint kazanlak =
            new GeoPoint(42.6194, 25.3930);

    private MapView map;
    private TextView status;
    private Button modeButton;

    private LocationManager locationManager;
    private LocationListener locationListener;

    private GeoPoint lastPosition;
    private Marker carMarker;

    private boolean followCar = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Configuration.getInstance()
                .setUserAgentValue(getPackageName());

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(10, 25, 10, 10);
        layout.setBackgroundColor(
                Color.rgb(15, 23, 42)
        );

        TextView title = new TextView(this);
        title.setText(
                "ИЗПИТЕН МАРШРУТ – РАДИ ДОБРЕВ"
        );
        title.setTextColor(Color.WHITE);
        title.setTextSize(19);
        layout.addView(title);

        status = new TextView(this);
        status.setText(
                "Версия 6.0 | GPS не е включен"
        );
        status.setTextColor(Color.WHITE);
        status.setTextSize(15);
        layout.addView(status);

        map = new MapView(this);
        map.setTileSource(
                TileSourceFactory.MAPNIK
        );
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

        Button gpsButton = new Button(this);
        gpsButton.setText(
                "МОЕТО МЕСТОПОЛОЖЕНИЕ / GPS"
        );

        gpsButton.setOnClickListener(v -> {
            if (lastPosition != null) {
                map.getController()
                        .animateTo(lastPosition);
                map.getController().setZoom(17.0);
            }

            startGps();
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
                    ? "Автоматично следване е включено"
                    : "Ръчен режим: мести картата свободно"
            );
        });

        layout.addView(modeButton);

        Button kazanlakButton = new Button(this);
        kazanlakButton.setText(
                "ПОКАЖИ КАЗАНЛЪК"
        );

        kazanlakButton.setOnClickListener(v -> {
            followCar = false;
            updateModeButton();

            map.getController().setZoom(15.0);
            map.getController().animateTo(kazanlak);

            showStatus(
                    "Казанлък | Ръчен режим"
            );
        });

        layout.addView(kazanlakButton);

        setContentView(layout);
    }

    private void updateModeButton() {
        modeButton.setText(
                followCar
                ? "СЛЕДВАНЕ: ВКЛ. (НАТИСНИ ЗА РЪЧЕН)"
                : "РЪЧЕН РЕЖИМ (НАТИСНИ ЗА СЛЕДВАНЕ)"
        );
    }

    private void showStatus(String text) {
        runOnUiThread(
                () -> status.setText(text)
        );
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
            showStatus(
                    "GPS услугата не е достъпна"
            );
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
                    carMarker.setTitle(
                            "Моят автомобил"
                    );

                    carMarker.setAnchor(
                            Marker.ANCHOR_CENTER,
                            Marker.ANCHOR_BOTTOM
                    );

                    map.getOverlays().add(
                            carMarker
                    );
                }

                carMarker.setPosition(
                        lastPosition
                );

                if (followCar) {
                    map.getController()
                            .animateTo(lastPosition);

                    map.getController()
                            .setZoom(17.0);
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

            showStatus(
                    "Изчакване на GPS сигнал (излез на открито)..."
            );

        } catch (SecurityException e) {
            showStatus(
                    "Няма разрешение за GPS"
            );

        } catch (Exception e) {
            showStatus(
                    "GPS грешка: " +
                    e.getClass().getSimpleName()
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
                        "Разрешението за местоположение е отказано"
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
