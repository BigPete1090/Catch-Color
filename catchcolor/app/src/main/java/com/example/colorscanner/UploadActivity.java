package com.example.colorscanner;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.IOException;

public class UploadActivity extends AppCompatActivity {
    private static final String TAG = "UploadActivity";
    private static final int REQUEST_CODE_PERMISSIONS = 20;
    private static final int REQUEST_CODE_PICK_IMAGE = 100;
    private static final String[] REQUIRED_PERMISSIONS = new String[]{
            Manifest.permission.READ_EXTERNAL_STORAGE
    };

    private ImageView imagePreview;
    private TextView colorHexText;
    private TextView colorNameText;
    private View colorPreview;
    private Button selectImageButton;
    private Button analyzeButton;
    private Bitmap selectedBitmap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upload);

        // Initialize views
        imagePreview = findViewById(R.id.image_preview);
        colorHexText = findViewById(R.id.color_hex);
        colorNameText = findViewById(R.id.color_name);
        colorPreview = findViewById(R.id.color_preview);
        selectImageButton = findViewById(R.id.select_image_button);
        analyzeButton = findViewById(R.id.analyze_button);

        // Check for storage permissions
        if (allPermissionsGranted()) {
            setupButtons();
        } else {
            requestStoragePermission();
        }
    }

    private void setupButtons() {
        // Set up listener for Select Image button
        selectImageButton.setOnClickListener(v -> pickImage());

        // Set up listener for Analyze button
        analyzeButton.setOnClickListener(v -> {
            if (selectedBitmap != null) {
                analyzeImage(selectedBitmap);
            } else {
                Toast.makeText(this, "Please select an image first", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void pickImage() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, REQUEST_CODE_PICK_IMAGE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_PICK_IMAGE && resultCode == RESULT_OK && data != null) {
            Uri selectedImageUri = data.getData();
            try {
                selectedBitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), selectedImageUri);
                imagePreview.setImageBitmap(selectedBitmap);
                // Make the analyze button visible once image is selected
                analyzeButton.setVisibility(View.VISIBLE);
            } catch (IOException e) {
                Log.e(TAG, "Error loading image: " + e.getMessage());
                Toast.makeText(this, "Error loading image", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void analyzeImage(Bitmap bitmap) {
        // Calculate the center 20% of the image as the sampling area
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        int boxWidth = (int)(width * 0.2);
        int boxHeight = (int)(height * 0.2);

        int left = (width - boxWidth) / 2;
        int top = (height - boxHeight) / 2;

        ColorInfo averageColor = extractAverageColorFromBox(bitmap,
                left, top, left + boxWidth, top + boxHeight);

        colorHexText.setText(averageColor.getHex());
        colorNameText.setText(averageColor.getName());
        colorPreview.setBackgroundColor(Color.parseColor(averageColor.getHex()));
    }

    private ColorInfo extractAverageColorFromBox(Bitmap bitmap, int left, int top, int right, int bottom) {
        try {
            // Ensure the box is within the bitmap boundaries
            int safeLeft = Math.max(0, left);
            int safeTop = Math.max(0, top);
            int safeRight = Math.min(bitmap.getWidth(), right);
            int safeBottom = Math.min(bitmap.getHeight(), bottom);

            // If the box is outside the bitmap, return a default color
            if (safeRight <= safeLeft || safeBottom <= safeTop) {
                Log.e(TAG, "Box is outside the bitmap boundaries");
                return new ColorInfo("#000000", "Black", 0, 0, 0);
            }

            long totalR = 0;
            long totalG = 0;
            long totalB = 0;
            int pixelCount = 0;

            // Calculate the sum of all pixel values in the specified region
            for (int y = safeTop; y < safeBottom; y++) {
                for (int x = safeLeft; x < safeRight; x++) {
                    int pixel = bitmap.getPixel(x, y);

                    // Skip transparent pixels
                    if (Color.alpha(pixel) < 128) continue;

                    totalR += Color.red(pixel);
                    totalG += Color.green(pixel);
                    totalB += Color.blue(pixel);
                    pixelCount++;
                }
            }

            // If no valid pixels, return default color
            if (pixelCount == 0) {
                return new ColorInfo("#000000", "Black", 0, 0, 0);
            }

            // Calculate the average color
            int avgR = (int) (totalR / pixelCount);
            int avgG = (int) (totalG / pixelCount);
            int avgB = (int) (totalB / pixelCount);

            int avgColor = Color.rgb(avgR, avgG, avgB);
            String hex = String.format("#%06X", (0xFFFFFF & avgColor));
            String name = getColorName(avgColor);

            return new ColorInfo(
                    hex,
                    name,
                    avgR,
                    avgG,
                    avgB
            );

        } catch (Exception e) {
            Log.e(TAG, "Error in extractAverageColorFromBox: " + e.getMessage());
            e.printStackTrace();
        }

        return new ColorInfo("#000000", "Black", 0, 0, 0);
    }

    private String getColorName(int color) {
        int r = Color.red(color);
        int g = Color.green(color);
        int b = Color.blue(color);

        // Calculate hue, saturation, and value
        float[] hsv = new float[3];
        Color.RGBToHSV(r, g, b, hsv);
        float hue = hsv[0];
        float saturation = hsv[1];
        float value = hsv[2];

        // Check for grayscale colors first
        if (saturation < 0.15) {
            if (value < 0.15) return "Black";
            if (value > 0.9) return "White";
            if (value < 0.3) return "Dark Gray";
            if (value > 0.7) return "Light Gray";
            return "Gray";
        }

        // Determine color based on hue
        if (hue < 10 || hue >= 350) {
            if (value < 0.5) return "Dark Red";
            if (value > 0.8) return "Bright Red";
            return "Red";
        }

        if (hue >= 10 && hue < 30) {
            if (value < 0.6 && saturation > 0.4) return "Brown";
            if (value > 0.8) return "Light Orange";
            return "Orange";
        }

        if (hue >= 30 && hue < 45) {
            if (value < 0.6 && saturation > 0.5) return "Brown";
            return "Gold";
        }

        if (hue >= 45 && hue < 70) {
            if (value < 0.7) return "Olive";
            if (value > 0.9) return "Bright Yellow";
            return "Yellow";
        }

        if (hue >= 70 && hue < 150) {
            if (value < 0.4) return "Dark Green";
            if (saturation > 0.7 && value < 0.6) return "Forest Green";
            if (value > 0.8 && saturation < 0.6) return "Lime Green";
            if (hue < 100) return "Yellow-Green";
            if (hue > 120) return "Teal";
            return "Green";
        }

        if (hue >= 150 && hue < 200) {
            if (value < 0.6) return "Deep Cyan";
            if (value > 0.8) return "Light Cyan";
            return "Cyan";
        }

        if (hue >= 200 && hue < 240) {
            if (value < 0.5) return "Navy Blue";
            if (value > 0.8) return "Sky Blue";
            return "Blue";
        }

        if (hue >= 240 && hue < 280) {
            if (value < 0.5) return "Deep Indigo";
            if (hue < 260) return "Indigo";
            return "Violet";
        }

        if (hue >= 280 && hue < 320) {
            if (value < 0.4) return "Deep Purple";
            if (value > 0.8) return "Light Purple";
            return "Purple";
        }

        if (hue >= 320 && hue < 350) {
            if (value < 0.5) return "Burgundy";
            if (value > 0.8) return "Pink";
            return "Magenta";
        }

        return "Unknown";
    }

    private boolean allPermissionsGranted() {
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    private void requestStoragePermission() {
        ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                setupButtons();
            } else {
                Toast.makeText(this, "Storage permission is required to use this feature", Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }

    // Add ColorInfo class since the reference to MainActivity.ColorInfo will cause errors
    public static class ColorInfo {
        private final String hex;
        private final String name;
        private final int red;
        private final int green;
        private final int blue;

        public ColorInfo(String hex, String name, int red, int green, int blue) {
            this.hex = hex;
            this.name = name;
            this.red = red;
            this.green = green;
            this.blue = blue;
        }

        public String getHex() {
            return hex;
        }

        public String getName() {
            return name;
        }

        public int getRed() {
            return red;
        }

        public int getGreen() {
            return green;
        }

        public int getBlue() {
            return blue;
        }
    }
}