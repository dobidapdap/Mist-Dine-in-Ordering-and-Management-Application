package com.example.mistcaferestaurantpanglao;

public class User {
    private String firstName;
    private String lastName;
    private String username;
    private String email;
    private String role;
    private String status;

    public User() {}

    public User(String firstName, String lastName, String username, String email, String role, String status) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.username = username;
        this.email = email;
        this.role = role;
        this.status = status;
    }

    // Getters
    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    public String getRole() {
        return role;
    }

    public String getStatus() {
        return status;
    }

    // Setters
    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getFullName() {
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

    public String getDisplayInitial() {
        String fullName = getFullName();
        if (fullName != null && !fullName.isEmpty()) {
            return fullName.substring(0, 1).toUpperCase();
        }
        return "?";
    }

    public boolean isActive() {
        return "active".equalsIgnoreCase(status);
    }

    @Override
    public String toString() {
        return "User{" +
                "firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", username='" + username + '\'' +
                ", email='" + email + '\'' +
                ", role='" + role + '\'' +
                ", status='" + status + '\'' +
                '}';
    }
}