package com.example.mistcaferestaurantpanglao;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class UserAdapter extends RecyclerView.Adapter<UserAdapter.UserViewHolder> {

    public interface OnUserActionListener {
        void onEdit(User user);
        void onDelete(User user);
        void onAccept(User user);
        void onReject(User user);
        void onResetPassword(User user);
        void onChangeUsername(User user);
    }

    private final List<User> userList;
    private final OnUserActionListener listener;
    private final boolean isActiveList;

    public UserAdapter(List<User> userList, OnUserActionListener listener, boolean isActiveList) {
        this.userList = userList;
        this.listener = listener;
        this.isActiveList = isActiveList;
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_user, parent, false);
        return new UserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        User user = userList.get(position);
        Context context = holder.itemView.getContext();

        holder.tvUsername.setText(user.getUsername());
        holder.tvRole.setText(user.getRole());

        // Set status text
        if (isActiveList) {
            holder.tvStatus.setText("Active");
            holder.tvStatus.setTextColor(ContextCompat.getColor(context, android.R.color.holo_green_dark));
        } else {
            holder.tvStatus.setText("Pending");
            holder.tvStatus.setTextColor(ContextCompat.getColor(context, android.R.color.holo_orange_dark));
        }

        // Set click listener on the entire item to show user details
        holder.itemView.setOnClickListener(v -> showUserDetailsDialog(v, user));

        if (isActiveList) {
            // Active accounts: Show Edit & Delete buttons
            holder.btnEdit.setVisibility(View.VISIBLE);
            holder.btnDeleteAccept.setVisibility(View.VISIBLE);
            holder.btnDeleteAccept.setText("Delete");
            holder.btnReject.setVisibility(View.GONE);

            holder.btnEdit.setOnClickListener(v -> showEditDialog(v, user));
            holder.btnDeleteAccept.setOnClickListener(v -> listener.onDelete(user));
        } else {
            // Pending accounts: Show Accept & Reject buttons, hide edit
            holder.btnEdit.setVisibility(View.GONE);
            holder.btnDeleteAccept.setVisibility(View.VISIBLE);
            holder.btnDeleteAccept.setText("Accept");

            holder.btnDeleteAccept.setOnClickListener(v -> listener.onAccept(user));

            holder.btnReject.setVisibility(View.VISIBLE);
            holder.btnReject.setOnClickListener(v -> listener.onReject(user));
        }
    }

    private void showEditDialog(View view, User user) {
        AlertDialog.Builder builder = new AlertDialog.Builder(view.getContext());
        builder.setTitle("Edit User: " + user.getUsername());
        builder.setMessage("Choose an action:");

        builder.setPositiveButton("Reset Password", (dialog, which) -> {
            listener.onResetPassword(user);
        });

        builder.setNegativeButton("Change Username", (dialog, which) -> {
            listener.onChangeUsername(user);
        });

        builder.setNeutralButton("Cancel", (dialog, which) -> {
            dialog.dismiss();
        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void showUserDetailsDialog(View view, User user) {
        AlertDialog.Builder builder = new AlertDialog.Builder(view.getContext());
        builder.setTitle("Account Details");

        StringBuilder details = new StringBuilder();
        details.append("Username: ").append(user.getUsername()).append("\n\n");

        String fullName = user.getFullName();
        if (fullName != null && !fullName.isEmpty() && !fullName.equals(user.getUsername())) {
            details.append("Full Name: ").append(fullName).append("\n\n");
        } else {
            details.append("Full Name: Not provided\n\n");
        }

        if (user.getEmail() != null && !user.getEmail().isEmpty()) {
            details.append("Email: ").append(user.getEmail()).append("\n\n");
        } else {
            details.append("Email: Not provided\n\n");
        }

        details.append("Role: ").append(user.getRole()).append("\n\n");
        details.append("Status: ").append(isActiveList ? "Active" : "Pending");

        builder.setMessage(details.toString());
        builder.setPositiveButton("Close", (dialog, which) -> dialog.dismiss());

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    @Override
    public int getItemCount() {
        return userList.size();
    }

    static class UserViewHolder extends RecyclerView.ViewHolder {
        TextView tvUsername, tvRole, tvStatus;
        Button btnEdit, btnDeleteAccept, btnReject;

        UserViewHolder(@NonNull View itemView) {
            super(itemView);
            tvUsername = itemView.findViewById(R.id.tvUsername);
            tvRole = itemView.findViewById(R.id.tvRole);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            btnEdit = itemView.findViewById(R.id.btnEdit);
            btnDeleteAccept = itemView.findViewById(R.id.btnDeleteAccept);
            btnReject = itemView.findViewById(R.id.btnReject);
        }
    }
}