package com.example.colorscanner;
import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.google.common.util.concurrent.ListenableFuture;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_CODE_PERMISSIONS = 10;
    private static final String[] REQUIRED_PERMISSIONS = new String[]{Manifest.permission.CAMERA};
    private PreviewView previewView;
    private ImageView capturedImage;
    private TextView colorHexText;
    private TextView colorNameText;
    private View colorPreview;
    private Button captureButton;
    private ImageCapture imageCapture;
    private final Executor executor = Executors.newSingleThreadExecutor();
    private boolean highPrecisionMode = false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        previewView = findViewById(R.id.preview_view);
        capturedImage = findViewById(R.id.captured_image);
        colorHexText = findViewById(R.id.color_hex);
        colorNameText = findViewById(R.id.color_name);
        colorPreview = findViewById(R.id.color_preview);
        captureButton = findViewById(R.id.capture_button);

        if (allPermissionsGranted()) {
            startCamera();
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
        }

        // Set up capture button listener
        captureButton.setOnClickListener(v -> captureImage());
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                imageCapture = new ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                        .build();
                CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;
                cameraProvider.unbindAll();

                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture);

            } catch (ExecutionException | InterruptedException e) {
                // Handle any errors
                e.printStackTrace();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void captureImage() {
        if (imageCapture == null) return;

        imageCapture.takePicture(executor, new ImageCapture.OnImageCapturedCallback() {
            @Override
            public void onCaptureSuccess(@NonNull ImageProxy image) {
                runOnUiThread(() -> {
                    // Process the captured image
                    Bitmap bitmap = imageToBitmap(image);
                    capturedImage.setImageBitmap(bitmap);
                    capturedImage.setVisibility(View.VISIBLE);

                    // Extract colors
                    ColorInfo dominantColor = extractDominantColor(bitmap);
                    
                    // Update UI with color information
                    colorHexText.setText(dominantColor.getHex());
                    colorNameText.setText(dominantColor.getName());
                    colorPreview.setBackgroundColor(Color.parseColor(dominantColor.getHex()));
                });
                image.close();
            }

            @Override
            public void onError(@NonNull ImageCaptureException exception) {
                exception.printStackTrace();
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, "Error capturing image: " + exception.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private Bitmap imageToBitmap(ImageProxy image) {
        // Convert ImageProxy to Bitmap
        ByteBuffer buffer = image.getPlanes()[0].getBuffer();
        byte[] bytes = new byte[buffer.capacity()];
        buffer.get(bytes);
        
        // Simplified conversion for demo purposes
        // In a production app, you would handle YUV to RGB conversion properly
        Bitmap bitmap = Bitmap.createBitmap(image.getWidth(), image.getHeight(), Bitmap.Config.ARGB_8888);
        // This is a placeholder - you'd need to properly decode the YUV data
        // For now, we'll create a sample bitmap to demonstrate the concept
        for (int y = 0; y < bitmap.getHeight(); y++) {
            for (int x = 0; x < bitmap.getWidth(); x++) {
                bitmap.setPixel(x, y, Color.rgb(bytes[(y * image.getWidth() + x) % bytes.length] & 0xFF,
                        bytes[(y * image.getWidth() + x + 1) % bytes.length] & 0xFF,
                        bytes[(y * image.getWidth() + x + 2) % bytes.length] & 0xFF));
            }
        }
        return bitmap;
    }

    private ColorInfo extractDominantColor(Bitmap bitmap) {
        // Resize bitmap for faster processing
        Bitmap resizedBitmap = Bitmap.createScaledBitmap(bitmap, 
                highPrecisionMode ? 300 : 100, 
                highPrecisionMode ? 300 : 100, 
                false);
        
        int width = resizedBitmap.getWidth();
        int height = resizedBitmap.getHeight();
        
        // Map to count color occurrences
        HashMap<Integer, Integer> colorMap = new HashMap<>();
        
        // Step size for sampling
        int step = highPrecisionMode ? 1 : 4;
        
        // Quantization level
        int quantLevel = highPrecisionMode ? 8 : 24;
        
        for (int y = 0; y < height; y += step) {
            for (int x = 0; x < width; x += step) {
                int pixel = resizedBitmap.getPixel(x, y);
                
                // Skip transparent pixels
                if (Color.alpha(pixel) < 128) continue;
                
                // Quantize colors to group similar ones
                int r = Math.round(Color.red(pixel) / quantLevel) * quantLevel;
                int g = Math.round(Color.green(pixel) / quantLevel) * quantLevel;
                int b = Math.round(Color.blue(pixel) / quantLevel) * quantLevel;
                
                int quantizedColor = Color.rgb(r, g, b);
                
                colorMap.put(quantizedColor, colorMap.getOrDefault(quantizedColor, 0) + 1);
            }
        }
        
        // Find the most frequent color
        Map.Entry<Integer, Integer> maxEntry = null;
        for (Map.Entry<Integer, Integer> entry : colorMap.entrySet()) {
            if (maxEntry == null || entry.getValue() > maxEntry.getValue()) {
                maxEntry = entry;
            }
        }
        
        if (maxEntry != null) {
            int dominantColor = maxEntry.getKey();
            String hex = String.format("#%06X", (0xFFFFFF & dominantColor));
            String name = getColorName(dominantColor);
            
            return new ColorInfo(
                hex, 
                name,
                Color.red(dominantColor),
                Color.green(dominantColor),
                Color.blue(dominantColor)
            );
        }
        
        // Fallback
        return new ColorInfo("#000000", "Black", 0, 0, 0);
    }
    
    private String getColorName(int color) {
        // Basic color name mapping - in a real app, you'd use a more comprehensive database
        // like the color-namer library in the Node.js example
        int r = Color.red(color);
        int g = Color.green(color);
        int b = Color.blue(color);
        
        // Simple algorithm to get basic color names
        if (r > 200 && g > 200 && b > 200) return "White";
        if (r < 50 && g < 50 && b < 50) return "Black";
        
        if (r > 200 && g < 100 && b < 100) return "Red";
        if (r < 100 && g > 200 && b < 100) return "Green";
        if (r < 100 && g < 100 && b > 200) return "Blue";
        
        if (r > 200 && g > 200 && b < 100) return "Yellow";
        if (r > 200 && g < 100 && b > 200) return "Magenta";
        if (r < 100 && g > 200 && b > 200) return "Cyan";
        
        if (r > 200 && g > 100 && b < 100) return "Orange";
        if (r > 100 && g < 100 && b > 200) return "Purple";
        if (r < 100 && g > 100 && b < 100) return "Forest Green";
        
        // Fallback
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

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera();
            } else {
                Toast.makeText(this, "Permissions not granted by the user.", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }
    
    public static class ColorInfo {
        private final String hex;
        private final String name;
        private final int r;
        private final int g;
        private final int b;
        
        public ColorInfo(String hex, String name, int r, int g, int b) {
            this.hex = hex;
            this.name = name;
            this.r = r;
            this.g = g;
            this.b = b;
        }
        
        public String getHex() {
            return hex;
        }
        
        public String getName() {
            return name;
        }
        
        public int getR() {
            return r;
        }
        
        public int getG() {
            return g;
        }
        
        public int getB() {
            return b;
        }
    }
}
