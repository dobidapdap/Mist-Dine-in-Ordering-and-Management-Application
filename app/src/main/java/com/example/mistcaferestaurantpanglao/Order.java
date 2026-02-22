package com.example.mistcaferestaurantpanglao;

import android.util.Log;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Order implements Serializable {
    private static final String TAG = "Order";

    private String orderId;
    private String tableNumber;
    private String status;
    private long timestamp;
    private List<OrderItem> items;
    private double totalAmount;
    private String customerName;

    // Default constructor
    public Order() {
        this.items = new ArrayList<>();
    }

    // Static method to create Order from Firestore document
    public static Order fromFirestore(DocumentSnapshot document) {
        Log.d(TAG, "Parsing document: " + document.getId());

        Order order = new Order();
        order.orderId = document.getId();
        order.tableNumber = document.getString("table");
        order.status = document.getString("status");
        order.customerName = document.getString("customerName");

        Log.d(TAG, "Basic info - Table: " + order.tableNumber + ", Status: " + order.status);

        // Handle total amount
        Object totalObj = document.get("totalAmount");
        if (totalObj instanceof Double) {
            order.totalAmount = (Double) totalObj;
        } else if (totalObj instanceof Long) {
            order.totalAmount = ((Long) totalObj).doubleValue();
        }
        Log.d(TAG, "Total amount: " + order.totalAmount);

        // Handle different timestamp formats
        Object timestampObj = document.get("timestamp");
        if (timestampObj instanceof Long) {
            order.timestamp = (long) timestampObj;
        } else if (timestampObj instanceof Timestamp) {
            Timestamp firestoreTimestamp = (Timestamp) timestampObj;
            order.timestamp = firestoreTimestamp.toDate().getTime();
        } else if (timestampObj instanceof com.google.firebase.firestore.ServerTimestamp) {
            order.timestamp = System.currentTimeMillis();
        } else {
            order.timestamp = System.currentTimeMillis();
        }

        // Parse order items - FIXED PARSING LOGIC
        order.items = new ArrayList<>();
        Object itemsObj = document.get("orderItems");

        Log.d(TAG, "Items object type: " + (itemsObj != null ? itemsObj.getClass().getSimpleName() : "null"));
        Log.d(TAG, "Items object: " + itemsObj);

        if (itemsObj instanceof List) {
            List<Map<String, Object>> itemsList = (List<Map<String, Object>>) itemsObj;
            Log.d(TAG, "Items list size: " + itemsList.size());

            for (int i = 0; i < itemsList.size(); i++) {
                Map<String, Object> itemMap = itemsList.get(i);
                Log.d(TAG, "Processing item " + i + ": " + itemMap);

                OrderItem item = new OrderItem();

                // FIXED: Item name - checking for dishName first (as shown in logs)
                String itemName = (String) itemMap.get("dishName");
                if (itemName == null) {
                    itemName = (String) itemMap.get("itemName");
                }
                if (itemName == null) {
                    itemName = (String) itemMap.get("name");
                }
                item.setItemName(itemName);
                Log.d(TAG, "Item name: " + itemName);

                // Quantity
                Object qtyObj = itemMap.get("quantity");
                if (qtyObj == null) {
                    qtyObj = itemMap.get("qty");
                }

                int quantity = 1; // Default quantity
                if (qtyObj instanceof Long) {
                    quantity = ((Long) qtyObj).intValue();
                } else if (qtyObj instanceof Integer) {
                    quantity = (Integer) qtyObj;
                } else if (qtyObj instanceof Double) {
                    quantity = ((Double) qtyObj).intValue();
                }
                item.setQuantity(quantity);
                Log.d(TAG, "Item quantity: " + quantity);

                // FIXED: Price calculation from totalPrice and quantity
                double itemPrice = 0.0;
                Object priceObj = itemMap.get("price");
                Object totalPriceObj = itemMap.get("totalPrice");

                if (priceObj instanceof Double) {
                    itemPrice = (Double) priceObj;
                } else if (priceObj instanceof Long) {
                    itemPrice = ((Long) priceObj).doubleValue();
                } else if (priceObj instanceof Integer) {
                    itemPrice = ((Integer) priceObj).doubleValue();
                } else if (totalPriceObj != null && quantity > 0) {
                    // Calculate individual price from totalPrice
                    if (totalPriceObj instanceof Double) {
                        itemPrice = (Double) totalPriceObj / quantity;
                    } else if (totalPriceObj instanceof Long) {
                        itemPrice = ((Long) totalPriceObj).doubleValue() / quantity;
                    } else if (totalPriceObj instanceof Integer) {
                        itemPrice = ((Integer) totalPriceObj).doubleValue() / quantity;
                    }
                }

                item.setPrice(itemPrice);
                Log.d(TAG, "Item price: " + itemPrice);

                // Notes
                String notes = (String) itemMap.get("notes");
                if (notes == null) {
                    notes = (String) itemMap.get("note");
                }
                item.setNotes(notes);
                Log.d(TAG, "Item notes: " + notes);

                order.items.add(item);
            }
        } else {
            Log.w(TAG, "Items field is not a List or is null");
        }

        Log.d(TAG, "Final order has " + order.items.size() + " items");
        return order;
    }

    // Getters
    public String getOrderId() {
        return orderId;
    }

    public String getTableNumber() {
        return tableNumber;
    }

    public String getStatus() {
        return status;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public List<OrderItem> getItems() {
        return items;
    }

    public double getTotalAmount() {
        return totalAmount;
    }

    public String getCustomerName() {
        return customerName;
    }

    // Setters
    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public void setTableNumber(String tableNumber) {
        this.tableNumber = tableNumber;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public void setItems(List<OrderItem> items) {
        this.items = items;
    }

    public void setTotalAmount(double totalAmount) {
        this.totalAmount = totalAmount;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    // Helper method to get total item count
    public int getTotalItemCount() {
        int total = 0;
        if (items != null) {
            for (OrderItem item : items) {
                total += item.getQuantity();
            }
        }
        return total;
    }

    // Helper method to get formatted order summary
    public String getOrderSummary() {
        StringBuilder summary = new StringBuilder();
        summary.append("Table: ").append(tableNumber).append("\n");
        summary.append("Items:\n");

        if (items != null && !items.isEmpty()) {
            for (OrderItem item : items) {
                summary.append("• ").append(item.getQuantity())
                        .append("x ").append(item.getItemName())
                        .append(" (₱").append(String.format("%.0f", item.getPrice())).append(" each)\n");
            }
        } else {
            summary.append("No items found\n");
        }

        summary.append("Total: ₱").append(String.format("%.0f", totalAmount));
        return summary.toString();
    }
}