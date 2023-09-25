package com.example.eyecantalk.adapters;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.example.eyecantalk.R;
import com.example.eyecantalk.imageData.ImageData;

public class CategoryViewHolder extends RecyclerView.ViewHolder {
    private TextView textView;

    public CategoryViewHolder(View itemView) {
        super(itemView);
        textView = itemView.findViewById(R.id.category_name);
    }

    public void bind(String category) {
        textView.setText(category);
    }
}