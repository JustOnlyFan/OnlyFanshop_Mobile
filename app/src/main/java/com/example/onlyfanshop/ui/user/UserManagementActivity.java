package com.example.onlyfanshop.ui.user;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.onlyfanshop.R;
import com.example.onlyfanshop.adapter.UserAdapter;
import com.example.onlyfanshop.api.ApiClient;
import com.example.onlyfanshop.api.UserApi;
import com.example.onlyfanshop.model.UserDTO;
import com.example.onlyfanshop.model.response.ApiResponse;
import com.example.onlyfanshop.model.response.UserPageResponse;

import java.util.List;
import java.util.stream.Collectors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class UserManagementActivity extends AppCompatActivity {

    private RecyclerView rcvUserList;
    private ProgressBar progressBar;
    private UserAdapter adapter;
    private UserApi userApi;

    private EditText edtSearchUser;
    private ImageButton btnSearchUser;
    private Spinner spinnerSearchType, spinnerRole;
    private Button btnAddUser;

    private int currentPage = 0;
    private int totalPages = 1;

    private String selectedSearchType = "Email";
    private String selectedRole = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_management);

        // Setup Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        // Ánh xạ view
        rcvUserList = findViewById(R.id.rcvUserList);
        progressBar = findViewById(R.id.progressBar);
        edtSearchUser = findViewById(R.id.edtSearchUser);
        btnSearchUser = findViewById(R.id.btnSearchUser);
        spinnerSearchType = findViewById(R.id.spinnerSearchType);
        spinnerRole = findViewById(R.id.spinnerRole);
        btnAddUser = findViewById(R.id.btnAddUser);

        btnAddUser.setOnClickListener(v -> {
            Intent intent = new Intent(UserManagementActivity.this, AddUserActivity.class);
            startActivityForResult(intent, 100);
        });

        // Setup RecyclerView
        rcvUserList.setLayoutManager(new LinearLayoutManager(this));

        // Khởi tạo API
        userApi = ApiClient.getPrivateClient(this).create(UserApi.class);

        // Setup search criteria spinner
        ArrayAdapter<CharSequence> searchTypeAdapter = ArrayAdapter.createFromResource(
                this, R.array.user_filter_options, android.R.layout.simple_spinner_item);
        searchTypeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSearchType.setAdapter(searchTypeAdapter);

        spinnerSearchType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedSearchType = parent.getItemAtPosition(position).toString();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // Setup role spinner (e.g. 3 types)
        String[] roles = {"All", "ADMIN", "CUSTOMER"};
        ArrayAdapter<String> roleAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, roles);
        roleAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerRole.setAdapter(roleAdapter);

        spinnerRole.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String role = parent.getItemAtPosition(position).toString();
                selectedRole = role.equals("All") ? null : role;
                currentPage = 0;
                loadUsers(null, selectedRole, "username", "ASC");
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // Search event
        btnSearchUser.setOnClickListener(v -> {
            String keyword = edtSearchUser.getText().toString().trim();
            if (keyword.isEmpty()) {
                Toast.makeText(this, "Please enter search keyword", Toast.LENGTH_SHORT).show();
                return;
            }
            currentPage = 0;
            loadUsers(keyword, selectedRole, "username", "ASC");
        });

        // Load initial list
        loadUsers(null, null, "username", "ASC");
    }

    private void loadUsers(String keyword, String role, String sortField, String sortDirection) {
        progressBar.setVisibility(View.VISIBLE);

        userApi.getAllUsers(keyword, role, currentPage, 10, sortField, sortDirection)
                .enqueue(new Callback<ApiResponse<UserPageResponse>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<UserPageResponse>> call,
                                           Response<ApiResponse<UserPageResponse>> response) {
                        progressBar.setVisibility(View.GONE);
                        if (response.isSuccessful() && response.body() != null) {
                            UserPageResponse pageData = response.body().getData();
                            if (pageData != null) {
                                List<UserDTO> userList = pageData.getContent();

                                // If search type selected, filter additionally on client
                                if (keyword != null && !keyword.isEmpty()) {
                                    userList = filterBySearchType(userList, keyword, selectedSearchType);
                                }

                                adapter = new UserAdapter(UserManagementActivity.this, userList,
                                        user -> Toast.makeText(UserManagementActivity.this,
                                                "Selected: " + user.getUsername(), Toast.LENGTH_SHORT).show());
                                rcvUserList.setAdapter(adapter);

                                totalPages = pageData.getTotalPages();
                            }
                        } else {
                            Toast.makeText(UserManagementActivity.this,
                                    "Không thể tải dữ liệu", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<UserPageResponse>> call, Throwable t) {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(UserManagementActivity.this,
                                "Lỗi: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // Hàm lọc client-side theo tiêu chí tìm kiếm
    private List<UserDTO> filterBySearchType(List<UserDTO> list, String keyword, String searchType) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return list; // không tìm gì thì trả lại toàn bộ
        }

        final String lowerKeyword = keyword.toLowerCase();
        return list.stream()
                .filter(u -> {
                    switch (searchType) {
                        case "Email":
                            return u.getEmail() != null && u.getEmail().toLowerCase().contains(lowerKeyword);
                        case "Tên đăng nhập":
                            return u.getUsername() != null && u.getUsername().toLowerCase().contains(lowerKeyword);
                        case "Số điện thoại":
                            return u.getPhoneNumber() != null && u.getPhoneNumber().contains(lowerKeyword);
                        default:
                            // nếu searchType bị null hoặc không khớp, tìm trong tất cả các trường
                            return (u.getEmail() != null && u.getEmail().toLowerCase().contains(lowerKeyword))
                                    || (u.getUsername() != null && u.getUsername().toLowerCase().contains(lowerKeyword))
                                    || (u.getPhoneNumber() != null && u.getPhoneNumber().contains(lowerKeyword));
                    }
                })
                .collect(Collectors.toList()); // tương thích Java 8 trở lên
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 100 && resultCode == RESULT_OK) {
            boolean addedUser = data != null && data.getBooleanExtra("addedUser", false);
            if (addedUser) {
                currentPage = 0;
                loadUsers(null, selectedRole, "username", "ASC"); // Gọi lại API để load danh sách mới
            }
        }
    }

}
