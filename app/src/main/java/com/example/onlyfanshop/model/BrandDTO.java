package com.example.onlyfanshop.model;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

public class BrandDTO implements Serializable {
    // Backend trả "brandID" (int32) -> để an toàn có thể nhận cả "id"
    @SerializedName(value = "brandID", alternate = {"id"})
    private Integer brandID;

    @SerializedName("name")
    private String name;

    @SerializedName(value = "imageURL", alternate = {"logoUrl", "logo"})
    private String imageURL;

    public Integer getBrandID() { return brandID; }
    public void setBrandID(Integer brandID) { this.brandID = brandID; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getImageURL() { return imageURL; }
    public void setImageURL(String imageURL) { this.imageURL = imageURL; }
}






