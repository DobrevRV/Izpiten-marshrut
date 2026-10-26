
package bg.radidobrev.izpitenmarshrut;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MainActivity extends Activity {

    private TextView status;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(30, 60, 30, 30);
        layout.setBackgroundColor(Color.rgb(15, 23, 42));

        TextView title = new TextView(this);
        title.setText("ИЗПИТЕН МАРШРУТ\nРАДИ ДОБРЕВ");
        title.setTextColor(Color.WHITE);
        title.setTextSize(25);
        title.setGravity(Gravity.CENTER);
        layout.addView(title);

        status = new TextView(this);
        status.setText("\nВерсия 4.0 - Диагностика\n\nПриложението се стартира успешно.");
        status.setTextColor(Color.GREEN);
        status.setTextSize(19);
        layout.addView(status);

        Button test = new Button(this);
        test.setText("ТЕСТ НА ПРИЛОЖЕНИЕТО");
        test.setOnClickListener(v -> {
            status.setText("ТЕСТ УСПЕШЕН!\n\nAndroid приложението работи.");
        });
        layout.addView(test);

        setContentView(layout);
    }
}
