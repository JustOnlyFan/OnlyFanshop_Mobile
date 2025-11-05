package com.example.onlyfanshop.model;

import com.google.gson.annotations.SerializedName;

public class OrderItemLiteDTO {
    @SerializedName("productName")
    private String productName;
    @SerializedName(value = "imageURL", alternate = {"imageUrl"})
    private String imageURL;
    @SerializedName("quantity")
    private Integer quantity;
    @SerializedName("price")
    private Double price;

    public String getProductName() { return productName; }
    public String getImageURL() { return imageURL; }
    public Integer getQuantity() { return quantity; }
    public Double getPrice() { return price; }
}





