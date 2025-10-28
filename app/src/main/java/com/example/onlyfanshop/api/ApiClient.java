package com.example.onlyfanshop.api;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Retrofit client với 2 loại:
 * - Public: không cần token
 * - Private: có chèn token qua AuthInterceptor hoặc setAuthToken(...)
 *
 * Không phụ thuộc BuildConfig. Dùng setDebugLoggingEnabled(...) để bật tắt logging.
 */
public final class ApiClient {

    private static final boolean IS_EMULATOR = android.os.Build.FINGERPRINT.contains("generic");
    private static volatile String BASE_URL = IS_EMULATOR
            ? "http://10.0.2.2:8080/"
            : "http://192.168.100.47:8080/";

    private ApiClient() {}

    private static volatile Retrofit retrofitPublic;
    private static volatile Retrofit retrofitPrivate;

    private static volatile OkHttpClient okHttpPublic;
    private static volatile OkHttpClient okHttpPrivate;

    // Mặc định cho Android Emulator
    private static volatile String authToken;

    // Bật/tắt logging mức BODY cho debug
    private static volatile boolean debugLoggingEnabled = false;

    // =========================
    // Config helpers
    // =========================
    public static void setBaseUrl(String baseUrl) {
        if (baseUrl == null) return;
        String trimmed = baseUrl.trim();
        if (trimmed.isEmpty()) return;
        if (!trimmed.startsWith("http://") && !trimmed.startsWith("https://")) {
            trimmed = "http://" + trimmed;
        }
        BASE_URL = trimmed.endsWith("/") ? trimmed : trimmed + "/";
        reset();
    }

    public static String getToken(Context context) {
        SharedPreferences prefs = context.getApplicationContext().getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE);
        return prefs.getString("jwt_token", null);
    }

    public static void setAuthToken(@Nullable String token) {
        authToken = token;
        synchronized (ApiClient.class) {
            retrofitPrivate = null;
            okHttpPrivate = null;
        }
    }

    public static void clearAuthToken() {
        setAuthToken(null);
    }

    public static void setDebugLoggingEnabled(boolean enabled) {
        debugLoggingEnabled = enabled;
        synchronized (ApiClient.class) {
            okHttpPublic = null;
            okHttpPrivate = null;
            retrofitPublic = null;
            retrofitPrivate = null;
        }
    }

    public static void reset() {
        synchronized (ApiClient.class) {
            retrofitPublic = null;
            retrofitPrivate = null;
            okHttpPublic = null;
            okHttpPrivate = null;
        }
    }

    public static Retrofit getPublicClient() {
        if (retrofitPublic == null) {
            synchronized (ApiClient.class) {
                if (retrofitPublic == null) {
                    if (okHttpPublic == null) {
                        okHttpPublic = buildOkHttp(false, null);
                    }
                    retrofitPublic = buildRetrofit(okHttpPublic);
                }
            }
        }
        return retrofitPublic;
    }

    // API cần token
    public static Retrofit getPrivateClient(Context context) {
        if (retrofitPrivate == null) {
            synchronized (ApiClient.class) {
                if (retrofitPrivate == null) {
                    if (okHttpPrivate == null) {
                        okHttpPrivate = buildOkHttp(true, context.getApplicationContext());
                    }
                    retrofitPrivate = buildRetrofit(okHttpPrivate);
                }
            }
        }
        return retrofitPrivate;
    }

    // =========================
    // Builders
    // =========================
    private static Retrofit buildRetrofit(OkHttpClient client) {
        return new Retrofit.Builder()
                .baseUrl(BASE_URL.trim())
                .addConverterFactory(GsonConverterFactory.create(buildGson()))
                .client(client)
                .build();
    }

    private static Gson buildGson() {
        return new GsonBuilder()
                .setLenient()
                .setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
                .create();
    }

    private static OkHttpClient buildOkHttp(boolean withAuth, @Nullable Context context) {
        OkHttpClient.Builder b = new OkHttpClient.Builder()
                .connectTimeout(5, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS);

        // Default headers (trước)
        b.addInterceptor(chain -> {
            Request original = chain.request();
            Request.Builder builder = original.newBuilder()
                    .header("Accept", "application/json");
            // Chỉ set Content-Type cho request có body
            String method = original.method();
            if ("POST".equalsIgnoreCase(method)
                    || "PUT".equalsIgnoreCase(method)
                    || "PATCH".equalsIgnoreCase(method)) {
                builder.header("Content-Type", "application/json");
            }
            return chain.proceed(builder.build());
        });

        // Authorization (giữa)
        if (withAuth) {
            if (authToken != null && !authToken.isEmpty()) {
                b.addInterceptor(chain -> {
                    Request req = chain.request().newBuilder()
                            .header("Authorization", "Bearer " + authToken)
                            .build();
                    return chain.proceed(req);
                });
            } else if (context != null) {
                // Dùng AuthInterceptor của bạn (đọc SharedPreferences "MyAppPrefs"/"jwt_token")
                b.addInterceptor(new AuthInterceptor(context.getApplicationContext()));
            }
        }

        // Logging (cuối) để log đầy đủ cả Authorization header
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(debugLoggingEnabled ? HttpLoggingInterceptor.Level.BODY
                : HttpLoggingInterceptor.Level.BASIC);
        b.addInterceptor(logging);

        return b.build();
    }

    // Tương thích chỗ gọi cũ
    public static Retrofit getInstance() {
        return getPublicClient();
    }
}