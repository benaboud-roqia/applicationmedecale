package com.dianerverotect.model;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.dianerverotect.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Adapter for displaying recommendations in a RecyclerView.
 */
public class RecommendationAdapter extends RecyclerView.Adapter<RecommendationAdapter.RecommendationViewHolder> {
    
    private final List<Recommendation> recommendations = new ArrayList<>();
    
    public RecommendationAdapter(Map<String, String> recommendationsMap) {
        // Convert map to list of Recommendation objects
        for (Map.Entry<String, String> entry : recommendationsMap.entrySet()) {
            recommendations.add(new Recommendation(entry.getKey(), entry.getValue()));
        }
    }
    
    @NonNull
    @Override
    public RecommendationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_recommendation, parent, false);
        return new RecommendationViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull RecommendationViewHolder holder, int position) {
        Recommendation recommendation = recommendations.get(position);
        holder.titleText.setText(recommendation.getTitle());
        holder.descriptionText.setText(recommendation.getDescription());
    }
    
    @Override
    public int getItemCount() {
        return recommendations.size();
    }
    
    /**
     * ViewHolder for recommendation items.
     */
    static class RecommendationViewHolder extends RecyclerView.ViewHolder {
        TextView titleText;
        TextView descriptionText;
        
        public RecommendationViewHolder(@NonNull View itemView) {
            super(itemView);
            titleText = itemView.findViewById(R.id.text_recommendation_title);
            descriptionText = itemView.findViewById(R.id.text_recommendation_description);
        }
    }
    
    /**
     * Model class for recommendations.
     */
    static class Recommendation {
        private final String title;
        private final String description;
        
        public Recommendation(String title, String description) {
            this.title = title;
            this.description = description;
        }
        
        public String getTitle() {
            return title;
        }
        
        public String getDescription() {
            return description;
        }
    }
}
