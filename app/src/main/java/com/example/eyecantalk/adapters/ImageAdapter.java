package com.example.eyecantalk.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.recyclerview.widget.RecyclerView;

import com.example.eyecantalk.R;
import com.example.eyecantalk.imageData.ImageData;

import java.util.List;

public class ImageAdapter extends RecyclerView.Adapter<ImageViewHolder> {
    private List<ImageData> imageDataList;
    private OnImageItemClickListener onImageItemClickListener;

    public ImageAdapter(List<ImageData> imageDataList, OnImageItemClickListener onImageItemClickListener) {
        this.imageDataList = imageDataList;
        this.onImageItemClickListener = onImageItemClickListener;
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
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onImageItemClickListener.onImageItemClick(imageData);
            }
        });
    }

    @Override
    public int getItemCount() {
        return imageDataList.size();
    }
}
