package com.example.eyecantalk.activity;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.eyecantalk.R;

import java.util.ArrayList;
import java.util.List;

public class ActActivity extends AppCompatActivity {
    private RecyclerView imageRecyclerView;
    private ImageAdapter imageAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_act);

        // 이미지 데이터 리스트 (예시)
        List<ImageData> imageDataList = new ArrayList<>();
        imageDataList.add(new ImageData(1, "@drawable/baseline_login_24", "Image 1")); // drawable 리소스 이름 사용
        imageDataList.add(new ImageData(2, "@drawable/baseline_login_24", "Image 2"));
        // ... (이미지 데이터 추가)

        imageRecyclerView = findViewById(R.id.imageRecyclerView);
        imageAdapter = new ImageAdapter(imageDataList);

        GridLayoutManager gridLayoutManager = new GridLayoutManager(this, 4);
        imageRecyclerView.setLayoutManager(gridLayoutManager);
        imageRecyclerView.setAdapter(imageAdapter);
    }
}
