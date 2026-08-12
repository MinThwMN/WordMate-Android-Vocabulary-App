package com.example.engapp.utils;

import android.content.Context;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

public class JsonHelper {

    public static String readJsonFromAssets(Context context, String fileName) {
        StringBuilder json = new StringBuilder();

        try {
            InputStream inputStream = context.getAssets().open(fileName);
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

            String line;
            while ((line = reader.readLine()) != null) {
                json.append(line);
            }

            reader.close();
            inputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return json.toString();
    }
}