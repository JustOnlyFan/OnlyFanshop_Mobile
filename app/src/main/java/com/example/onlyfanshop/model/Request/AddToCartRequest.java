package com.example.onlyfanshop.model.Request;

public class AddToCartRequest {
    private Integer productId;
    private Integer quantity;
    private String userName;

    public AddToCartRequest(Integer productID, Integer quantity, String userName) {
        this.productId = productID;
        this.quantity = quantity;
        this.userName = userName;
    }

    public Integer getProductID() {
        return productId;
    }

    public void setProductID(Integer productID) {
        this.productId = productID;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }
}
