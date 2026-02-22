package com.example.mistcaferestaurantpanglao;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class KitchenOrder {
    private String orderId;
    private String customerName;
    private String items;
    private String orderTime;
    private String status;
    private String tableNumber;
    private String specialInstructions;
    private int preparationTime;
    private boolean isUrgent;
    private double totalAmount;
    private List<OrderItem> orderItems; // Keep reference to original items
    private long timestamp;

    // Constructor
    public KitchenOrder(String orderId, String customerName, String items, String orderTime,
                        String status, String tableNumber, String specialInstructions,
                        int preparationTime, boolean isUrgent, double totalAmount) {
        this.orderId = orderId;
        this.customerName = customerName;
        this.items = items;
        this.orderTime = orderTime;
        this.status = status;
        this.tableNumber = tableNumber;
        this.specialInstructions = specialInstructions;
        this.preparationTime = preparationTime;
        this.isUrgent = isUrgent;
        this.totalAmount = totalAmount;
    }

    // Enhanced constructor with OrderItems
    public KitchenOrder(String orderId, String customerName, String items, String orderTime,
                        String status, String tableNumber, String specialInstructions,
                        int preparationTime, boolean isUrgent, double totalAmount,
                        List<OrderItem> orderItems, long timestamp) {
        this(orderId, customerName, items, orderTime, status, tableNumber,
                specialInstructions, preparationTime, isUrgent, totalAmount);
        this.orderItems = orderItems;
        this.timestamp = timestamp;
    }

    // Static method to create KitchenOrder from Order
    public static KitchenOrder fromOrder(Order order) {
        // Format order time
        SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.getDefault());
        String orderTime = sdf.format(new Date(order.getTimestamp()));

        // Build items string with better formatting
        StringBuilder itemsBuilder = new StringBuilder();
        if (order.getItems() != null && !order.getItems().isEmpty()) {
            for (int i = 0; i < order.getItems().size(); i++) {
                OrderItem item = order.getItems().get(i);
                if (i > 0) itemsBuilder.append(", ");
                itemsBuilder.append(item.getQuantity())
                        .append("x ")
                        .append(item.getItemName());

                // Add price info for kitchen staff
                if (item.getPrice() > 0) {
                    itemsBuilder.append(" (₱").append(String.format("%.0f", item.getPrice())).append(")");
                }
            }
        } else {
            itemsBuilder.append("No items");
        }

        // Collect special instructions from order items
        StringBuilder instructionsBuilder = new StringBuilder();
        if (order.getItems() != null) {
            for (OrderItem item : order.getItems()) {
                if (item.getNotes() != null && !item.getNotes().trim().isEmpty()) {
                    if (instructionsBuilder.length() > 0) {
                        instructionsBuilder.append("; ");
                    }
                    instructionsBuilder.append(item.getItemName()).append(": ").append(item.getNotes());
                }
            }
        }

        // Calculate estimated preparation time based on item count and complexity
        int totalItems = order.getTotalItemCount();
        int estimatedPrepTime = Math.max(10, totalItems * 3); // Base: 3 minutes per item, minimum 10 minutes

        // Determine if urgent (orders older than 20 minutes for in-progress, 45 minutes for cooking)
        long currentTime = System.currentTimeMillis();
        long orderAge = currentTime - order.getTimestamp();
        boolean isUrgent = false;

        if ("in-progress".equals(order.getStatus())) {
            isUrgent = orderAge > (20 * 60 * 1000); // 20 minutes for in-progress orders
        } else if ("cooking".equals(order.getStatus())) {
            isUrgent = orderAge > (45 * 60 * 1000); // 45 minutes for cooking orders
        } else if ("ready".equals(order.getStatus())) {
            isUrgent = orderAge > (10 * 60 * 1000); // 10 minutes for ready orders
        }

        return new KitchenOrder(
                order.getOrderId(),
                order.getCustomerName() != null ? order.getCustomerName() : "Walk-in Customer",
                itemsBuilder.toString(),
                orderTime,
                order.getStatus(),
                order.getTableNumber() != null ? order.getTableNumber() : "N/A",
                instructionsBuilder.toString(),
                estimatedPrepTime,
                isUrgent,
                order.getTotalAmount(),
                order.getItems(),
                order.getTimestamp()
        );
    }

    // Helper method to get formatted order age
    public String getOrderAge() {
        if (timestamp == 0) return "";

        long currentTime = System.currentTimeMillis();
        long ageInMinutes = (currentTime - timestamp) / (60 * 1000);

        if (ageInMinutes < 60) {
            return ageInMinutes + " min ago";
        } else {
            long hours = ageInMinutes / 60;
            long minutes = ageInMinutes % 60;
            return hours + "h " + minutes + "m ago";
        }
    }

    // Helper method to get detailed items breakdown
    public String getDetailedItems() {
        if (orderItems == null || orderItems.isEmpty()) {
            return items;
        }

        StringBuilder detailed = new StringBuilder();
        for (int i = 0; i < orderItems.size(); i++) {
            OrderItem item = orderItems.get(i);
            if (i > 0) detailed.append("\n");
            detailed.append("• ").append(item.getQuantity())
                    .append("x ").append(item.getItemName());

            if (item.getNotes() != null && !item.getNotes().trim().isEmpty()) {
                detailed.append(" - ").append(item.getNotes());
            }
        }
        return detailed.toString();
    }

    // Getters
    public String getOrderId() { return orderId; }
    public String getCustomerName() { return customerName; }
    public String getItems() { return items; }
    public String getOrderTime() { return orderTime; }
    public String getStatus() { return status; }
    public String getTableNumber() { return tableNumber; }
    public String getSpecialInstructions() { return specialInstructions; }
    public int getPreparationTime() { return preparationTime; }
    public boolean isUrgent() { return isUrgent; }
    public double getTotalAmount() { return totalAmount; }
    public List<OrderItem> getOrderItems() { return orderItems; }
    public long getTimestamp() { return timestamp; }

    // Setters
    public void setOrderId(String orderId) { this.orderId = orderId; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }
    public void setItems(String items) { this.items = items; }
    public void setOrderTime(String orderTime) { this.orderTime = orderTime; }
    public void setStatus(String status) { this.status = status; }
    public void setTableNumber(String tableNumber) { this.tableNumber = tableNumber; }
    public void setSpecialInstructions(String specialInstructions) { this.specialInstructions = specialInstructions; }
    public void setPreparationTime(int preparationTime) { this.preparationTime = preparationTime; }
    public void setUrgent(boolean urgent) { isUrgent = urgent; }
    public void setTotalAmount(double totalAmount) { this.totalAmount = totalAmount; }
    public void setOrderItems(List<OrderItem> orderItems) { this.orderItems = orderItems; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}