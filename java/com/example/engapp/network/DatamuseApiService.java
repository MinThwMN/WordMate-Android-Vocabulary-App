package com.example.engapp.network;

import com.example.engapp.network.models.DatamuseSuggestion;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface DatamuseApiService {

    @GET("sug")
    Call<List<DatamuseSuggestion>> getSuggestions(
            @Query("s") String query,
            @Query("max") int max
    );
}