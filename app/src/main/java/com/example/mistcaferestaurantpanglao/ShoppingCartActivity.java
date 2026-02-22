package com.example.mistcaferestaurantpanglao;

import static android.content.ContentValues.TAG;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Html;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;


public class ShoppingCartActivity extends AppCompatActivity implements CartAdapter.OnCartUpdateListener {

    private static final int CAMERA_PERMISSION_REQUEST = 100;
    private static final long NAV_DELAY_MS = 300L;

    private RecyclerView recyclerViewCart;
    private CartAdapter cartAdapter;
    private List<CartItem> cartItems;
    private TextView tvTotalAmount;
    private Button btnCheckout;
    private Button btnContinueShopping;
    private AppCompatImageButton btnBack;
    private static final List<CartItem> staticCartList = new ArrayList<>();

    private ActivityResultLauncher<ScanOptions> barcodeLauncher;
    private ActivityResultLauncher<Intent> imagePickerLauncher;
    private AlertDialog checkoutDialog;
    private AlertDialog staffAuthDialog;
    private TextView tvScannedResult;
    private Button btnScanQR;
    private Button btnUploadQR;
    private String scannedQRData = "";
    private String tableNumber = "";

    private Handler uploadButtonHandler = new Handler(Looper.getMainLooper());
    private Runnable showUploadButtonRunnable;
    private static final long UPLOAD_BUTTON_DELAY = 5000; // 5 seconds

