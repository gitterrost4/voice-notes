package com.mayororganizer;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
import java.util.function.Consumer;

public class CategoriesAdapter extends RecyclerView.Adapter<CategoriesAdapter.ViewHolder> {
    
    private final List<String> categories;
    private final Consumer<String> onDeleteClick;
    
    public CategoriesAdapter(List<String> categories, Consumer<String> onDeleteClick) {
        this.categories = categories;
        this.onDeleteClick = onDeleteClick;
    }
    
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_category, parent, false);
        return new ViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String category = categories.get(position);
        holder.bind(category);
    }
    
    @Override
    public int getItemCount() {
        return categories.size();
    }
    
    class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView categoryNameText;
        private final ImageButton deleteButton;
        
        ViewHolder(@NonNull View itemView) {
            super(itemView);
            categoryNameText = itemView.findViewById(R.id.categoryNameText);
            deleteButton = itemView.findViewById(R.id.deleteCategoryButton);
        }
        
        void bind(String category) {
            categoryNameText.setText(category);
            
            // Set delete button color
            deleteButton.setImageTintList(ColorStateList.valueOf(Color.parseColor("#F44336"))); // Red
            deleteButton.setOnClickListener(v -> onDeleteClick.accept(category));
        }
    }
}