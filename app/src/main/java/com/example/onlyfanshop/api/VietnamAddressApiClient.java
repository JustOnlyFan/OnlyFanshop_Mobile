package com.example.onlyfanshop.api;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Separate API client for Vietnam Address API v2
 * Base URL: https://provinces.open-api.vn/api/v2/
 * Latest data after 2025 province merger (34 provinces)
 * Note: API structure changed - provinces return wards directly (no districts)
 */
public class VietnamAddressApiClient {
    
    private static final String BASE_URL = "https://provinces.open-api.vn/api/v2/";
    
    private static volatile Retrofit retrofit;
    private static volatile OkHttpClient okHttpClient;

    private VietnamAddressApiClient() {}

    public static Retrofit getInstance() {
        if (retrofit == null) {
            synchronized (VietnamAddressApiClient.class) {
                if (retrofit == null) {
                    retrofit = buildRetrofit();
                }
            }
        }
        return retrofit;
    }

    private static Retrofit buildRetrofit() {
        return new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(buildOkHttpClient())
                .addConverterFactory(GsonConverterFactory.create(buildGson()))
                .build();
    }

    private static Gson buildGson() {
        return new GsonBuilder()
                .setLenient()
                .create();
    }

    private static OkHttpClient buildOkHttpClient() {
        if (okHttpClient == null) {
            synchronized (VietnamAddressApiClient.class) {
                if (okHttpClient == null) {
                    OkHttpClient.Builder builder = new OkHttpClient.Builder()
                            .connectTimeout(15, TimeUnit.SECONDS)
                            .readTimeout(15, TimeUnit.SECONDS)
                            .writeTimeout(15, TimeUnit.SECONDS);

                    // Logging interceptor
                    HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
                    logging.setLevel(HttpLoggingInterceptor.Level.BASIC);
                    builder.addInterceptor(logging);

                    okHttpClient = builder.build();
                }
            }
        }
        return okHttpClient;
    }
}

