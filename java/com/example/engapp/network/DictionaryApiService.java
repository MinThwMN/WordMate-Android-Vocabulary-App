package com.example.engapp.network;

import com.example.engapp.network.models.DictionaryResponse;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

public interface DictionaryApiService {

    @GET("api/v2/entries/en/{word}")
    Call<List<DictionaryResponse>> searchWord(@Path("word") String word);
}