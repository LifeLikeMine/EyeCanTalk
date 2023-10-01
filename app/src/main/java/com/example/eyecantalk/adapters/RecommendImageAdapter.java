package com.example.eyecantalk.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.eyecantalk.R;
import com.example.eyecantalk.imageData.ImageData;

import java.util.List;

public class RecommendImageAdapter extends RecyclerView.Adapter<RecommendImageAdapter.RecommendImageViewHolder> {

    private List<ImageData> recommendImages;

    private OnImageItemClickListener onImageItemClickListener;

    public RecommendImageAdapter(List<ImageData> recommendImages, OnImageItemClickListener onImageItemClickListener) {
        this.recommendImages = recommendImages;
        this.onImageItemClickListener = onImageItemClickListener;
    }

    @NonNull
    @Override
    public RecommendImageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_recommned, parent, false);
        return new RecommendImageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecommendImageViewHolder holder, int position) {
        ImageData imageData = recommendImages.get(position);
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
        return recommendImages.size();
    }

    public void addImageData(ImageData imageData) {
        recommendImages.add(imageData);
        notifyItemInserted(recommendImages.size() - 1);
    }

    public class RecommendImageViewHolder extends RecyclerView.ViewHolder {
        private ImageView selectedImageView;
        private TextView selectedTextView;

        public RecommendImageViewHolder(@NonNull View itemView) {
            super(itemView);
            selectedImageView = itemView.findViewById(R.id.selectedImageView);
            selectedTextView = itemView.findViewById(R.id.selectedTextView);
        }

        public void bind(ImageData imageData) {
            selectedImageView.setImageDrawable(imageData.getImageResId());
            selectedTextView.setText(imageData.getImageDescription());
        }
    }
}
