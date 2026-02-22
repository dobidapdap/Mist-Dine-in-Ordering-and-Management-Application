package com.example.mistcaferestaurantpanglao;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class DessertActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private MenuItemAdapter adapter;
    private List<MenuItem> menuItemList;
    private FirebaseFirestore db;
    private ImageView floatingCartIcon;
    private ListenerRegistration menuItemsListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dessert);

        initViews();
        setupRecyclerView();
        setupFloatingCartIcon();
        loadMenuItems();
    }

    private void initViews() {
        recyclerView = findViewById(R.id.recyclerViewDessert);
        floatingCartIcon = findViewById(R.id.floatingCartIcon);
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        db = FirebaseFirestore.getInstance();
        menuItemList = new ArrayList<>();
    }

    private void setupRecyclerView() {
        adapter = new MenuItemAdapter(this, menuItemList);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }

    private void setupFloatingCartIcon() {
        floatingCartIcon.setOnClickListener(v -> {
            Intent intent = new Intent(DessertActivity.this, ShoppingCartActivity.class);
            startActivity(intent);
        });
    }

    private void loadMenuItems() {
        if (menuItemsListener != null) {
            menuItemsListener.remove();
        }

        menuItemsListener = db.collection("menuItems")
                .whereEqualTo("category", "Dessert")
                .addSnapshotListener((queryDocumentSnapshots, error) -> {
                    if (error != null) {
                        Log.e("DessertActivity", "Listen failed: " + error.getMessage(), error);
                        Toast.makeText(this, "Error loading desserts: " + error.getMessage(),
                                Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (queryDocumentSnapshots != null) {
                        menuItemList.clear();

                        for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                            Boolean availableField = document.getBoolean("available");
                            boolean isAvailable = availableField != null ? availableField : true;

                            MenuItem menuItem = new MenuItem(
                                    document.getId(),
                                    document.getString("dishName"),
                                    document.getString("dishName"),
                                    document.getString("description"),
                                    document.getString("price"),
                                    document.getString("category"),
                                    document.getString("imageUrl"),
                                    isAvailable,
                                    document.getString("specialties")
                            );

                            menuItemList.add(menuItem);

                            Log.d("DessertActivity", "Loaded item: " + menuItem.getDishName() +
                                    ", Available: " + menuItem.isAvailable());
                        }

                        adapter.notifyDataSetChanged();

                        if (menuItemList.isEmpty()) {
                            Toast.makeText(this, "No desserts found", Toast.LENGTH_SHORT).show();
                        } else {
                            Log.d("DessertActivity", "Loaded " + menuItemList.size() + " dessert items");
                        }
                    }
                });
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (menuItemsListener != null) {
            menuItemsListener.remove();
        }
    }
}