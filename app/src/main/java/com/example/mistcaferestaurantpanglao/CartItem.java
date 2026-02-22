package com.example.mistcaferestaurantpanglao;

public class CartItem {
    private String dishName;
    private double price;
    private int quantity;
    private double totalPrice;
    private String imageUri;

    public CartItem(String dishName, double price, int quantity, double totalPrice, String imageUri, String description) {
        this.dishName = dishName;
        this.price = price;
        this.quantity = quantity;
        this.totalPrice = price * quantity;
        this.imageUri = imageUri;
    }

    public CartItem(String dishName, double price, int quantity, String imageUri) {
        this.dishName = dishName;
        this.price = price;
        this.quantity = quantity;
        this.totalPrice = price * quantity;
        this.imageUri = imageUri;
    }

    public String getDishName() {
        return dishName;
    }

    public void setDishName(String dishName) {
        this.dishName = dishName;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
        updateTotalPrice();
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
        updateTotalPrice();
    }

    public double getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(double totalPrice) {
        this.totalPrice = totalPrice;
    }

    public String getImageUri() {
        return imageUri;
    }

    public void setImageUri(String imageUri) {
        this.imageUri = imageUri;
    }

    private void updateTotalPrice() {
        this.totalPrice = this.price * this.quantity;
    }
}