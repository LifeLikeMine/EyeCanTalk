package com.example.eyecantalk.adapters;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.example.eyecantalk.R;
import com.example.eyecantalk.imageData.ImageData;

public class ImageViewHolder extends RecyclerView.ViewHolder {
    private ImageView imageView;
    private TextView textView;

    public ImageViewHolder(View itemView) {
        super(itemView);
        imageView = itemView.findViewById(R.id.imageView);
        textView = itemView.findViewById(R.id.item_name);
    }

    public void bind(ImageData imageData) {
        imageView.setImageDrawable(imageData.getImageResId());
        textView.setText(imageData.getImageDescription());
    }
}
