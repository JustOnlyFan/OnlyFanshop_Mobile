package com.example.onlyfanshop.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.onlyfanshop.R;
import com.example.onlyfanshop.map.models.PlaceSuggestion;

import java.util.List;

public class SuggestionAdapter extends RecyclerView.Adapter<SuggestionAdapter.ViewHolder> {

    public interface OnSuggestionClickListener {
        void onSuggestionClick(PlaceSuggestion suggestion);
    }

    private final List<PlaceSuggestion> suggestions;
    private final OnSuggestionClickListener listener;

    public SuggestionAdapter(List<PlaceSuggestion> suggestions, OnSuggestionClickListener listener) {
        this.suggestions = suggestions;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_suggestion, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        PlaceSuggestion suggestion = suggestions.get(position);
        holder.primaryText.setText(suggestion.primaryText);
        holder.secondaryText.setText(suggestion.secondaryText);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onSuggestionClick(suggestion);
        });
    }

    @Override
    public int getItemCount() {
        return suggestions.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView primaryText, secondaryText;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            primaryText = itemView.findViewById(R.id.tvPrimaryText);
            secondaryText = itemView.findViewById(R.id.tvSecondaryText);
        }
    }
}