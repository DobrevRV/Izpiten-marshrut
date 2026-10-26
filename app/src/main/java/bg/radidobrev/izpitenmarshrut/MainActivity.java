package bg.radidobrev.izpitenmarshrut;

import android.Manifest;
import android.app.Activity;
import android.os.Bundle;
import android.os.Build;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.speech.tts.TextToSpeech;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.Locale;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

public class MainActivity extends Activity {
    private MapView map;
    private TextView status;
    private TextToSpeech voice;
    private boolean voiceReady = false;
    private LocationManager locationManager;
    private LocationListener locationListener;
    private Marker positionMarker;
    private static final int GPS_REQUEST = 100;
    private final GeoPoint kazanlak = new GeoPoint(42.6194, 25.3930);

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Configuration.getInstance().setUserAgentValue(getPackageName());

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(12, 25, 12, 12);
        root.setBackgroundColor(Color.rgb(15, 23, 42));

        TextView title = new TextView(this);
        title.setText("ИЗПИТЕН МАРШРУТ - РАДИ ДОБРЕВ");
        title.setTextColor(Color.WHITE);
        title.setTextSize(20);
        root.addView(title);

        status = new TextView(this);
        status.setText("Казанлък | GPS не е активиран");
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

        addButton(root, "МОЕТО МЕСТОПОЛОЖЕНИЕ", () -> startGPS());
        addButton(root, "ПОКАЖИ КАЗАНЛЪК", () -> {
            map.getController().animateTo(kazanlak);
            map.getController().setZoom(15.0);
        });
        addButton(root, "ТЕСТ НА ГЛАСА", () -> speak(
            "След сто метра завийте надясно."
        ));

        setContentView(root);

        voice = new TextToSpeech(this, result -> {
            if (result == TextToSpeech.SUCCESS && voice != null) {
                int language = voice.setLanguage(new Locale("bg", "BG"));
                if (language >= TextToSpeech.LANG_AVAILABLE) {
                    voiceReady = true;
                    voice.setSpeechRate(0.85f);
                } else {
                    showStatus("Българският глас не е наличен.");
                }
            } else {
                showStatus("Гласовият двигател не работи.");
            }
        });
    }

    private void addButton(LinearLayout root, String name, Runnable action) {
        Button button = new Button(this);
        button.setText(name);
        button.setOnClickListener(v -> action.run());
        root.addView(button);
    }

    private void showStatus(String message) {
        runOnUiThread(() -> status.setText(message));
    }

    private boolean hasGPSPermission() {
        return checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED ||
            checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
            == PackageManager.PERMISSION_GRANTED;
    }

    private void startGPS() {
        if (!hasGPSPermission()) {
            requestPermissions(new String[]{
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            }, GPS_REQUEST);
            return;
        }

        locationManager = (LocationManager)
            getSystemService(LOCATION_SERVICE);

        if (locationManager == null ||
            !locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            showStatus("Включи местоположението (GPS) на телефона.");
            return;
        }

        if (locationListener != null) {
            locationManager.removeUpdates(locationListener);
        }

        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                GeoPoint point = new GeoPoint(
                    location.getLatitude(), location.getLongitude()
                );

                if (positionMarker == null) {
                    positionMarker = new Marker(map);
                    positionMarker.setTitle("Моят автомобил");
                    positionMarker.setAnchor(
                        Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM
                    );
                    map.getOverlays().add(positionMarker);
                }

                positionMarker.setPosition(point);
                map.getController().animateTo(point);
                map.getController().setZoom(17.0);
                map.invalidate();
                showStatus("GPS: местоположението е обновено");
            }
        };

        try {
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                2000L, 2f, locationListener
            );
            showStatus("Търсене на GPS сигнал...");
        } catch (SecurityException e) {
            showStatus("Няма разрешение за GPS.");
        }
    }

    @Override
    public void onRequestPermissionsResult(
        int requestCode, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(requestCode, permissions, results);
        if (requestCode == GPS_REQUEST) {
            if (hasGPSPermission()) {
                startGPS();
            } else {
                showStatus("GPS разрешението е отказано.");
            }
        }
    }

    private void speak(String message) {
        if (voiceReady && voice != null) {
            int result = voice.speak(
                message, TextToSpeech.QUEUE_FLUSH, null, "navigation"
            );
            if (result == TextToSpeech.ERROR) {
                showStatus("Грешка при възпроизвеждане на гласа.");
            }
        } else {
            showStatus("Гласът не е готов.");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (map != null) map.onResume();
    }

    @Override
    protected void onPause() {
        if (locationManager != null && locationListener != null) {
            locationManager.removeUpdates(locationListener);
        }
        if (map != null) map.onPause();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        if (voice != null) {
            voice.stop();
            voice.shutdown();
        }
        if (map != null) map.onDetach();
        super.onDestroy();
    }
}
