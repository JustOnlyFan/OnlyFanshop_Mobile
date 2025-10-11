package com.example.onlyfanshop.ui;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.onlyfanshop.R;
import com.example.onlyfanshop.api.ApiClient;
import com.example.onlyfanshop.api.ProfileApi;
import com.example.onlyfanshop.model.User;
import com.example.onlyfanshop.model.Request.UpdateUserRequest;
import com.example.onlyfanshop.model.response.UserResponse;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class EditProfileFragment extends Fragment {

    private TextInputLayout tilUsername, tilEmail, tilPhone, tilAddress;
    private TextInputEditText etUsername, etEmail, etPhone, etAddress;
    private MaterialButton btnSave;
    private View progressBar;

    private ProfileApi api;
    private User baseUser;

    public EditProfileFragment() {
        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_edit_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(v, savedInstanceState);

        api = ApiClient.getPrivateClient(requireContext()).create(ProfileApi.class);
        initViews(v);
        loadUser();

        // System back -> popBackStack trước khi DashboardActivity xử lý về Home
        requireActivity().getOnBackPressedDispatcher().addCallback(
                getViewLifecycleOwner(),
                new OnBackPressedCallback(true) {
                    @Override
                    public void handleOnBackPressed() {
                        if (isEnabled()) {
                            requireActivity().getSupportFragmentManager().popBackStack();
                        }
                    }
                }
        );
    }

    private void initViews(@NonNull View root) {
        MaterialToolbar toolbar = root.findViewById(R.id.toolbar);
        if (toolbar != null) {
            toolbar.setNavigationOnClickListener(v ->
                    requireActivity().getSupportFragmentManager().popBackStack()
            );
        }

        tilUsername = root.findViewById(R.id.tilUsername);
        tilEmail    = root.findViewById(R.id.tilEmail);
        tilPhone    = root.findViewById(R.id.tilPhone);
        tilAddress  = root.findViewById(R.id.tilAddress);

        etUsername = root.findViewById(R.id.etUsername);
        etEmail    = root.findViewById(R.id.etEmail);
        etPhone    = root.findViewById(R.id.etPhone);
        etAddress  = root.findViewById(R.id.etAddress);

        btnSave    = root.findViewById(R.id.btnSave);
        progressBar= root.findViewById(R.id.progressBar);

        if (btnSave != null) {
            btnSave.setOnClickListener(v -> submit());
        }
    }

    private void setLoading(boolean loading) {
        if (progressBar != null) progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        if (btnSave != null) btnSave.setEnabled(!loading);
    }

    private void loadUser() {
        setLoading(true);
        api.getUser().enqueue(new Callback<UserResponse>() {
            @Override
            public void onResponse(@NonNull Call<UserResponse> call, @NonNull Response<UserResponse> response) {
                if (!isAdded()) return;
                setLoading(false);
                if (response.isSuccessful() && response.body() != null) {
                    UserResponse body = response.body();
                    if (body.getStatusCode() == 200 && body.getData() != null) {
                        baseUser = body.getData();
                        prefill(baseUser);
                    } else {
                        Toast.makeText(requireContext(), body.getMessage(), Toast.LENGTH_SHORT).show();
                        requireActivity().getSupportFragmentManager().popBackStack();
                    }
                } else if (response.code() == 401) {
                    Toast.makeText(requireContext(), "Phiên đăng nhập hết hạn. Vui lòng đăng nhập lại.", Toast.LENGTH_LONG).show();
                    requireActivity().getSupportFragmentManager().popBackStack();
                } else {
                    Toast.makeText(requireContext(), "Lỗi: " + response.code(), Toast.LENGTH_SHORT).show();
                    requireActivity().getSupportFragmentManager().popBackStack();
                }
            }

            @Override
            public void onFailure(@NonNull Call<UserResponse> call, @NonNull Throwable t) {
                if (!isAdded()) return;
                setLoading(false);
                Toast.makeText(requireContext(), "Kết nối thất bại: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                requireActivity().getSupportFragmentManager().popBackStack();
            }
        });
    }

    // Giá trị null/rỗng/"string"/"null" => coi như thiếu
    private boolean isMissing(String v) {
        if (v == null) return true;
        String s = v.trim();
        return s.isEmpty() || "string".equalsIgnoreCase(s) || "null".equalsIgnoreCase(s);
    }

    private void prefill(User u) {
        if (etUsername != null) etUsername.setText(u.getUsername() == null ? "" : u.getUsername());
        if (etEmail    != null) etEmail.setText(u.getEmail() == null ? "" : u.getEmail());
        if (etPhone    != null) etPhone.setText(isMissing(u.getPhoneNumber()) ? "" : u.getPhoneNumber());
        if (etAddress  != null) etAddress.setText(isMissing(u.getAddress()) ? "" : u.getAddress());
    }

    private boolean validate() {
        if (tilUsername != null) tilUsername.setError(null);
        if (tilEmail != null) tilEmail.setError(null);

        if (etUsername == null || TextUtils.isEmpty(etUsername.getText())) {
            if (tilUsername != null) tilUsername.setError("Không được bỏ trống");
            return false;
        }
        String email = etEmail == null ? "" : String.valueOf(etEmail.getText()).trim();
        if (TextUtils.isEmpty(email)) {
            if (tilEmail != null) tilEmail.setError("Không được bỏ trống");
            return false;
        }
        if (!email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            if (tilEmail != null) tilEmail.setError("Email không hợp lệ");
            return false;
        }
        return true;
    }

    private void submit() {
        if (baseUser == null) return;
        if (!validate()) return;

        setLoading(true);

        UpdateUserRequest body = new UpdateUserRequest(
                baseUser.getUserID(),
                String.valueOf(etUsername.getText()).trim(),
                String.valueOf(etEmail.getText()).trim(),
                etPhone == null ? "" : String.valueOf(etPhone.getText()).trim(),
                etAddress == null ? "" : String.valueOf(etAddress.getText()).trim(),
                baseUser.getRole(),
                baseUser.getAuthProvider(),
                baseUser.getToken()
        );

        api.updateUser(body).enqueue(new Callback<UserResponse>() {
            @Override
            public void onResponse(@NonNull Call<UserResponse> call, @NonNull Response<UserResponse> response) {
                if (!isAdded()) return;
                setLoading(false);
                if (response.isSuccessful() && response.body() != null) {
                    UserResponse r = response.body();
                    if (r.getStatusCode() == 200) {
                        Toast.makeText(requireContext(), "Cập nhật hồ sơ thành công", Toast.LENGTH_SHORT).show();
                        // Pop về Profile
                        requireActivity().getSupportFragmentManager().popBackStack();
                    } else {
                        Toast.makeText(requireContext(), r.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                } else if (response.code() == 401) {
                    Toast.makeText(requireContext(), "Phiên đăng nhập hết hạn. Vui lòng đăng nhập lại.", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(requireContext(), "Lỗi: " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<UserResponse> call, @NonNull Throwable t) {
                if (!isAdded()) return;
                setLoading(false);
                Toast.makeText(requireContext(), "Kết nối thất bại: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}