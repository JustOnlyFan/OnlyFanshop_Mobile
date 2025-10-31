package com.example.onlyfanshop.model;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

public class CategoryManagementDTO implements Serializable {
    @SerializedName(value = "categoryID", alternate = {"id"})
    private Integer categoryID;

    // Backend trả "name", còn trong code cũ bạn dùng categoryName -> map cả hai
    @SerializedName(value = "categoryName", alternate = {"name"})
    private String categoryName;
    @SerializedName("active")
    private boolean isActive;

    public CategoryManagementDTO() {
    }

    public Integer getCategoryID() {
        return categoryID;
    }

    public void setCategoryID(Integer categoryID) {
        this.categoryID = categoryID;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }
}
