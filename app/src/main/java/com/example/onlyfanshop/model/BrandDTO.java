package com.example.onlyfanshop.model;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

public class BrandDTO implements Serializable {
    // Backend trả "brandID" (int32) -> để an toàn có thể nhận cả "id"
    @SerializedName(value = "brandID", alternate = {"id"})
    private Long brandID;

    @SerializedName("name")
    private String name;

    public Long getBrandID() { return brandID; }
    public void setBrandID(Long brandID) { this.brandID = brandID; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}


