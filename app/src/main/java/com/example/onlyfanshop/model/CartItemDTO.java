package com.example.onlyfanshop.model;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

public class CartItemDTO implements Serializable {
    @SerializedName("cartItemID")
    private Integer cartItemID;
    @SerializedName("quantity")
    private Integer quantity;
    @SerializedName("price")
    private Double price;

    @SerializedName("checked")
    private boolean isChecked;
    @SerializedName("product")
    private ProductDTO productDTO;

    public boolean isChecked() {
        return isChecked;
    }

    public void setChecked(boolean checked) {
        isChecked = checked;
    }

    public CartItemDTO() {
    }

    public CartItemDTO(Integer cartItemID, Integer quantity, Double price, ProductDTO productDTO) {
        this.cartItemID = cartItemID;
        this.quantity = quantity;
        this.price = price;
        this.productDTO = productDTO;

    }


    public Integer getCartItemID() {
        return cartItemID;
    }

    public void setCartItemID(Integer cartItemID) {
        this.cartItemID = cartItemID;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public Double getPrice() {
        return price;
    }

    public void setPrice(Double price) {
        this.price = price;
    }

    public ProductDTO getProductDTO() {
        return productDTO;
    }

    public void setProductDTO(ProductDTO productDTO) {
        this.productDTO = productDTO;
    }
}
