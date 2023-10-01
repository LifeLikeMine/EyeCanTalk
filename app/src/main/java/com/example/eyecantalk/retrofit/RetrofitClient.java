package com.example.eyecantalk.retrofit;

import android.graphics.drawable.Drawable;
import android.util.Log;

import com.example.eyecantalk.BuildConfig;
import com.example.eyecantalk.imageData.AacResponse;
import com.example.eyecantalk.imageData.ImageDataList;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.Body;
import retrofit2.http.POST;

public class RetrofitClient {
    private static final String BASE_URL = BuildConfig.SERVER_URL;

    // Retrofit 싱글톤
    private static Retrofit retrofit;
    private static OkHttpClient okHttpClient = new OkHttpClient.Builder()
            .connectTimeout(1, TimeUnit.MINUTES)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .build();

    public static Retrofit getRetrofit(){
        Gson gson = new GsonBuilder()
                .setLenient()
                .create();

        if (retrofit == null)
        {
            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(okHttpClient)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit;
    }

    // 서버 API 인터페이스 정의
    public interface ApiService {
        @POST("/recommend")
        Call<AacResponse> uploadImageList(@Body ImageDataList imageDataList);
    }
}
