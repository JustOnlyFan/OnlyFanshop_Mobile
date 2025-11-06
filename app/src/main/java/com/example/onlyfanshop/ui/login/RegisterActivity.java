package com.example.onlyfanshop.ui.login;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;


import com.example.onlyfanshop.R;
import com.example.onlyfanshop.api.ApiClient;
import com.example.onlyfanshop.api.UserApi;
import com.example.onlyfanshop.model.response.ApiResponse;
import com.example.onlyfanshop.utils.Validation;
import com.google.android.material.textfield.TextInputLayout;

import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RegisterActivity extends AppCompatActivity {

    private UserApi userApi;
    private EditText edtUsername, edtPassword, edtEmail, edtPhone, edtAddress, edtConfirmPassword;
    private Button btnRegister;
    private TextView btnBack;
    private TextInputLayout layoutPassword;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        userApi = ApiClient.getPublicClient().create(UserApi.class);

        edtUsername = findViewById(R.id.edtUsername);
        edtPassword = findViewById(R.id.edtPassword);
        edtConfirmPassword = findViewById(R.id.edtConfirmPassword);
        edtEmail    = findViewById(R.id.edtEmail);
        edtPhone    = findViewById(R.id.edtPhone);
        edtAddress  = findViewById(R.id.edtAddress);
        btnRegister = findViewById(R.id.btnRegister);
        btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());
        btnRegister.setOnClickListener(v -> register());
        layoutPassword= findViewById(R.id.layoutPassword);
    }

    private void register() {
        String username = edtUsername.getText().toString().trim();
        String password = edtPassword.getText().toString().trim();
        String email    = edtEmail.getText().toString().trim();
        String phone    = edtPhone.getText().toString().trim();
        String address  = edtAddress.getText().toString().trim();
        String confirmPassword = edtConfirmPassword.getText().toString().trim();

        boolean hasError = false;
        edtUsername.setBackgroundResource(android.R.drawable.edit_text);
        edtEmail.setBackgroundResource(android.R.drawable.edit_text);
        edtPassword.setBackgroundResource(android.R.drawable.edit_text);
        edtConfirmPassword.setBackgroundResource(android.R.drawable.edit_text);
        edtPhone.setBackgroundResource(android.R.drawable.edit_text);
        edtAddress.setBackgroundResource(android.R.drawable.edit_text);

        if (username.isEmpty()) {
            edtUsername.setBackgroundResource(R.drawable.edittext_error);
            edtUsername.setError("Vui lòng nhập Username");
            hasError = true;
        }
        if (password.isEmpty()) {
            edtPassword.setBackgroundResource(R.drawable.edittext_error);
            edtPassword.setError("Vui lòng nhập Password");
            hasError = true;
        }
        if (confirmPassword.isEmpty()) {
            edtConfirmPassword.setBackgroundResource(R.drawable.edittext_error);
            edtConfirmPassword.setError("Vui lòng nhập Confirm Password");
            hasError = true;
        }
        if (email.isEmpty()) {
            edtEmail.setBackgroundResource(R.drawable.edittext_error);
            edtEmail.setError("Vui lòng nhập Email");
            hasError = true;
        }
        if (!password.equals(confirmPassword)) {
            edtConfirmPassword.setBackgroundResource(R.drawable.edittext_error);
            edtConfirmPassword.setError("Password và Confirm Password không khớp!");
            return;
        }
        if (hasError) return;

        if (!Validation.isValidEmail(email)) {
            edtEmail.setBackgroundResource(R.drawable.edittext_error);
            edtEmail.setError("Email không hợp lệ");
            Toast.makeText(this, "Email không hợp lệ", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!Validation.isValidPassword(password)) {
            edtPassword.setBackgroundResource(R.drawable.edittext_error);
            layoutPassword.setError("Mật khẩu phải có ít nhất 8 ký tự, gồm chữ, số và ký tự đặc biệt");
            Toast.makeText(this, "Mật khẩu không hợp lệ!", Toast.LENGTH_SHORT).show();
            return;
        }else {
            layoutPassword.setError(null);
            layoutPassword.setErrorEnabled(false); // 🔹 khôi phục lại icon con mắt
        }

        // 🔹 Gọi API check username/email trước
        userApi.checkAccount(username, email).enqueue(new Callback<Map<String, Boolean>>() {
            @Override
            public void onResponse(Call<Map<String, Boolean>> call, Response<Map<String, Boolean>> response) {

                if (response.isSuccessful() && response.body() != null) {
                    Map<String, Boolean> result = response.body();
                    boolean usernameExists = result.containsKey("usernameExists") ? result.get("usernameExists") : false;
                    boolean emailExists = result.containsKey("emailExists") ? result.get("emailExists") : false;

                    if (usernameExists) {
                        edtUsername.setBackgroundResource(R.drawable.edittext_error);
                        edtUsername.setError("Username đã tồn tại!");
                        return;
                    }
                    if (emailExists) {
                        edtEmail.setBackgroundResource(R.drawable.edittext_error);
                        edtEmail.setError("Email đã tồn tại!");
                        return;
                    }

                    // ✅ Nếu không tồn tại -> gửi OTP
                    sendOtp(email);

                    // Chuyển sang màn hình OTP
                    Intent intent = new Intent(RegisterActivity.this, OtpActivity.class);
                    intent.putExtra("username", username);
                    intent.putExtra("password", password);
                    intent.putExtra("confirmPassword", confirmPassword);
                    intent.putExtra("email", email);
                    intent.putExtra("phone", phone);
                    intent.putExtra("address", address);
                    startActivity(intent);
                } else {
                    Toast.makeText(RegisterActivity.this, "Server error while checking account", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Map<String, Boolean>> call, Throwable t) {
                Toast.makeText(RegisterActivity.this, "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }


    public void sendOtp(String email) {
        Call<ApiResponse<Void>> call = userApi.sendOtp(email);

        call.enqueue(new Callback<ApiResponse<Void>>() {
            @Override
            public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<Void> apiResponse = response.body();
                    Toast.makeText(RegisterActivity.this, apiResponse.getMessage(), Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(RegisterActivity.this, "Không thể gửi OTP", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                Log.e("RegisterActivityLog", "Lỗi gửi OTP: " + t.getMessage());
                Toast.makeText(RegisterActivity.this, "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

}
