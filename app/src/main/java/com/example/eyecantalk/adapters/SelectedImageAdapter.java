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

public class SelectedImageAdapter extends RecyclerView.Adapter<SelectedImageAdapter.SelectedImageViewHolder> {
    private List<ImageData> selectedImages;

    public SelectedImageAdapter(List<ImageData> selectedImages) {
        this.selectedImages = selectedImages;
    }

    @NonNull
    @Override
    public SelectedImageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_selected_image, parent, false);
        return new SelectedImageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SelectedImageViewHolder holder, int position) {
        ImageData imageData = selectedImages.get(position);
        holder.bind(imageData);
    }

    @Override
    public int getItemCount() {
        return selectedImages.size();
    }

    public void addImageData(ImageData imageData) {
        selectedImages.add(imageData);
        notifyItemInserted(selectedImages.size() - 1);

    }

    public class SelectedImageViewHolder extends RecyclerView.ViewHolder {
        private ImageView selectedImageView;
        private TextView selectedTextView;

        public SelectedImageViewHolder(@NonNull View itemView) {
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

