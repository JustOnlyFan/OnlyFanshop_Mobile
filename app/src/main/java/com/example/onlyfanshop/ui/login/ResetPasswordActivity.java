package com.example.onlyfanshop.ui.login;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.example.onlyfanshop.R;
import com.example.onlyfanshop.api.ApiClient;
import com.example.onlyfanshop.api.UserApi;
import com.example.onlyfanshop.model.response.ApiResponse;
import com.example.onlyfanshop.utils.Validation;
import com.google.android.material.textfield.TextInputLayout;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ResetPasswordActivity extends AppCompatActivity {
    private UserApi userApi;
    private EditText edtNewPassword, edtConfirmPassword;
    private Button btnResetPassword;
    private TextInputLayout layoutPassword ;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_reset_password);
        userApi = ApiClient.getPublicClient().create(UserApi.class);
        String email = getIntent().getStringExtra("email");
        edtNewPassword = findViewById(R.id.edtNewPassword);
        edtConfirmPassword = findViewById(R.id.edtConfirmPassword);
        btnResetPassword = findViewById(R.id.btnResetPassword);
        layoutPassword = findViewById(R.id.layoutPassword);
        btnResetPassword.setOnClickListener(v -> {
            String password = edtNewPassword.getText().toString().trim();
            String confirmPassword = edtConfirmPassword.getText().toString().trim();

            resetPassword(email, password, confirmPassword);
        });



    }

    private void resetPassword(String email, String newPassword, String confirmPassword) {
        if (newPassword.isEmpty()) {
            edtNewPassword.setBackgroundResource(R.drawable.edittext_error);
            edtNewPassword.setError("Vui lòng nhập mật khẩu mới");
            return;
        }
        if (confirmPassword.isEmpty()) {
            edtConfirmPassword.setBackgroundResource(R.drawable.edittext_error);
            edtConfirmPassword.setError("Vui lòng nhập xác nhận mật khẩu");
            return;
        }
        if (!newPassword.equals(confirmPassword)) {
            edtConfirmPassword.setBackgroundResource(R.drawable.edittext_error);
            edtConfirmPassword.setError("Mật khẩu không khớp");
            return;
        }
        if (!Validation.isValidPassword(newPassword)) {
            edtNewPassword.setBackgroundResource(R.drawable.edittext_error);
            layoutPassword.setError("Mật khẩu phải có ít nhất 8 ký tự, gồm chữ, số và ký tự đặc biệt");
            Toast.makeText(this, "Mật khẩu không hợp lệ!", Toast.LENGTH_SHORT).show();
            return;
        }else {
            layoutPassword.setError(null);
            layoutPassword.setErrorEnabled(false); // 🔹 khôi phục lại icon con mắt
        }

        Call<ApiResponse<Void>> call = userApi.resetPassword(email, newPassword);
        call.enqueue(new Callback<ApiResponse<Void>>() {
                         @Override
                         public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {
                             if (response.isSuccessful() && response.body() != null) {
                                 ApiResponse<Void> apiResponse = response.body();
                                 Toast.makeText(ResetPasswordActivity.this, apiResponse.getMessage(), Toast.LENGTH_SHORT).show();
                                 Intent intent = new Intent(ResetPasswordActivity.this, LoginActivity.class);
                                 intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                                 startActivity(intent);
                                 finish(); // đóng ResetPassActivity
                             } else {
                                 Toast.makeText(ResetPasswordActivity.this, "Không thể đặt lại mật khẩu", Toast.LENGTH_SHORT).show();
                             }
                         }

                         @Override
                         public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                             Toast.makeText(ResetPasswordActivity.this, "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                         }
                     }
        );
    }
}