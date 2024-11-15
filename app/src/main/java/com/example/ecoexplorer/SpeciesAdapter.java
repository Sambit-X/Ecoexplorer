package com.example.ecoexplorer;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import org.json.JSONArray;
import org.json.JSONObject;

public class SpeciesAdapter extends RecyclerView.Adapter<SpeciesAdapter.SpeciesViewHolder> {

    private final JSONArray speciesList;
    private final Context context;

    public SpeciesAdapter(Context context, JSONArray speciesList) {
        this.context = context;
        this.speciesList = speciesList;
    }

    @NonNull
    @Override
    public SpeciesViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_species, parent, false);
        return new SpeciesViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SpeciesViewHolder holder, int position) {
        try {
            JSONObject species = speciesList.getJSONObject(position);

            // Set common name
            if (species.has("common") && species.getJSONArray("common").length() > 0) {
                holder.commonName.setText("Common Name: " + species.getJSONArray("common").getString(0));
            } else {
                holder.commonName.setText("Common Name: N/A");
            }

            // Set scientific name
            if (species.has("scientificname") && !species.isNull("scientificname")) {
                holder.scientificName.setText("Scientific Name: " + species.getString("scientificname"));
            } else {
                holder.scientificName.setText("Scientific Name: N/A");
            }

            // Load image
            if (species.has("asset_url") && !species.isNull("asset_url")) {
                String assetUrl = species.getString("asset_url");
                String imageUrl = "https://storage.googleapis.com/mol-assets2/mid/" + assetUrl + ".jpg";

                Glide.with(context)
                        .load(imageUrl)
                        .placeholder(R.drawable.placeholder) // Add a placeholder image
                        .into(holder.speciesImage);
            } else {
                holder.speciesImage.setImageResource(R.drawable.placeholder); // Default image
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public int getItemCount() {
        return speciesList.length();
    }

    public static class SpeciesViewHolder extends RecyclerView.ViewHolder {
        TextView commonName, scientificName;
        ImageView speciesImage;

        public SpeciesViewHolder(@NonNull View itemView) {
            super(itemView);
            commonName = itemView.findViewById(R.id.common_name);
            scientificName = itemView.findViewById(R.id.scientific_name);
            speciesImage = itemView.findViewById(R.id.species_image);
        }
    }
}

