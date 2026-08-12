package com.example.engapp.utils;

import android.content.Context;
import android.speech.tts.TextToSpeech;
import android.widget.Toast;

import com.example.engapp.R;

import java.util.Locale;

public class TtsManager {

    private TextToSpeech textToSpeech;
    private boolean isReady = false;
    private final Context context;

    public TtsManager(Context context) {
        this.context = context.getApplicationContext();

        textToSpeech = new TextToSpeech(this.context, status -> {
            if (status == TextToSpeech.SUCCESS) {
                int result = textToSpeech.setLanguage(Locale.US);

                if (result == TextToSpeech.LANG_MISSING_DATA
                        || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    isReady = false;
                    Toast.makeText(
                            this.context,
                            this.context.getString(R.string.tts_english_not_supported),
                            Toast.LENGTH_SHORT
                    ).show();
                } else {
                    isReady = true;
                    textToSpeech.setSpeechRate(0.9f);
                    textToSpeech.setPitch(1.0f);
                }
            } else {
                isReady = false;
                Toast.makeText(
                        this.context,
                        this.context.getString(R.string.tts_failed),
                        Toast.LENGTH_SHORT
                ).show();
            }
        });
    }

    public void speak(String text) {
        if (text == null || text.trim().isEmpty()) {
            return;
        }

        if (!isReady || textToSpeech == null) {
            Toast.makeText(
                    context,
                    context.getString(R.string.audio_not_ready),
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        textToSpeech.speak(
                text,
                TextToSpeech.QUEUE_FLUSH,
                null,
                "tts_audio"
        );
    }

    public void stop() {
        if (textToSpeech != null) {
            textToSpeech.stop();
        }
    }

    public void shutdown() {
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
            textToSpeech = null;
        }
    }
}