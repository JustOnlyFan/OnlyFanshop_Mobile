package com.example.onlyfanshop.model;

public class OrderDTO {
    private int orderID;
    private String paymentMethod;
    private String billingAddress;
    private String orderStatus;
    private String orderDate;
    private double totalPrice;

    // First product info for list display
    private String firstProductName;
    private String firstProductImage;
    private Integer firstProductQuantity;
    private Double firstProductPrice;

    // Getter & Setter
    public int getOrderID() { return orderID; }
    public void setOrderID(int orderID) { this.orderID = orderID; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public String getBillingAddress() { return billingAddress; }
    public void setBillingAddress(String billingAddress) { this.billingAddress = billingAddress; }

    public String getOrderStatus() { return orderStatus; }
    public void setOrderStatus(String orderStatus) { this.orderStatus = orderStatus; }

    public String getOrderDate() { return orderDate; }
    public void setOrderDate(String orderDate) { this.orderDate = orderDate; }

    public double getTotalPrice() { return totalPrice; }
    public void setTotalPrice(double totalPrice) { this.totalPrice = totalPrice; }

    public String getFirstProductName() { return firstProductName; }
    public void setFirstProductName(String firstProductName) { this.firstProductName = firstProductName; }

    public String getFirstProductImage() { return firstProductImage; }
    public void setFirstProductImage(String firstProductImage) { this.firstProductImage = firstProductImage; }

    public Integer getFirstProductQuantity() { return firstProductQuantity; }
    public void setFirstProductQuantity(Integer firstProductQuantity) { this.firstProductQuantity = firstProductQuantity; }

    public Double getFirstProductPrice() { return firstProductPrice; }
    public void setFirstProductPrice(Double firstProductPrice) { this.firstProductPrice = firstProductPrice; }
}

