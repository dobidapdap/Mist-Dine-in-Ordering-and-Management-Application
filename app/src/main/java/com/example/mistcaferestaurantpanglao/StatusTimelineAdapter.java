package com.example.mistcaferestaurantpanglao;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class StatusTimelineAdapter extends RecyclerView.Adapter<StatusTimelineAdapter.StatusTimelineViewHolder> {

    private static final String TAG = "StatusTimelineAdapter";

    private Context context;
    private List<StatusItem> statusItems;
    private String currentOrderStatus;
    private Date currentTimestamp;
    private SimpleDateFormat dateFormat;

    // Firestore real-time listener
    private FirebaseFirestore db;
    private ListenerRegistration statusListener;
    private String orderId;
    private String collectionPath = "orders"; // Default collection path

    private static final String[] ORDER_STATUSES = {
            "pending",
            "in progress",
            "cooking",
            "ready",
            "completed",
            "served"
    };

    private static final String[][] STATUS_MAPPING = {
            {"pending", "pending"},
            {"new", "pending"},
            {"in-progress", "in progress"},
            {"in_kitchen", "in progress"},
            {"preparing", "cooking"},
            {"cooking", "cooking"},
            {"ready", "ready"},
            {"completed", "completed"},
            {"served", "served"},
            {"delivered", "served"}
    };

    public StatusTimelineAdapter(Context context) {
        this.context = context;
        this.statusItems = new ArrayList<>();
        this.currentOrderStatus = "pending";
        this.dateFormat = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());
        this.db = FirebaseFirestore.getInstance();
        initializeStatusItems();
    }

    private void initializeStatusItems() {
        statusItems.clear();

        statusItems.add(new StatusItem("Pending", "Order received and waiting to be processed", "pending"));
        statusItems.add(new StatusItem("In Progress", "Order sent to kitchen and being worked on", "in progress"));
        statusItems.add(new StatusItem("Cooking", "Chef is currently cooking your order", "cooking"));
        statusItems.add(new StatusItem("Ready", "Order is ready for serving or pickup", "ready"));
        statusItems.add(new StatusItem("Completed", "Order completed and awaiting serving", "completed"));
        statusItems.add(new StatusItem("Served", "Order has been served to customer", "served"));

        Log.d(TAG, "Initialized " + statusItems.size() + " status items");
    }

    /**
     * Start listening to real-time status updates for a specific order
     * @param orderId The Firestore document ID of the order to monitor
     */
    public void startListening(String orderId) {
        startListening(orderId, "orders");
    }

    /**
     * Start listening to real-time status updates for a specific order
     * @param orderId The Firestore document ID of the order to monitor
     * @param collectionPath The Firestore collection path (e.g., "orders", "table_orders")
     */
    public void startListening(String orderId, String collectionPath) {
        // Stop any existing listener first
        stopListening();

        if (orderId == null || orderId.trim().isEmpty()) {
            Log.w(TAG, "Cannot start listening: orderId is null or empty");
            return;
        }

        this.orderId = orderId;
        this.collectionPath = collectionPath;

        Log.d(TAG, "Starting real-time listener for order: " + orderId + " in collection: " + collectionPath);

        statusListener = db.collection(collectionPath)
                .document(orderId)
                .addSnapshotListener((documentSnapshot, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Error listening to order updates", error);
                        return;
                    }

                    if (documentSnapshot != null && documentSnapshot.exists()) {
                        handleOrderUpdate(documentSnapshot);
                    } else {
                        Log.w(TAG, "Order document does not exist: " + orderId);
                        updateStatus("no_order", new Date());
                    }
                });
    }

    /**
     * Handle incoming order updates from Firestore
     */
    private void handleOrderUpdate(DocumentSnapshot document) {
        try {
            // Extract status field (adjust field name based on your Firestore structure)
            String status = document.getString("status");

            // Extract timestamp - try multiple possible field names
            Date timestamp = null;
            if (document.contains("updatedAt")) {
                timestamp = document.getDate("updatedAt");
            } else if (document.contains("lastUpdated")) {
                timestamp = document.getDate("lastUpdated");
            } else if (document.contains("timestamp")) {
                timestamp = document.getDate("timestamp");
            } else if (document.contains("statusUpdatedAt")) {
                timestamp = document.getDate("statusUpdatedAt");
            }

            // Use current time if no timestamp found
            if (timestamp == null) {
                timestamp = new Date();
            }

            Log.d(TAG, "Received real-time update - Status: " + status + ", Timestamp: " + timestamp);

            // Update the UI
            updateStatus(status != null ? status : "pending", timestamp);

        } catch (Exception e) {
            Log.e(TAG, "Error processing order update", e);
        }
    }

    /**
     * Stop listening to real-time updates
     * IMPORTANT: Call this in onDestroy() or onPause() to prevent memory leaks
     */
    public void stopListening() {
        if (statusListener != null) {
            Log.d(TAG, "Stopping real-time listener for order: " + orderId);
            statusListener.remove();
            statusListener = null;
        }
    }

    /**
     * Check if the adapter is currently listening for updates
     */
    public boolean isListening() {
        return statusListener != null;
    }

    public void updateStatus(String newStatus, Date timestamp) {
        try {
            String mappedStatus = mapFirestoreToTimelineStatus(newStatus);
            Log.d(TAG, "Updating status from '" + currentOrderStatus + "' to '" + mappedStatus + "' (original: '" + newStatus + "')");

            this.currentOrderStatus = mappedStatus;
            this.currentTimestamp = timestamp;
            notifyDataSetChanged();

        } catch (Exception e) {
            Log.e(TAG, "Error updating status", e);
            this.currentOrderStatus = "pending";
            this.currentTimestamp = new Date();
            notifyDataSetChanged();
        }
    }

    public void updateStatus(String newStatus, long timestamp) {
        try {
            Date date = new Date(timestamp);
            updateStatus(newStatus, date);
        } catch (Exception e) {
            Log.e(TAG, "Error converting timestamp to Date", e);
            updateStatus(newStatus, new Date());
        }
    }

    public void updateStatus(String newStatus) {
        updateStatus(newStatus, new Date());
    }

    private String mapFirestoreToTimelineStatus(String firestoreStatus) {
        if (firestoreStatus == null || firestoreStatus.trim().isEmpty()) {
            Log.w(TAG, "Status is null or empty, defaulting to 'pending'");
            return "pending";
        }

        String cleanStatus = firestoreStatus.trim().toLowerCase();

        if (cleanStatus.equals("no_order")) {
            return "no_order";
        }

        for (String[] mapping : STATUS_MAPPING) {
            if (mapping[0].equals(cleanStatus)) {
                Log.d(TAG, "Mapped Firestore status '" + cleanStatus + "' to Timeline status '" + mapping[1] + "'");
                return mapping[1];
            }
        }

        for (String timelineStatus : ORDER_STATUSES) {
            if (cleanStatus.contains(timelineStatus) || timelineStatus.contains(cleanStatus)) {
                Log.d(TAG, "Partial match: mapped '" + cleanStatus + "' to '" + timelineStatus + "'");
                return timelineStatus;
            }
        }

        Log.w(TAG, "Unknown Firestore status '" + cleanStatus + "', defaulting to 'pending'");
        return "pending";
    }

    private int getStatusIndex(String status) {
        if (status == null || status.trim().isEmpty()) return 0;

        String cleanStatus = status.trim().toLowerCase();
        for (int i = 0; i < ORDER_STATUSES.length; i++) {
            if (ORDER_STATUSES[i].equals(cleanStatus)) {
                return i;
            }
        }
        return 0;
    }

    private boolean isStatusCompleted(String status, int position) {
        try {
            if (status == null || status.trim().isEmpty() || status.equals("no_order")) {
                return false;
            }

            int currentStatusIndex = getStatusIndex(status);
            return position <= currentStatusIndex;

        } catch (Exception e) {
            Log.e(TAG, "Error checking if status is completed", e);
            return false;
        }
    }

    @NonNull
    @Override
    public StatusTimelineViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.status_timeline_item, parent, false);
        return new StatusTimelineViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull StatusTimelineViewHolder holder, int position) {
        try {
            if ("no_order".equals(currentOrderStatus)) {
                handleNoOrderUI(holder, position);
                return;
            }

            StatusItem statusItem = statusItems.get(position);
            holder.tvStatusTitle.setText(statusItem.getTitle());
            holder.tvStatusDescription.setText(statusItem.getDescription());

            boolean isCompleted = isStatusCompleted(currentOrderStatus, position);
            boolean isCurrent = isCurrentStatus(currentOrderStatus, position);
            updateStatusUI(holder, isCompleted, isCurrent);

            if (isCurrent && currentTimestamp != null) {
                holder.tvTimestamp.setVisibility(View.VISIBLE);
                holder.tvTimestamp.setText(dateFormat.format(currentTimestamp));
            } else {
                holder.tvTimestamp.setVisibility(View.GONE);
            }

        } catch (Exception e) {
            Log.e(TAG, "Error binding view holder at position " + position, e);
        }
    }

    private void handleNoOrderUI(StatusTimelineViewHolder holder, int position) {
        if (position == 0) {
            holder.tvStatusTitle.setText("No Order Found");
            holder.tvStatusDescription.setText("No matching order found for this table or user.");
            holder.tvStatusTitle.setTextColor(ContextCompat.getColor(context, android.R.color.holo_red_dark));
            holder.tvStatusDescription.setTextColor(ContextCompat.getColor(context, android.R.color.holo_red_dark));
            holder.statusIndicator.setBackgroundTintList(ContextCompat.getColorStateList(context, android.R.color.holo_red_dark));
            holder.tvTimestamp.setVisibility(View.VISIBLE);
            holder.tvTimestamp.setText(dateFormat.format(currentTimestamp != null ? currentTimestamp : new Date()));
        } else {
            holder.tvStatusTitle.setText("");
            holder.tvStatusDescription.setText("");
            holder.statusIndicator.setVisibility(View.GONE);
            holder.tvTimestamp.setVisibility(View.GONE);
        }
    }

    private boolean isCurrentStatus(String status, int position) {
        if (status == null || status.trim().isEmpty()) return position == 0;
        if (status.equals("no_order")) return position == 0;
        return position == getStatusIndex(status);
    }

    private void updateStatusUI(StatusTimelineViewHolder holder, boolean isCompleted, boolean isCurrent) {
        if (isCompleted) {
            holder.statusIndicator.setBackgroundTintList(ContextCompat.getColorStateList(context, android.R.color.holo_green_dark));
            holder.tvStatusTitle.setTextColor(ContextCompat.getColor(context, android.R.color.holo_green_dark));
            holder.tvStatusDescription.setTextColor(ContextCompat.getColor(context, android.R.color.holo_green_dark));
        } else if (isCurrent) {
            holder.statusIndicator.setBackgroundTintList(ContextCompat.getColorStateList(context, android.R.color.holo_orange_dark));
            holder.tvStatusTitle.setTextColor(ContextCompat.getColor(context, android.R.color.holo_orange_dark));
            holder.tvStatusDescription.setTextColor(ContextCompat.getColor(context, android.R.color.holo_orange_dark));
        } else {
            holder.statusIndicator.setBackgroundTintList(ContextCompat.getColorStateList(context, android.R.color.darker_gray));
            holder.tvStatusTitle.setTextColor(ContextCompat.getColor(context, android.R.color.darker_gray));
            holder.tvStatusDescription.setTextColor(ContextCompat.getColor(context, android.R.color.darker_gray));
        }
    }

    @Override
    public int getItemCount() {
        return statusItems.size();
    }

    static class StatusTimelineViewHolder extends RecyclerView.ViewHolder {
        TextView tvStatusTitle, tvStatusDescription, tvTimestamp;
        View statusIndicator;

        StatusTimelineViewHolder(@NonNull View itemView) {
            super(itemView);
            tvStatusTitle = itemView.findViewById(R.id.tv_status_title);
            tvStatusDescription = itemView.findViewById(R.id.tv_status_description);
            tvTimestamp = itemView.findViewById(R.id.tv_status_time);
            statusIndicator = itemView.findViewById(R.id.timeline_dot);
        }
    }

    static class StatusItem {
        private final String title;
        private final String description;
        private final String statusKey;

        StatusItem(String title, String description, String statusKey) {
            this.title = title;
            this.description = description;
            this.statusKey = statusKey;
        }

        public String getTitle() { return title; }
        public String getDescription() { return description; }
        public String getStatusKey() { return statusKey; }
    }
}