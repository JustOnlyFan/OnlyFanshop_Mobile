package com.example.onlyfanshop.ui.login;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.example.onlyfanshop.R;
import com.example.onlyfanshop.api.ApiClient;
import com.example.onlyfanshop.api.UserApi;
import com.example.onlyfanshop.model.response.ApiResponse;
import com.example.onlyfanshop.model.Request.RegisterRequest;
import com.google.gson.Gson;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class OtpActivity extends AppCompatActivity {
    private TextView tvOtpMessage, tvOtpStatus, tvOtpEmail;
    private EditText etOtp;
    private Button btnVerifyOtp, btnResendOtp;
    private TextView btnBack;
    private UserApi userApi;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_otp);
        userApi = ApiClient.getPublicClient().create(UserApi.class);
        String email = getIntent().getStringExtra("email");

        tvOtpMessage = findViewById(R.id.tvOtpMessage);
        tvOtpStatus = findViewById(R.id.tvOtpStatus);
        tvOtpEmail = findViewById(R.id.tvOtpEmail);
        etOtp = findViewById(R.id.edtOtp);
        btnVerifyOtp = findViewById(R.id.btnVerifyOtp);
        btnResendOtp = findViewById(R.id.btnResendOtp);
        btnBack = findViewById(R.id.btnBack);

        // Khi bấm nút Back ở màn OTP sẽ chuyển về LoginActivity (không chỉ finish)
        btnBack.setOnClickListener(v -> {
            Intent intent = new Intent(OtpActivity.this, LoginActivity.class);
            // Nếu LoginActivity đã tồn tại trên back stack, reuse nó; nếu không, tạo mới.
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        });

        tvOtpEmail.setText(email);
        btnResendOtp.setOnClickListener(v -> {
            Call<ApiResponse<Void>> call = userApi.sendOtp(email);

            call.enqueue(new Callback<ApiResponse<Void>>() {
                @Override
                public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        ApiResponse<Void> apiResponse = response.body();
                        Toast.makeText(OtpActivity.this, apiResponse.getMessage(), Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(OtpActivity.this, "Không thể gửi OTP", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                    Log.e("RegisterActivityLog", "Lỗi gửi OTP: " + t.getMessage());
                    Toast.makeText(OtpActivity.this, "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        });

        btnVerifyOtp.setOnClickListener(v -> {
            String otp = etOtp.getText().toString().trim();
            if (otp.isEmpty()) {
                etOtp.setError("Vui lòng nhập OTP");
                return;
            }
            verifyOtp(email, otp);
        });

    }

    private void verifyOtp(String email, String otp) {
        Call<ApiResponse<Void>> call = userApi.verifyOtp(email, otp);
        call.enqueue(new Callback<ApiResponse<Void>>() {
            @Override
            public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<Void> apiResponse = response.body();
                    if (apiResponse.getMessage().equals("Xác thực thành công")) {
                        register();
                        finish();
                    } else {
                        Toast.makeText(OtpActivity.this, "OTP không hợp lệ!", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(OtpActivity.this, "Lỗi xác thực OTP", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                Toast.makeText(OtpActivity.this, "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void register() {
        Log.d("RegisterActivityLog", "register() called");
        String username = getIntent().getStringExtra("username");
        String password = getIntent().getStringExtra("password");
        String confirmPassword = getIntent().getStringExtra("confirmPassword");
        String email = getIntent().getStringExtra("email");
        String phone = getIntent().getStringExtra("phone");
        String address = getIntent().getStringExtra("address");
        RegisterRequest request = new RegisterRequest(username, password, confirmPassword, email, phone, address);
        userApi.register(request).enqueue(new Callback<ApiResponse<Void>>() {
            @Override
            public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {
                ApiResponse<Void> apiResponse = response.body();

                if (apiResponse == null && response.errorBody() != null) {
                    try {
                        apiResponse = new Gson().fromJson(response.errorBody().string(), ApiResponse.class);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                if (apiResponse != null) {
                    Toast.makeText(OtpActivity.this, apiResponse.getMessage(), Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(OtpActivity.this, LoginActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finish();
                } else {
                    Toast.makeText(OtpActivity.this, "Đăng ký thất bại", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                Toast.makeText(OtpActivity.this, "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

}