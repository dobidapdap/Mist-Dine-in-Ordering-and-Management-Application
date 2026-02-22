package com.example.mistcaferestaurantpanglao;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private static final Pattern PASSWORD_PATTERN =
            Pattern.compile("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)[a-zA-Z\\d@$!%*?&]{8,}$");

    private MaterialButton btnUser, btnStaff;
    private FirebaseFirestore db;
    private AlertDialog currentDialog;
    private String currentUsername;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initializeFirestore();
        initializeViews();
        setupClickListeners();
    }

    private void initializeFirestore() {
        db = FirebaseFirestore.getInstance();
    }

    private void initializeViews() {
        btnUser = findViewById(R.id.btnUser);
        btnStaff = findViewById(R.id.btnStaff);
    }

    private void setupClickListeners() {
        btnUser.setOnClickListener(v -> navigateToCategories());
        btnStaff.setOnClickListener(v -> showStaffLoginDialog());
    }

    private void navigateToCategories() {
        Intent intent = new Intent(MainActivity.this, CategoriesActivity.class);
        intent.putExtra("user_type", "customer");
        startActivity(intent);
    }

    private void showStaffLoginDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_staff_login, null);
        builder.setView(dialogView);

        currentDialog = builder.create();
        setupStaffLoginDialog(dialogView);
        currentDialog.show();
    }

    private void setupStaffLoginDialog(View dialogView) {
        TextInputEditText etUsername = dialogView.findViewById(R.id.etStaffUsername);
        TextInputEditText etPassword = dialogView.findViewById(R.id.etStaffPassword);
        MaterialButton btnLogin = dialogView.findViewById(R.id.btnStaffLogin);
        TextView tvCreateAccount = dialogView.findViewById(R.id.tvCreateAccount);
        TextInputLayout tilUsername = dialogView.findViewById(R.id.tilUsername);
        TextInputLayout tilPassword = dialogView.findViewById(R.id.tilPassword);

        // Add error reset listeners
        setupErrorResetListeners(etUsername, etPassword, tilUsername, tilPassword);

        btnLogin.setOnClickListener(v -> handleStaffLogin(etUsername, etPassword, tilUsername, tilPassword, btnLogin));
        tvCreateAccount.setOnClickListener(v -> {
            dismissCurrentDialog();
            showCreateAccountDialog();
        });
    }

    private void setupErrorResetListeners(TextInputEditText etUsername, TextInputEditText etPassword,
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

    private void handleStaffLogin(TextInputEditText etUsername, TextInputEditText etPassword,
                                  TextInputLayout tilUsername, TextInputLayout tilPassword,
                                  MaterialButton btnLogin) {

        String username = getText(etUsername);
        String password = getText(etPassword);

        currentUsername = username;

        clearErrors(tilUsername, tilPassword);

        if (!validateLoginInputs(username, password, tilUsername, tilPassword)) {
            return;
        }

        setLoadingState(btnLogin, true, "Signing In...");
        authenticateUser(username, password, tilUsername, tilPassword, btnLogin);
    }

    private void authenticateUser(String username, String password,
                                  TextInputLayout tilUsername, TextInputLayout tilPassword,
                                  MaterialButton btnLogin) {

        db.collection("users").document(username).get()
                .addOnCompleteListener(task -> {
                    setLoadingState(btnLogin, false, "Sign In");

                    if (!task.isSuccessful()) {
                        showErrorDialog("Login Error", "Failed to connect to server.");
                        return;
                    }

                    DocumentSnapshot document = task.getResult();
                    if (!document.exists()) {
                        tilUsername.setErrorEnabled(true);
                        tilUsername.setError("User not found");
                        return;
                    }

                    processAuthenticationResult(document, password, tilPassword);
                });
    }

    private void processAuthenticationResult(DocumentSnapshot document, String password,
                                             TextInputLayout tilPassword) {
        String dbPassword = document.getString("password");
        String role = document.getString("role");
        String status = document.getString("status");

        if (!password.equals(dbPassword)) {
            tilPassword.setErrorEnabled(true);
            tilPassword.setError("Incorrect password");
            return;
        }

        handleUserAccess(role, status);
    }

    private void handleUserAccess(String role, String status) {
        if ("pending".equalsIgnoreCase(status)) {
            showInfoDialog("Account Pending", "Your account is pending approval.");
            return;
        }

        if ("inactive".equalsIgnoreCase(status)) {
            showInfoDialog("Account Inactive", "Your account has been deactivated.");
            return;
        }

        SharedPreferences prefs = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        prefs.edit().putString("username", getCurrentUsername()).apply();

        dismissCurrentDialog();
        Intent intent;

        if ("admin".equalsIgnoreCase(role)) {
            Toast.makeText(this, "Welcome, Admin!", Toast.LENGTH_SHORT).show();
            intent = new Intent(MainActivity.this, AdminActivity.class);
            intent.putExtra("user_type", "admin");
        } else if ("kitchen".equalsIgnoreCase(role)) {
            Toast.makeText(this, "Welcome, Kitchen!", Toast.LENGTH_SHORT).show();
            intent = new Intent(MainActivity.this, KitchenActivity.class);
            intent.putExtra("userRole", role);
            intent.putExtra("user_type", "staff");
        } else if ("counter".equalsIgnoreCase(role)) {
            Toast.makeText(this, "Welcome, Counter!", Toast.LENGTH_SHORT).show();
            intent = new Intent(MainActivity.this, CounterActivity.class);
            intent.putExtra("userRole", role);
            intent.putExtra("user_type", "staff");
        } else {
            showInfoDialog("Access Denied", "You do not have sufficient privileges.");
            return;
        }

        startActivity(intent);
    }

    private void showCreateAccountDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_create_account, null);
        builder.setView(dialogView);

        currentDialog = builder.create();
        setupCreateAccountDialog(dialogView);
        currentDialog.show();
    }

    private void setupCreateAccountDialog(View dialogView) {
        TextInputEditText etFirstName = dialogView.findViewById(R.id.etFirstName);
        TextInputEditText etLastName = dialogView.findViewById(R.id.etLastName);
        TextInputEditText etUsername = dialogView.findViewById(R.id.etUsername);
        TextInputEditText etPassword = dialogView.findViewById(R.id.etPassword);
        TextInputEditText etGmail = dialogView.findViewById(R.id.etGmail);
        AutoCompleteTextView spnRole = dialogView.findViewById(R.id.spnRole);
        MaterialButton btnCreate = dialogView.findViewById(R.id.btnCreateAccount);
        TextView tvLoginHere = dialogView.findViewById(R.id.tvLoginHere);

        TextInputLayout tilFirstName = dialogView.findViewById(R.id.tilFirstName);
        TextInputLayout tilLastName = dialogView.findViewById(R.id.tilLastName);
        TextInputLayout tilUsername = dialogView.findViewById(R.id.tilUsername);
        TextInputLayout tilPassword = dialogView.findViewById(R.id.tilPassword);
        TextInputLayout tilEmail = dialogView.findViewById(R.id.tilEmail);
        TextInputLayout tilRole = dialogView.findViewById(R.id.tilRole);

        setupRoleDropdown(spnRole);

        btnCreate.setOnClickListener(v -> handleAccountCreation(
                etFirstName, etLastName, etUsername, etPassword, etGmail, spnRole,
                tilFirstName, tilLastName, tilUsername, tilPassword, tilEmail, tilRole, btnCreate));

        tvLoginHere.setOnClickListener(v -> {
            dismissCurrentDialog();
            showStaffLoginDialog();
        });
    }

    private void setupRoleDropdown(AutoCompleteTextView spnRole) {
        String[] roles = {"Admin", "Kitchen", "Counter"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, roles);
        spnRole.setAdapter(adapter);
    }

    private void handleAccountCreation(TextInputEditText etFirstName, TextInputEditText etLastName,
                                       TextInputEditText etUsername, TextInputEditText etPassword,
                                       TextInputEditText etGmail, AutoCompleteTextView spnRole,
                                       TextInputLayout tilFirstName, TextInputLayout tilLastName,
                                       TextInputLayout tilUsername, TextInputLayout tilPassword,
                                       TextInputLayout tilEmail, TextInputLayout tilRole,
                                       MaterialButton btnCreate) {

        String firstName = getText(etFirstName);
        String lastName = getText(etLastName);
        String username = getText(etUsername);
        String password = getText(etPassword);
        String email = getText(etGmail);
        String role = getText(spnRole);

        clearErrors(tilFirstName, tilLastName, tilUsername, tilPassword, tilEmail, tilRole);

        if (!validateAccountCreationInputs(firstName, lastName, username, password, email, role,
                tilFirstName, tilLastName, tilUsername, tilPassword, tilEmail, tilRole)) {
            return;
        }

        setLoadingState(btnCreate, true, "Creating Account...");
        checkUsernameAvailabilityAndCreate(firstName, lastName, username, password, email, role,
                tilUsername, btnCreate);
    }

    private void checkUsernameAvailabilityAndCreate(String firstName, String lastName,
                                                    String username, String password, String email,
                                                    String role, TextInputLayout tilUsername,
                                                    MaterialButton btnCreate) {

        db.collection("users").document(username).get()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        setLoadingState(btnCreate, false, "Create Account");
                        showErrorDialog("Connection Error", "Failed to connect to server.");
                        return;
                    }

                    if (task.getResult().exists()) {
                        setLoadingState(btnCreate, false, "Create Account");
                        tilUsername.setError("Username already exists");
                        return;
                    }

                    createUserAccount(firstName, lastName, username, password, email, role, btnCreate);
                });
    }

    private void createUserAccount(String firstName, String lastName, String username,
                                   String password, String email, String role,
                                   MaterialButton btnCreate) {
        Map<String, Object> user = new HashMap<>();
        user.put("firstName", firstName);
        user.put("lastName", lastName);
        user.put("username", username);
        user.put("password", password);
        user.put("email", email);
        user.put("role", role.toLowerCase());
        user.put("status", determineAccountStatus(role));
        user.put("createdAt", System.currentTimeMillis());

        db.collection("users")
                .document(username)
                .set(user)
                .addOnCompleteListener(task -> {
                    setLoadingState(btnCreate, false, "Create Account");

                    if (task.isSuccessful()) {
                        handleAccountCreationSuccess(user);
                    } else {
                        showErrorDialog("Account Creation Failed", "Failed to create account.");
                    }
                });
    }

    private String determineAccountStatus(String role) {
        return "pending";
    }

    private void handleAccountCreationSuccess(Map<String, Object> user) {
        String message = "Account created successfully! ";
        message += "pending".equals(user.get("status")) ?
                "It's pending approval." : "You can now sign in.";

        showSuccessDialog("Account Created", message);
        dismissCurrentDialog();
        showStaffLoginDialog();
    }

    private boolean validateLoginInputs(String username, String password,
                                        TextInputLayout tilUsername, TextInputLayout tilPassword) {
        boolean isValid = true;

        if (TextUtils.isEmpty(username)) {
            tilUsername.setErrorEnabled(true);
            tilUsername.setError("Username is required");
            isValid = false;
        }

        if (TextUtils.isEmpty(password)) {
            tilPassword.setErrorEnabled(true);
            tilPassword.setError("Password is required");
            isValid = false;
        }

        return isValid;
    }

    private boolean validateAccountCreationInputs(String firstName, String lastName, String username,
                                                  String password, String email, String role,
                                                  TextInputLayout tilFirstName, TextInputLayout tilLastName,
                                                  TextInputLayout tilUsername, TextInputLayout tilPassword,
                                                  TextInputLayout tilEmail, TextInputLayout tilRole) {
        boolean isValid = true;

        if (TextUtils.isEmpty(firstName) || firstName.length() < 2) {
            tilFirstName.setError("First name must be at least 2 characters");
            isValid = false;
        }

        if (TextUtils.isEmpty(lastName) || lastName.length() < 2) {
            tilLastName.setError("Last name must be at least 2 characters");
            isValid = false;
        }

        if (TextUtils.isEmpty(username) || username.length() < 3) {
            tilUsername.setError("Username must be at least 3 characters");
            isValid = false;
        } else if (!username.matches("^[a-zA-Z0-9_]+$")) {
            tilUsername.setError("Username can only contain letters, numbers, and underscores");
            isValid = false;
        }

        if (TextUtils.isEmpty(password) || password.length() < 8) {
            tilPassword.setError("Password must be at least 8 characters");
            isValid = false;
        } else if (!PASSWORD_PATTERN.matcher(password).matches()) {
            tilPassword.setError("Password must contain uppercase, lowercase, and number");
            isValid = false;
        }

        if (TextUtils.isEmpty(email) || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.setError("Please enter a valid email address");
            isValid = false;
        }

        if (TextUtils.isEmpty(role)) {
            tilRole.setError("Please select a role");
            isValid = false;
        }

        return isValid;
    }

    private String getText(TextInputEditText editText) {
        return editText.getText() != null ? editText.getText().toString().trim() : "";
    }

    private String getText(AutoCompleteTextView textView) {
        return textView.getText() != null ? textView.getText().toString().trim() : "";
    }

    private void clearErrors(TextInputLayout... layouts) {
        for (TextInputLayout layout : layouts) {
            layout.setError(null);
            layout.setErrorEnabled(false);
        }
    }

    private void setLoadingState(MaterialButton button, boolean loading, String text) {
        button.setEnabled(!loading);
        button.setText(text);
    }

    private void dismissCurrentDialog() {
        if (currentDialog != null && currentDialog.isShowing()) {
            currentDialog.dismiss();
            currentDialog = null;
        }
    }

    private void showErrorDialog(String title, String message) {
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton("OK", null)
                .show();
    }

    private void showSuccessDialog(String title, String message) {
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setIcon(android.R.drawable.ic_dialog_info)
                .setPositiveButton("OK", null)
                .show();
    }

    private void showInfoDialog(String title, String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(title)
                .setMessage(message)
                .setIcon(android.R.drawable.ic_dialog_info)
                .setPositiveButton("OK", null);

        AlertDialog dialog = builder.create();
        dialog.show();

        TextView textView = dialog.findViewById(android.R.id.message);
        if (textView != null) {
            textView.setMaxLines(Integer.MAX_VALUE);
            textView.setVerticalScrollBarEnabled(true);
            textView.setMovementMethod(android.text.method.ScrollingMovementMethod.getInstance());
        }
    }

    private String getCurrentUsername() {
        return currentUsername;
    }

    @Override
    protected void onDestroy() {
        dismissCurrentDialog();
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        if (currentDialog != null && currentDialog.isShowing()) {
            dismissCurrentDialog();
        } else {
            super.onBackPressed();
        }
    }
}