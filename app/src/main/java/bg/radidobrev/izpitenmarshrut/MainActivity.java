
package bg.radidobrev.izpitenmarshrut;

import android.app.Activity;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.graphics.Color;
import android.media.AudioManager;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.Locale;

public class MainActivity extends Activity {
    private TextToSpeech voice;
    private TextView status;
    private boolean voiceReady = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

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
        status.setText("Град: Казанлък\nКатегория Б\nГлас: Зареждане...");
        status.setTextColor(Color.WHITE);
        status.setTextSize(19);
        layout.addView(status);

        addButton(layout, "Избери изпитен маршрут", () -> {
            speak("Избран е изпитен маршрут в Казанлък");
        });

        addButton(layout, "Автоматичен маршрут", () -> {
            speak("Избран е автоматичен маршрут");
        });

        addButton(layout, "Тест на гласовите указания", () -> {
            speak("След сто метра завийте надясно.");
        });

        setContentView(layout);

        setVolumeControlStream(AudioManager.STREAM_MUSIC);

        voice = new TextToSpeech(this, result -> {
            if (result == TextToSpeech.SUCCESS && voice != null) {
                int languageResult = voice.setLanguage(
                    new Locale("bg", "BG")
                );

                if (languageResult == TextToSpeech.LANG_MISSING_DATA ||
                    languageResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                    showStatus("Българският глас не е инсталиран или не се поддържа.");
                } else {
                    voiceReady = true;
                    voice.setSpeechRate(0.85f);
                    voice.setPitch(1.0f);

                    voice.setOnUtteranceProgressListener(
                        new UtteranceProgressListener() {
                            @Override
                            public void onStart(String id) {}

                            @Override
                            public void onDone(String id) {}

                            @Override
                            public void onError(String id) {
                                showStatus("Грешка при възпроизвеждане на гласа.");
                            }
                        }
                    );

                    showStatus("Гласът е готов. Натисни теста.");
                }
            } else {
                showStatus("Гласовият двигател не се стартира.");
            }
        });
    }

    void addButton(LinearLayout layout, String text, Runnable action) {
        Button button = new Button(this);
        button.setText(text);
        button.setOnClickListener(v -> action.run());
        layout.addView(button);
    }

    void showStatus(String message) {
        runOnUiThread(() -> status.setText(message));
    }

    void speak(String message) {
        if (!voiceReady || voice == null) {
            showStatus("Гласът не е готов. Провери настройките за синтез на реч.");
            return;
        }

        int result = voice.speak(
            message,
            TextToSpeech.QUEUE_FLUSH,
            null,
            "route"
        );

        if (result == TextToSpeech.ERROR) {
            showStatus("Неуспешно изпращане на гласовата команда.");
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
