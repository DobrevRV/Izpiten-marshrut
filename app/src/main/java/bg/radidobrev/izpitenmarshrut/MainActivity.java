
package bg.radidobrev.izpitenmarshrut;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;

public class MainActivity extends Activity {

    private MapView map;
    private TextView status;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(10, 25, 10, 10);
        layout.setBackgroundColor(Color.rgb(15, 23, 42));

        TextView title = new TextView(this);
        title.setText("ИЗПИТЕН МАРШРУТ – РАДИ ДОБРЕВ");
        title.setTextColor(Color.WHITE);
        title.setTextSize(19);
        layout.addView(title);

        status = new TextView(this);
        status.setText("Версия 5.0 – Зареждане на картата...");
        status.setTextColor(Color.GREEN);
        status.setTextSize(16);
        layout.addView(status);

        setContentView(layout);

        try {
            Configuration.getInstance()
                .setUserAgentValue(getPackageName());

            map = new MapView(this);
            map.setTileSource(TileSourceFactory.MAPNIK);
            map.setMultiTouchControls(true);

            layout.addView(
                map,
                new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    0,
                    1f
                )
            );

            GeoPoint kazanlak = new GeoPoint(42.6194, 25.3930);

            map.getController().setZoom(15.0);
            map.getController().setCenter(kazanlak);

            status.setText("Карта на Казанлък – тест");

            Button centerButton = new Button(this);
            centerButton.setText("ПОКАЖИ КАЗАНЛЪК");
            centerButton.setOnClickListener(v -> {
                map.getController().setZoom(15.0);
                map.getController().animateTo(kazanlak);
            });
            layout.addView(centerButton);

        } catch (Throwable e) {
            status.setText(
                "ГРЕШКА ПРИ КАРТАТА:\n" +
                e.getClass().getSimpleName() +
                "\n" +
                e.getMessage()
            );
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
