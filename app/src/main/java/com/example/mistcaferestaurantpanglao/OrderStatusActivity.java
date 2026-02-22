package com.example.mistcaferestaurantpanglao;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

public class OrderStatusActivity extends AppCompatActivity {

    private static final String TAG = "OrderStatusActivity";

    // UI Components
    private TextView tvTableNumber, tvCustomerName, tvOrderId;
    private RecyclerView rvStatusTimeline;
    private ProgressBar progressBar;
    private ImageView btnBack, btnRefresh;

    // Adapter with real-time capability
    private StatusTimelineAdapter timelineAdapter;

    // Firebase
    private FirebaseFirestore db;
    private ListenerRegistration orderListener;

    // Order data
    private String customerName;
    private String tableNumber;
    private String currentOrderId;
    private String currentDocumentId; // Firestore document ID for real-time updates

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_status);

        try {
            initializeViews();
            setupFirebase();

            if (getIntentData()) {
                setupRecyclerView();
                setupClickListeners();
                findAndListenToOrder();
            } else {
                showErrorAndFinish("Missing order information. Please try again.");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in onCreate", e);
            showErrorAndFinish("Failed to initialize order status screen");
        }
    }

    private void initializeViews() {
        try {
            tvTableNumber = findViewById(R.id.tv_table_number);
            tvCustomerName = findViewById(R.id.tv_customer_name);
            tvOrderId = findViewById(R.id.tv_order_id);
            rvStatusTimeline = findViewById(R.id.rv_status_timeline);
            progressBar = findViewById(R.id.progress_bar);
            btnBack = findViewById(R.id.btn_back);
            btnRefresh = findViewById(R.id.btn_refresh);

            if (tvTableNumber != null) tvTableNumber.setText("Loading...");
            if (tvCustomerName != null) tvCustomerName.setText("Loading...");
            if (tvOrderId != null) tvOrderId.setText("--");

            Log.d(TAG, "Views initialized successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error initializing views", e);
            throw e;
        }
    }

    private void setupFirebase() {
        try {
            db = FirebaseFirestore.getInstance();
            if (db == null) {
                throw new RuntimeException("Failed to initialize Firestore");
            }
            Log.d(TAG, "Firebase initialized successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error setting up Firebase", e);
            throw e;
        }
    }

    private boolean getIntentData() {
        try {
            if (getIntent() == null) {
                Log.e(TAG, "Intent is null");
                return false;
            }

            customerName = getIntent().getStringExtra("customerName");
            tableNumber = getIntent().getStringExtra("tableNumber");

            Log.d(TAG, "Intent data - Customer: " + customerName + ", Table: " + tableNumber);

            if (customerName == null || customerName.trim().isEmpty()) {
                Log.e(TAG, "Customer name is missing");
                return false;
            }

            if (tableNumber == null || tableNumber.trim().isEmpty()) {
                Log.e(TAG, "Table number is missing");
                return false;
            }

            // Update UI with customer info
            if (tvTableNumber != null) tvTableNumber.setText(tableNumber);
            if (tvCustomerName != null) tvCustomerName.setText(customerName);

            return true;

        } catch (Exception e) {
            Log.e(TAG, "Error processing intent data", e);
            return false;
        }
    }

    private void setupRecyclerView() {
        try {
            timelineAdapter = new StatusTimelineAdapter(this);
            if (rvStatusTimeline != null) {
                rvStatusTimeline.setLayoutManager(new LinearLayoutManager(this));
                rvStatusTimeline.setAdapter(timelineAdapter);
            }
            Log.d(TAG, "RecyclerView setup complete");
        } catch (Exception e) {
            Log.e(TAG, "Error setting up RecyclerView", e);
            throw e;
        }
    }

    private void setupClickListeners() {
        try {
            if (btnBack != null) {
                btnBack.setOnClickListener(v -> {
                    Log.d(TAG, "Back button clicked");
                    onBackPressed();
                });
            }

            if (btnRefresh != null) {
                btnRefresh.setOnClickListener(v -> {
                    Log.d(TAG, "Refresh button clicked");
                    Toast.makeText(this, "Real-time updates active - no refresh needed!",
                            Toast.LENGTH_SHORT).show();
                });
            }

            Log.d(TAG, "Click listeners setup complete");
        } catch (Exception e) {
            Log.e(TAG, "Error setting up click listeners", e);
        }
    }

    /**
     * Find the order document and start real-time listening
     */
    private void findAndListenToOrder() {
        if (customerName == null || customerName.trim().isEmpty() ||
                tableNumber == null || tableNumber.trim().isEmpty()) {
            Log.e(TAG, "Cannot find order: missing customer name or table number");
            showErrorAndFinish("Missing customer or table information");
            return;
        }

        try {
            showLoading(true);
            Log.d(TAG, "Finding order for Customer: " + customerName + ", Table: " + tableNumber);

            // Stop any existing listener
            stopAllListeners();

            // Query for the most recent order
            Query orderQuery = db.collection("orders")
                    .whereEqualTo("customerName", customerName)
                    .whereEqualTo("tableNumber", tableNumber)
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .limit(1);

            orderQuery.get().addOnCompleteListener(task -> {
                showLoading(false);

                if (task.isSuccessful() && task.getResult() != null && !task.getResult().isEmpty()) {
                    DocumentSnapshot document = task.getResult().getDocuments().get(0);
                    startRealtimeUpdates(document);
                } else {
                    Log.d(TAG, "No order found with primary query, trying alternatives");
                    tryAlternativeQueries();
                }
            });

        } catch (Exception e) {
            Log.e(TAG, "Error finding order", e);
            showLoading(false);
            Toast.makeText(this, "Failed to find order: " + e.getMessage(),
                    Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Try alternative query patterns if primary query fails
     */
    private void tryAlternativeQueries() {
        Log.d(TAG, "Trying alternative query with 'table' field");

        Query alternativeQuery = db.collection("orders")
                .whereEqualTo("customerName", customerName)
                .whereEqualTo("table", tableNumber)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(1);

        alternativeQuery.get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null && !task.getResult().isEmpty()) {
                Log.d(TAG, "Found order with 'table' field");
                DocumentSnapshot document = task.getResult().getDocuments().get(0);
                startRealtimeUpdates(document);
            } else {
                tryCaseInsensitiveSearch();
            }
        });
    }

    /**
     * Try case-insensitive search as last resort
     */
    private void tryCaseInsensitiveSearch() {
        Log.d(TAG, "Trying case-insensitive search");

        db.collection("orders")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(50)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        for (DocumentSnapshot doc : task.getResult().getDocuments()) {
                            String docCustomerName = doc.getString("customerName");
                            String docTableNumber = doc.getString("tableNumber");
                            String docTable = doc.getString("table");

                            boolean customerMatch = docCustomerName != null &&
                                    docCustomerName.trim().equalsIgnoreCase(customerName.trim());
                            boolean tableMatch = (docTableNumber != null &&
                                    docTableNumber.trim().equalsIgnoreCase(tableNumber.trim())) ||
                                    (docTable != null &&
                                            docTable.trim().equalsIgnoreCase(tableNumber.trim()));

                            if (customerMatch && tableMatch) {
                                Log.d(TAG, "Found order with case-insensitive match: " + doc.getId());
                                startRealtimeUpdates(doc);
                                return;
                            }
                        }

                        Log.d(TAG, "No matching order found after all attempts");
                        showNoOrderFound();
                    } else {
                        Log.e(TAG, "Case-insensitive search failed");
                        showNoOrderFound();
                    }
                });
    }

    /**
     * Start real-time updates for the found order document
     */
    private void startRealtimeUpdates(DocumentSnapshot document) {
        try {
            currentDocumentId = document.getId();

            // Extract order ID for display
            currentOrderId = document.getString("orderId");
            if (currentOrderId == null || currentOrderId.trim().isEmpty()) {
                currentOrderId = document.getString("orderID");
            }
            if (currentOrderId == null || currentOrderId.trim().isEmpty()) {
                currentOrderId = document.getString("id");
            }
            if (currentOrderId == null || currentOrderId.trim().isEmpty()) {
                currentOrderId = currentDocumentId;
            }

            Log.d(TAG, "Starting real-time updates for document: " + currentDocumentId);
            Log.d(TAG, "Order ID: " + currentOrderId);

            // Update Order ID in UI
            updateOrderIdDisplay();

            // Start real-time status updates in adapter
            if (timelineAdapter != null) {
                timelineAdapter.startListening(currentDocumentId, "orders");
                Log.d(TAG, "✓ Real-time status updates activated!");
                Toast.makeText(this, "Real-time updates active", Toast.LENGTH_SHORT).show();
            }

        } catch (Exception e) {
            Log.e(TAG, "Error starting real-time updates", e);
            Toast.makeText(this, "Error starting real-time updates: " + e.getMessage(),
                    Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Update the Order ID display in the UI
     */
    private void updateOrderIdDisplay() {
        try {
            if (tvOrderId != null && currentOrderId != null) {
                if (!currentOrderId.equals("No Order Found")) {
                    // Truncate long order IDs
                    if (currentOrderId.length() > 20) {
                        tvOrderId.setText(currentOrderId.substring(0, 20) + "...");
                    } else {
                        tvOrderId.setText(currentOrderId);
                    }
                    tvOrderId.setTextColor(getResources().getColor(android.R.color.black));
                    Log.d(TAG, "Displaying Order ID: " + currentOrderId);
                } else {
                    tvOrderId.setText("No Order Found");
                    tvOrderId.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error updating Order ID display", e);
        }
    }

    /**
     * Show no order found state
     */
    private void showNoOrderFound() {
        Log.d(TAG, "No order found - showing error state");

        currentOrderId = "No Order Found";
        currentDocumentId = null;

        String message = "No order found for customer '" + customerName +
                "' at table '" + tableNumber + "'";
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();

        // Update UI
        updateOrderIdDisplay();

        // Show "no order" state in timeline
        if (timelineAdapter != null) {
            timelineAdapter.updateStatus("no_order", System.currentTimeMillis());
        }
    }

    /**
     * Show/hide loading indicator
     */
    private void showLoading(boolean show) {
        try {
            if (progressBar != null) {
                progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error toggling loading indicator", e);
        }
    }

    /**
     * Show error message and close activity
     */
    private void showErrorAndFinish(String message) {
        Log.e(TAG, "Error: " + message);
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        new android.os.Handler().postDelayed(this::finish, 2000);
    }

    /**
     * Stop all active Firestore listeners
     */
    private void stopAllListeners() {
        try {
            if (orderListener != null) {
                orderListener.remove();
                orderListener = null;
                Log.d(TAG, "Order listener removed");
            }

            if (timelineAdapter != null) {
                timelineAdapter.stopListening();
                Log.d(TAG, "Timeline adapter listener removed");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error stopping listeners", e);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            Log.d(TAG, "Activity destroying - cleaning up listeners");
            stopAllListeners();
        } catch (Exception e) {
            Log.e(TAG, "Error in onDestroy", e);
        }
    }

    @Override
    public void onBackPressed() {
        try {
            Log.d(TAG, "Back pressed");
            super.onBackPressed();
        } catch (Exception e) {
            Log.e(TAG, "Error in onBackPressed", e);
            finish();
        }
    }
}