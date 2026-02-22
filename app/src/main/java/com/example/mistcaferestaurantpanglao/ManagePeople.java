package com.example.mistcaferestaurantpanglao;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.InputType;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class ManagePeople extends AppCompatActivity {

    private RecyclerView rvActiveAccounts, rvPendingAccounts;
    private UserAdapter activeAdapter, pendingAdapter;
    private ImageButton btnBack;

    private List<User> activeList = new ArrayList<>();
    private List<User> pendingList = new ArrayList<>();

    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_people);

        db = FirebaseFirestore.getInstance();

        initializeViews();
        setupClickListeners();
        loadUsers();
    }

    private void initializeViews() {
        rvActiveAccounts = findViewById(R.id.rvActiveAccounts);
        rvPendingAccounts = findViewById(R.id.rvPendingAccounts);
        btnBack = findViewById(R.id.btnBack);

        rvActiveAccounts.setLayoutManager(new LinearLayoutManager(this));
        rvPendingAccounts.setLayoutManager(new LinearLayoutManager(this));
    }

    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> onBackPressed());
    }

    private void loadUsers() {
        db.collection("users")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    activeList.clear();
                    pendingList.clear();

                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        User user = doc.toObject(User.class);
                        if (user != null) {
                            user.setUsername(doc.getId());
                            if ("pending".equalsIgnoreCase(user.getStatus())) {
                                pendingList.add(user);
                            } else {
                                activeList.add(user);
                            }
                        }
                    }

                    setupAdapters();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to load users: " + e.getMessage(), Toast.LENGTH_LONG).show()
                );
    }

    private void setupAdapters() {
        UserAdapter.OnUserActionListener activeListener = new UserAdapter.OnUserActionListener() {
            @Override
            public void onEdit(User user) {
            }

            @Override
            public void onDelete(User user) {
                db.collection("users").document(user.getUsername())
                        .delete()
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(ManagePeople.this, "Deleted user: " + user.getUsername(), Toast.LENGTH_SHORT).show();
                            loadUsers();
                        })
                        .addOnFailureListener(e -> Toast.makeText(ManagePeople.this, "Failed to delete: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onAccept(User user) {
            }

            @Override
            public void onReject(User user) {
            }

            @Override
            public void onResetPassword(User user) {
                showResetPasswordDialog(user);
            }

            @Override
            public void onChangeUsername(User user) {
                showChangeUsernameDialog(user);
            }
        };

        UserAdapter.OnUserActionListener pendingListener = new UserAdapter.OnUserActionListener() {
            @Override
            public void onEdit(User user) {
            }

            @Override
            public void onDelete(User user) {
            }

            @Override
            public void onAccept(User user) {
                db.collection("users").document(user.getUsername())
                        .update("status", "active")
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(ManagePeople.this, "User accepted: " + user.getUsername(), Toast.LENGTH_SHORT).show();
                            loadUsers();
                        })
                        .addOnFailureListener(e -> Toast.makeText(ManagePeople.this, "Failed to accept: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onReject(User user) {
                db.collection("users").document(user.getUsername())
                        .delete()
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(ManagePeople.this, "User rejected: " + user.getUsername(), Toast.LENGTH_SHORT).show();
                            loadUsers();
                        })
                        .addOnFailureListener(e -> Toast.makeText(ManagePeople.this, "Failed to reject: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResetPassword(User user) {
            }

            @Override
            public void onChangeUsername(User user) {
            }
        };

        activeAdapter = new UserAdapter(activeList, activeListener, true);
        pendingAdapter = new UserAdapter(pendingList, pendingListener, false);

        rvActiveAccounts.setAdapter(activeAdapter);
        rvPendingAccounts.setAdapter(pendingAdapter);
    }

    private void showResetPasswordDialog(User user) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Reset Password");
        builder.setMessage("Enter new password for " + user.getUsername());

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        input.setHint("New password");
        builder.setView(input);

        builder.setPositiveButton("Reset", (dialog, which) -> {
            String newPassword = input.getText().toString().trim();
            if (newPassword.isEmpty()) {
                Toast.makeText(ManagePeople.this, "Password cannot be empty", Toast.LENGTH_SHORT).show();
                return;
            }

            db.collection("users").document(user.getUsername())
                    .update("password", newPassword)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(ManagePeople.this, "Password reset successfully for " + user.getUsername(), Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(ManagePeople.this, "Failed to reset password: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    private void showChangeUsernameDialog(User user) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Change Username");
        builder.setMessage("Enter new username for " + user.getUsername());

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setHint("New username");
        input.setText(user.getUsername());
        builder.setView(input);

        builder.setPositiveButton("Change", (dialog, which) -> {
            String newUsername = input.getText().toString().trim();
            if (newUsername.isEmpty()) {
                Toast.makeText(ManagePeople.this, "Username cannot be empty", Toast.LENGTH_SHORT).show();
                return;
            }

            if (newUsername.equals(user.getUsername())) {
                Toast.makeText(ManagePeople.this, "New username is the same as current username", Toast.LENGTH_SHORT).show();
                return;
            }

            db.collection("users").document(newUsername)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            Toast.makeText(ManagePeople.this, "Username already exists", Toast.LENGTH_SHORT).show();
                        } else {
                            db.collection("users").document(user.getUsername())
                                    .get()
                                    .addOnSuccessListener(oldDoc -> {
                                        if (oldDoc.exists()) {
                                            db.collection("users").document(newUsername)
                                                    .set(oldDoc.getData())
                                                    .addOnSuccessListener(aVoid -> {
                                                        db.collection("users").document(user.getUsername())
                                                                .delete()
                                                                .addOnSuccessListener(aVoid1 -> {
                                                                    Toast.makeText(ManagePeople.this, "Username changed successfully", Toast.LENGTH_SHORT).show();
                                                                    loadUsers();
                                                                })
                                                                .addOnFailureListener(e -> {
                                                                    Toast.makeText(ManagePeople.this, "Failed to delete old username: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                                                });
                                                    })
                                                    .addOnFailureListener(e -> {
                                                        Toast.makeText(ManagePeople.this, "Failed to create new username: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                                    });
                                        }
                                    });
                        }
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(ManagePeople.this, "Failed to check username: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
        builder.show();
    }
}