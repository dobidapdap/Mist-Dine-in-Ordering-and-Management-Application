package com.example.mistcaferestaurantpanglao;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class OrderHistory extends AppCompatActivity {
    private static final String TAG = "OrderHistoryActivity";

    private RecyclerView recyclerOrderHistory;
    private LinearLayout emptyStateContainer;
    private LinearLayout loadingContainer;
    private TextView tvEmptyHistory;
    private ImageView btnBack;
    private ImageView btnFilter;
    private OrderHistoryAdapter orderHistoryAdapter;
    private List<Order> orderHistoryList;
    private List<Order> filteredOrderList;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_history);

        initializeViews();
        setupRecyclerView();
        setupClickListeners();
        loadOrderHistory();
    }

    private void initializeViews() {
        recyclerOrderHistory = findViewById(R.id.recycler_order_history);
        emptyStateContainer = findViewById(R.id.empty_state_container);
        loadingContainer = findViewById(R.id.loading_container);
        tvEmptyHistory = findViewById(R.id.tv_empty_history);
        btnBack = findViewById(R.id.btn_back);
        btnFilter = findViewById(R.id.btn_filter);

        db = FirebaseFirestore.getInstance();
        orderHistoryList = new ArrayList<>();
        filteredOrderList = new ArrayList<>();
    }

    private void setupRecyclerView() {
        orderHistoryAdapter = new OrderHistoryAdapter(filteredOrderList);
        recyclerOrderHistory.setLayoutManager(new LinearLayoutManager(this));
        recyclerOrderHistory.setAdapter(orderHistoryAdapter);
    }

    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> onBackPressed());
        btnFilter.setOnClickListener(v -> showFilterOptions());
    }

    private void showFilterOptions() {
        FilterDialogFragment filterDialog = FilterDialogFragment.newInstance(filterOptions -> {
            applyFilters(filterOptions);
        });
        filterDialog.show(getSupportFragmentManager(), "FilterDialog");
    }

    private void applyFilters(FilterDialogFragment.FilterOptions filterOptions) {
        filteredOrderList.clear();

        // Apply price filter
        for (Order order : orderHistoryList) {
            if (order.getTotalAmount() >= filterOptions.getMinAmount() &&
                    order.getTotalAmount() <= filterOptions.getMaxAmount()) {
                filteredOrderList.add(order);
            }
        }

        // Apply sorting
        switch (filterOptions.getSortBy()) {
            case "newest":
                filteredOrderList.sort((o1, o2) -> Long.compare(o2.getTimestamp(), o1.getTimestamp()));
                break;
            case "oldest":
                filteredOrderList.sort((o1, o2) -> Long.compare(o1.getTimestamp(), o2.getTimestamp()));
                break;
            case "highest_price":
                filteredOrderList.sort((o1, o2) -> Double.compare(o2.getTotalAmount(), o1.getTotalAmount()));
                break;
            case "lowest_price":
                filteredOrderList.sort((o1, o2) -> Double.compare(o1.getTotalAmount(), o2.getTotalAmount()));
                break;
        }

        if (filteredOrderList.isEmpty()) {
            showEmptyState();
        } else {
            recyclerOrderHistory.setVisibility(View.VISIBLE);
            emptyStateContainer.setVisibility(View.GONE);
            loadingContainer.setVisibility(View.GONE);
            orderHistoryAdapter.notifyDataSetChanged();
        }

        Toast.makeText(this, "Filters applied", Toast.LENGTH_SHORT).show();
    }

    private void loadOrderHistory() {
        showLoadingState();
        Log.d(TAG, "Loading order history from archivedOrders...");

        db.collection("archivedOrders")
                .whereEqualTo("status", "paid")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        orderHistoryList.clear();

                        for (QueryDocumentSnapshot document : task.getResult()) {
                            try {
                                Order order = Order.fromFirestore(document);
                                // Double check status is paid
                                if ("paid".equalsIgnoreCase(order.getStatus())) {
                                    orderHistoryList.add(order);
                                    Log.d(TAG, "Added paid order: " + order.getOrderId());
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Error parsing order document: " + document.getId(), e);
                            }
                        }

                        updateUI();

                    } else {
                        Log.e(TAG, "Error getting order history", task.getException());
                        Toast.makeText(OrderHistory.this, "Failed to load order history", Toast.LENGTH_SHORT).show();
                        showEmptyState();
                    }
                });
    }

    private void updateUI() {
        filteredOrderList.clear();
        filteredOrderList.addAll(orderHistoryList);

        if (filteredOrderList.isEmpty()) {
            showEmptyState();
        } else {
            recyclerOrderHistory.setVisibility(View.VISIBLE);
            emptyStateContainer.setVisibility(View.GONE);
            loadingContainer.setVisibility(View.GONE);
            orderHistoryAdapter.notifyDataSetChanged();
            Log.d(TAG, "Updated UI with " + filteredOrderList.size() + " orders");
        }
    }

    private void showEmptyState() {
        recyclerOrderHistory.setVisibility(View.GONE);
        emptyStateContainer.setVisibility(View.VISIBLE);
        loadingContainer.setVisibility(View.GONE);
    }

    private void showLoadingState() {
        recyclerOrderHistory.setVisibility(View.GONE);
        emptyStateContainer.setVisibility(View.GONE);
        loadingContainer.setVisibility(View.VISIBLE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh the order history when returning to this activity
        loadOrderHistory();
    }
}