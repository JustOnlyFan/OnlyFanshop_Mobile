package com.example.onlyfanshop.ui.user;

import android.content.Intent;
import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.example.onlyfanshop.R;
import com.example.onlyfanshop.api.ApiClient;
import com.example.onlyfanshop.api.UserApi;
import com.example.onlyfanshop.model.Request.RegisterRequest;
import com.example.onlyfanshop.model.response.ApiResponse;
import com.example.onlyfanshop.utils.Validation;

import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AddUserActivity extends AppCompatActivity {

    private EditText edtUsername, edtEmail, edtPhone, edtAddress, edtPassword, edtConfirmPassword;
    private Button btnSaveUser;
    private UserApi userApi;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_user);

        edtUsername = findViewById(R.id.edtUsername);
        edtEmail = findViewById(R.id.edtEmail);
        edtPhone = findViewById(R.id.edtPhone);
        edtAddress = findViewById(R.id.edtAddress);
        edtPassword = findViewById(R.id.edtPassword);
        edtConfirmPassword = findViewById(R.id.edtConfirmPassword);
        btnSaveUser = findViewById(R.id.btnAddUser);


        userApi = ApiClient.getPrivateClient(this).create(UserApi.class);

        btnSaveUser.setOnClickListener(v -> createUser());
    }

    private void createUser() {
        String username = edtUsername.getText().toString().trim();
        String email = edtEmail.getText().toString().trim();
        String phone = edtPhone.getText().toString().trim();
        String address = edtAddress.getText().toString().trim();
        String password = edtPassword.getText().toString().trim();
        String confirmPassword = edtConfirmPassword.getText().toString().trim();

        // 🔹 Kiểm tra dữ liệu nhập
        if (username.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!Validation.isValidEmail(email)) {
            edtEmail.setError("Email không hợp lệ");
            return;
        }

        if (!password.equals(confirmPassword)) {
            edtConfirmPassword.setError("Mật khẩu xác nhận không khớp");
            return;
        }



        // 🔹 Check username/email tồn tại
        userApi.checkAccount(username, email).enqueue(new Callback<Map<String, Boolean>>() {
            @Override
            public void onResponse(Call<Map<String, Boolean>> call, Response<Map<String, Boolean>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Map<String, Boolean> result = response.body();
                    boolean usernameExists = result.getOrDefault("usernameExists", false);
                    boolean emailExists = result.getOrDefault("emailExists", false);

                    if (usernameExists) {

                        edtUsername.setError("Username đã tồn tại!");
                        return;
                    }
                    if (emailExists) {

                        edtEmail.setError("Email already exists!");
                        return;
                    }

                    // ✅ If not duplicate then call API to create user
                    RegisterRequest request = new RegisterRequest(username, password, confirmPassword, email, phone, address);
                    registerUser(request);
                } else {

                    Toast.makeText(AddUserActivity.this, "Could not check account", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Map<String, Boolean>> call, Throwable t) {

                Toast.makeText(AddUserActivity.this, "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void registerUser(RegisterRequest request) {
        userApi.register(request).enqueue(new Callback<ApiResponse<Void>>() {
            @Override
            public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {

                if (response.isSuccessful()) {
                    Toast.makeText(AddUserActivity.this, "✅ User added successfully", Toast.LENGTH_SHORT).show();
                    Intent resultIntent = new Intent();
                    resultIntent.putExtra("addedUser", true);
                    setResult(RESULT_OK, resultIntent);
                    finish();
                } else {
                    Toast.makeText(AddUserActivity.this, "❌ Thêm thất bại: " + response.message(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {

                Toast.makeText(AddUserActivity.this, "Lỗi: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
