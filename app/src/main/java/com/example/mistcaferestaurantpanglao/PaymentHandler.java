package com.example.mistcaferestaurantpanglao;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Transaction;

import java.util.HashMap;
import java.util.Map;

public class PaymentHandler {
    private final FirebaseFirestore db;

    public interface PaymentCallback {
        void onSuccess();
        void onFailure(String error);
    }

    public PaymentHandler() {
        this.db = FirebaseFirestore.getInstance();
    }

    /**
     * Process payment and move order to archivedOrders collection
     */
    public void processPayment(String orderId, PaymentCallback callback) {
        DocumentReference orderRef = db.collection("orders").document(orderId);

        orderRef.get()
                .addOnSuccessListener(orderDoc -> {
                    if (!orderDoc.exists()) {
                        callback.onFailure("Order not found");
                        return;
                    }

                    archiveOrder(orderDoc, callback);
                })
                .addOnFailureListener(e -> {
                    callback.onFailure("Error fetching order: " + e.getMessage());
                });
    }

    /**
     * Archive order to archivedOrders collection and delete from orders collection
     */
    private void archiveOrder(DocumentSnapshot orderDoc, PaymentCallback callback) {
        String orderId = orderDoc.getId();
        long currentTime = System.currentTimeMillis();

        db.runTransaction((Transaction.Function<Void>) transaction -> {
                    Map<String, Object> archivedOrder = new HashMap<>();

                    // Copy all existing order data
                    Map<String, Object> orderData = orderDoc.getData();
                    if (orderData != null) {
                        archivedOrder.putAll(orderData);
                    }

                    // Add payment-related fields with Long timestamps
                    archivedOrder.put("status", "paid");
                    archivedOrder.put("paidAt", currentTime);
                    archivedOrder.put("archivedAt", currentTime);
                    archivedOrder.put("orderId", orderId);

                    // Ensure timestamp is Long
                    Object existingTimestamp = archivedOrder.get("timestamp");
                    if (existingTimestamp == null) {
                        archivedOrder.put("timestamp", currentTime);
                    } else if (existingTimestamp instanceof com.google.firebase.Timestamp) {
                        long timestampMs = ((com.google.firebase.Timestamp) existingTimestamp).toDate().getTime();
                        archivedOrder.put("timestamp", timestampMs);
                    } else if (!(existingTimestamp instanceof Long)) {
                        archivedOrder.put("timestamp", currentTime);
                    }

                    // Write to archivedOrders collection
                    DocumentReference archivedRef = db.collection("archivedOrders").document(orderId);
                    transaction.set(archivedRef, archivedOrder);

                    // Delete from orders collection
                    DocumentReference orderRef = db.collection("orders").document(orderId);
                    transaction.delete(orderRef);

                    return null;
                })
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onFailure("Failed to archive order: " + e.getMessage()));
    }

    /**
     * Alternative method: Just mark as paid without archiving
     */
    public void markAsPaid(String orderId, PaymentCallback callback) {
        long currentTime = System.currentTimeMillis();

        Map<String, Object> updates = new HashMap<>();
        updates.put("status", "paid");
        updates.put("paidAt", currentTime);

        db.collection("orders")
                .document(orderId)
                .update(updates)
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onFailure("Failed to update order: " + e.getMessage()));
    }
}