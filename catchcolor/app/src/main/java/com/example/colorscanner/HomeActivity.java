package com.example.colorscanner;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class HomeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        Button cameraButton = findViewById(R.id.camera_button);
        Button galleryButton = findViewById(R.id.gallery_button);

        // Set click listener for "Take Image" button
        cameraButton.setOnClickListener(v -> {
            // Launch MainActivity which has the camera functionality
            Intent intent = new Intent(HomeActivity.this, MainActivity.class);
            startActivity(intent);
        });

        // Set click listener for "Upload Photo" button
        galleryButton.setOnClickListener(v -> {
            // Launch upload activity
            Intent intent = new Intent(HomeActivity.this, UploadActivity.class);
            startActivity(intent);
        });
    }
}