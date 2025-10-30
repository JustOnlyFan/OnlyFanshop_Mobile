package com.example.onlyfanshop.utils;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

/**
 * Singleton using LiveData to broadcast/receive app-wide events.
 */
public class AppEvents {

    private static final AppEvents INSTANCE = new AppEvents();

    // Event: cart has changed (increase/decrease/add/remove)
    private final MutableLiveData<Long> cartUpdated = new MutableLiveData<>();

    private AppEvents() {}

    public static AppEvents get() {
        return INSTANCE;
    }

    public LiveData<Long> cartUpdated() {
        return cartUpdated;
    }

    // Gọi hàm này sau khi addToCart / delete / change quantity thành công
    public void notifyCartUpdated() {
        // postValue để an toàn thread (Retrofit callback thường chạy main thread, nhưng post vẫn OK)
        cartUpdated.postValue(System.currentTimeMillis());
    }
}