package com.example.mistcaferestaurantpanglao;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentManager;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import java.util.Collections;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class CounterActivity extends AppCompatActivity {

    private static final String TAG = "CounterActivity";
    private static final int TIME_UPDATE_INTERVAL = 1000;
    private static final int REQUEST_BLUETOOTH_PERMISSIONS = 100;

    private MaterialToolbar toolbar;
    private TextView tvStaffName, tvCurrentTime;
    private ImageButton btnRefresh;
    private ImageButton btnPrinter;
    private MaterialCardView cardNewOrders, cardInKitchen, cardServedOrders;
    private TextView tvNewOrdersCount, tvInKitchenCount, tvServedOrdersCount;
    private MaterialButton btnSendAllToKitchen, btnSalesReport;
    private TabLayout tabLayout;
    private ViewPager2 viewPager;
    private ExtendedFloatingActionButton fabEmergency;
    private FloatingActionButton fabLogout;
    private View loadingOverlay;
    private TextView tvLoadingMessage;

    private FirebaseFirestore db;
    private PaymentHandler paymentHandler;
    private List<Order> allOrders;
    private List<Order> newOrders, inKitchenOrders, servedOrders;
    private OrderPagerAdapter pagerAdapter;

    private ListenerRegistration ordersListener;

    private Handler timeHandler;
    private Runnable timeRunnable;

    private BluetoothPrinterManager printerManager;
    private ActivityResultLauncher<Intent> enableBluetoothLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_counter);

        configureFirestore();
        initializeVariables();
        initViews();
        setupToolbar();
        setupClickListeners();
        setupViewPager();
        setupTimeUpdater();
        initializePrinter();
        startRealtimeOrdersListener();
    }

    private void configureFirestore() {
        FirebaseFirestore firebaseDb = FirebaseFirestore.getInstance();
        FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(false)
                .build();

        try {
            firebaseDb.setFirestoreSettings(settings);
            Log.d(TAG, "✓ Firestore configured for real-time sync");
        } catch (IllegalStateException e) {
            Log.d(TAG, "Firestore settings already configured");
        }
    }

    private void initializeVariables() {
        db = FirebaseFirestore.getInstance();
        paymentHandler = new PaymentHandler();
        allOrders = new ArrayList<>();
        newOrders = new ArrayList<>();
        inKitchenOrders = new ArrayList<>();
        servedOrders = new ArrayList<>();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        tvStaffName = findViewById(R.id.tv_staff_name);
        tvCurrentTime = findViewById(R.id.tv_current_time);
        btnRefresh = findViewById(R.id.btn_refresh);
        btnPrinter = findViewById(R.id.btn_printer);

        cardNewOrders = findViewById(R.id.card_new_orders);
        cardInKitchen = findViewById(R.id.card_in_kitchen);
        cardServedOrders = findViewById(R.id.card_ready);

        tvNewOrdersCount = findViewById(R.id.tv_new_orders_count);
        tvInKitchenCount = findViewById(R.id.tv_in_kitchen_count);
        tvServedOrdersCount = findViewById(R.id.tv_ready_count);

        btnSendAllToKitchen = findViewById(R.id.btn_send_all_to_kitchen);
        btnSalesReport = findViewById(R.id.btn_sales_report);

        tabLayout = findViewById(R.id.tab_layout);
        viewPager = findViewById(R.id.view_pager);

        fabEmergency = findViewById(R.id.fab_emergency);
        fabLogout = findViewById(R.id.fab_logout);
        loadingOverlay = findViewById(R.id.loading_overlay);
        tvLoadingMessage = findViewById(R.id.tv_loading_message);

        tvStaffName.setText("Staff: Counter Staff");
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
    }

    private void setupClickListeners() {
        btnRefresh.setOnClickListener(v ->
                Toast.makeText(this, "Orders update in real-time", Toast.LENGTH_SHORT).show()
        );

        btnPrinter.setOnClickListener(v -> showPrinterSelectionDialog());

        cardNewOrders.setOnClickListener(v -> viewPager.setCurrentItem(0, true));
        cardInKitchen.setOnClickListener(v -> viewPager.setCurrentItem(1, true));
        cardServedOrders.setOnClickListener(v -> viewPager.setCurrentItem(2, true));

        btnSendAllToKitchen.setOnClickListener(v -> sendAllNewOrdersToKitchen());
        btnSalesReport.setOnClickListener(v -> openSalesReport());
        fabEmergency.setOnClickListener(v -> handleEmergencyStop());

        fabLogout.setOnClickListener(v -> showLogoutConfirmationDialog());
    }

    private void showLogoutConfirmationDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Logout", (dialog, which) -> performLogout())
                .setNegativeButton("Cancel", null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    private void performLogout() {
        Log.d(TAG, "User logged out");
        Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show();

        Intent intent = new Intent(CounterActivity.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @SuppressLint("MissingSuperCall")
    @Override
    public void onBackPressed() {
        new AlertDialog.Builder(this)
                .setTitle("Exit Counter")
                .setMessage("Do you want to logout and return to the login screen?")
                .setPositiveButton("Logout", (dialog, which) -> performLogout())
                .setNegativeButton("Stay", null)
                .show();
    }

    private void setupViewPager() {
        pagerAdapter = new OrderPagerAdapter(this, newOrders, inKitchenOrders, servedOrders);
        viewPager.setAdapter(pagerAdapter);

        viewPager.setOffscreenPageLimit(2);

        pagerAdapter.setOrderActionListener(new OrderAdapter.OnOrderActionListener() {
            @Override
            public void onMoveToKitchen(Order order) {
                updateOrderStatus(order.getOrderId(), "in-progress");
            }
            @Override
            public void onMarkReady(Order order) {
                updateOrderStatus(order.getOrderId(), "ready");
            }
            @Override
            public void onCompleteOrder(Order order) {
                updateOrderStatus(order.getOrderId(), "completed");
            }
            @Override
            public void onProcessPayment(Order order) {
                processPaymentAndArchive(order);
            }
            @Override
            public void onShowReceipt(Order order) {
                // Removed - no longer needed
            }
            @Override
            public void onDiscardOrder(Order order) {
                showDiscardConfirmationDialog(order);
            }
            @Override
            public void onServeToCustomer(Order order) {
                updateOrderStatus(order.getOrderId(), "served");
            }
        });

        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            switch (position) {
                case 0:
                    tab.setText("New Orders");
                    break;
                case 1:
                    tab.setText("Serve to Customer");
                    break;
                case 2:
                    tab.setText("Served Orders");
                    break;
            }
        }).attach();
    }

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

    private void updateCurrentTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("h:mm a", Locale.getDefault());
        String currentTime = sdf.format(new Date());
        tvCurrentTime.setText(currentTime);
    }

    private void initializePrinter() {
        printerManager = new BluetoothPrinterManager(this);

        enableBluetoothLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        Toast.makeText(this, "Bluetooth enabled", Toast.LENGTH_SHORT).show();
                        showPrinterSelectionDialog();
                    } else {
                        Toast.makeText(this, "Bluetooth is required for printing", Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }

    private void showPrinterSelectionDialog() {
        if (!printerManager.hasBluetoothPermissions()) {
            requestBluetoothPermissions();
            return;
        }

        if (!printerManager.isBluetoothEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            try {
                enableBluetoothLauncher.launch(enableBtIntent);
            } catch (SecurityException e) {
                Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show();
            }
            return;
        }

        List<BluetoothDevice> pairedDevices = new ArrayList<>();
        try {
            BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            if (bluetoothAdapter != null) {
                Set<BluetoothDevice> bondedDevices = bluetoothAdapter.getBondedDevices();
                if (bondedDevices != null) {
                    pairedDevices.addAll(bondedDevices);
                }
            }
        } catch (SecurityException e) {
            Toast.makeText(this, "Permission denied to access Bluetooth devices", Toast.LENGTH_SHORT).show();
            return;
        }

        if (pairedDevices.isEmpty()) {
            new AlertDialog.Builder(this)
                    .setTitle("No Paired Printers")
                    .setMessage("No Bluetooth devices are paired.\n\n" +
                            "Please pair your thermal printer:\n\n" +
                            "1. Turn on your printer\n" +
                            "2. Go to Settings → Bluetooth\n" +
                            "3. Look for your thermal printer name\n" +
                            "4. Tap to pair (PIN: 0000 or 1234)\n" +
                            "5. Return to this app")
                    .setPositiveButton("Open Bluetooth Settings", (dialog, which) -> {
                        Intent intent = new Intent(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS);
                        startActivity(intent);
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
            return;
        }

        String[] deviceNames = new String[pairedDevices.size()];
        for (int i = 0; i < pairedDevices.size(); i++) {
            try {
                String name = pairedDevices.get(i).getName();
                String address = pairedDevices.get(i).getAddress();
                deviceNames[i] = (name != null ? name : "Unknown Device") + "\n" + address;
            } catch (SecurityException e) {
                deviceNames[i] = "Device " + (i + 1);
            }
        }

        new AlertDialog.Builder(this)
                .setTitle("Select Printer")
                .setItems(deviceNames, (dialog, which) -> {
                    BluetoothDevice selectedDevice = pairedDevices.get(which);
                    connectToPrinter(selectedDevice);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void requestBluetoothPermissions() {
        List<String> permissionsNeeded = new ArrayList<>();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
                    != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(Manifest.permission.BLUETOOTH_CONNECT);
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN)
                    != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(Manifest.permission.BLUETOOTH_SCAN);
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH)
                    != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(Manifest.permission.BLUETOOTH);
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADMIN)
                    != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(Manifest.permission.BLUETOOTH_ADMIN);
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED) {
                    permissionsNeeded.add(Manifest.permission.ACCESS_FINE_LOCATION);
                }
            }
        }

        if (!permissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(
                    this,
                    permissionsNeeded.toArray(new String[0]),
                    REQUEST_BLUETOOTH_PERMISSIONS
            );
        } else {
            showPrinterSelectionDialog();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_BLUETOOTH_PERMISSIONS) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }

            if (allGranted) {
                showPrinterSelectionDialog();
            } else {
                Toast.makeText(this, "Bluetooth permissions required for printing", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void connectToPrinter(BluetoothDevice device) {
        Toast.makeText(this, "Connecting to printer...", Toast.LENGTH_SHORT).show();

        printerManager.connectToPrinter(device, new BluetoothPrinterManager.ConnectionCallback() {
            @Override
            public void onConnected(BluetoothDevice device) {
                runOnUiThread(() -> {
                    try {
                        String deviceName = device.getName();
                        Toast.makeText(CounterActivity.this,
                                "✓ Printer connected: " + deviceName,
                                Toast.LENGTH_SHORT).show();
                        Log.d(TAG, "Printer connected: " + deviceName);
                    } catch (SecurityException e) {
                        Toast.makeText(CounterActivity.this,
                                "✓ Printer connected!",
                                Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onDisconnected() {
                runOnUiThread(() -> {
                    Toast.makeText(CounterActivity.this,
                            "Printer disconnected",
                            Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "Printer disconnected");
                });
            }

            @Override
            public void onConnectionFailed(String error) {
                runOnUiThread(() -> {
                    Toast.makeText(CounterActivity.this,
                            "Connection failed: " + error,
                            Toast.LENGTH_LONG).show();
                    Log.e(TAG, "Printer connection failed: " + error);
                });
            }
        });
    }

    private void processPaymentAndArchive(Order order) {
        Log.d(TAG, "processPaymentAndArchive called for order: " + order.getOrderId());

        PaymentDialogFragment paymentDialog = PaymentDialogFragment.newInstance(
                order.getOrderId(),
                order.getTotalAmount()
        );

        paymentDialog.setOnPaymentConfirmListener((orderId, totalAmount, cashGiven, change) -> {
            Log.d(TAG, "Payment confirmed - Cash: " + cashGiven + ", Change: " + change);
            if (printerManager != null && printerManager.isConnected()) {
                showLoading("Printing receipt...");

                new Thread(() -> {
                    try {
                        printReceiptWithPayment(order, cashGiven, change);

                        runOnUiThread(() -> {
                            processPayment(orderId);
                        });

                    } catch (Exception e) {
                        Log.e(TAG, "Error printing receipt before payment", e);

                        runOnUiThread(() -> {
                            hideLoading();
                            new AlertDialog.Builder(CounterActivity.this)
                                    .setTitle("Print Failed")
                                    .setMessage("Receipt printing failed. Do you want to continue with payment anyway?")
                                    .setPositiveButton("Continue", (dialog, which) -> processPayment(orderId))
                                    .setNegativeButton("Cancel", null)
                                    .show();
                        });
                    }
                }).start();
            } else {
                new AlertDialog.Builder(this)
                        .setTitle("Printer Not Connected")
                        .setMessage("No printer is connected. Do you want to process payment without printing?")
                        .setPositiveButton("Yes, Continue", (dialog, which) -> processPayment(orderId))
                        .setNegativeButton("Cancel", null)
                        .show();
            }
        });

        FragmentManager fragmentManager = getSupportFragmentManager();
        paymentDialog.show(fragmentManager, "PaymentDialog");
    }


    private void printReceiptWithPayment(Order order, double cashGiven, double change) throws Exception {
        OutputStream os = printerManager.getOutputStream();
        if (os == null) {
            throw new Exception("Printer output stream not available");
        }

        os.write(new byte[]{0x1B, 0x40});
        os.write(new byte[]{0x1B, 0x61, 0x01});
        os.write(new byte[]{0x1B, 0x45, 0x01});
        os.write(new byte[]{0x1D, 0x21, 0x11});
        os.write("MIST CAFE\n".getBytes());
        os.write("RESTAURANT\n".getBytes());
        os.write(new byte[]{0x1D, 0x21, 0x00});
        os.write(new byte[]{0x1B, 0x45, 0x00});
        os.write("Panglao, Bohol\n".getBytes());
        os.write("================================\n".getBytes());
        os.write(new byte[]{0x1B, 0x61, 0x00});
        os.write(String.format("Order #: %s\n", order.getOrderId()).getBytes());
        os.write(String.format(order.getTableNumber()).getBytes());
        os.write(String.format("\nCustomer: %s\n", order.getCustomerName()).getBytes());

        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault());
        os.write(String.format("Date: %s\n", sdf.format(new Date(order.getTimestamp()))).getBytes());
        os.write("================================\n".getBytes());
        os.write(new byte[]{0x1B, 0x45, 0x01});
        os.write(String.format("%-3s %-20s %6s\n", "QTY", "ITEM", "AMOUNT").getBytes());
        os.write(new byte[]{0x1B, 0x45, 0x00});
        os.write("================================\n".getBytes());

        double subtotal = 0;
        for (OrderItem item : order.getItems()) {
            String itemName = item.getItemName();
            if (itemName.length() > 20) {
                itemName = itemName.substring(0, 17) + "...";
            }

            double itemTotal = item.getPrice() * item.getQuantity();
            subtotal += itemTotal;

            os.write(String.format("%-3d %-20s %6.2f\n",
                    item.getQuantity(),
                    itemName,
                    itemTotal).getBytes());
        }

        os.write("================================\n".getBytes());

        double vat = subtotal * 0.12;
        double total = subtotal;

        os.write(String.format("%-24s %7.2f\n", "Subtotal:", subtotal).getBytes());
        os.write(String.format("%-24s %7.2f\n", "VAT (12%):", vat).getBytes());
        os.write("(for display only)\n".getBytes());
        os.write("================================\n".getBytes());
        os.write(new byte[]{0x1B, 0x45, 0x01});
        os.write(new byte[]{0x1D, 0x21, 0x01});
        os.write(String.format("%-24s %7.2f\n", "TOTAL:", total).getBytes());
        os.write(new byte[]{0x1D, 0x21, 0x00});
        os.write(new byte[]{0x1B, 0x45, 0x00});
        os.write("================================\n".getBytes());

        // Payment Details
        os.write(new byte[]{0x1B, 0x45, 0x01});
        os.write(String.format("%-24s %7.2f\n", "CASH GIVEN:", cashGiven).getBytes());
        os.write(String.format("%-24s %7.2f\n", "CHANGE:", change).getBytes());
        os.write(new byte[]{0x1B, 0x45, 0x00});
        os.write("================================\n\n".getBytes());
        os.write(new byte[]{0x1B, 0x61, 0x01});
        os.write("Thank you for dining with us!\n".getBytes());
        os.write("Please come again!\n\n".getBytes());
        os.write(new byte[]{0x1B, 0x64, 0x03});
        os.write(new byte[]{0x1D, 0x56, 0x00});
        os.flush();

        Log.d(TAG, "Receipt with payment details printed successfully");
    }
    private void startRealtimeOrdersListener() {
        showLoading("Loading orders...");

        ordersListener = db.collection("orders")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener((queryDocumentSnapshots, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Error listening to orders", error);
                        Toast.makeText(this, "Error loading orders", Toast.LENGTH_SHORT).show();
                        hideLoading();
                        return;
                    }

                    if (queryDocumentSnapshots != null) {
                        updateOrderLists(queryDocumentSnapshots);
                        updateOrderCounts();
                        updateViewPager();
                        hideLoading();

                        Log.d(TAG, "Real-time update - Total: " + allOrders.size() +
                                ", New: " + newOrders.size() +
                                ", Serve to Customer: " + inKitchenOrders.size() +
                                ", Served: " + servedOrders.size());
                    }
                });
    }

    private void updateOrderLists(com.google.firebase.firestore.QuerySnapshot queryDocumentSnapshots) {
        allOrders.clear();
        newOrders.clear();
        inKitchenOrders.clear();
        servedOrders.clear();

        for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
            Order order = Order.fromFirestore(document);
            String status = order.getStatus().toLowerCase().trim();

            Log.d(TAG, "Order: " + order.getOrderId() + " - Status: '" + status + "'");

            allOrders.add(order);

            if (status.equals("standby")) {
                Log.d(TAG, "  -> STANDBY - Not displayed");
                continue;
            }

            if (status.equals("pending")) {
                newOrders.add(order);
                Log.d(TAG, "  -> Added to NEW ORDERS");
            } else if (status.equals("in-progress") || status.equals("in_progress")) {
                Log.d(TAG, "  -> IN PROGRESS - Not displayed in counter");
            } else if (status.equals("ready")) {
                Log.d(TAG, "  -> READY - Not displayed in counter");
            } else if (status.equals("completed")) {
                inKitchenOrders.add(order);
                Log.d(TAG, "  -> Added to SERVE TO CUSTOMER (completed)");
            } else if (status.equals("served")) {
                servedOrders.add(order);
                Log.d(TAG, "  -> Added to SERVED ORDERS");
            } else {
                Log.d(TAG, "  -> Unknown status: '" + status + "' - Not displayed");
            }
        } // End of for loop

        // Sort served orders by table number for easy lookup
        Collections.sort(servedOrders, (o1, o2) -> {
            String table1 = o1.getTableNumber().replaceAll("[^0-9]", "");
            String table2 = o2.getTableNumber().replaceAll("[^0-9]", "");

            try {
                int num1 = Integer.parseInt(table1.isEmpty() ? "999" : table1);
                int num2 = Integer.parseInt(table2.isEmpty() ? "999" : table2);
                return Integer.compare(num1, num2);
            } catch (NumberFormatException e) {
                return o1.getTableNumber().compareTo(o2.getTableNumber());
            }
        });
    }
    private void updateOrderStatus(String orderId, String newStatus) {
        showLoading("Updating order status...");

        Map<String, Object> updates = new HashMap<>();
        updates.put("status", newStatus);

        db.collection("orders")
                .document(orderId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Order " + orderId + " status updated to " + newStatus);
                    Toast.makeText(this, "Order status updated!", Toast.LENGTH_SHORT).show();
                    hideLoading();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error updating order status", e);
                    Toast.makeText(this, "Failed to update order status", Toast.LENGTH_SHORT).show();
                    hideLoading();
                });
    }

    private void processPayment(String orderId) {
        showLoading("Processing payment...");

        paymentHandler.processPayment(orderId, new PaymentHandler.PaymentCallback() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "✅ Payment processed and order archived: " + orderId);
                Toast.makeText(CounterActivity.this, "Payment completed! Order archived.", Toast.LENGTH_SHORT).show();
                hideLoading();
            }

            @Override
            public void onFailure(String error) {
                Log.e(TAG, "❌ Payment processing failed: " + error);
                Toast.makeText(CounterActivity.this, "Payment failed: " + error, Toast.LENGTH_SHORT).show();
                hideLoading();
            }
        });
    }

    private void showDiscardConfirmationDialog(Order order) {
        new AlertDialog.Builder(this)
                .setTitle("Discard Order")
                .setMessage("Are you sure you want to discard Order #" + order.getOrderId() + "?\n\n" +
                        "This action cannot be undone. The order will be permanently deleted from the system.")
                .setPositiveButton("Discard", (dialog, which) -> discardOrder(order))
                .setNegativeButton("Cancel", null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    private void discardOrder(Order order) {
        showLoading("Discarding order...");

        db.collection("orders")
                .document(order.getOrderId())
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Order " + order.getOrderId() + " successfully deleted");
                    Toast.makeText(this, "Order #" + order.getOrderId() + " has been discarded", Toast.LENGTH_SHORT).show();
                    hideLoading();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error deleting order " + order.getOrderId(), e);
                    Toast.makeText(this, "Failed to discard order. Please try again.", Toast.LENGTH_SHORT).show();
                    hideLoading();
                });
    }

    private void sendAllNewOrdersToKitchen() {
        if (newOrders.isEmpty()) {
            Toast.makeText(this, "No new orders to send to kitchen", Toast.LENGTH_SHORT).show();
            return;
        }

        showLoading("Sending all orders to kitchen...");

        int totalOrders = newOrders.size();
        int[] completedCount = {0};

        for (Order order : newOrders) {
            Map<String, Object> updates = new HashMap<>();
            updates.put("status", "in-progress");

            db.collection("orders")
                    .document(order.getOrderId())
                    .update(updates)
                    .addOnSuccessListener(aVoid -> {
                        completedCount[0]++;
                        if (completedCount[0] == totalOrders) {
                            Toast.makeText(this, "All orders sent to kitchen!", Toast.LENGTH_SHORT).show();
                            hideLoading();
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error updating order status", e);
                        completedCount[0]++;
                        if (completedCount[0] == totalOrders) {
                            hideLoading();
                        }
                    });
        }
    }

    private void openSalesReport() {
        long todayStart = getTodayStartTime();
        long todayEnd = System.currentTimeMillis();

        db.collection("archivedOrders")
                .whereEqualTo("status", "paid")
                .whereGreaterThanOrEqualTo("timestamp", todayStart)
                .whereLessThanOrEqualTo("timestamp", todayEnd)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    double totalSales = 0;
                    int totalOrders = queryDocumentSnapshots.size();

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Order order = Order.fromFirestore(document);
                        totalSales += order.getTotalAmount();
                    }

                    String salesReport = String.format(Locale.getDefault(),
                            "Today's Sales Report\n\n" +
                                    "Total Orders: %d\n" +
                                    "Total Revenue: ₱%.2f\n" +
                                    "Average Order: ₱%.2f",
                            totalOrders, totalSales, totalOrders > 0 ? totalSales / totalOrders : 0);

                    new AlertDialog.Builder(this)
                            .setTitle("Sales Report")
                            .setMessage(salesReport)
                            .setPositiveButton("OK", null)
                            .show();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading sales data", e);
                    Toast.makeText(this, "Failed to load sales data", Toast.LENGTH_SHORT).show();
                });
    }

    private long getTodayStartTime() {
        java.util.Calendar calendar = java.util.Calendar.getInstance();
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 0);
        calendar.set(java.util.Calendar.MINUTE, 0);
        calendar.set(java.util.Calendar.SECOND, 0);
        calendar.set(java.util.Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }

    private void updateOrderCounts() {
        tvNewOrdersCount.setText(String.valueOf(newOrders.size()));
        tvInKitchenCount.setText(String.valueOf(inKitchenOrders.size()));
        tvServedOrdersCount.setText(String.valueOf(servedOrders.size()));
    }

    private void updateViewPager() {
        if (pagerAdapter != null) {
            pagerAdapter.updateOrders(newOrders, inKitchenOrders, servedOrders);
        }
    }

    private void handleEmergencyStop() {
        new AlertDialog.Builder(this)
                .setTitle("Emergency Stop")
                .setMessage("Are you sure you want to activate emergency stop?\n\nThis will pause all order processing.")
                .setPositiveButton("Activate", (dialog, which) -> {
                    Toast.makeText(this, "Emergency stop activated!", Toast.LENGTH_LONG).show();
                    fabEmergency.setVisibility(View.GONE);
                })
                .setNegativeButton("Cancel", null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    private void showLoading(String message) {
        tvLoadingMessage.setText(message);
        loadingOverlay.setVisibility(View.VISIBLE);
    }

    private void hideLoading() {
        loadingOverlay.setVisibility(View.GONE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (timeHandler != null && timeRunnable != null) {
            timeHandler.removeCallbacks(timeRunnable);
        }
        if (ordersListener != null) {
            ordersListener.remove();
            Log.d(TAG, "Firestore listener removed");
        }
        if (printerManager != null) {
            printerManager.release();
            Log.d(TAG, "Printer manager released");
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
}