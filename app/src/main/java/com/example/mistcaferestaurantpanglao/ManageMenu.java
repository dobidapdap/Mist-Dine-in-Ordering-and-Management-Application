package com.example.mistcaferestaurantpanglao;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.*;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import okhttp3.*;

public class ManageMenu extends AppCompatActivity {

    private static final int PICK_IMAGE_REQUEST = 1;
    private static final String TAG = "ManageMenu";

    private ImageView ivDishImage, btnBack;
    private EditText etDishName, etDishDescription, etPrice;
    private Spinner spnCategory;
    private MaterialCheckBox cbIsAddOn;
    private TextInputLayout tilCategory;
    private Button btnSelectImage, btnAddDish, btnCancel;

    private Uri imageUri;
    private FirebaseFirestore db;
    private OkHttpClient httpClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_menu);

        ivDishImage = findViewById(R.id.ivDishImage);
        btnBack = findViewById(R.id.btnBack);
        etDishName = findViewById(R.id.etDishName);
        etDishDescription = findViewById(R.id.etDishDescription);
        etPrice = findViewById(R.id.etPrice);
        spnCategory = findViewById(R.id.spnCategory);
        cbIsAddOn = findViewById(R.id.cbIsAddOn);
        tilCategory = findViewById(R.id.tilCategory);
        btnSelectImage = findViewById(R.id.btnSelectImage);
        btnAddDish = findViewById(R.id.btnAddDish);
        btnCancel = findViewById(R.id.btnCancel);

        db = FirebaseFirestore.getInstance();
        httpClient = new OkHttpClient();

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this, R.array.dish_categories, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spnCategory.setAdapter(adapter);

        setupPriceValidation();
        setupAddOnCheckbox();
        setupClickListeners();
    }

    /**
     * Setup all click listeners including back button
     */
    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> onBackPressed());
        btnSelectImage.setOnClickListener(v -> openImageChooser());
        btnAddDish.setOnClickListener(v -> uploadImageToSupabase());
        btnCancel.setOnClickListener(v -> clearFields());
    }

    @Override
    public void onBackPressed() {
        // Check if there's unsaved data
        if (hasUnsavedData()) {
            new androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("Discard Changes?")
                    .setMessage("You have unsaved changes. Do you want to discard them?")
                    .setPositiveButton("Discard", (dialog, which) -> {
                        super.onBackPressed();
                        finish();
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        } else {
            super.onBackPressed();
            finish();
        }
    }

    /**
     * Check if user has entered any data
     */
    private boolean hasUnsavedData() {
        return !etDishName.getText().toString().trim().isEmpty() ||
                !etDishDescription.getText().toString().trim().isEmpty() ||
                !etPrice.getText().toString().trim().isEmpty() ||
                imageUri != null;
    }

    private void setupAddOnCheckbox() {
        cbIsAddOn.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                // Disable category selection when add-on is checked
                tilCategory.setEnabled(false);
                spnCategory.setEnabled(false);
                tilCategory.setAlpha(0.5f);
                spnCategory.setSelection(0); // Reset to first item
            } else {
                // Enable category selection when add-on is unchecked
                tilCategory.setEnabled(true);
                spnCategory.setEnabled(true);
                tilCategory.setAlpha(1.0f);
            }
        });
    }

    private void setupPriceValidation() {
        etPrice.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                String input = s.toString();

                // Remove any non-digit and non-decimal point characters
                if (!input.matches("[0-9.]*")) {
                    etPrice.setError("Only numbers and decimal point allowed");
                }
            }
        });
    }

    private boolean isValidPrice(String price) {
        if (price.isEmpty()) {
            return false;
        }

        if (!price.matches("^\\d+\\.\\d+$")) {
            Toast.makeText(this, "Price must be in format: 100.00 or 100.50", Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    private String formatPrice(String price) {
        // If price doesn't have decimal, add .00
        if (!price.contains(".")) {
            return price + ".00";
        }

        // If price has decimal but only one digit after, add 0
        String[] parts = price.split("\\.");
        if (parts.length == 2 && parts[1].length() == 1) {
            return price + "0";
        }

        return price;
    }

    private void openImageChooser() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(Intent.createChooser(intent, "Select Dish Image"), PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            imageUri = data.getData();
            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
                ivDishImage.setImageBitmap(bitmap);
                Log.d(TAG, "Image selected successfully: " + imageUri.toString());
            } catch (IOException e) {
                Log.e(TAG, "Failed to load image", e);
                Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void uploadImageToSupabase() {
        String dishName = etDishName.getText().toString().trim();
        String price = etPrice.getText().toString().trim();

        if (imageUri == null || dishName.isEmpty()) {
            Toast.makeText(this, "Select an image and enter a dish name", Toast.LENGTH_SHORT).show();
            return;
        }

        // Validate price format
        if (!isValidPrice(price)) {
            etPrice.setError("Price must include decimal (e.g., ₱100.00)");
            etPrice.requestFocus();
            return;
        }

        try {
            String uniqueId = UUID.randomUUID().toString().substring(0, 8);
            String fileName = "menu/" + dishName.replaceAll("[^a-zA-Z0-9]", "_") + "_" + uniqueId + ".jpg";

            String uploadUrl = SupabaseConfig.PROJECT_URL + "/storage/v1/object/" + SupabaseConfig.BUCKET_NAME + "/" + fileName;

            Log.d(TAG, "Upload URL: " + uploadUrl);
            Log.d(TAG, "File name: " + fileName);

            File tempFile = createTempFileFromUri(imageUri);
            if (tempFile == null) {
                Toast.makeText(this, "Failed to create temp file", Toast.LENGTH_SHORT).show();
                return;
            }

            RequestBody requestBody = RequestBody.create(tempFile, MediaType.parse("image/jpeg"));

            Request request = new Request.Builder()
                    .url(uploadUrl)
                    .post(requestBody)
                    .addHeader("apikey", SupabaseConfig.SERVICE_ROLE_KEY)
                    .addHeader("Authorization", "Bearer " + SupabaseConfig.SERVICE_ROLE_KEY)
                    .addHeader("Content-Type", "image/jpeg")
                    .build();

            Log.d(TAG, "Making request to upload image...");

            httpClient.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e(TAG, "Upload request failed", e);
                    runOnUiThread(() -> Toast.makeText(ManageMenu.this,
                            "Image upload failed: " + e.getMessage(), Toast.LENGTH_LONG).show());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String responseBody = response.body() != null ? response.body().string() : "No response body";
                    Log.d(TAG, "Upload response code: " + response.code());
                    Log.d(TAG, "Upload response: " + responseBody);

                    if (response.isSuccessful()) {
                        String publicUrl = SupabaseConfig.PROJECT_URL + "/storage/v1/object/public/" +
                                SupabaseConfig.BUCKET_NAME + "/" + fileName;
                        Log.d(TAG, "Upload successful. Public URL: " + publicUrl);
                        runOnUiThread(() -> addDishToFirestore(publicUrl));
                    } else {
                        Log.e(TAG, "Upload failed with code: " + response.code() + ", message: " + response.message());
                        runOnUiThread(() -> Toast.makeText(ManageMenu.this,
                                "Upload failed: " + response.code() + " - " + responseBody, Toast.LENGTH_LONG).show());
                    }
                }
            });

        } catch (Exception e) {
            Log.e(TAG, "Error in uploadImageToSupabase", e);
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private File createTempFileFromUri(Uri uri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(uri);
            if (inputStream == null) {
                Log.e(TAG, "Could not open input stream for URI: " + uri);
                return null;
            }

            File tempFile = File.createTempFile("upload_", ".jpg", getCacheDir());
            FileOutputStream outputStream = new FileOutputStream(tempFile);

            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }

            outputStream.flush();
            outputStream.close();
            inputStream.close();

            Log.d(TAG, "Temp file created: " + tempFile.getAbsolutePath() + ", size: " + tempFile.length());
            return tempFile;

        } catch (IOException e) {
            Log.e(TAG, "Error creating temp file", e);
            return null;
        }
    }

    private void addDishToFirestore(String imageUrl) {
        String dishName = etDishName.getText().toString().trim();
        String description = etDishDescription.getText().toString().trim();
        String price = etPrice.getText().toString().trim();
        boolean isAddOn = cbIsAddOn.isChecked();
        String category = isAddOn ? "" : spnCategory.getSelectedItem().toString();

        if (description.isEmpty() || price.isEmpty()) {
            Toast.makeText(this, "Please complete all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        price = formatPrice(price);

        Map<String, Object> dish = new HashMap<>();
        dish.put("dishName", dishName);
        dish.put("description", description);
        dish.put("price", price);
        dish.put("imageUrl", imageUrl);

        if (isAddOn) {
            dish.put("specialties", "addons");
        } else {
            dish.put("category", category);
        }

        Log.d(TAG, "Adding dish to Firestore: " + dishName + ", isAddOn: " + isAddOn);

        db.collection("menuItems").document(dishName).set(dish)
                .addOnSuccessListener(unused -> {
                    Log.d(TAG, "Dish added successfully to Firestore");
                    String message = isAddOn ? "Add-on added successfully!" : "Dish added successfully!";
                    Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
                    clearFields();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to add dish to Firestore", e);
                    Toast.makeText(this, "Failed to add dish: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void clearFields() {
        etDishName.setText("");
        etDishDescription.setText("");
        etPrice.setText("");
        spnCategory.setSelection(0);
        cbIsAddOn.setChecked(false);
        ivDishImage.setImageResource(R.drawable.mistlogo);
        imageUri = null;

        tilCategory.setEnabled(true);
        spnCategory.setEnabled(true);
        tilCategory.setAlpha(1.0f);
    }
}