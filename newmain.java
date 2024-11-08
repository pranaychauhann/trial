package com.example.pbl;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class MainActivity extends AppCompatActivity {

    private static final int CAMERA_PERMISSION_REQUEST_CODE = 100;
    private static final int VIDEO_REQUEST_CODE = 101;
    private static final int POLL_INTERVAL_MS = 5000; // Poll every 5 seconds

    private Button buttonOpenCamera;
    private Button buttonCloseCamera;
    private TextView textViewOutput;
    private Handler handler;
    private Runnable fetchOutputRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initializeViews();
        setButtonListeners();

        // Initialize the Handler and Runnable for periodic output fetching
        handler = new Handler(Looper.getMainLooper());
        fetchOutputRunnable = new Runnable() {
            @Override
            public void run() {
                fetchOutput(); // Fetch output from the server
                handler.postDelayed(this, POLL_INTERVAL_MS); // Schedule the next fetch
            }
        };
    }

    private void initializeViews() {
        buttonOpenCamera = findViewById(R.id.button_open_camera);
        buttonCloseCamera = findViewById(R.id.button_close_camera);
        textViewOutput = findViewById(R.id.textView_output);
    }

    private void setButtonListeners() {
        buttonOpenCamera.setOnClickListener(v -> {
            if (isCameraAndAudioPermissionGranted()) {
                openCamera();
            } else {
                requestCameraAndAudioPermissions();
            }
        });

        buttonCloseCamera.setOnClickListener(v -> finish());
    }

    private boolean isCameraAndAudioPermissionGranted() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestCameraAndAudioPermissions() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO},
                CAMERA_PERMISSION_REQUEST_CODE);
    }

    private void openCamera() {
        Intent cameraIntent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
        if (cameraIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(cameraIntent, VIDEO_REQUEST_CODE);
        } else {
            showToast("No app can handle this action");
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            openCamera();
        } else {
            showToast("Camera permission is required to use this feature.");
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == VIDEO_REQUEST_CODE && resultCode == RESULT_OK) {
            Uri videoUri = data.getData();
            uploadVideo(videoUri);
        } else {
            showToast("Video capture canceled.");
        }
    }

    private void uploadVideo(Uri videoUri) {
        new Thread(() -> {
            try {
                HttpURLConnection connection = createHttpURLConnection("http://172.20.10.2:5000/upload");
                OutputStream outputStream = connection.getOutputStream();
                writeVideoDataToStream(videoUri, outputStream);

                int responseCode = connection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    runOnUiThread(() -> {
                        showToast("Video uploaded successfully");
                        fetchOutput(); // Fetch the output after uploading
                    });
                } else {
                    runOnUiThread(() -> showToast("Upload failed: " + responseCode));
                }

                connection.disconnect();
            } catch (IOException e) {
                runOnUiThread(() -> showToast("Upload error: " + e.getMessage()));
            }
        }).start();
    }

    private HttpURLConnection createHttpURLConnection(String urlString) throws IOException {
        URL url = new URL(urlString);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);
        connection.setRequestProperty("Connection", "Keep-Alive");
        connection.setRequestProperty("Cache-Control", "no-cache");
        connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=*****");
        return connection;
    }

    private void writeVideoDataToStream(Uri videoUri, OutputStream outputStream) throws IOException {
        String boundary = "*****";
        String twoHyphens = "--";
        String lineEnd = "\r\n";

        outputStream.write((twoHyphens + boundary + lineEnd).getBytes());
        outputStream.write(("Content-Disposition: form-data; name=\"file\"; filename=\"video.mp4\"" + lineEnd).getBytes());
        outputStream.write(("Content-Type: video/mp4" + lineEnd).getBytes());
        outputStream.write(lineEnd.getBytes());

        InputStream inputStream = getContentResolver().openInputStream(videoUri);
        byte[] buffer = new byte[4096];
        int bytesRead;
        while ((bytesRead = inputStream.read(buffer)) != -1) {
            outputStream.write(buffer, 0, bytesRead);
        }
        inputStream.close();

        outputStream.write(lineEnd.getBytes());
        outputStream.write((twoHyphens + boundary + twoHyphens + lineEnd).getBytes());
        outputStream.flush();
        outputStream.close();
    }

    private void fetchOutput() {
        new Thread(() -> {
            try {
                // Send a GET request to fetch the output from the server
                URL url = new URL("http://172.20.10.2:5000/output"); // Adjust this URL to your Flask server endpoint
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");

                // Check the server response code
                int responseCode = connection.getResponseCode();
                if (responseCode != HttpURLConnection.HTTP_OK) {
                    throw new IOException("Server returned non-OK response: " + responseCode);
                }

                // Read the response
                InputStream inputStream = connection.getInputStream();
                byte[] buffer = new byte[4096];
                int bytesRead;
                StringBuilder response = new StringBuilder();
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    response.append(new String(buffer, 0, bytesRead));
                }
                inputStream.close();

                // Update UI with the server's response
                runOnUiThread(() -> textViewOutput.setText(response.toString()));

                connection.disconnect();
            } catch (IOException e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(MainActivity.this, "Error fetching output: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private void showToast(String message) {
        runOnUiThread(() -> Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show());
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Start polling for output when the app is in the foreground
        handler.post(fetchOutputRunnable);
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Stop polling when the app is in the background
        handler.removeCallbacks(fetchOutputRunnable);
    }
}
