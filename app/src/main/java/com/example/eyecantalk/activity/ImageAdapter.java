package com.example.eyecantalk.activity;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.recyclerview.widget.RecyclerView;

import com.example.eyecantalk.R;

import java.util.List;

public class ImageAdapter extends RecyclerView.Adapter<ImageViewHolder> {
    private List<ImageData> imageDataList;

    public ImageAdapter(List<ImageData> imageDataList) {
        this.imageDataList = imageDataList;
    }

    @Override
    public ImageViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_image, parent, false);
        return new ImageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ImageViewHolder holder, int position) {
        ImageData imageData = imageDataList.get(position);
        holder.bind(imageData);
    }

    @Override
    public int getItemCount() {
        return imageDataList.size();
    }
}
