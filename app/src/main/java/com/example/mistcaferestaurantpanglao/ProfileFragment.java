package com.example.mistcaferestaurantpanglao;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class ProfileFragment extends Fragment {

    private static final String TAG = "ProfileFragment";

    private TextView textUsername, textEmail;
    private TextView profileTitle, profileImageText;
    private TextView textMemberSince, textLastLogin;
    private Button btnEditProfile, btnChangePassword;
    private MaterialButton btnLogout;

    private FirebaseFirestore db;
    private String username;

    public ProfileFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = FirebaseFirestore.getInstance();

        SharedPreferences prefs = requireContext().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        username = prefs.getString("username", null);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        textUsername = view.findViewById(R.id.textUsername);
        textEmail = view.findViewById(R.id.textEmail);
        profileTitle = view.findViewById(R.id.profileTitle);
        profileImageText = view.findViewById(R.id.profileImageText);
        textMemberSince = view.findViewById(R.id.textMemberSince);
        textLastLogin = view.findViewById(R.id.textLastLogin);
        btnEditProfile = view.findViewById(R.id.btnEditProfile);
        btnChangePassword = view.findViewById(R.id.btnChangePassword);
        btnLogout = view.findViewById(R.id.btnLogout);

        btnEditProfile.setOnClickListener(v -> showEditProfileDialog());
        btnChangePassword.setOnClickListener(v -> showChangePasswordDialog());

        btnLogout.setOnClickListener(v -> showLogoutConfirmationDialog());

        loadUserProfile();

        return view;
    }
    private void showLogoutConfirmationDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Logout", (dialog, which) -> performLogout())
                .setNegativeButton("Cancel", null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }
    private void performLogout() {
        SharedPreferences prefs = requireContext().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.clear();
        editor.apply();

        Log.d(TAG, "User logged out: " + username);
        Toast.makeText(requireContext(), "Logged out successfully", Toast.LENGTH_SHORT).show();

        Intent intent = new Intent(requireActivity(), MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        requireActivity().finish();
    }

    private void loadUserProfile() {
        if (username == null || username.isEmpty()) {
            showError("No logged-in user found");
            return;
        }

        setLoadingState(true);

        db.collection("users")
                .document(username)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    setLoadingState(false);

                    if (documentSnapshot.exists()) {
                        String firstName = documentSnapshot.getString("firstName");
                        String lastName = documentSnapshot.getString("lastName");
                        String email = documentSnapshot.getString("email");
                        String role = documentSnapshot.getString("role");
                        String status = documentSnapshot.getString("status");
                        Long createdAt = documentSnapshot.getLong("createdAt");

                        String fullName = buildFullName(firstName, lastName);

                        updateProfileUI(fullName, email, role, status);

                        updateAccountDetails(createdAt);

                    } else {
                        showError("User profile not found");
                    }
                })
                .addOnFailureListener(e -> {
                    setLoadingState(false);
                    showError("Error loading profile");
                    Log.e(TAG, "Firestore error", e);
                });
    }

    private void showEditProfileDialog() {
        if (username == null || username.isEmpty()) {
            Toast.makeText(getContext(), "No user logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("users")
                .document(username)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String currentFirstName = documentSnapshot.getString("firstName");
                        String currentLastName = documentSnapshot.getString("lastName");
                        String currentEmail = documentSnapshot.getString("email");

                        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
                        builder.setTitle("Edit Profile");

                        LinearLayout layout = new LinearLayout(requireContext());
                        layout.setOrientation(LinearLayout.VERTICAL);
                        layout.setPadding(50, 40, 50, 10);

                        TextView labelFirstName = new TextView(requireContext());
                        labelFirstName.setText("First Name");
                        labelFirstName.setTextSize(14);
                        labelFirstName.setTypeface(null, Typeface.BOLD);
                        labelFirstName.setPadding(0, 10, 0, 5);
                        layout.addView(labelFirstName);

                        final EditText inputFirstName = new EditText(requireContext());
                        inputFirstName.setHint("Enter first name");
                        inputFirstName.setText(currentFirstName != null ? currentFirstName : "");
                        inputFirstName.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
                        layout.addView(inputFirstName);

                        TextView labelLastName = new TextView(requireContext());
                        labelLastName.setText("Last Name");
                        labelLastName.setTextSize(14);
                        labelLastName.setTypeface(null, Typeface.BOLD);
                        labelLastName.setPadding(0, 20, 0, 5);
                        layout.addView(labelLastName);

                        final EditText inputLastName = new EditText(requireContext());
                        inputLastName.setHint("Enter last name");
                        inputLastName.setText(currentLastName != null ? currentLastName : "");
                        inputLastName.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
                        layout.addView(inputLastName);

                        TextView labelEmail = new TextView(requireContext());
                        labelEmail.setText("Email");
                        labelEmail.setTextSize(14);
                        labelEmail.setTypeface(null, Typeface.BOLD);
                        labelEmail.setPadding(0, 20, 0, 5);
                        layout.addView(labelEmail);

                        final EditText inputEmail = new EditText(requireContext());
                        inputEmail.setHint("Enter email address");
                        inputEmail.setText(currentEmail != null ? currentEmail : "");
                        inputEmail.setInputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
                        layout.addView(inputEmail);

                        builder.setView(layout);

                        builder.setPositiveButton("Save", (dialog, which) -> {
                            String newFirstName = inputFirstName.getText().toString().trim();
                            String newLastName = inputLastName.getText().toString().trim();
                            String newEmail = inputEmail.getText().toString().trim();

                            if (newEmail.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(newEmail).matches()) {
                                Toast.makeText(getContext(), "Please enter a valid email", Toast.LENGTH_SHORT).show();
                                return;
                            }

                            Map<String, Object> updates = new HashMap<>();
                            updates.put("firstName", newFirstName);
                            updates.put("lastName", newLastName);
                            updates.put("email", newEmail);

                            db.collection("users").document(username)
                                    .update(updates)
                                    .addOnSuccessListener(aVoid -> {
                                        Toast.makeText(getContext(), "Profile updated successfully", Toast.LENGTH_SHORT).show();
                                        loadUserProfile();
                                    })
                                    .addOnFailureListener(e -> {
                                        Toast.makeText(getContext(), "Failed to update profile: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                        Log.e(TAG, "Update error", e);
                                    });
                        });

                        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
                        builder.show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Failed to load user data", Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Fetch error", e);
                });
    }

    private void showChangePasswordDialog() {
        if (username == null || username.isEmpty()) {
            Toast.makeText(getContext(), "No user logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Change Password");

        LinearLayout layout = new LinearLayout(requireContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 10);

        TextView labelCurrentPassword = new TextView(requireContext());
        labelCurrentPassword.setText("Current Password");
        labelCurrentPassword.setTextSize(14);
        labelCurrentPassword.setTypeface(null, Typeface.BOLD);
        labelCurrentPassword.setPadding(0, 10, 0, 5);
        layout.addView(labelCurrentPassword);

        final EditText inputCurrentPassword = new EditText(requireContext());
        inputCurrentPassword.setHint("Enter current password");
        inputCurrentPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        layout.addView(inputCurrentPassword);

        TextView labelNewPassword = new TextView(requireContext());
        labelNewPassword.setText("New Password");
        labelNewPassword.setTextSize(14);
        labelNewPassword.setTypeface(null, Typeface.BOLD);
        labelNewPassword.setPadding(0, 20, 0, 5);
        layout.addView(labelNewPassword);

        final EditText inputNewPassword = new EditText(requireContext());
        inputNewPassword.setHint("Enter new password");
        inputNewPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        layout.addView(inputNewPassword);

        TextView labelConfirmPassword = new TextView(requireContext());
        labelConfirmPassword.setText("Confirm New Password");
        labelConfirmPassword.setTextSize(14);
        labelConfirmPassword.setTypeface(null, Typeface.BOLD);
        labelConfirmPassword.setPadding(0, 20, 0, 5);
        layout.addView(labelConfirmPassword);

        final EditText inputConfirmPassword = new EditText(requireContext());
        inputConfirmPassword.setHint("Re-enter new password");
        inputConfirmPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        layout.addView(inputConfirmPassword);

        builder.setView(layout);

        builder.setPositiveButton("Change", (dialog, which) -> {
            String currentPassword = inputCurrentPassword.getText().toString().trim();
            String newPassword = inputNewPassword.getText().toString().trim();
            String confirmPassword = inputConfirmPassword.getText().toString().trim();

            if (currentPassword.isEmpty() || newPassword.isEmpty() || confirmPassword.isEmpty()) {
                Toast.makeText(getContext(), "All fields are required", Toast.LENGTH_SHORT).show();
                return;
            }

            if (newPassword.length() < 6) {
                Toast.makeText(getContext(), "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!newPassword.equals(confirmPassword)) {
                Toast.makeText(getContext(), "New passwords do not match", Toast.LENGTH_SHORT).show();
                return;
            }

            db.collection("users").document(username)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String storedPassword = documentSnapshot.getString("password");

                            if (storedPassword != null && storedPassword.equals(currentPassword)) {
                                db.collection("users").document(username)
                                        .update("password", newPassword)
                                        .addOnSuccessListener(aVoid -> {
                                            Toast.makeText(getContext(), "Password changed successfully", Toast.LENGTH_SHORT).show();
                                        })
                                        .addOnFailureListener(e -> {
                                            Toast.makeText(getContext(), "Failed to change password: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                            Log.e(TAG, "Password update error", e);
                                        });
                            } else {
                                Toast.makeText(getContext(), "Current password is incorrect", Toast.LENGTH_SHORT).show();
                            }
                        }
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(getContext(), "Error verifying password", Toast.LENGTH_SHORT).show();
                        Log.e(TAG, "Fetch error", e);
                    });
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    private String buildFullName(String firstName, String lastName) {
        StringBuilder fullNameBuilder = new StringBuilder();

        if (firstName != null && !firstName.trim().isEmpty()) {
            fullNameBuilder.append(firstName.trim());
        }

        if (lastName != null && !lastName.trim().isEmpty()) {
            if (fullNameBuilder.length() > 0) {
                fullNameBuilder.append(" ");
            }
            fullNameBuilder.append(lastName.trim());
        }

        String fullName = fullNameBuilder.toString();
        return fullName.isEmpty() ? username : fullName;
    }

    private void updateProfileUI(String fullName, String email, String role, String status) {
        textUsername.setText(username != null && !username.isEmpty() ? username : "N/A");

        textEmail.setText(email != null && !email.isEmpty() ? email : "N/A");

        profileTitle.setText(fullName != null && !fullName.isEmpty() ? fullName : username);

        String displayText = fullName != null && !fullName.isEmpty() ? fullName : username;
        if (displayText != null && !displayText.isEmpty()) {
            String firstLetter = displayText.substring(0, 1).toUpperCase();
            profileImageText.setText(firstLetter);
        } else {
            profileImageText.setText("?");
        }
    }

    private void updateAccountDetails(Long createdAt) {
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy 'at' h:mm a", Locale.getDefault());
        String currentTime = sdf.format(new Date());
        textLastLogin.setText(currentTime);

        if (createdAt != null) {
            SimpleDateFormat memberSinceSdf = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());
            String memberSinceDate = memberSinceSdf.format(new Date(createdAt));
            textMemberSince.setText(memberSinceDate);
        } else {
            textMemberSince.setText("Unknown");
        }
    }

    private void setLoadingState(boolean isLoading) {
        if (isLoading) {
            textUsername.setText("Loading...");
            textEmail.setText("Loading...");
            profileTitle.setText("Loading...");
            textMemberSince.setText("Loading...");
            textLastLogin.setText("Loading...");
        }
    }

    private void showError(String message) {
        if (getContext() != null) {
            Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
        }

        textUsername.setText(username != null ? username : "Unknown User");
        textEmail.setText("N/A");
        profileTitle.setText("Unknown User");
        textMemberSince.setText("N/A");
        textLastLogin.setText("N/A");

        if (username != null && !username.isEmpty()) {
            profileImageText.setText(username.substring(0, 1).toUpperCase());
        } else {
            profileImageText.setText("?");
        }
    }

    public void refreshProfile() {
        loadUserProfile();
    }
}