package com.example.engapp.network;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class DatamuseRetrofitClient {

    private static final String BASE_URL = "https://api.datamuse.com/";

    private static Retrofit retrofit;

    private DatamuseRetrofitClient() {
    }

    public static DatamuseApiService getDatamuseApiService() {
        if (retrofit == null) {
            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }

        return retrofit.create(DatamuseApiService.class);
    }
}