package bg.radidobrev.izpitenmarshrut;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ScrollView;
import android.view.ViewGroup;
import android.util.Log;

public class MainActivity extends Activity {

    private LinearLayout layout;
    private TextView status;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(25, 50, 25, 25);
        layout.setBackgroundColor(Color.rgb(15, 23, 42));

        TextView title = new TextView(this);
        title.setText("ИЗПИТЕН МАРШРУТ\nРАДИ ДОБРЕВ");
        title.setTextColor(Color.WHITE);
        title.setTextSize(24);
        layout.addView(title);

        status = new TextView(this);
        status.setText("\nДиагностика: началният екран работи.\n");
        status.setTextColor(Color.WHITE);
        status.setTextSize(18);
        layout.addView(status);

        Button button = new Button(this);
        button.setText("ЗАРЕДИ КАРТАТА");
        button.setOnClickListener(v -> loadMap());
        layout.addView(button);

        setContentView(layout);
    }

    private void loadMap() {
        status.setText("Опит за зареждане на картата...");

        try {
            org.osmdroid.config.Configuration.getInstance()
                .setUserAgentValue(getPackageName());

            org.osmdroid.views.MapView map =
                new org.osmdroid.views.MapView(this);

            map.setTileSource(
                org.osmdroid.tileprovider.tilesource
                    .TileSourceFactory.MAPNIK
            );

            map.setMultiTouchControls(true);
            map.getController().setZoom(15.0);
            map.getController().setCenter(
                new org.osmdroid.util.GeoPoint(42.6194, 25.3930)
            );

            layout.addView(map,
                new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    0, 1f
                )
            );

            status.setText("Картата е заредена успешно.");

        } catch (Throwable e) {
            Log.e("RouteDiagnostic", "Map crash", e);

            ScrollView scroll = new ScrollView(this);
            TextView error = new TextView(this);
            error.setTextColor(Color.RED);
            error.setTextSize(14);
            error.setPadding(20, 30, 20, 20);
            error.setText("ГРЕШКА ПРИ КАРТАТА:\n\n" +
                Log.getStackTraceString(e));

            scroll.addView(error);
            setContentView(scroll);
        }
    }
}coordinates");

                ArrayList<GeoPoint> path =
                    new ArrayList<>();

                for (int i = 0; i < points.length(); i++) {
                    JSONArray pair = points.getJSONArray(i);

                    path.add(new GeoPoint(
                        pair.getDouble(1),
                        pair.getDouble(0)
                    ));
                }

                if (path.isEmpty()) {
                    throw new Exception(
                        "Маршрутът няма координати."
                    );
                }

                double km =
                    route.getDouble("distance") / 1000.0;

                runOnUiThread(() -> {
                    try {
                        clearRoute();

                        routeLine = new Polyline();
                        routeLine.setPoints(path);
                        routeLine.getOutlinePaint().setColor(
                            Color.rgb(30, 110, 255)
                        );
                        routeLine.getOutlinePaint()
                            .setStrokeWidth(13f);

                        map.getOverlays().add(routeLine);

                        destinationMarker = new Marker(map);
                        destinationMarker.setPosition(destination);
                        destinationMarker.setTitle(
                            "Край на маршрута"
                        );
                        map.getOverlays().add(destinationMarker);

                        map.zoomToBoundingBox(
                            routeLine.getBounds(), true, 80
                        );

                        map.invalidate();

                        message(String.format(
                            Locale.getDefault(),
                            "Маршрут: %.1f км",
                            km
                        ));

                    } catch (Exception error) {
                        message(
                            "Грешка при рисуване: " +
                            error.toString()
                        );
                    }
                });

            } catch (Exception error) {
                Log.e(
                    "IzpitenMarshrut",
                    "Route error",
                    error
                );

                message(
                    "Грешка при маршрута: " +
                    error.toString()
                );

            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        }).start();
    }

    private void clearRoute() {
        if (map == null) return;

        if (routeLine != null) {
            map.getOverlays().remove(routeLine);
        }

        if (destinationMarker != null) {
            map.getOverlays().remove(destinationMarker);
        }

        routeLine = null;
        destinationMarker = null;
        map.invalidate();
    }

    @Override
    public void onRequestPermissionsResult(
        int requestCode,
        String[] permissions,
        int[] results) {

        super.onRequestPermissionsResult(
            requestCode, permissions, results
        );

        if (requestCode == GPS_PERMISSION) {
            if (hasPermission()) {
                startGps();
            } else {
                message("GPS разрешението е отказано.");
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (map != null) map.onResume();
    }

    @Override
    protected void onPause() {
        if (locationManager != null &&
            locationListener != null) {

            locationManager.removeUpdates(locationListener);
        }

        if (map != null) map.onPause();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        if (map != null) map.onDetach();
        super.onDestroy();
    }
}
