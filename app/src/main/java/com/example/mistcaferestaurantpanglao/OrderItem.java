package com.example.mistcaferestaurantpanglao;

import java.io.Serializable;

public class OrderItem implements Serializable {
    private String itemName;
    private int quantity;
    private double price;
    private String notes;

    public OrderItem() {}

    public OrderItem(String itemName, int quantity, double price, String notes) {
        this.itemName = itemName;
        this.quantity = quantity;
        this.price = price;
        this.notes = notes;
    }

    public String getItemName() {
        return itemName;
    }

    public int getQuantity() {
        return quantity;
    }

    public double getPrice() {
        return price;
    }

    public String getNotes() {
        return notes;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public double getTotalPrice() {
        return price * quantity;
    }
}