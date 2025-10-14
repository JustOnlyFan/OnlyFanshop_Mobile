package com.example.onlyfanshop.model.Request;

public class ProductRequest {


    private String productName;
    private String briefDescription;
    private String fullDescription;
    private String technicalSpecifications;
    private Double price;
    private String imageURL;
    private Integer brandID;     // chỉ gửi ID
    private Integer categoryID;  // chỉ gửi ID

    // Getter & Setter
    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    public String getBriefDescription() { return briefDescription; }
    public void setBriefDescription(String briefDescription) { this.briefDescription = briefDescription; }

    public String getFullDescription() { return fullDescription; }
    public void setFullDescription(String fullDescription) { this.fullDescription = fullDescription; }

    public String getTechnicalSpecifications() { return technicalSpecifications; }
    public void setTechnicalSpecifications(String technicalSpecifications) { this.technicalSpecifications = technicalSpecifications; }

    public Double getPrice() { return price; }
    public void setPrice(Double price) { this.price = price; }

    public String getImageURL() { return imageURL; }
    public void setImageURL(String imageURL) { this.imageURL = imageURL; }

    public Integer getBrandID() { return brandID; }
    public void setBrandID(Integer brandID) { this.brandID = brandID; }

    public Integer getCategoryID() { return categoryID; }
    public void setCategoryID(Integer categoryID) { this.categoryID = categoryID; }
}

