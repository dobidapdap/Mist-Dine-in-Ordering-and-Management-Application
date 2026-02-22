package com.example.mistcaferestaurantpanglao;

import android.os.Parcel;
import android.os.Parcelable;

public class MenuItem implements Parcelable {
    private String id;
    private String name;
    private String dishName;
    private String description;
    private String price;
    private String category;
    private String imageUrl;
    private String imageUri;
    private boolean available;
    private String specialties;

    public MenuItem() {
        this.available = true;
        this.specialties = "";
    }

    public MenuItem(String id, String name, String description, String price, String category, boolean available) {
        this.id = id;
        this.name = name;
        this.dishName = name;
        this.description = description;
        this.price = price;
        this.category = category;
        this.available = available;
        this.specialties = "";
    }

    public MenuItem(String dishName, String description, String price, String category, String imageUrl) {
        this.dishName = dishName;
        this.name = dishName;
        this.description = description;
        this.price = price;
        this.category = category;
        this.imageUrl = imageUrl;
        this.imageUri = imageUrl;
        this.available = true;
        this.specialties = "";
    }

    public MenuItem(String id, String name, String dishName, String description, String price,
                    String category, String imageUrl, boolean available, String specialties) {
        this.id = id;
        this.name = name;
        this.dishName = dishName;
        this.description = description;
        this.price = price;
        this.category = category;
        this.imageUrl = imageUrl;
        this.imageUri = imageUrl;
        this.available = available;
        this.specialties = specialties != null ? specialties : "";
    }

    protected MenuItem(Parcel in) {
        id = in.readString();
        name = in.readString();
        dishName = in.readString();
        description = in.readString();
        price = in.readString();
        category = in.readString();
        imageUrl = in.readString();
        imageUri = in.readString();
        available = in.readByte() != 0;
        specialties = in.readString();
    }

    public static final Creator<MenuItem> CREATOR = new Creator<MenuItem>() {
        @Override
        public MenuItem createFromParcel(Parcel in) {
            return new MenuItem(in);
        }

        @Override
        public MenuItem[] newArray(int size) {
            return new MenuItem[size];
        }
    };

    public String getId() {
        return id;
    }

    public String getName() {
        return name != null ? name : dishName;
    }

    public String getDishName() {
        return dishName != null ? dishName : name;
    }

    public String getDescription() {
        return description;
    }

    public String getPrice() {
        return price;
    }

    public String getCategory() {
        return category;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public String getImageUri() {
        return imageUri != null ? imageUri : imageUrl;
    }

    public boolean getAvailable() {
        return available;
    }

    public boolean isAvailable() {
        return available;
    }

    public String getSpecialties() {
        return specialties != null ? specialties : "";
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
        if (this.dishName == null) {
            this.dishName = name;
        }
    }

    public void setDishName(String dishName) {
        this.dishName = dishName;
        if (this.name == null) {
            this.name = dishName;
        }
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setPrice(String price) {
        this.price = price;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
        if (this.imageUri == null) {
            this.imageUri = imageUrl;
        }
    }

    public void setImageUri(String imageUri) {
        this.imageUri = imageUri;
        if (this.imageUrl == null) {
            this.imageUrl = imageUri;
        }
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }

    public void setSpecialties(String specialties) {
        this.specialties = specialties != null ? specialties : "";
    }

    public boolean hasSpecialties() {
        return specialties != null && !specialties.trim().isEmpty();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(id);
        dest.writeString(name);
        dest.writeString(dishName);
        dest.writeString(description);
        dest.writeString(price);
        dest.writeString(category);
        dest.writeString(imageUrl);
        dest.writeString(imageUri);
        dest.writeByte((byte) (available ? 1 : 0));
        dest.writeString(specialties);
    }
}