package com.example.eyecantalk.activity;

import android.view.View;
import android.widget.ImageView;
import androidx.recyclerview.widget.RecyclerView;

import com.example.eyecantalk.R;
import com.example.eyecantalk.imageData.ImageData;

public class ImageViewHolder extends RecyclerView.ViewHolder {
    private ImageView imageView;

    public ImageViewHolder(View itemView) {
        super(itemView);
        imageView = itemView.findViewById(R.id.imageView);
    }

    public void bind(ImageData imageData) {
        imageView.setImageResource(imageData.getImageResId());
        itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });
    }


}
