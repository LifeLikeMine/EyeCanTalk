package com.example.eyecantalk.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.recyclerview.widget.RecyclerView;

import com.example.eyecantalk.R;
import com.example.eyecantalk.imageData.ImageData;

import java.util.List;

public class CategoryAdapter extends RecyclerView.Adapter<CategoryViewHolder> {
    private List<String> categoryList;
    private OnCategoryItemClickListener onCategoryItemClickListener;

    public CategoryAdapter(List<String> categoryList, OnCategoryItemClickListener onCategoryItemClickListener) {
        this.categoryList = categoryList;
        this.onCategoryItemClickListener = onCategoryItemClickListener;
    }

    public void setCategoryList(List<String> categoryList) {
        this.categoryList = categoryList;
        notifyDataSetChanged();
    }

    @Override
    public CategoryViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_category, parent, false);

        return new CategoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(CategoryViewHolder holder, int position) {
        String category = categoryList.get(position);
        holder.bind(category);
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onCategoryItemClickListener.onCategoryItemClick(category);
            }
        });
    }

    @Override
    public int getItemCount() {
        return categoryList.size();
    }
}
