package com.example.colorscanner;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class ResourceAdapter extends RecyclerView.Adapter<ResourceAdapter.ResourceViewHolder> {

    private List<ResourceItem> resources;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(String url);
    }

    public ResourceAdapter(List<ResourceItem> resources, OnItemClickListener listener) {
        this.resources = resources;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ResourceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_resource, parent, false);
        return new ResourceViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ResourceViewHolder holder, int position) {
        ResourceItem item = resources.get(position);

        holder.titleText.setText(item.getTitle());
        holder.descriptionText.setText(item.getDescription());
        holder.urlText.setText(item.getUrl());
        holder.previewImage.setVisibility(View.GONE);
        holder.cardView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(item.getUrl());
            }
        });
    }

    @Override
    public int getItemCount() {
        return resources.size();
    }
    static class ResourceViewHolder extends RecyclerView.ViewHolder {
        CardView cardView;
        TextView titleText;
        TextView descriptionText;
        TextView urlText;
        ImageView previewImage;
        ResourceViewHolder(View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.resource_card);
            titleText = itemView.findViewById(R.id.resource_title);
            descriptionText = itemView.findViewById(R.id.resource_description);
            urlText = itemView.findViewById(R.id.resource_url);
            previewImage = itemView.findViewById(R.id.resource_image);
        }
    }
}
