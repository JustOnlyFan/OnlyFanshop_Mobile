package com.example.onlyfanshop.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.onlyfanshop.R;
import com.example.onlyfanshop.model.Attraction;

import java.util.List;

public class AttractionAdapter extends RecyclerView.Adapter<AttractionAdapter.AttractionViewHolder> {

    private List<Attraction> attractions;
    private OnAttractionClickListener listener;

    public interface OnAttractionClickListener {
        void onAttractionClick(Attraction attraction);
        void onDirectionsClick(Attraction attraction);
    }

    public AttractionAdapter(List<Attraction> attractions, OnAttractionClickListener listener) {
        this.attractions = attractions;
        this.listener = listener;
    }

    @NonNull
    @Override
    public AttractionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_attraction, parent, false);
        return new AttractionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AttractionViewHolder holder, int position) {
        Attraction attraction = attractions.get(position);
        
        holder.tvTitle.setText(attraction.getTitle());
        holder.tvDescription.setText(attraction.getDescription());
        
        // Load image with Glide
        if (attraction.getImageUrl() != null && !attraction.getImageUrl().isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(attraction.getImageUrl())
                    .placeholder(R.drawable.ic_launcher_foreground)
                    .error(R.drawable.ic_launcher_foreground)
                    .into(holder.imgAttraction);
        }

        // Set click listeners
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onAttractionClick(attraction);
            }
        });

        holder.btnDirections.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDirectionsClick(attraction);
            }
        });
    }

    @Override
    public int getItemCount() {
        return attractions != null ? attractions.size() : 0;
    }

    public void updateAttractions(List<Attraction> newAttractions) {
        this.attractions = newAttractions;
        notifyDataSetChanged();
    }

    static class AttractionViewHolder extends RecyclerView.ViewHolder {
        ImageView imgAttraction;
        TextView tvTitle;
        TextView tvDescription;
        Button btnDirections;

        public AttractionViewHolder(@NonNull View itemView) {
            super(itemView);
            imgAttraction = itemView.findViewById(R.id.imgAttraction);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvDescription = itemView.findViewById(R.id.tvDescription);
            btnDirections = itemView.findViewById(R.id.btnDirections);
        }
    }
}

