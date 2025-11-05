package com.example.onlyfanshop.api;

import com.example.onlyfanshop.model.VietnamProvince;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

public interface VietnamAddressApi {
    
    @GET("p/")
    Call<List<VietnamProvince>> getProvinces();
    
    @GET("p/{provinceCode}?depth=2")
    Call<VietnamProvince> getProvinceWithWards(@Path("provinceCode") int provinceCode);
}

