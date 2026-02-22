package com.example.mistcaferestaurantpanglao;

public class RatingCommentModel {

    private String customerName;
    private String tableNumber;
    private int rating;
    private String comment;

    public RatingCommentModel(String customerName, String tableNumber, int rating, String comment) {
        this.customerName = customerName;
        this.tableNumber = tableNumber;
        this.rating = rating;
        this.comment = comment;
    }

    public String getCustomerName() {
        return customerName;
    }
    public int getRating() {
        return rating;
    }

    public String getComment() {
        return comment;
    }
}
