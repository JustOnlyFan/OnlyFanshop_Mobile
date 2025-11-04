package com.example.onlyfanshop.model;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

public class BrandManagementDTO implements Serializable {
    @SerializedName(value = "brandID", alternate = {"id"})
    private Integer brandID;

    @SerializedName("name")
    private String name;

    @SerializedName("country")
    private String country;

    @SerializedName("description")
    private String description;

    @SerializedName(value = "imageURL", alternate = {"logoUrl", "logo"})
    private String imageURL;

    @SerializedName("active")
    private boolean isActive;

    public BrandManagementDTO() {
    }

    public BrandManagementDTO(Integer brandID, String name, String country, String description, String imageURL, boolean isActive) {
        this.brandID = brandID;
        this.name = name;
        this.country = country;
        this.description = description;
        this.imageURL = imageURL;
        this.isActive = isActive;
    }

    public Integer getBrandID() {
        return brandID;
    }

    public void setBrandID(Integer brandID) {
        this.brandID = brandID;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public String getImageURL() {
        return imageURL;
    }

    public void setImageURL(String imageURL) {
        this.imageURL = imageURL;
    }
}
