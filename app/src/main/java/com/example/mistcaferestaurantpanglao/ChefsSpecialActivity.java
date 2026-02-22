package com.example.mistcaferestaurantpanglao;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

public class ChefsSpecialActivity extends AppCompatActivity {
    private static final String TAG = "ChefsSpecialActivity";

    private RecyclerView recyclerView;
    private MenuItemAdapter adapter;
    private List<MenuItem> chefsSpecialItems;
    private TextView tvEmptyState;
    private MaterialToolbar toolbar;
    private FloatingActionButton fabCart;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chefs_special);

        initializeViews();
        setupToolbar();
        loadChefsSpecialItems();
        setupRecyclerView();
        setupClickListeners();
    }

    private void initializeViews() {
        toolbar = findViewById(R.id.toolbar);
        recyclerView = findViewById(R.id.recyclerViewChefsSpecial);
        fabCart = findViewById(R.id.fabCart);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Chef's Special");
        }

        toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }

    private void loadChefsSpecialItems() {
        chefsSpecialItems = getIntent().getParcelableArrayListExtra("chef_special_items");

        if (chefsSpecialItems == null) {
            chefsSpecialItems = new ArrayList<>();
        }

        updateEmptyState();
    }

    private void setupRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new MenuItemAdapter(this, chefsSpecialItems);
        recyclerView.setAdapter(adapter);
    }

    private void setupClickListeners() {
        fabCart.setOnClickListener(v -> {
            Intent intent = new Intent(this, ShoppingCartActivity.class);
            startActivity(intent);
        });
    }

    private void updateEmptyState() {
        if (chefsSpecialItems.isEmpty()) {
            tvEmptyState.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            tvEmptyState.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}