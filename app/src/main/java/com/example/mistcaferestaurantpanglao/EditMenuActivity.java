package com.example.mistcaferestaurantpanglao;

import android.app.Dialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import java.io.IOException;

public class EditMenuActivity extends AppCompatActivity implements MenuItemAdapter.OnMenuItemClickListener {

    private static final String TAG = "EditMenuActivity";
    private static final String SUPABASE_URL = "https://flxifyyxvhnriljkdmsm.supabase.co";
    private static final String SUPABASE_ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImZseGlmeXl4dmhucmlsamtkbXNtIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NDk1MzgwNzYsImV4cCI6MjA2NTExNDA3Nn0.6EtXnKd78_NbbRHitWFuYzGZtSDWxCkWChwnBZzUsY4";
    private static final String BUCKET_NAME = "images";

    private Toolbar toolbar;
    private EditText etSearch;
    private ChipGroup chipGroup;
    private RecyclerView rvMenuItems;
    private Chip chipAll, chipPasta, chipMainCourse, chipSoup, chipSalad;
    private Chip chipPyroSeries, chipPlatter, chipSnack, chipDrinks, chipDessert;

    private List<MenuItem> allMenuItems;
    private List<MenuItem> filteredMenuItems;
    private MenuItemAdapter adapter;
    private String currentFilter = "All Items";
    private FirebaseFirestore db;
    private OkHttpClient httpClient;

    private Uri selectedImageUri;
    private MenuItem currentEditingItem;
    private Dialog currentDialog;
    private ImageView dialogImageView;
    private ActivityResultLauncher<Intent> imagePickerLauncher;

    private final String[] categories = {
            "Pasta", "Main Course", "Soup", "Salad", "Pyro Series",
            "Platter", "Snack", "Drinks", "Dessert"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_edit_menu);

        db = FirebaseFirestore.getInstance();
        httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();

