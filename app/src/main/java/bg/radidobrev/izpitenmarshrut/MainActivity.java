package bg.radidobrev.izpitenmarshrut;

import android.app.Activity;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.graphics.Color;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.Locale;

public class MainActivity extends Activity {
    TextToSpeech voice;
    TextView status;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        voice = new TextToSpeech(this, result -> {
            if (result == TextToSpeech.SUCCESS) {
                voice.setLanguage(new Locale("bg", "BG"));
            }
        });

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(30, 70, 30, 30);
        layout.setBackgroundColor(Color.rgb(15, 23, 42));

        TextView title = new TextView(this);
        title.setText("ИЗПИТЕН МАРШРУТ\nРАДИ ДОБРЕВ");
        title.setTextColor(Color.WHITE);
        title.setTextSize(26);
        title.setGravity(Gravity.CENTER);
        layout.addView(title);

        status = new TextView(this);
        status.setText("\nГрад: Казанлък\nКатегория Б\n");
        status.setTextColor(Color.WHITE);
        status.setTextSize(19);
        layout.addView(status);

        addButton(layout, "Избери изпитен маршрут", () -> {
            status.setText("Режим: Изпитен маршрут");
            speak("Избран е изпитен маршрут в Казанлък");
        });

        addButton(layout, "Автоматичен маршрут", () -> {
            status.setText("Режим: Автоматичен маршрут");
            speak("Избран е автоматичен маршрут");
        });

        addButton(layout, "Тест на гласовите указания", () -> {
            speak("След сто метра завийте надясно.");
        });

        setContentView(layout);
    }

    void addButton(LinearLayout layout, String text, Runnable action) {
        Button button = new Button(this);
        button.setText(text);
        button.setOnClickListener(v -> action.run());
        layout.addView(button);
    }

    void speak(String message) {
        if (voice != null) {
            voice.speak(message, TextToSpeech.QUEUE_FLUSH, null, "route");
        }
    }

    @Override
    protected void onDestroy() {
        if (voice != null) {
            voice.stop();
            voice.shutdown();
        }
        super.onDestroy();
    }
}
