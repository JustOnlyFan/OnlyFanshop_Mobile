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
import com.example.onlyfanshop.utils.Validation;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ForgotActivity extends AppCompatActivity {
    private UserApi userApi;
    private EditText edtEmail, edtOtp;
    private Button btnVerifyOtp, btnResendOtp;
    private TextView btnBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_forgot);
        userApi = ApiClient.getPublicClient().create(UserApi.class);
        edtEmail = findViewById(R.id.edtEmail);
        edtOtp = findViewById(R.id.edtOtp);
        btnVerifyOtp = findViewById(R.id.btnVerifyOtp);
        btnResendOtp = findViewById(R.id.btnResendOtp);
        btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        btnVerifyOtp.setOnClickListener(v -> {
            String email = edtEmail.getText().toString().trim();
            String otp   = edtOtp.getText().toString().trim();
            verifyOtp(email, otp);
        });

        btnResendOtp.setOnClickListener(v -> {
            String email = edtEmail.getText().toString().trim();
            sendOtp(email);
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
                        Toast.makeText(ForgotActivity.this, "Xác thực thành công!", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(ForgotActivity.this, ResetPasswordActivity.class);
                        intent.putExtra("email", email);
                        startActivity(intent);

                    } else {
                        Toast.makeText(ForgotActivity.this, "OTP không hợp lệ!", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(ForgotActivity.this, "Lỗi xác thực OTP", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                Toast.makeText(ForgotActivity.this, "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
    public void sendOtp(String email) {
        Log.d("ForgotActivityLog", "sendOtp() called with: email = [" + email + "]");
        if (email.isEmpty()) {  // check đúng: nếu email trống thì báo lỗi
            edtEmail.setBackgroundResource(R.drawable.edittext_error);
            edtEmail.setError("Vui lòng nhập email");
            return;
        }
        if (!Validation.isValidEmail(email)) {
            edtEmail.setBackgroundResource(R.drawable.edittext_error);
            edtEmail.setError("Email không hợp lệ");
            Toast.makeText(this, "Email không hợp lệ", Toast.LENGTH_SHORT).show();
            return;
        }
        Call<ApiResponse<Void>> call = userApi.sendOtp(email);
        call.enqueue(new Callback<ApiResponse<Void>>() {
            @Override
            public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<Void> apiResponse = response.body();
                    Toast.makeText(ForgotActivity.this, apiResponse.getMessage(), Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(ForgotActivity.this, "Không thể gửi OTP", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                Log.e("ForgotActivityLog", "Lỗi gửi OTP: " + t.getMessage());
                Toast.makeText(ForgotActivity.this, "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

}