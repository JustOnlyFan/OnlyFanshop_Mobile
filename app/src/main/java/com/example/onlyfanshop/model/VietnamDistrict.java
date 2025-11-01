package com.example.onlyfanshop.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class VietnamDistrict {
    @SerializedName("code")
    private int code;
    
    @SerializedName("name")
    private String name;
    
    @SerializedName("name_en")
    private String nameEn;
    
    @SerializedName("full_name")
    private String fullName;
    
    @SerializedName("full_name_en")
    private String fullNameEn;
    
    @SerializedName("code_name")
    private String codeName;
    
    @SerializedName("division_type")
    private String divisionType;
    
    @SerializedName("province_code")
    private int provinceCode;
    
    @SerializedName("wards")
    private List<VietnamWard> wards;

    // Getters and Setters
    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getNameEn() {
        return nameEn;
    }

    public void setNameEn(String nameEn) {
        this.nameEn = nameEn;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getFullNameEn() {
        return fullNameEn;
    }

    public void setFullNameEn(String fullNameEn) {
        this.fullNameEn = fullNameEn;
    }

    public String getCodeName() {
        return codeName;
    }

    public void setCodeName(String codeName) {
        this.codeName = codeName;
    }

    public String getDivisionType() {
        return divisionType;
    }

    public void setDivisionType(String divisionType) {
        this.divisionType = divisionType;
    }

    public int getProvinceCode() {
        return provinceCode;
    }

    public void setProvinceCode(int provinceCode) {
        this.provinceCode = provinceCode;
    }

    public List<VietnamWard> getWards() {
        return wards;
    }

    public void setWards(List<VietnamWard> wards) {
        this.wards = wards;
    }
}

