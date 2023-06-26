package com.example.eyecantalk.activity;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import androidx.recyclerview.widget.RecyclerView;

import com.example.eyecantalk.R;

public class ImageViewHolder extends RecyclerView.ViewHolder {
    private ImageView imageView;

    public ImageViewHolder(View itemView) {
        super(itemView);
        imageView = itemView.findViewById(R.id.imageView);
    }

    public void bind(ImageData imageData) {
        int imageResId = itemView.getContext().getResources().getIdentifier(imageData.getImageUrl(), "drawable", itemView.getContext().getPackageName());
        imageView.setImageResource(imageResId);

        itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendImageDataToServer(imageData);
            }
        });
    }

    private void sendImageDataToServer(ImageData imageData) {
        // TODO: Retrofit을 사용하여 서버에 이미지 정보를 전송하는 코드 작성
    }
}
