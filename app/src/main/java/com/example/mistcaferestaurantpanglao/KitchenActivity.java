package com.example.mistcaferestaurantpanglao;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class KitchenActivity extends AppCompatActivity {

    private static final String TAG = "KitchenActivity";
    private static final int TIME_UPDATE_INTERVAL = 1000;

    private ViewPager2 viewPager;
    private TabLayout tabLayout;
    private TextView tvCurrentTime;
    private TextView tvStandbyCount;
    private TextView tvCookingCount;
    private TextView tvReadyCount;
    private FloatingActionButton fabLogout;

    private FirebaseFirestore db;
    private List<KitchenOrder> allOrders;
    private KitchenPagerAdapter pagerAdapter;
    private ListenerRegistration ordersListener;

    private Handler timeHandler;
    private Runnable timeRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_kitchen);

        db = FirebaseFirestore.getInstance();
        allOrders = new ArrayList<>();

        initViews();
        setupViewPager();
        setupTimeUpdater();
        setupClickListeners();
        loadOrdersFromDatabase();
    }

    private void initViews() {
        viewPager = findViewById(R.id.viewpager_kitchen);
        tabLayout = findViewById(R.id.tab_layout);
        tvCurrentTime = findViewById(R.id.tv_current_time);
        tvStandbyCount = findViewById(R.id.tv_standby_count);
        tvCookingCount = findViewById(R.id.tv_cooking_count);
        tvReadyCount = findViewById(R.id.tv_ready_count);
        fabLogout = findViewById(R.id.fab_logout);
    }

    /**
     * NEW METHOD: Setup click listeners including logout button
     */
    private void setupClickListeners() {
        if (fabLogout != null) {
            fabLogout.setOnClickListener(v -> showLogoutConfirmationDialog());
        }
    }

    /**
     * NEW METHOD: Show logout confirmation dialog
     */
    private void showLogoutConfirmationDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Logout", (dialog, which) -> performLogout())
                .setNegativeButton("Cancel", null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    /**
     * NEW METHOD: Perform logout and return to MainActivity
     */
    private void performLogout() {
        Log.d(TAG, "User logged out from Kitchen");
        Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show();

        // Navigate to MainActivity (login page)
        Intent intent = new Intent(KitchenActivity.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    /**
     * OVERRIDE: Disable back button to prevent returning to MainActivity
     * Users must use the logout button to exit
     */
    @SuppressLint("MissingSuperCall")
    @Override
    public void onBackPressed() {
        new AlertDialog.Builder(this)
                .setTitle("Exit Kitchen")
                .setMessage("Do you want to logout and return to the login screen?")
                .setPositiveButton("Logout", (dialog, which) -> performLogout())
                .setNegativeButton("Stay", null)
                .show();
    }

    private void setupViewPager() {
        if (viewPager != null && tabLayout != null) {
            pagerAdapter = new KitchenPagerAdapter(this);
            viewPager.setAdapter(pagerAdapter);

            viewPager.setOffscreenPageLimit(2);

            new TabLayoutMediator(tabLayout, viewPager,
                    (tab, position) -> {
                        switch (position) {
                            case 0:
                                tab.setText("On Standby");
                                break;
                            case 1:
                                tab.setText("Cooking");
                                break;
                            case 2:
                                tab.setText("Send to Counter");
                                break;
                        }
                    }).attach();
        }
    }

    /**
     * NEW METHOD: Setup time updater that runs every second
     */
    private void setupTimeUpdater() {
        timeHandler = new Handler(Looper.getMainLooper());
        timeRunnable = new Runnable() {
            @Override
            public void run() {
                updateCurrentTime();
                timeHandler.postDelayed(this, TIME_UPDATE_INTERVAL);
            }
        };
        timeHandler.post(timeRunnable);
    }

    private void loadOrdersFromDatabase() {
        ordersListener = db.collection("orders")
                .whereIn("status", List.of("in-progress", "cooking", "ready"))
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.w(TAG, "Listen failed.", error);
                        return;
                    }

                    if (value != null) {
                        allOrders.clear();
                        for (QueryDocumentSnapshot doc : value) {
                            try {
                                Order order = Order.fromFirestore(doc);
                                KitchenOrder kitchenOrder = KitchenOrder.fromOrder(order);
                                allOrders.add(kitchenOrder);
                            } catch (Exception e) {
                                Log.e(TAG, "Error parsing order: " + doc.getId(), e);
                            }
                        }

                        Log.d(TAG, "Loaded " + allOrders.size() + " orders from database");
                        updateOrderCounts();
                        notifyFragmentsDataChanged();
                    }
                });
    }

    private void updateCurrentTime() {
        if (tvCurrentTime != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("h:mm a", Locale.getDefault());
            String currentTime = sdf.format(new Date());
            tvCurrentTime.setText(currentTime);
        }
    }

    private void updateOrderCounts() {
        int standbyCount = 0;
        int cookingCount = 0;
        int readyCount = 0;

        for (KitchenOrder order : allOrders) {
            switch (order.getStatus()) {
                case "in-progress":
                    standbyCount++;
                    break;
                case "cooking":
                    cookingCount++;
                    break;
                case "ready":
                    readyCount++;
                    break;
            }
        }

        if (tvStandbyCount != null) tvStandbyCount.setText(String.valueOf(standbyCount));
        if (tvCookingCount != null) tvCookingCount.setText(String.valueOf(cookingCount));
        if (tvReadyCount != null) tvReadyCount.setText(String.valueOf(readyCount));
    }

    private void notifyFragmentsDataChanged() {
        if (pagerAdapter != null) {
            // Notify all fragments that data has changed
            for (int i = 0; i < pagerAdapter.getItemCount(); i++) {
                Fragment fragment = getSupportFragmentManager().findFragmentByTag("f" + i);
                if (fragment instanceof StandbyOrdersFragment) {
                    ((StandbyOrdersFragment) fragment).updateOrders(getOrdersByStatus("in-progress"));
                } else if (fragment instanceof CookingOrdersFragment) {
                    ((CookingOrdersFragment) fragment).updateOrders(getOrdersByStatus("cooking"));
                } else if (fragment instanceof ReadyOrdersFragment) {
                    ((ReadyOrdersFragment) fragment).updateOrders(getOrdersByStatus("ready"));
                }
            }
        }
    }

    public List<KitchenOrder> getOrdersByStatus(String status) {
        List<KitchenOrder> filteredOrders = new ArrayList<>();
        for (KitchenOrder order : allOrders) {
            if (order.getStatus().equals(status)) {
                filteredOrders.add(order);
            }
        }
        return filteredOrders;
    }

    public void updateOrderStatus(String orderId, String newStatus) {
        db.collection("orders").document(orderId)
                .update("status", newStatus)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Order status updated successfully");
                    // The snapshot listener will automatically update the UI
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Error updating order status", e);
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Clean up time handler
        if (timeHandler != null && timeRunnable != null) {
            timeHandler.removeCallbacks(timeRunnable);
        }

        // Remove Firestore listener
        if (ordersListener != null) {
            ordersListener.remove();
            Log.d(TAG, "Firestore listener removed");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "Activity resumed");
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "Activity paused");
    }

    private static class KitchenPagerAdapter extends FragmentStateAdapter {

        public KitchenPagerAdapter(FragmentActivity fragmentActivity) {
            super(fragmentActivity);
        }

        @Override
        public Fragment createFragment(int position) {
            switch (position) {
                case 0:
                    return StandbyOrdersFragment.newInstance();
                case 1:
                    return CookingOrdersFragment.newInstance();
                case 2:
                    return ReadyOrdersFragment.newInstance();
                default:
                    return StandbyOrdersFragment.newInstance();
            }
        }

        @Override
        public int getItemCount() {
            return 3;
        }
    }
}