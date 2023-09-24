package com.example.eyecantalk.retrofit;

import android.graphics.drawable.Drawable;
import android.util.Log;

import com.example.eyecantalk.imageData.AacResponse;
import com.example.eyecantalk.imageData.ImageDataList;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.List;

import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.Body;
import retrofit2.http.POST;

public class RetrofitClient {
    private static final String BASE_URL = "http://13.125.2.41:8000";

    // Retrofit 싱글톤
    private static Retrofit retrofit;

    public static Retrofit getRetrofit(){
        Gson gson = new GsonBuilder()
                .setLenient()
                .create();

        if (retrofit == null)
        {
            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(new OkHttpClient.Builder().build())
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

    public static void sendImageData(String suuid, List<Integer> imageData) {
        ApiService apiService = retrofit.create(ApiService.class);
        ImageDataList imageDataListWrapper = new ImageDataList(suuid, imageData);

        Call<AacResponse> call = apiService.uploadImageList(imageDataListWrapper);

        call.enqueue(new Callback<AacResponse>() {
            @Override
            public void onResponse(Call<AacResponse> call, Response<AacResponse> response) {
                if (response.isSuccessful()) {
                    // 성공적으로 요청이 처리됨
                    // 서버 응답 처리
                    try{
                        AacResponse data = response.body();
                        List<String> aacIds = data.getAacId();
                        Log.d("response", aacIds.get(4));
                    } catch (Exception e){
                        Log.d("response", "response fail");
                    }
                } else {
                    // 요청 실패
                    // 오류 처리
                }
            }

            @Override
            public void onFailure(Call<AacResponse> call, Throwable t) {
                // 네트워크 오류 등으로 인한 요청 실패
                // 오류 처리
            }
        });
    }
}
