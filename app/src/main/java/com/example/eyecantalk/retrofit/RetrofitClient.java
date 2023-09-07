package com.example.eyecantalk.retrofit;

import com.example.eyecantalk.imageData.ImageDataList;

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
    private static Retrofit retrofit = new Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(new OkHttpClient.Builder().build())
            .addConverterFactory(GsonConverterFactory.create())
            .build();

    // 서버 API 인터페이스 정의
    public interface ApiService {
        @POST("/recommend")
        Call<Void> uploadImageList(@Body ImageDataList imageDataList);
    }

    public static void sendImageData(String suuid, List<Integer> imageData) {
        ApiService apiService = retrofit.create(ApiService.class);
        ImageDataList imageDataListWrapper = new ImageDataList(suuid, imageData);

        Call<Void> call = apiService.uploadImageList(imageDataListWrapper);

        call.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    // 성공적으로 요청이 처리됨
                    // 서버 응답 처리
                } else {
                    // 요청 실패
                    // 오류 처리
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                // 네트워크 오류 등으로 인한 요청 실패
                // 오류 처리
            }
        });
    }
}