    private FirebaseFirestore db;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private ListenerRegistration dishesLeftListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shopping_cart);

        db = FirebaseFirestore.getInstance();

        initViews();
        setupRecyclerView();
        setupClickListeners();
        setupQRScanner();
        setupImagePicker();
        setupRealtimeDishesLeftListener();
        handleIncomingData();
        updateTotalAmount();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (cartItems == null) cartItems = new ArrayList<>();
        cartItems.clear();
        cartItems.addAll(staticCartList);
        if (cartAdapter != null) cartAdapter.notifyDataSetChanged();
        updateTotalAmount();
    }

    @Override
    protected void onDestroy() {
        if (dishesLeftListener != null) {
            dishesLeftListener.remove();
        }
        if (checkoutDialog != null && checkoutDialog.isShowing()) {
            checkoutDialog.dismiss();
            checkoutDialog = null;
        }
        if (staffAuthDialog != null && staffAuthDialog.isShowing()) {
            staffAuthDialog.dismiss();
            staffAuthDialog = null;
        }
        if (uploadButtonHandler != null && showUploadButtonRunnable != null) {
            uploadButtonHandler.removeCallbacks(showUploadButtonRunnable);
        }
        super.onDestroy();
    }

    @SuppressLint("WrongViewCast")
    private void initViews() {
        recyclerViewCart = findViewById(R.id.recyclerViewCart);
        tvTotalAmount = findViewById(R.id.tvTotalAmount);
        btnCheckout = findViewById(R.id.btnCheckout);
        btnContinueShopping = findViewById(R.id.btnContinueShopping);
        btnBack = findViewById(R.id.btnBack);

        cartItems = new ArrayList<>(staticCartList);
    }

    private void setupRecyclerView() {
        cartAdapter = new CartAdapter(this, cartItems, this);
        recyclerViewCart.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewCart.setAdapter(cartAdapter);
    }

    private void setupClickListeners() {
        btnCheckout.setOnClickListener(v -> {
            if (cartItems == null || cartItems.isEmpty()) {
                Toast.makeText(this, "Your cart is empty!", Toast.LENGTH_SHORT).show();
                return;
            }
            showCheckoutDialog();
        });

        btnContinueShopping.setOnClickListener(v -> finish());
        btnBack.setOnClickListener(v -> finish());
    }

    private void setupRealtimeDishesLeftListener() {
        dishesLeftListener = db.collection("menuItems")
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) {
                        android.util.Log.e(TAG, "Listen failed", error);
                        return;
                    }

                    if (snapshots != null && cartItems != null) {
                        for (DocumentSnapshot doc : snapshots.getDocuments()) {
                            String dishName = doc.getString("dishName");
                            Long dishesLeft = doc.getLong("dishesLeft");
                            Boolean available = doc.getBoolean("available");

                            for (CartItem cartItem : cartItems) {
                                if (cartItem.getDishName().equals(dishName)) {
                                    if (available != null && !available && dishesLeft != null && dishesLeft == 0) {
                                        Toast.makeText(ShoppingCartActivity.this,
                                                dishName + " is now unavailable",
                                                Toast.LENGTH_SHORT).show();
                                    }
                                }
                            }
                        }
                    }
                });
    }
    private void setupQRScanner() {
        barcodeLauncher = registerForActivityResult(new ScanContract(), result -> {
            cancelUploadButtonTimer();

            if (result == null || result.getContents() == null) {
                Toast.makeText(this, "Scan cancelled", Toast.LENGTH_SHORT).show();
                if (btnUploadQR != null) {
                    btnUploadQR.setVisibility(View.VISIBLE);
                }
                if (btnScanQR != null) {
                    btnScanQR.setVisibility(View.VISIBLE);
                }
                return;
            }

            scannedQRData = result.getContents();
            String parsed = parseTableNumberFromQR(scannedQRData);

            if (parsed != null) {
                tableNumber = parsed;
                if (tvScannedResult != null) {
                    tvScannedResult.setText(tableNumber);
                    tvScannedResult.setVisibility(View.VISIBLE);
                }

                if (btnScanQR != null) {
                    btnScanQR.setVisibility(View.GONE);
                }
                if (btnUploadQR != null) {
                    btnUploadQR.setVisibility(View.GONE);
                }

                String message = "Table QR Code scanned successfully! <big><b>" + tableNumber + "</b></big>";
                Toast.makeText(this, Html.fromHtml(message, Html.FROM_HTML_MODE_LEGACY), Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Invalid QR code format. Please scan a valid table QR code.", Toast.LENGTH_LONG).show();
                scannedQRData = "";
                tableNumber = "";
                if (tvScannedResult != null) tvScannedResult.setVisibility(View.GONE);
                if (btnScanQR != null) btnScanQR.setVisibility(View.VISIBLE);
                if (btnUploadQR != null) btnUploadQR.setVisibility(View.VISIBLE);
            }
        });
    }

    private void setupImagePicker() {
        imagePickerLauncher = registerForActivityResult(
                new androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri imageUri = result.getData().getData();
                        if (imageUri != null) {
                            decodeQRFromImage(imageUri);
                        }
                    } else {
                        Toast.makeText(this, "No image selected", Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }

    private void decodeQRFromImage(Uri imageUri) {
        try {
            android.graphics.Bitmap bitmap = android.provider.MediaStore.Images.Media.getBitmap(
                    getContentResolver(), imageUri);

            int[] intArray = new int[bitmap.getWidth() * bitmap.getHeight()];
            bitmap.getPixels(intArray, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());

            com.google.zxing.LuminanceSource source = new com.google.zxing.RGBLuminanceSource(
                    bitmap.getWidth(), bitmap.getHeight(), intArray);
            com.google.zxing.BinaryBitmap binaryBitmap = new com.google.zxing.BinaryBitmap(
                    new com.google.zxing.common.HybridBinarizer(source));

            com.google.zxing.Reader reader = new com.google.zxing.qrcode.QRCodeReader();
            com.google.zxing.Result result = reader.decode(binaryBitmap);

            String qrContent = result.getText();
            String parsed = parseTableNumberFromQR(qrContent);

            if (parsed != null) {
                tableNumber = parsed;
                scannedQRData = qrContent;

                if (tvScannedResult != null) {
                    tvScannedResult.setText(tableNumber);
                    tvScannedResult.setVisibility(View.VISIBLE);
                }

                if (btnScanQR != null) {
                    btnScanQR.setVisibility(View.GONE);
                }
                if (btnUploadQR != null) {
                    btnUploadQR.setVisibility(View.GONE);
                }

                String message = "Table QR Code uploaded successfully! <big><b>" + tableNumber + "</b></big>";
                Toast.makeText(this, Html.fromHtml(message, Html.FROM_HTML_MODE_LEGACY), Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Invalid QR code format in image. Please upload a valid table QR code.",
                        Toast.LENGTH_LONG).show();
            }

        } catch (Exception e) {
            Toast.makeText(this, "Failed to read QR code from image: " + e.getMessage(),
                    Toast.LENGTH_LONG).show();
        }
    }

    private String parseTableNumberFromQR(String qrData) {
        if (qrData == null) return null;
        try {
            if (!qrData.startsWith("mistcafe://table")) return null;
            Uri uri = Uri.parse(qrData);
            String tableId = uri.getQueryParameter("id");
            if (tableId == null || tableId.trim().isEmpty()) return null;
            int id = Integer.parseInt(tableId.trim());
            if (id < 1) return null;
            return "Table " + id;
        } catch (Exception e) {
            return null;
        }
    }

    private void showCheckoutDialog() {
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_checkout, null);

        EditText etCustomerName = dialogView.findViewById(R.id.etCustomerName);
        TextView tvOrderSummary = dialogView.findViewById(R.id.tvOrderSummary);
        TextView tvDialogTotal = dialogView.findViewById(R.id.tvDialogTotal);
        btnScanQR = dialogView.findViewById(R.id.btnScanQR);
        btnUploadQR = dialogView.findViewById(R.id.btnUploadQR);
        Button btnConfirmOrder = dialogView.findViewById(R.id.btnConfirmOrder);
        Button btnCancelOrder = dialogView.findViewById(R.id.btnCancelOrder);
        Button btnDialogBack = dialogView.findViewById(R.id.btnBack);
        tvScannedResult = dialogView.findViewById(R.id.tvScannedResult);

        StringBuilder orderSummary = new StringBuilder();
        double totalAmount = 0.0;
        for (CartItem item : cartItems) {
            orderSummary.append("• ").append(item.getDishName())
                    .append(" x").append(item.getQuantity())
                    .append(" - ₱").append(String.format(Locale.getDefault(), "%.2f", item.getTotalPrice()))
                    .append("\n");
            totalAmount += item.getTotalPrice();
        }
        final double finalTotalAmount = totalAmount;

        tvOrderSummary.setText(orderSummary.toString());
        tvDialogTotal.setText("Total: ₱" + String.format(Locale.getDefault(), "%.2f", finalTotalAmount));

        scannedQRData = "";
        tableNumber = "";
        tvScannedResult.setVisibility(View.GONE);
        btnScanQR.setVisibility(View.VISIBLE);
        btnUploadQR.setVisibility(View.GONE);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(dialogView);
        builder.setCancelable(false);
        checkoutDialog = builder.create();

        btnScanQR.setOnClickListener(v -> {
            checkCameraPermissionAndScan();
            startUploadButtonTimer();
        });

        btnUploadQR.setOnClickListener(v -> {
            cancelUploadButtonTimer();
            showStaffAuthenticationDialog();
        });

        btnCancelOrder.setOnClickListener(v -> {
            cancelUploadButtonTimer();
            if (checkoutDialog != null && checkoutDialog.isShowing()) checkoutDialog.dismiss();
        });

        btnDialogBack.setOnClickListener(v -> {
            cancelUploadButtonTimer();
            if (checkoutDialog != null && checkoutDialog.isShowing()) checkoutDialog.dismiss();
        });

        btnConfirmOrder.setOnClickListener(v -> {
            String customerName = etCustomerName.getText() == null ? "" : etCustomerName.getText().toString().trim();
            if (customerName.isEmpty()) {
                Toast.makeText(this, "Please enter your name", Toast.LENGTH_SHORT).show();
                return;
            }
            if (tableNumber == null || tableNumber.isEmpty()) {
                Toast.makeText(this, "Please scan a valid table QR code before confirming your order", Toast.LENGTH_LONG).show();
                return;
            }

            btnConfirmOrder.setEnabled(false);
            btnConfirmOrder.setText("Processing...");

            uploadOrderToFirestore(customerName, finalTotalAmount, tableNumber, btnConfirmOrder);
        });

        checkoutDialog.show();
    }

    private void showStaffAuthenticationDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_staff_login, null);
        builder.setView(dialogView);
        builder.setCancelable(true);

        staffAuthDialog = builder.create();

        TextInputEditText etUsername = dialogView.findViewById(R.id.etStaffUsername);
        TextInputEditText etPassword = dialogView.findViewById(R.id.etStaffPassword);
        MaterialButton btnLogin = dialogView.findViewById(R.id.btnStaffLogin);
        TextView tvCreateAccount = dialogView.findViewById(R.id.tvCreateAccount);
        TextInputLayout tilUsername = dialogView.findViewById(R.id.tilUsername);
        TextInputLayout tilPassword = dialogView.findViewById(R.id.tilPassword);

        if (tvCreateAccount != null) {
            tvCreateAccount.setVisibility(View.GONE);
        }

        if (btnLogin != null) {
            btnLogin.setText("Authenticate");
        }

        setupAuthErrorResetListeners(etUsername, etPassword, tilUsername, tilPassword);

        btnLogin.setOnClickListener(v -> {
            String username = etUsername.getText() != null ? etUsername.getText().toString().trim() : "";
            String password = etPassword.getText() != null ? etPassword.getText().toString().trim() : "";

            tilUsername.setError(null);
            tilUsername.setErrorEnabled(false);
            tilPassword.setError(null);
            tilPassword.setErrorEnabled(false);

            if (TextUtils.isEmpty(username)) {
                tilUsername.setErrorEnabled(true);
                tilUsername.setError("Username is required");
                return;
            }

            if (TextUtils.isEmpty(password)) {
                tilPassword.setErrorEnabled(true);
                tilPassword.setError("Password is required");
                return;
            }

            btnLogin.setEnabled(false);
            btnLogin.setText("Authenticating...");

            authenticateStaffForUpload(username, password, tilUsername, tilPassword, btnLogin);
        });

        staffAuthDialog.show();
    }
    private void setupAuthErrorResetListeners(TextInputEditText etUsername, TextInputEditText etPassword,
                                              TextInputLayout tilUsername, TextInputLayout tilPassword) {
        etUsername.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (tilUsername.isErrorEnabled()) {
                    tilUsername.setError(null);
                    tilUsername.setErrorEnabled(false);
                }
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {}
        });

        etPassword.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (tilPassword.isErrorEnabled()) {
                    tilPassword.setError(null);
                    tilPassword.setErrorEnabled(false);
                }
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {}
        });
    }

    private void authenticateStaffForUpload(String username, String password,
                                            TextInputLayout tilUsername, TextInputLayout tilPassword,
                                            MaterialButton btnLogin) {
        db.collection("users").document(username).get()
                .addOnCompleteListener(task -> {
                    btnLogin.setEnabled(true);
                    btnLogin.setText("Authenticate");

                    if (!task.isSuccessful()) {
                        Toast.makeText(this, "Failed to connect to server", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    DocumentSnapshot document = task.getResult();
                    if (!document.exists()) {
                        tilUsername.setErrorEnabled(true);
                        tilUsername.setError("User not found");
                        return;
                    }

                    String dbPassword = document.getString("password");
                    if (!password.equals(dbPassword)) {
                        tilPassword.setErrorEnabled(true);
                        tilPassword.setError("Incorrect password");
                        return;
                    }

                    String role = document.getString("role");
                    String status = document.getString("status");

                    if (!"active".equalsIgnoreCase(status)) {
                        Toast.makeText(this, "Account is not active", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (role == null || (!role.equalsIgnoreCase("admin") &&
                            !role.equalsIgnoreCase("kitchen") &&
                            !role.equalsIgnoreCase("counter"))) {
                        Toast.makeText(this, "Only staff members can upload QR codes", Toast.LENGTH_LONG).show();
                        return;
                    }

                    Toast.makeText(this, "Authentication successful!", Toast.LENGTH_SHORT).show();

                    if (staffAuthDialog != null && staffAuthDialog.isShowing()) {
                        staffAuthDialog.dismiss();
                    }

                    openImagePicker();
                });
    }

    private void uploadOrderToFirestore(String customerName, double totalAmount, String tableNumber, Button btnConfirmOrder) {
        if (tableNumber == null || tableNumber.isEmpty()) {
            Toast.makeText(this, "Cannot place order without table information. Please scan a valid table QR code.", Toast.LENGTH_LONG).show();
            if (btnConfirmOrder != null) {
                btnConfirmOrder.setEnabled(true);
                btnConfirmOrder.setText("Confirm Order");
            }
            return;
        }

        String timestamp = new SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault()).format(new Date());
        String orderId = "ORDER" + timestamp;

        List<Map<String, Object>> orderItems = new ArrayList<>();
        for (CartItem item : cartItems) {
            Map<String, Object> itemMap = new HashMap<>();
            itemMap.put("dishName", item.getDishName());
            itemMap.put("quantity", item.getQuantity());
            itemMap.put("totalPrice", item.getTotalPrice());
            orderItems.add(itemMap);
        }

        Map<String, Object> orderData = new HashMap<>();
        orderData.put("orderId", orderId);
        orderData.put("customerName", customerName);
        orderData.put("totalAmount", totalAmount);
        orderData.put("orderItems", orderItems);
        orderData.put("timestamp", new Date());
        orderData.put("status", "pending");
        orderData.put("table", tableNumber);

        db.collection("orders")
                .document(orderId)
                .set(orderData)
                .addOnSuccessListener(unused -> {
                    cartItems.clear();
                    synchronized (staticCartList) {
                        staticCartList.clear();
                    }
                    if (cartAdapter != null) cartAdapter.notifyDataSetChanged();
                    updateTotalAmount();

                    if (checkoutDialog != null && checkoutDialog.isShowing()) {
                        checkoutDialog.dismiss();
                    }

                    showOrderSuccessDialog(customerName, orderId, tableNumber);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to place order. Please try again.", Toast.LENGTH_LONG).show();
                    if (btnConfirmOrder != null) {
                        btnConfirmOrder.setEnabled(true);
                        btnConfirmOrder.setText("Confirm Order");
                    }
                });
    }

    private void showOrderSuccessDialog(String customerName, String orderId, String tableNumber) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        String message = "Thank you for your order!<br><br>" +
                "Customer: <b>" + escapeHtml(customerName) + "</b><br>" +
                "<big><b>" + escapeHtml(tableNumber) + "</b></big><br>" +
                "Order ID: " + escapeHtml(orderId) + "<br><br>" +
                "Your order has been sent to the kitchen. Please wait while your food is being prepared.";

        builder.setTitle("Order Placed Successfully!")
                .setMessage(Html.fromHtml(message, Html.FROM_HTML_MODE_COMPACT))
                .setCancelable(false)
                .setPositiveButton("OK", (dialog, which) -> {
                    dialog.dismiss();

                    mainHandler.postDelayed(() -> {
                        if (!isFinishing() && !isDestroyed()) {
                            Intent intent = new Intent(ShoppingCartActivity.this, CategoriesActivity.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(intent);
                            finish();
                        }
                    }, NAV_DELAY_MS);
                });

        AlertDialog successDialog = builder.create();
        successDialog.show();
    }

    private String escapeHtml(String s) {
        if (s == null) return "";
        return Html.escapeHtml(s);
    }

    private void checkCameraPermissionAndScan() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            startQRScanner();
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA},
                    CAMERA_PERMISSION_REQUEST);
        }
    }

    private void startQRScanner() {
        ScanOptions options = new ScanOptions();
        options.setDesiredBarcodeFormats(ScanOptions.QR_CODE);
        options.setPrompt("Scan QR Code for Table Number");
        options.setCameraId(0);
        options.setBeepEnabled(true);
        options.setBarcodeImageEnabled(true);
        options.setOrientationLocked(false);

        if (barcodeLauncher != null) barcodeLauncher.launch(options);
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        imagePickerLauncher.launch(intent);
    }

    private void startUploadButtonTimer() {
        cancelUploadButtonTimer();

        showUploadButtonRunnable = new Runnable() {
            @Override
            public void run() {
                if (btnUploadQR != null && tableNumber.isEmpty()) {
                    btnUploadQR.setVisibility(View.VISIBLE);
                    Toast.makeText(ShoppingCartActivity.this,
                            "Having trouble scanning? Ask for a Staff for uploading QR code",
                            Toast.LENGTH_LONG).show();
                }
            }
        };

        uploadButtonHandler.postDelayed(showUploadButtonRunnable, UPLOAD_BUTTON_DELAY);
    }

    private void cancelUploadButtonTimer() {
        if (uploadButtonHandler != null && showUploadButtonRunnable != null) {
            uploadButtonHandler.removeCallbacks(showUploadButtonRunnable);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == CAMERA_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startQRScanner();
            } else {
                Toast.makeText(this, "Camera permission is required to scan QR codes",
                        Toast.LENGTH_LONG).show();
                if (btnUploadQR != null) {
                    btnUploadQR.setVisibility(View.VISIBLE);
                }
            }
        }
    }

    private void handleIncomingData() {
        Intent intent = getIntent();
        if (intent == null) return;

        String action = intent.getStringExtra("action");
        if (!"add_item".equals(action)) return;

        String dishName = intent.getStringExtra("dish_name");
        double dishPrice = intent.getDoubleExtra("dish_price", 0.0);
        int quantity = intent.getIntExtra("dish_quantity", 1);
        double totalPrice = intent.getDoubleExtra("dish_total_price", dishPrice * quantity);
        String imageUri = intent.getStringExtra("dish_image_uri");
        String description = intent.getStringExtra("dish_description");

        if (dishName == null || dishName.trim().isEmpty()) return;

        CartItem existingItem = findCartItemByName(dishName);
        if (existingItem != null) {
            existingItem.setQuantity(existingItem.getQuantity() + quantity);
            existingItem.setTotalPrice(existingItem.getPrice() * existingItem.getQuantity());
            Toast.makeText(this, "Updated " + dishName + " quantity in cart", Toast.LENGTH_SHORT).show();
        } else {
            CartItem newItem = new CartItem(dishName, dishPrice, quantity, totalPrice, imageUri, description);
            cartItems.add(newItem);
            synchronized (staticCartList) {
                staticCartList.add(newItem);
            }
            Toast.makeText(this, "Added " + dishName + " to cart", Toast.LENGTH_SHORT).show();
        }
        if (cartAdapter != null) cartAdapter.notifyDataSetChanged();
        updateTotalAmount();
    }

    private void restoreDishesLeft(CartItem removedItem) {
        if (removedItem == null || removedItem.getDishName() == null) return;

        db.collection("menuItems")
                .whereEqualTo("dishName", removedItem.getDishName())
                .limit(1)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        DocumentSnapshot document = queryDocumentSnapshots.getDocuments().get(0);
                        String itemId = document.getId();
                        Long currentDishesLeft = document.getLong("dishesLeft");

                        if (currentDishesLeft != null) {
                            long newDishesLeft = currentDishesLeft + removedItem.getQuantity();

                            db.collection("menuItems")
                                    .document(itemId)
                                    .update("dishesLeft", newDishesLeft, "available", true)
                                    .addOnFailureListener(e -> {
                                        Toast.makeText(ShoppingCartActivity.this,
                                                "Error restoring stock", Toast.LENGTH_SHORT).show();
                                    });
                        }
                    }
                });
    }
    private void restoreDishesLeftByQuantity(String dishName, int quantity) {
        if (dishName == null || quantity <= 0) return;

        db.collection("menuItems")
                .whereEqualTo("dishName", dishName)
                .limit(1)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        DocumentSnapshot document = queryDocumentSnapshots.getDocuments().get(0);
                        String itemId = document.getId();
                        Long currentDishesLeft = document.getLong("dishesLeft");

                        if (currentDishesLeft != null) {
                            long newDishesLeft = currentDishesLeft + quantity;

                            db.collection("menuItems")
                                    .document(itemId)
                                    .update("dishesLeft", newDishesLeft, "available", true)
                                    .addOnFailureListener(e -> {
                                        Toast.makeText(ShoppingCartActivity.this,
                                                "Error restoring stock", Toast.LENGTH_SHORT).show();
                                    });
                        }
                    }
                });
    }    private CartItem findCartItemByName(String dishName) {
        if (dishName == null) return null;
        for (CartItem item : cartItems) {
            if (dishName.equalsIgnoreCase(item.getDishName())) return item;
        }
        return null;
    }

    @Override
    public void onCartUpdated() {
        updateTotalAmount();
        if (cartItems == null || cartItems.isEmpty()) {
            Toast.makeText(this, "Your cart is empty", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateTotalAmount() {
        double total = 0.0;
        if (cartItems != null) {
            for (CartItem item : cartItems) total += item.getTotalPrice();
        }
        if (tvTotalAmount != null) {
            tvTotalAmount.setText("Total: ₱" + String.format(Locale.getDefault(), "%.2f", total));
        }
    }

    public static void addToCart(CartItem item) {
        if (item == null) return;
        synchronized (staticCartList) {
            staticCartList.add(item);
        }
    }

    public static void addToStaticCart(CartItem item) {
        if (item == null) return;
        synchronized (staticCartList) {
            for (CartItem existing : staticCartList) {
                if (existing.getDishName().equals(item.getDishName())) {
                    existing.setQuantity(existing.getQuantity() + item.getQuantity());
                    existing.setTotalPrice(existing.getPrice() * existing.getQuantity());
                    return;
                }
            }
            staticCartList.add(item);
        }
    }

    public static void removeFromStaticCart(CartItem itemToRemove) {
        if (itemToRemove == null) return;
        synchronized (staticCartList) {
            staticCartList.remove(itemToRemove);
        }
    }

    public static String formatDishNameWithQuantity(String dishName, int quantity) {
        if (dishName == null) return "";
        return quantity > 1 ? dishName + " x" + quantity : dishName;
    }

    public void removeCartItem(int position) {
        if (cartItems == null) return;
        if (position >= 0 && position < cartItems.size()) {
            CartItem removedItem = cartItems.remove(position);

            restoreDishesLeft(removedItem);

            synchronized (staticCartList) {
                staticCartList.remove(removedItem);
            }
            if (cartAdapter != null) cartAdapter.notifyItemRemoved(position);
            updateTotalAmount();
            Toast.makeText(this, "Removed " + removedItem.getDishName() + " from cart", Toast.LENGTH_SHORT).show();
        }
    }

    public void updateCartItemQuantity(int position, int newQuantity) {
        if (cartItems == null) return;
        if (position >= 0 && position < cartItems.size()) {
            CartItem item = cartItems.get(position);
            int oldQuantity = item.getQuantity();
            int quantityDifference = oldQuantity - newQuantity;

            item.setQuantity(newQuantity);
            item.setTotalPrice(item.getPrice() * newQuantity);

            if (quantityDifference > 0) {
                restoreDishesLeftByQuantity(item.getDishName(), quantityDifference);
            }

            if (cartAdapter != null) cartAdapter.notifyItemChanged(position);
            updateTotalAmount();
        }
    }
}