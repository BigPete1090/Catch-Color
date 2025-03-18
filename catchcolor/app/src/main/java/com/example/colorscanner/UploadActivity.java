package com.example.colorscanner;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.view.MotionEvent;

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
    private static final String[] REQUIRED_PERMISSIONS;
    static {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13 and above
            REQUIRED_PERMISSIONS = new String[]{Manifest.permission.READ_MEDIA_IMAGES};
        } else {
            // Android 12 and below
            REQUIRED_PERMISSIONS = new String[]{Manifest.permission.READ_EXTERNAL_STORAGE};
        }
    }

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

        imagePreview = findViewById(R.id.image_preview);
        colorHexText = findViewById(R.id.color_hex);
        colorNameText = findViewById(R.id.color_name);
        colorPreview = findViewById(R.id.color_preview);
        selectImageButton = findViewById(R.id.select_image_button);
        analyzeButton = findViewById(R.id.analyze_button);

        if (allPermissionsGranted()) {
            Log.d(TAG, "All permissions already granted");
            setupButtons();
        } else {
            Log.d(TAG, "Requesting permissions");
            requestStoragePermission();
        }
        setupImageTouchListener();
    }
    private void setupImageTouchListener() {
        imagePreview.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN || event.getAction() == MotionEvent.ACTION_MOVE) {
                if (selectedBitmap != null) {
                    float x = event.getX();
                    float y = event.getY();
                    int bitmapX = (int) (x * selectedBitmap.getWidth() / v.getWidth());
                    int bitmapY = (int) (y * selectedBitmap.getHeight() / v.getHeight());

                    if (bitmapX >= 0 && bitmapX < selectedBitmap.getWidth() &&
                            bitmapY >= 0 && bitmapY < selectedBitmap.getHeight()) {
                        int pixel = selectedBitmap.getPixel(bitmapX, bitmapY);
                        int r = Color.red(pixel);
                        int g = Color.green(pixel);
                        int b = Color.blue(pixel);
                        String hex = String.format("#%06X", (0xFFFFFF & pixel));
                        String name = getColorName(pixel);

                        ColorInfo touchedColor = new ColorInfo(hex, name, r, g, b);
                        colorHexText.setText(touchedColor.getHex());
                        colorNameText.setText(touchedColor.getName());
                        colorPreview.setBackgroundColor(Color.parseColor(touchedColor.getHex()));

                        Log.d(TAG, "Touch detected color: " + touchedColor.getName() + " (" + touchedColor.getHex() + ")");
                    }
                    return true;
                }
            }
            return false;
        });
    }

    private void setupButtons() {
        selectImageButton.setOnClickListener(v -> pickImage());

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
                analyzeButton.setVisibility(View.VISIBLE);
            } catch (IOException e) {
                Log.e(TAG, "Error loading image: " + e.getMessage());
                Toast.makeText(this, "Error loading image", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void analyzeImage(Bitmap bitmap) {
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
            int safeLeft = Math.max(0, left);
            int safeTop = Math.max(0, top);
            int safeRight = Math.min(bitmap.getWidth(), right);
            int safeBottom = Math.min(bitmap.getHeight(), bottom);

            if (safeRight <= safeLeft || safeBottom <= safeTop) {
                Log.e(TAG, "Box is outside the bitmap boundaries");
                return new ColorInfo("#000000", "Black", 0, 0, 0);
            }

            long totalR = 0;
            long totalG = 0;
            long totalB = 0;
            int pixelCount = 0;

            for (int y = safeTop; y < safeBottom; y++) {
                for (int x = safeLeft; x < safeRight; x++) {
                    int pixel = bitmap.getPixel(x, y);

                    if (Color.alpha(pixel) < 128) continue;

                    totalR += Color.red(pixel);
                    totalG += Color.green(pixel);
                    totalB += Color.blue(pixel);
                    pixelCount++;
                }
            }

            if (pixelCount == 0) {
                return new ColorInfo("#000000", "Black", 0, 0, 0);
            }

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

        float[] hsv = new float[3];
        Color.RGBToHSV(r, g, b, hsv);
        float hue = hsv[0];
        float saturation = hsv[1];
        float value = hsv[2];

        // Improved grayscale detection
        if (saturation < 0.15) {
            if (value < 0.1) return "Black (Negro)";
            if (value < 0.2) return "Very Dark Gray (Gris Muy Oscuro)";
            if (value < 0.35) return "Dark Gray (Gris Oscuro)";
            if (value < 0.65) return "Gray (Gris)";
            if (value < 0.85) return "Light Gray (Gris Claro)";
            if (value < 0.95) return "Very Light Gray (Gris Muy Claro)";
            return "White (Blanco)";
        }

        // Determine color based on hue
        if (hue < 10 || hue >= 350) {
            if (value < 0.3) return "Very Dark Red (Rojo Muy Oscuro)";
            if (value < 0.5) return "Dark Red (Rojo Oscuro)";
            if (value > 0.85) return "Bright Red (Rojo Brillante)";
            return "Red (Rojo)";
        }

        // Improved brown-orange scale
        if (hue >= 10 && hue < 30) {
            if (value < 0.3) return "Deep Brown (Marrón Profundo)";
            if (value < 0.5 && saturation > 0.4) return "Brown (Marrón)";
            if (value < 0.6 && saturation > 0.6) return "Medium Brown (Marrón Medio)";
            if (value < 0.7 && saturation > 0.7) return "Light Brown (Marrón Claro)";
            if (value > 0.85) return "Light Orange (Naranja Clara)";
            return "Orange (Naranja)";
        }

        // Better distinction between brown and gold
        if (hue >= 30 && hue < 45) {
            if (value < 0.3) return "Deep Brown (Marrón Profundo)";
            if (value < 0.5 && saturation > 0.5) return "Brown (Marrón)";
            if (value < 0.6 && saturation > 0.6) return "Medium Brown (Marrón Medio)";
            if (saturation > 0.7 && value > 0.7) return "Gold (Oro)";
            if (value > 0.8) return "Light Gold (Oro Claro)";
            return "Amber (Ámbar)";
        }

        if (hue >= 45 && hue < 70) {
            if (value < 0.3) return "Dark Olive (Oliva Oscura)";
            if (value < 0.5) return "Olive (Oliva)";
            if (value < 0.7) return "Mustard (Mostaza)";
            if (value > 0.9) return "Bright Yellow (Amarillo Brillante)";
            return "Yellow (Amarillo)";
        }

        if (hue >= 70 && hue < 150) {
            if (value < 0.3) return "Deep Green (Verde Profundo)";
            if (value < 0.5) return "Dark Green (Verde Oscuro)";
            if (saturation > 0.7 && value < 0.6) return "Forest Green (Verde Bosque)";
            if (value > 0.8 && saturation < 0.6) return "Lime Green (Verde Lima)";
            if (hue < 100) return "Yellow-Green (Amarillo-Verde)";
            if (hue > 120) return "Teal (Verde Azulado)";
            return "Green (Verde)";
        }

        if (hue >= 150 && hue < 200) {
            if (value < 0.3) return "Deep Teal (Verde Azulado Profundo)";
            if (value < 0.6) return "Deep Cyan (Cian Intenso)";
            if (value > 0.8) return "Light Cyan (Cian Claro)";
            return "Cyan (Cian)";
        }

        if (hue >= 200 && hue < 240) {
            if (value < 0.3) return "Deep Navy (Azul Marino Profundo)";
            if (value < 0.5) return "Navy Blue (Azul Marino)";
            if (value > 0.8) return "Sky Blue (Azul Cielo)";
            return "Blue (Azul)";
        }

        if (hue >= 240 && hue < 280) {
            if (value < 0.3) return "Deep Indigo (Índigo Profundo)";
            if (value < 0.5) return "Indigo (Índigo)";
            if (hue < 260) return "Blue-Violet (Azul-Violeta)";
            return "Violet (Violeta)";
        }

        if (hue >= 280 && hue < 320) {
            if (value < 0.3) return "Deep Purple (Púrpura Profundo)";
            if (value < 0.5) return "Purple (Púrpura)";
            if (value > 0.8) return "Light Purple (Púrpura Claro)";
            return "Medium Purple (Púrpura Medio)";
        }

        if (hue >= 320 && hue < 350) {
            if (value < 0.3) return "Deep Burgundy (Borgoña Profundo)";
            if (value < 0.5) return "Burgundy (Borgoña)";
            if (value > 0.8) return "Pink (Rosa)";
            return "Magenta (Magenta)";
        }

        return "Unknown (Desconocido)";
    }

    private boolean allPermissionsGranted() {
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                Log.e(TAG, "Permission not granted: " + permission);
                return false;
            }
        }
        return true;
    }

    private void requestStoragePermission() {
        // Show rationale if needed before requesting permission
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, REQUIRED_PERMISSIONS[0])) {
            Toast.makeText(this,
                    "Storage access is needed to select images from your gallery",
                    Toast.LENGTH_LONG).show();
        }

        ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            boolean allGranted = true;
            // Check each permission result
            for (int i = 0; i < permissions.length; i++) {
                if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                    Log.e(TAG, "Permission denied: " + permissions[i]);
                    allGranted = false;
                    break;
                }
            }

            if (allGranted) {
                Log.d(TAG, "All permissions granted, setting up buttons");
                setupButtons();
            } else {
                Log.e(TAG, "Permission denied by user");
                Toast.makeText(this, "Storage permission is required to use this feature", Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }

    // ColorInfo class for storing color data
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