        setupImagePicker();
        initViews();
        setupToolbar();
        setupRecyclerView();
        setupSearchFunctionality();
        setupFilterChips();
        loadMenuItemsFromFirestore();
    }

    private void setupImagePicker() {
        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        selectedImageUri = result.getData().getData();
                        if (selectedImageUri != null && dialogImageView != null) {
                            Glide.with(this)
                                    .load(selectedImageUri)
                                    .centerCrop()
                                    .into(dialogImageView);
                        }
                    }
                }
        );
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        etSearch = findViewById(R.id.etSearch);
        chipGroup = findViewById(R.id.chipGroup);
        rvMenuItems = findViewById(R.id.rvMenuItems);
        chipAll = findViewById(R.id.chipAll);
        chipPasta = findViewById(R.id.chipPasta);
        chipMainCourse = findViewById(R.id.chipMainCourse);
        chipSoup = findViewById(R.id.chipSoup);
        chipSalad = findViewById(R.id.chipSalad);
        chipPyroSeries = findViewById(R.id.chipPyroSeries);
        chipPlatter = findViewById(R.id.chipPlatter);
        chipSnack = findViewById(R.id.chipSnack);
        chipDrinks = findViewById(R.id.chipDrinks);
        chipDessert = findViewById(R.id.chipDessert);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setTitle("Edit Menu");
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }

    private void setupRecyclerView() {
        allMenuItems = new ArrayList<>();
        filteredMenuItems = new ArrayList<>();
        adapter = new MenuItemAdapter(filteredMenuItems, this);
        rvMenuItems.setLayoutManager(new LinearLayoutManager(this));
        rvMenuItems.setAdapter(adapter);
    }

    private void setupSearchFunctionality() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterMenuItems(s.toString(), currentFilter);
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void setupFilterChips() {
        chipAll.setChecked(true);
        chipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) {
                chipAll.setChecked(true);
                currentFilter = "All Items";
            } else {
                int checkedId = checkedIds.get(0);
                Chip selectedChip = findViewById(checkedId);
                currentFilter = selectedChip.getText().toString();
            }
            filterMenuItems(etSearch.getText().toString(), currentFilter);
        });
    }

    private void loadMenuItemsFromFirestore() {
        db.collection("menuItems").get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                allMenuItems.clear();
                for (QueryDocumentSnapshot document : task.getResult()) {
                    try {
                        String id = document.getId();
                        String dishName = document.getString("dishName");
                        String name = document.getString("name");
                        String description = document.getString("description");

                        String price = PriceUtils.getPriceFromDocument(document, "price");

                        String category = document.getString("category");
                        String imageUrl = document.getString("imageUrl");
                        String specialties = document.getString("specialties");
                        Boolean available = document.getBoolean("available");
                        boolean isAvailable = available != null ? available : true;

                        MenuItem menuItem = new MenuItem(id, name != null ? name : dishName,
                                dishName, description, price, category, imageUrl, isAvailable, specialties);
                        allMenuItems.add(menuItem);
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing menu item from document: " + document.getId(), e);
                    }
                }
                filteredMenuItems.clear();
                filteredMenuItems.addAll(allMenuItems);
                adapter.notifyDataSetChanged();
            } else {
                Log.e(TAG, "Error getting menu items from Firestore", task.getException());
                Toast.makeText(this, "Failed to load menu items", Toast.LENGTH_LONG).show();
            }
        });
    }

    private void filterMenuItems(String searchQuery, String category) {
        filteredMenuItems.clear();
        for (MenuItem item : allMenuItems) {
            boolean matchesSearch = searchQuery.isEmpty() ||
                    (item.getName() != null && item.getName().toLowerCase().contains(searchQuery.toLowerCase())) ||
                    (item.getDishName() != null && item.getDishName().toLowerCase().contains(searchQuery.toLowerCase())) ||
                    (item.getDescription() != null && item.getDescription().toLowerCase().contains(searchQuery.toLowerCase()));
            boolean matchesCategory = category.equals("All Items") ||
                    (item.getCategory() != null && item.getCategory().equals(category));
            if (matchesSearch && matchesCategory) {
                filteredMenuItems.add(item);
            }
        }
        adapter.notifyDataSetChanged();
    }

    @Override
    public void onEditClick(MenuItem item) {
        showEditDialog(item);
    }

    private void showEditDialog(MenuItem item) {
        currentDialog = new Dialog(this);
        currentDialog.setContentView(R.layout.dialog_edit_menu_item);
        currentDialog.getWindow().setLayout(
                getResources().getDisplayMetrics().widthPixels - 64,
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT
        );

        currentEditingItem = item;
        selectedImageUri = null;

        dialogImageView = currentDialog.findViewById(R.id.ivDishImage);
        MaterialButton btnChangeImage = currentDialog.findViewById(R.id.btnChangeImage);
        TextInputEditText etDishName = currentDialog.findViewById(R.id.etDishName);
        TextInputEditText etDescription = currentDialog.findViewById(R.id.etDescription);
        TextInputEditText etPrice = currentDialog.findViewById(R.id.etPrice);
        AutoCompleteTextView etCategory = currentDialog.findViewById(R.id.etCategory);
        TextInputEditText etDishesLeft = currentDialog.findViewById(R.id.etDishesLeft);
        MaterialCheckBox cbRecommended = currentDialog.findViewById(R.id.cbRecommended);
        MaterialCheckBox cbMistDaySpecial = currentDialog.findViewById(R.id.cbMistDaySpecial);
        MaterialCheckBox cbBestSellers = currentDialog.findViewById(R.id.cbBestSellers);
        MaterialCheckBox cbAddOns = currentDialog.findViewById(R.id.cbAddOns);
        MaterialSwitch switchAvailable = currentDialog.findViewById(R.id.switchAvailable);
        MaterialButton btnCancel = currentDialog.findViewById(R.id.btnCancel);
        MaterialButton btnSave = currentDialog.findViewById(R.id.btnSave);

        if (item.getImageUrl() != null && !item.getImageUrl().isEmpty()) {
            Glide.with(this)
                    .load(item.getImageUrl())
                    .centerCrop()
                    .placeholder(R.drawable.mistlogo)
                    .into(dialogImageView);
        } else {
            dialogImageView.setImageResource(R.drawable.mistlogo);
        }

        btnChangeImage.setOnClickListener(v -> openImagePicker());

        ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, categories);
        etCategory.setAdapter(categoryAdapter);

        etDishName.setText(item.getDishName() != null ? item.getDishName() : item.getName());
        etDescription.setText(item.getDescription());
        etPrice.setText(PriceUtils.removeCurrencySymbol(item.getPrice()));
        etCategory.setText(item.getCategory(), false);

        // Load dishes left from Firestore
        db.collection("menuItems").document(item.getId()).get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                Long dishesLeft = documentSnapshot.getLong("dishesLeft");
                if (dishesLeft != null && dishesLeft > 0) {
                    etDishesLeft.setText(String.valueOf(dishesLeft));
                }
            }
        });

        String specialties = item.getSpecialties();
        if (specialties != null) {
            cbRecommended.setChecked(specialties.contains("recommended"));
            cbMistDaySpecial.setChecked(specialties.contains("mistday"));
            cbBestSellers.setChecked(specialties.contains("bestseller"));
            cbAddOns.setChecked(specialties.contains("addons"));
        }

        switchAvailable.setChecked(item.isAvailable());

        // Add listener to dishes left field to auto-update availability
        etDishesLeft.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String dishesLeftStr = s.toString().trim();
                if (!dishesLeftStr.isEmpty()) {
                    try {
                        int dishesLeftCount = Integer.parseInt(dishesLeftStr);
                        if (dishesLeftCount == 0) {
                            switchAvailable.setChecked(false);
                        }
                    } catch (NumberFormatException e) {
                        // Invalid number, ignore
                    }
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        btnCancel.setOnClickListener(v -> currentDialog.dismiss());

        btnSave.setOnClickListener(v -> {
            String dishName = etDishName.getText().toString().trim();
            String description = etDescription.getText().toString().trim();
            String priceInput = etPrice.getText().toString().trim();
            String price = PriceUtils.ensurePesoSign(priceInput);
            String category = etCategory.getText().toString().trim();
            String dishesLeftStr = etDishesLeft.getText().toString().trim();
            boolean isRecommended = cbRecommended.isChecked();
            boolean isMistDaySpecial = cbMistDaySpecial.isChecked();
            boolean isBestSellers = cbBestSellers.isChecked();
            boolean isAddOns = cbAddOns.isChecked();
            boolean available = switchAvailable.isChecked();

            if (dishName.isEmpty() || description.isEmpty() || priceInput.isEmpty() || category.isEmpty()) {
                Toast.makeText(this, "Please fill in all required fields", Toast.LENGTH_SHORT).show();
                return;
            }

            // Parse dishes left
            Integer dishesLeft = null;
            if (!dishesLeftStr.isEmpty()) {
                try {
                    dishesLeft = Integer.parseInt(dishesLeftStr);
                    if (dishesLeft == 0) {
                        available = false; // Auto-set to unavailable when 0
                    }
                } catch (NumberFormatException e) {
                    Toast.makeText(this, "Invalid dishes left value", Toast.LENGTH_SHORT).show();
                    return;
                }
            }

            String newSpecialties = buildSpecialtiesString(isRecommended, isMistDaySpecial, isBestSellers, isAddOns);

            if (selectedImageUri != null) {
                uploadImageToSupabase(item, dishName, description, price, category, newSpecialties, available, dishesLeft);
            } else {
                updateMenuItem(item, dishName, description, price, category, newSpecialties, available, dishesLeft, currentDialog);
            }
        });

        currentDialog.show();
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        imagePickerLauncher.launch(intent);
    }
    private void uploadImageToSupabase(MenuItem item, String dishName, String description,
                                       String price, String category, String specialties, boolean available, Integer dishesLeft) {
        Toast.makeText(this, "Uploading image...", Toast.LENGTH_SHORT).show();

        try {
            String extension = getFileExtension(selectedImageUri);
            String fileName = item.getId() + extension;

            File imageFile = getFileFromUri(selectedImageUri);
            if (imageFile == null) {
                Toast.makeText(this, "Failed to read image file", Toast.LENGTH_SHORT).show();
                return;
            }

            deleteImageFromSupabase(item, new DeleteCallback() {
                @Override
                public void onImageDeleted(boolean success, String message) {
                    uploadNewImage(imageFile, fileName, item, dishName, description, price, category, specialties, available, dishesLeft);
                }
            });

        } catch (Exception e) {
            Log.e(TAG, "Error preparing image upload", e);
            Toast.makeText(this, "Failed to upload image", Toast.LENGTH_SHORT).show();
        }
    }

    private void uploadNewImage(File imageFile, String fileName, MenuItem item, String dishName,
                                String description, String price, String category, String specialties, boolean available, Integer dishesLeft) {
        RequestBody fileBody = RequestBody.create(imageFile, MediaType.parse("image/*"));
        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", fileName, fileBody)
                .build();

        String uploadUrl = SUPABASE_URL + "/storage/v1/object/" + BUCKET_NAME + "/" + fileName;

        Request request = new Request.Builder()
                .url(uploadUrl)
                .post(requestBody)
                .addHeader("Authorization", "Bearer " + SUPABASE_ANON_KEY)
                .addHeader("apikey", SUPABASE_ANON_KEY)
                .build();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    Log.e(TAG, "Image upload failed", e);
                    Toast.makeText(EditMenuActivity.this, "Failed to upload image", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseBody = response.body() != null ? response.body().string() : "";

                runOnUiThread(() -> {
                    if (response.isSuccessful()) {
                        String imageUrl = SUPABASE_URL + "/storage/v1/object/public/" + BUCKET_NAME + "/" + fileName;
                        updateMenuItem(item, dishName, description, price, category, specialties, available, dishesLeft, imageUrl, currentDialog);
                        Toast.makeText(EditMenuActivity.this, "Image uploaded successfully", Toast.LENGTH_SHORT).show();
                    } else {
                        Log.e(TAG, "Upload failed: " + response.code() + " - " + responseBody);
                        Toast.makeText(EditMenuActivity.this, "Failed to upload image: " + response.code(), Toast.LENGTH_SHORT).show();
                    }
                });
                response.close();
            }
        });
    }

    private File getFileFromUri(Uri uri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(uri);
            if (inputStream == null) return null;

            File tempFile = File.createTempFile("upload", ".jpg", getCacheDir());
            FileOutputStream outputStream = new FileOutputStream(tempFile);

            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }

            outputStream.close();
            inputStream.close();

            return tempFile;
        } catch (Exception e) {
            Log.e(TAG, "Error converting URI to file", e);
            return null;
        }
    }

    private String getFileExtension(Uri uri) {
        String mimeType = getContentResolver().getType(uri);
        if (mimeType != null) {
            if (mimeType.contains("jpeg") || mimeType.contains("jpg")) return ".jpg";
            if (mimeType.contains("png")) return ".png";
            if (mimeType.contains("webp")) return ".webp";
        }
        return ".jpg";
    }

    private String buildSpecialtiesString(boolean isRecommended, boolean isMistDaySpecial, boolean isBestSellers, boolean isAddOns) {
        List<String> specialtiesList = new ArrayList<>();

        if (isRecommended) {
            specialtiesList.add("recommended");
        }
        if (isMistDaySpecial) {
            specialtiesList.add("mistday");
        }
        if (isBestSellers) {
            specialtiesList.add("bestseller");
        }
        if (isAddOns) {
            specialtiesList.add("addons");
        }
        return String.join(", ", specialtiesList);
    }

    private void updateMenuItem(MenuItem item, String dishName, String description,
                                String price, String category, String specialties,
                                boolean available, Integer dishesLeft, Dialog dialog) {
        updateMenuItem(item, dishName, description, price, category, specialties, available, dishesLeft, null, dialog);
    }

    private void updateMenuItem(MenuItem item, String dishName, String description,
                                String price, String category, String specialties,
                                boolean available, Integer dishesLeft, String newImageUrl, Dialog dialog) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("dishName", dishName);
        updates.put("name", dishName);
        updates.put("description", description);
        updates.put("price", price);
        updates.put("category", category);
        updates.put("specialties", specialties);
        updates.put("available", available);

        // Add dishesLeft field
        if (dishesLeft != null) {
            updates.put("dishesLeft", dishesLeft);
        } else {
            updates.put("dishesLeft", null); // Remove field if empty
        }

        if (newImageUrl != null) {
            updates.put("imageUrl", newImageUrl);
        }

        db.collection("menuItems").document(item.getId()).update(updates)
                .addOnSuccessListener(aVoid -> {
                    item.setDishName(dishName);
                    item.setName(dishName);
                    item.setDescription(description);
                    item.setPrice(price);
                    item.setCategory(category);
                    item.setSpecialties(specialties);
                    item.setAvailable(available);

                    if (newImageUrl != null) {
                        item.setImageUrl(newImageUrl);
                    }

                    adapter.notifyDataSetChanged();
                    dialog.dismiss();
                    Toast.makeText(this, "Menu item updated successfully", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error updating menu item", e);
                    Toast.makeText(this, "Failed to update menu item", Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    public void onDeleteClick(MenuItem item) {
        showDeleteConfirmation(item);
    }

    @Override
    public void onAvailabilityToggle(MenuItem item, boolean isAvailable) {
        item.setAvailable(isAvailable);
        updateMenuItemAvailability(item, isAvailable);
    }

    private void updateMenuItemAvailability(MenuItem item, boolean isAvailable) {
        db.collection("menuItems").document(item.getId()).update("available", isAvailable)
                .addOnSuccessListener(aVoid -> {
                    String status = isAvailable ? "available" : "unavailable";
                    Toast.makeText(this, item.getName() + " is now " + status, Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to update availability", Toast.LENGTH_SHORT).show();
                    item.setAvailable(!isAvailable);
                    adapter.notifyDataSetChanged();
                });
    }

    private void showDeleteConfirmation(MenuItem item) {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Delete Menu Item")
                .setMessage("Are you sure you want to delete '" + item.getName() + "'?")
                .setPositiveButton("Delete", (dialog, which) -> deleteMenuItemAndImage(item))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteMenuItemAndImage(MenuItem item) {
        deleteImageFromSupabase(item, new DeleteCallback() {
            @Override
            public void onImageDeleted(boolean success, String message) {
                deleteFromFirestore(item);
            }
        });
    }

    private void deleteImageFromSupabase(MenuItem item, DeleteCallback callback) {
        String imageUrl = item.getImageUrl();
        if (imageUrl == null || imageUrl.isEmpty()) {
            callback.onImageDeleted(true, "No image to delete");
            return;
        }

        String filePath = extractFilePathFromUrl(imageUrl);
        if (filePath == null) {
            deleteImageByItemId(item, callback);
            return;
        }

        deleteImageWithUrl(filePath, callback, 0);
    }

    private void deleteImageWithUrl(String filePath, DeleteCallback callback, int urlFormatIndex) {
        String[] deleteUrlFormats = {
                SUPABASE_URL + "/storage/v1/object/" + BUCKET_NAME + "/" + filePath,
                SUPABASE_URL + "/storage/v1/object/authenticated/" + BUCKET_NAME + "/" + filePath
        };

        if (urlFormatIndex >= deleteUrlFormats.length) {
            callback.onImageDeleted(false, "Failed to delete image");
            return;
        }

        String deleteUrl = deleteUrlFormats[urlFormatIndex];
        Request request = new Request.Builder()
                .url(deleteUrl)
                .delete()
                .addHeader("Authorization", "Bearer " + SUPABASE_ANON_KEY)
                .addHeader("apikey", SUPABASE_ANON_KEY)
                .build();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> callback.onImageDeleted(false, "Network error"));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful() || response.code() == 404) {
                    runOnUiThread(() -> callback.onImageDeleted(true, "Image deleted"));
                } else {
                    runOnUiThread(() -> deleteImageWithUrl(filePath, callback, urlFormatIndex + 1));
                }
                response.close();
            }
        });
    }

    private void deleteImageByItemId(MenuItem item, DeleteCallback callback) {
        if (item.getId() == null || item.getId().isEmpty()) {
            callback.onImageDeleted(false, "No item ID available");
            return;
        }

        String[] extensions = {".jpg", ".jpeg", ".png", ".webp"};
        deleteImageWithExtension(item.getId(), extensions[0], callback, extensions, 0);
    }

    private void deleteImageWithExtension(String itemId, String extension, DeleteCallback callback,
                                          String[] extensions, int currentIndex) {
        String imagePath = itemId + extension;

        deleteImageWithUrl(imagePath, new DeleteCallback() {
            @Override
            public void onImageDeleted(boolean success, String message) {
                if (success) {
                    callback.onImageDeleted(true, message);
                } else {
                    if (currentIndex + 1 < extensions.length) {
                        deleteImageWithExtension(itemId, extensions[currentIndex + 1], callback, extensions, currentIndex + 1);
                    } else {
                        callback.onImageDeleted(true, "No image found to delete");
                    }
                }
            }
        }, 0);
    }

    private void deleteFromFirestore(MenuItem item) {
        db.collection("menuItems").document(item.getId()).delete()
                .addOnSuccessListener(aVoid -> {
                    allMenuItems.remove(item);
                    filteredMenuItems.remove(item);
                    adapter.notifyDataSetChanged();
                    Toast.makeText(this, "'" + item.getName() + "' deleted successfully", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to delete menu item", Toast.LENGTH_LONG).show();
                });
    }

    private String extractFilePathFromUrl(String imageUrl) {
        try {
            if (imageUrl.contains("/storage/v1/object/public/" + BUCKET_NAME + "/")) {
                String[] parts = imageUrl.split("/storage/v1/object/public/" + BUCKET_NAME + "/");
                if (parts.length > 1) {
                    String filePath = parts[1];
                    if (filePath.contains("?")) {
                        filePath = filePath.substring(0, filePath.indexOf("?"));
                    }
                    return filePath;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error extracting file path", e);
        }
        return null;
    }

    private interface DeleteCallback {
        void onImageDeleted(boolean success, String message);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadMenuItemsFromFirestore();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (httpClient != null) {
            httpClient.dispatcher().executorService().shutdown();
        }
    }
}