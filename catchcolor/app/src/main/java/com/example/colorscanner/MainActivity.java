package com.example.colorscanner;
import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.core.ZoomState;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.Observer;
import com.google.common.util.concurrent.ListenableFuture;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private static final int REQUEST_CODE_PERMISSIONS = 10;
    private static final String[] REQUIRED_PERMISSIONS = new String[]{Manifest.permission.CAMERA};
    private PreviewView previewView;
    private TextView colorHexText;
    private TextView colorNameText;
    private View colorPreview;
    private Button captureButton;
    private ImageButton backButton;
    private ImageCapture imageCapture;
    private FrameLayout previewContainer;
    private BoxOverlayView boxOverlayView;
    private final Executor executor = Executors.newSingleThreadExecutor();
    private ImageView capturedImageView;
    private Bitmap capturedBitmap;
    private View previewOverlay;
    private Camera camera;
    private float currentZoomRatio = 1.0f;
    private SeekBar zoomSeekBar;
    private TextView zoomLevelText;
    private Button zoomInButton;
    private Button zoomOutButton;
    private ScaleGestureDetector scaleGestureDetector;
    private boolean inPreviewMode = false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        previewView = findViewById(R.id.preview_view);
        colorHexText = findViewById(R.id.color_hex);
        colorNameText = findViewById(R.id.color_name);
        colorPreview = findViewById(R.id.color_preview);
        captureButton = findViewById(R.id.capture_button);
        previewContainer = findViewById(R.id.preview_container);

        capturedImageView = findViewById(R.id.captured_image_view);
        previewOverlay = findViewById(R.id.preview_overlay);

        backButton = findViewById(R.id.back_button);
        if (backButton != null) {
            backButton.setOnClickListener(v -> {
                finish();
            });
        } else {
            Log.e(TAG, "Back button not found in layout");
        }
        zoomSeekBar = findViewById(R.id.zoom_seek_bar);
        zoomLevelText = findViewById(R.id.zoom_level_text);
        zoomInButton = findViewById(R.id.zoom_in_button);
        zoomOutButton = findViewById(R.id.zoom_out_button);
        boxOverlayView = new BoxOverlayView(this);
        previewContainer.addView(boxOverlayView);
        setupCapturedImageTouchListener();

        scaleGestureDetector = new ScaleGestureDetector(this, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override
            public boolean onScale(ScaleGestureDetector detector) {
                float scaleFactor = detector.getScaleFactor();
                updateZoom(currentZoomRatio * scaleFactor);
                return true;
            }
        });

        previewView.setOnTouchListener((v, event) -> {
            scaleGestureDetector.onTouchEvent(event);
            return true;
        });
        setupZoomControls();
        if (allPermissionsGranted()) {
            Log.d(TAG, "All permissions are granted, starting camera");
            startCamera();
        } else {
            Log.d(TAG, "Requesting camera permissions");
            requestCameraPermission();
        }
        captureButton.setOnClickListener(v -> {
            if (inPreviewMode) {
                resetToCamera();
            } else {
                captureImage();
            }
        });
    }

    private void resetToCamera() {
        // Check this part
        capturedImageView.setVisibility(View.GONE);
        previewOverlay.setVisibility(View.GONE);
        captureButton.setText("Capture Color");
        inPreviewMode = false;
        findViewById(R.id.zoom_controls).setVisibility(View.VISIBLE);
        boxOverlayView.setVisibility(View.VISIBLE);

        Log.d(TAG, "Reset to camera mode for next capture");
    }
    private void setupCapturedImageTouchListener() {
        capturedImageView.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN || event.getAction() == MotionEvent.ACTION_MOVE) {
                if (capturedBitmap != null) {
                    float x = event.getX();
                    float y = event.getY();
                    int bitmapX = (int) (x * capturedBitmap.getWidth() / v.getWidth());
                    int bitmapY = (int) (y * capturedBitmap.getHeight() / v.getHeight());
                    if (bitmapX >= 0 && bitmapX < capturedBitmap.getWidth() &&
                            bitmapY >= 0 && bitmapY < capturedBitmap.getHeight()) {
                        int pixel = capturedBitmap.getPixel(bitmapX, bitmapY);
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
                }
                return true;
            }
            return false;
        });
    }

    private void setupZoomControls() {
        zoomSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && camera != null) {
                    float maxZoom = getMaxZoomRatio();
                    float zoomRatio = 1.0f + ((maxZoom - 1.0f) * progress / 100.0f);
                    camera.getCameraControl().setZoomRatio(zoomRatio);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        zoomInButton.setOnClickListener(v -> {
            if (camera != null) {
                float step = (getMaxZoomRatio() - 1.0f) / 10.0f;
                updateZoom(Math.min(currentZoomRatio + step, getMaxZoomRatio()));
            }
        });

        zoomOutButton.setOnClickListener(v -> {
            if (camera != null) {
                float step = (getMaxZoomRatio() - 1.0f) / 10.0f;
                updateZoom(Math.max(currentZoomRatio - step, 1.0f));
            }
        });
    }

    private void updateZoom(float zoomRatio) {
        if (camera == null) return;

        float maxZoom = getMaxZoomRatio();
        float minZoom = 1.0f;
        zoomRatio = Math.max(minZoom, Math.min(maxZoom, zoomRatio));

        camera.getCameraControl().setZoomRatio(zoomRatio);

        Log.d(TAG, "Zoom updated to: " + zoomRatio + "x");
    }

    private float getMaxZoomRatio() {
        if (camera == null) return 3.0f; 

        ZoomState zoomState = camera.getCameraInfo().getZoomState().getValue();
        if (zoomState != null) {
            return zoomState.getMaxZoomRatio();
        }
        return 3.0f; 
    }

    private void requestCameraPermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)) {
            Log.d(TAG, "Showing permission rationale dialog");
            new AlertDialog.Builder(this)
                    .setTitle("Camera Permission Required")
                    .setMessage("This app needs camera access to detect colors. Please grant the camera permission.")
                    .setPositiveButton("OK", (dialog, which) -> {
                        Log.d(TAG, "User acknowledged permission rationale, requesting permission");
                        ActivityCompat.requestPermissions(MainActivity.this,
                                REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
                    })
                    .setNegativeButton("Cancel", (dialog, which) -> {
                        Log.d(TAG, "User declined to request permission after rationale");
                        Toast.makeText(MainActivity.this,
                                "Camera permission is required. App will close.",
                                Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .create()
                    .show();
        } else {
            Log.d(TAG, "Requesting permission directly");
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
        }
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

                camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture);

                initializeZoomControls();

                Log.d(TAG, "Camera started successfully");

                if (camera.getCameraInfo().getZoomState().getValue() != null) {
                    float maxZoom = camera.getCameraInfo().getZoomState().getValue().getMaxZoomRatio();
                    Log.d(TAG, "Camera max zoom: " + maxZoom);
                } else {
                    Log.d(TAG, "Camera zoom state not available");
                }

            } catch (ExecutionException | InterruptedException e) {
                Log.e(TAG, "Error starting camera: " + e.getMessage());
                e.printStackTrace();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void initializeZoomControls() {
        if (camera == null) return;

        camera.getCameraInfo().getZoomState().observe(this, new Observer<ZoomState>() {
            @Override
            public void onChanged(ZoomState zoomState) {
                if (zoomState != null) {
                    currentZoomRatio = zoomState.getZoomRatio();

                    runOnUiThread(() -> {
                        zoomLevelText.setText(String.format("%.1fx", currentZoomRatio));

                        float maxZoom = zoomState.getMaxZoomRatio();
                        int progress = Math.round((currentZoomRatio - 1.0f) / (maxZoom - 1.0f) * 100);
                        zoomSeekBar.setProgress(progress);
                    });
                }
            }
        });
    }

    private void captureImage() {
        if (imageCapture == null) {
            Log.e(TAG, "imageCapture is null");
            return;
        }

        Log.d(TAG, "Capturing image");
        imageCapture.takePicture(executor, new ImageCapture.OnImageCapturedCallback() {
            @Override
            public void onCaptureSuccess(@NonNull ImageProxy image) {
                Log.d(TAG, "Image captured successfully, format: " + image.getFormat());

                runOnUiThread(() -> {
                    try {
                        Log.d(TAG, "Converting image to bitmap");
                        Bitmap bitmap = imageToBitmap(image);

                        if (bitmap == null) {
                            Log.e(TAG, "Failed to convert image to bitmap");
                            Toast.makeText(MainActivity.this, "Error processing image: bitmap conversion failed", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        Log.d(TAG, "Bitmap created successfully: " + bitmap.getWidth() + "x" + bitmap.getHeight());

                        capturedBitmap = bitmap;

                        capturedImageView.setImageBitmap(bitmap);
                        capturedImageView.setVisibility(View.VISIBLE);

                        if (previewOverlay != null) {
                            previewOverlay.setVisibility(View.VISIBLE);
                        }

                        Log.d(TAG, "Getting scaled box rectangle");
                        Rect boxRect = getScaledBoxRect(bitmap.getWidth(), bitmap.getHeight());
                        Log.d(TAG, "Box rectangle: " + boxRect.toString());

                        Log.d(TAG, "Extracting average color from box");
                        ColorInfo averageColor = extractAverageColorFromBox(bitmap, boxRect);

                        colorHexText.setText(averageColor.getHex());
                        colorNameText.setText(averageColor.getName());
                        colorPreview.setBackgroundColor(Color.parseColor(averageColor.getHex()));

                        captureButton.setText("Take Next Picture");

                        boxOverlayView.setVisibility(View.GONE);
                        findViewById(R.id.zoom_controls).setVisibility(View.GONE);

                        inPreviewMode = true;

                        Log.d(TAG, "Detected color: " + averageColor.getName() + " (" + averageColor.getHex() + ")");

                        Toast.makeText(MainActivity.this,
                                "Touch the image to pick specific colors",
                                Toast.LENGTH_LONG).show();

                    } catch (Exception e) {
                        Log.e(TAG, "Error processing captured image: " + e.getMessage());
                        e.printStackTrace();
                        Toast.makeText(MainActivity.this, "Error processing image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
                image.close();
            }

            @Override
            public void onError(@NonNull ImageCaptureException exception) {
                Log.e(TAG, "Error capturing image: " + exception.getMessage());
                exception.printStackTrace();
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, "Error capturing image: " + exception.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private Rect getScaledBoxRect(int bitmapWidth, int bitmapHeight) {
        Rect boxRect = boxOverlayView.getBoxRect();

        float scaleX = (float) bitmapWidth / previewView.getWidth();
        float scaleY = (float) bitmapHeight / previewView.getHeight();

        return new Rect(
                (int) (boxRect.left * scaleX),
                (int) (boxRect.top * scaleY),
                (int) (boxRect.right * scaleX),
                (int) (boxRect.bottom * scaleY)
        );
    }

    private Bitmap imageToBitmap(ImageProxy image) {
        try {
            Bitmap bitmap = previewView.getBitmap();
            if (bitmap != null) {
                return bitmap;
            }

            ByteBuffer buffer = image.getPlanes()[0].getBuffer();
            byte[] bytes = new byte[buffer.capacity()];
            buffer.get(bytes);

            // use jpegs prolly
            if (image.getFormat() == ImageFormat.JPEG) {
                return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
            }

            if (image.getFormat() == ImageFormat.YUV_420_888) {
                YuvImage yuvImage = new YuvImage(
                        bytes,
                        ImageFormat.NV21,
                        image.getWidth(),
                        image.getHeight(),
                        null);

                ByteArrayOutputStream out = new ByteArrayOutputStream();
                yuvImage.compressToJpeg(
                        new Rect(0, 0, image.getWidth(), image.getHeight()),
                        100,
                        out);

                byte[] jpegBytes = out.toByteArray();
                return BitmapFactory.decodeByteArray(jpegBytes, 0, jpegBytes.length);
            }

            Log.e(TAG, "Unsupported image format: " + image.getFormat());
            return null;
        } catch (Exception e) {
            Log.e(TAG, "Error in imageToBitmap: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    private ColorInfo extractAverageColorFromBox(Bitmap bitmap, Rect boxRect) {
        try {
            Rect safeRect = new Rect(
                    Math.max(0, boxRect.left),
                    Math.max(0, boxRect.top),
                    Math.min(bitmap.getWidth(), boxRect.right),
                    Math.min(bitmap.getHeight(), boxRect.bottom)
            );

            if (safeRect.width() <= 0 || safeRect.height() <= 0) {
                Log.e(TAG, "Box is outside the bitmap boundaries");
                return new ColorInfo("#000000", "Black", 0, 0, 0);
            }

            Bitmap boxBitmap = Bitmap.createBitmap(
                    bitmap,
                    safeRect.left,
                    safeRect.top,
                    safeRect.width(),
                    safeRect.height()
            );

            int width = boxBitmap.getWidth();
            int height = boxBitmap.getHeight();

            long totalR = 0;
            long totalG = 0;
            long totalB = 0;
            int pixelCount = 0;

            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int pixel = boxBitmap.getPixel(x, y);

                    // Skip transparent pixels
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

        if (saturation < 0.15) {
            if (value < 0.15) return "Black (Negro)";
            if (value > 0.9) return "White (Blanco)";
            if (value < 0.3) return "Dark Gray (Gris Oscuro)";
            if (value > 0.7) return "Light Gray (Gris Claro)";
            return "Gray (Gris)";
        }

        // Determine color based on hue
        if (hue < 10 || hue >= 350) {
            if (value < 0.5) return "Dark Red (Rojo Oscuro)";
            if (value > 0.8) return "Bright Red (Rojo Claro)";
            return "Red (Rojo)";
        }

        if (hue >= 10 && hue < 30) {
            if (value < 0.6 && saturation > 0.4) return "Brown (Marrón)";
            if (value > 0.8) return "Light Orange (Naranja Clara)";
            return "Orange (Naranja)";
        }

        if (hue >= 30 && hue < 45) {
            if (value < 0.6 && saturation > 0.5) return "Brown (Marrón)";
            return "Gold (Oro)";
        }

        if (hue >= 45 && hue < 70) {
            if (value < 0.7) return "Olive (aceituna)";
            if (value > 0.9) return "Bright Yellow (Amarilla brillante)";
            return "Yellow (Amarilla)";
        }

        if (hue >= 70 && hue < 150) {
            if (value < 0.4) return "Dark Green (Verde Oscuro)";
            if (saturation > 0.7 && value < 0.6) return "Forest Green (Bosque Verde)";
            if (value > 0.8 && saturation < 0.6) return "Lime Green (Verde lima)";
            if (hue < 100) return "Yellow-Green (Amarillo-Verde)";
            if (hue > 120) return "Teal";
            return "Green";
        }

        if (hue >= 150 && hue < 200) {
            if (value < 0.6) return "Deep Cyan (Cian Intenso)";
            if (value > 0.8) return "Light Cyan (Cian Claro)";
            return "Cyan (Cian)";
        }

        if (hue >= 200 && hue < 240) {
            if (value < 0.5) return "Navy Blue (Azul marino)";
            if (value > 0.8) return "Sky Blue (Azul cielo)";
            return "Blue (Azul)";
        }

        if (hue >= 240 && hue < 280) {
            if (value < 0.5) return "Deep Indigo (indigo profundo)";
            if (hue < 260) return "Indigo (Indigo)";
            return "Violet (violeta)";
        }

        if (hue >= 280 && hue < 320) {
            if (value < 0.4) return "Deep Purple (Púrpura profund)a";
            if (value > 0.8) return "Light Purple (Púrpura claro)";
            return "Purple (Púrpura)";
        }

        if (hue >= 320 && hue < 350) {
            if (value < 0.5) return "Burgundy (Borgoña)";
            if (value > 0.8) return "Pink (Rosa)";
            return "Magenta (Magenta)";
        }

        return "Unknown (desconocida)";
    }

    private boolean allPermissionsGranted() {
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "Permission not granted: " + permission);
                return false;
            }
        }
        Log.d(TAG, "All permissions granted");
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        Log.d(TAG, "onRequestPermissionsResult: requestCode=" + requestCode);

        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                Log.d(TAG, "Permission request granted, starting camera");
                startCamera();
            } else {
                Log.d(TAG, "Permission request denied");
                Toast.makeText(this, "Camera permission is required to use this app. Please enable it in settings.", Toast.LENGTH_LONG).show();

                if (!ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)) {
                    Log.d(TAG, "Permission permanently denied, showing instruction to enable in settings");
                    Toast.makeText(this, "Please enable camera permission in Settings -> Apps -> ColorScanner -> Permissions", Toast.LENGTH_LONG).show();
                }

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
