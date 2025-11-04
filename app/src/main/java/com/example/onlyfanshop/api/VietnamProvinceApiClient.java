package com.example.onlyfanshop.api;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import java.util.concurrent.TimeUnit;

/**
 * Client riêng cho VietnamProvince API (base URL khác với backend chính)
 */
public class VietnamProvinceApiClient {
    
    private static final String BASE_URL = "https://vietnamlabs.com";
    
    private static volatile Retrofit retrofit;
    private static volatile VietnamProvinceApi api;
    
    private VietnamProvinceApiClient() {}
    
    public static VietnamProvinceApi getApi() {
        if (api == null) {
            synchronized (VietnamProvinceApiClient.class) {
                if (api == null) {
                    api = getRetrofit().create(VietnamProvinceApi.class);
                }
            }
        }
        return api;
    }
    
    private static Retrofit getRetrofit() {
        if (retrofit == null) {
            synchronized (VietnamProvinceApiClient.class) {
                if (retrofit == null) {
                    OkHttpClient client = new OkHttpClient.Builder()
                            .connectTimeout(10, TimeUnit.SECONDS)
                            .readTimeout(15, TimeUnit.SECONDS)
                            .writeTimeout(15, TimeUnit.SECONDS)
                            .addInterceptor(new HttpLoggingInterceptor()
                                    .setLevel(HttpLoggingInterceptor.Level.BODY))
                            .build();
                    
                    Gson gson = new GsonBuilder()
                            .setLenient()
                            .create();
                    
                    retrofit = new Retrofit.Builder()
                            .baseUrl(BASE_URL)
                            .client(client)
                            .addConverterFactory(GsonConverterFactory.create(gson))
                            .build();
                }
            }
        }
        return retrofit;
    }
}

