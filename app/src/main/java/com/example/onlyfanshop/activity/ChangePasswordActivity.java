package com.example.onlyfanshop.activity;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.onlyfanshop.R;
import com.example.onlyfanshop.api.ApiClient;
import com.example.onlyfanshop.api.ProfileApi;
import com.example.onlyfanshop.model.Request.ChangePasswordRequest;
import com.example.onlyfanshop.model.response.ApiResponse;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ChangePasswordActivity extends AppCompatActivity {

    private TextInputEditText edtOldPassword, edtNewPassword, edtConfirmPassword;
    private MaterialButton btnResetPassword;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_password);

        initViews();
        setupActions();
    }

    private void initViews() {
        edtOldPassword = findViewById(R.id.edtOldPassword);
        edtNewPassword = findViewById(R.id.edtNewPassword);
        edtConfirmPassword = findViewById(R.id.edtConfirmPassword);
        btnResetPassword = findViewById(R.id.btnResetPassword);
        progressBar = findViewById(R.id.progressBar);
    }

    private void setupActions() {
        btnResetPassword.setOnClickListener(v -> {
            if (!validateInputs()) return;
            doChangePassword();
        });
    }

    private boolean validateInputs() {
        String oldPass = getText(edtOldPassword);
        String newPass = getText(edtNewPassword);
        String confirmPass = getText(edtConfirmPassword);

        if (TextUtils.isEmpty(oldPass)) {
            edtOldPassword.setError("Vui lòng nhập mật khẩu hiện tại");
            edtOldPassword.requestFocus();
            return false;
        }

        if (TextUtils.isEmpty(newPass)) {
            edtNewPassword.setError("Vui lòng nhập mật khẩu mới");
            edtNewPassword.requestFocus();
            return false;
        }

//        if (newPass.length() < 6) {
//            edtNewPassword.setError("Mật khẩu mới phải từ 6 ký tự");
//            edtNewPassword.requestFocus();
//            return false;
//        }

        if (!newPass.equals(confirmPass)) {
            edtConfirmPassword.setError("Xác nhận mật khẩu không khớp");
            edtConfirmPassword.requestFocus();
            return false;
        }

        if (oldPass.equals(newPass)) {
            edtNewPassword.setError("Mật khẩu mới không được trùng mật khẩu hiện tại");
            edtNewPassword.requestFocus();
            return false;
        }

        return true;
    }

    private String getText(TextInputEditText editText) {
        return editText.getText() == null ? "" : editText.getText().toString().trim();
    }

    private void doChangePassword() {
        setLoading(true);

        String oldPass = getText(edtOldPassword);
        String newPass = getText(edtNewPassword);

        ProfileApi api = ApiClient.getPrivateClient(this).create(ProfileApi.class);
        ChangePasswordRequest request = new ChangePasswordRequest(oldPass, newPass);

        api.changePassword(request).enqueue(new Callback<ApiResponse>() {
            @Override
            public void onResponse(Call<ApiResponse> call, Response<ApiResponse> response) {
                setLoading(false);
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse body = response.body();
                    // Xem như thành công nếu statusCode == 200 (hoặc một số backend trả 0)
                    if (body.getStatusCode() == 200 || body.getStatusCode() == 0) {
                        Toast.makeText(ChangePasswordActivity.this, body.getMessage() != null ? body.getMessage() : "Đổi mật khẩu thành công", Toast.LENGTH_LONG).show();
                        finish();
                    } else {
                        Toast.makeText(ChangePasswordActivity.this, body.getMessage() != null ? body.getMessage() : "Đổi mật khẩu thất bại", Toast.LENGTH_LONG).show();
                    }
                } else if (response.code() == 400) {
                    Toast.makeText(ChangePasswordActivity.this, "Mật khẩu hiện tại không đúng", Toast.LENGTH_LONG).show();
                } else if (response.code() == 401) {
                    Toast.makeText(ChangePasswordActivity.this, "Phiên đăng nhập hết hạn. Vui lòng đăng nhập lại.", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(ChangePasswordActivity.this, "Lỗi: " + response.code(), Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse> call, Throwable t) {
                setLoading(false);
                Toast.makeText(ChangePasswordActivity.this, "Kết nối thất bại: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void setLoading(boolean loading) {
        btnResetPassword.setEnabled(!loading);
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
    }
}