package com.example.colorscanner;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class ResourcesActivity extends AppCompatActivity {

    private RecyclerView resourcesRecyclerView;
    private ResourceAdapter resourceAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_resources);

        ImageButton backButton = findViewById(R.id.back_button);
        resourcesRecyclerView = findViewById(R.id.resources_recycler_view);

        // Set up back button
        backButton.setOnClickListener(v -> finish());

        // Set up RecyclerView
        setupRecyclerView();
    }

    private void setupRecyclerView() {
        // Create list of resource items
        List<ResourceItem> resources = new ArrayList<>();

        // Add colorblind support resources
        resources.add(new ResourceItem(
                "Colorblind Awareness",
                "Information, resources and support for color vision deficiency",
                "https://www.colourblindawareness.org"));

        resources.add(new ResourceItem(
                "National Eye Institute",
                "Information about color blindness causes, symptoms, and management",
                "https://www.nei.nih.gov/learn-about-eye-health/eye-conditions-and-diseases/color-blindness"));

        resources.add(new ResourceItem(
                "EnChroma",
                "Color blind glasses and online color blindness tests",
                "https://enchroma.com"));

        resources.add(new ResourceItem(
                "Color Blind Essentials",
                "Community forum and resources for colorblind individuals",
                "https://www.colorblindessentials.com"));

        resources.add(new ResourceItem(
                "We Are Colorblind",
                "Resources and articles about color blindness accessibility",
                "https://wearecolorblind.com"));

        resources.add(new ResourceItem(
                "American Foundation for the Blind",
                "Support and resources for people with vision impairments",
                "https://www.afb.org"));

        // Set up adapter
        resourceAdapter = new ResourceAdapter(resources, url -> {
            // Open URL when item is clicked
            try {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                startActivity(browserIntent);
            } catch (Exception e) {
                Toast.makeText(ResourcesActivity.this,
                        "Cannot open URL: " + e.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });

        // Set layout manager and adapter
        resourcesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        resourcesRecyclerView.setAdapter(resourceAdapter);
    }
}