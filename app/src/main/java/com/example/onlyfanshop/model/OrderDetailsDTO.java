package com.example.onlyfanshop.model;

import com.example.onlyfanshop.model.response.CartDTO;

public class OrderDetailsDTO {
    private Integer orderID;
    private String paymentMethod;
    private String billingAddress;
    private String orderStatus;
    private String orderDate;
    private Double totalPrice;

    private String address;
    private String customerName;
    private String email;
    private String phone;

    private CartDTO cartDTO; // chứa danh sách sản phẩm
    private java.util.List<OrderItemLiteDTO> itemsLite; // danh sách nhẹ từ server

    // Getter và Setter
    public Integer getOrderID() { return orderID; }
    public void setOrderID(Integer orderID) { this.orderID = orderID; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public String getBillingAddress() { return billingAddress; }
    public void setBillingAddress(String billingAddress) { this.billingAddress = billingAddress; }

    public String getOrderStatus() { return orderStatus; }
    public void setOrderStatus(String orderStatus) { this.orderStatus = orderStatus; }

    public String getOrderDate() { return orderDate; }
    public void setOrderDate(String orderDate) { this.orderDate = orderDate; }

    public Double getTotalPrice() { return totalPrice; }
    public void setTotalPrice(Double totalPrice) { this.totalPrice = totalPrice; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public CartDTO getCartDTO() { return cartDTO; }
    public void setCartDTO(CartDTO cartDTO) { this.cartDTO = cartDTO; }
    public java.util.List<OrderItemLiteDTO> getItemsLite() { return itemsLite; }
}

