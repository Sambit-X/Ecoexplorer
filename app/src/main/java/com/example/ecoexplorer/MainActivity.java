package com.example.ecoexplorer;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {
    private static final String API_URL = "https://next-api-dot-api-2-x-dot-map-of-life.appspot.com/2.x/spatial/species/list";
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private FusedLocationProviderClient fusedLocationClient;
    private TextView coordsView;
    private ProgressBar loadingSpinner;
    private RecyclerView recyclerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        Button findButton = findViewById(R.id.find);
        coordsView = findViewById(R.id.coords);
        loadingSpinner = findViewById(R.id.loading_spinner);

        findButton.setOnClickListener(v -> {
            // Check for location permission
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                getLastLocation();
            } else {
                // Request location permission
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
            }
        });



    }

    private void getLastLocation() {
        // Check if permission is granted again
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        // Get the last known location
        fusedLocationClient.getLastLocation().addOnSuccessListener(new OnSuccessListener<Location>() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onSuccess(Location location) {
                if (location != null) {
                    // Get latitude and longitude
                    double latitude = location.getLatitude();
                    double longitude = location.getLongitude();

                    // Display latitude and longitude in the TextView
                    coordsView.setText(String.format("Latitude: %s\nLongitude: %s", latitude, longitude));

                    // Send the latitude and longitude to the API
                    sendApiRequest(latitude, longitude);
                } else {
                    coordsView.setText("Location not available");
                }
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted
                getLastLocation();
            } else {
                // Permission denied
                Toast.makeText(this, "Location permission required to get location", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void sendApiRequest(double latitude, double longitude) {
        loadingSpinner.setVisibility(View.VISIBLE); // Show loading spinner

        ExecutorService executorService = Executors.newSingleThreadExecutor();
        executorService.submit(() -> {
            try {
                String jsonInputString = String.format(
                        "{\"lang\":\"en\", \"lat\":%f, \"lng\":%f, \"radius\":\"25000\"}",
                        latitude, longitude
                );

                URL url = new URL(API_URL);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json; utf-8");
                connection.setRequestProperty("Accept", "application/json");
                connection.setDoOutput(true);

                try (OutputStream os = connection.getOutputStream()) {
                    byte[] input = jsonInputString.getBytes("utf-8");
                    os.write(input, 0, input.length);
                }

                try (BufferedReader br = new BufferedReader(
                        new InputStreamReader(connection.getInputStream(), "utf-8")
                )) {
                    StringBuilder response = new StringBuilder();
                    String responseLine;
                    while ((responseLine = br.readLine()) != null) {
                        response.append(responseLine.trim());
                    }

                    new Handler(Looper.getMainLooper()).post(() -> {
                        loadingSpinner.setVisibility(View.GONE); // Hide loading spinner
                        parseAndDisplaySpecies(response.toString());
                    });
                }

            } catch (Exception e) {
                e.printStackTrace();
                new Handler(Looper.getMainLooper()).post(() -> {
                    loadingSpinner.setVisibility(View.GONE); // Hide loading spinner on error
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void parseAndDisplaySpecies(String responseJson) {
        try {
            JSONObject responseObject = new JSONObject(responseJson);
            JSONArray taxasArray = responseObject.getJSONArray("taxas");

            JSONArray speciesList = new JSONArray();

            for (int i = 0; i < taxasArray.length(); i++) {
                JSONObject taxa = taxasArray.getJSONObject(i);

                if (taxa.has("species") && !taxa.isNull("species")) {
                    JSONArray speciesArray = taxa.getJSONArray("species");

                    for (int j = 0; j < speciesArray.length(); j++) {
                        speciesList.put(speciesArray.getJSONObject(j));
                    }
                }
            }

            // Set up RecyclerView
            recyclerView = findViewById(R.id.recycler_view);
            recyclerView.setLayoutManager(new LinearLayoutManager(this));
            SpeciesAdapter adapter = new SpeciesAdapter(this, speciesList);
            recyclerView.setAdapter(adapter);

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error parsing response: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

}
