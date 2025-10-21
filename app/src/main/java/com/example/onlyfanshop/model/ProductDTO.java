package com.example.onlyfanshop.model;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;

public class ProductDTO implements Serializable {

    // Backend trả "id" -> ánh xạ về productID để đồng bộ với các chỗ khác trong app
    @SerializedName(value = "productID", alternate = {"id"})
    private Integer productID;

    @SerializedName("productName")
    private String productName;

    @SerializedName("price")
    private Double price;

    @SerializedName(value = "imageURL", alternate = {"imageUrl"})
    private String imageURL;

    @SerializedName("briefDescription")
    private String briefDescription;

    @SerializedName("brand")
    private BrandDTO brand;

    @SerializedName("category")
    private CategoryDTO category;

    @SerializedName(("isActive"))
    private boolean isActive;
    public Integer getProductID() { return productID; }
    public void setProductID(Integer productID) { this.productID = productID; }

    public String getProductName() { return productName != null ? productName : ""; }
    public void setProductName(String productName) { this.productName = productName; }

    public Double getPrice() { return price != null ? price : 0d; }
    public void setPrice(Double price) { this.price = price; }

    public String getImageURL() { return imageURL; }
    public void setImageURL(String imageURL) { this.imageURL = imageURL; }

    public String getBriefDescription() { return briefDescription; }
    public void setBriefDescription(String briefDescription) { this.briefDescription = briefDescription; }

    public BrandDTO getBrand() { return brand; }
    public void setBrand(BrandDTO brand) { this.brand = brand; }

    public CategoryDTO getCategory() { return category; }
    public void setCategory(CategoryDTO category) { this.category = category; }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }
}