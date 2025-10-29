package com.example.onlyfanshop.ui.notification;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.onlyfanshop.R;
import com.example.onlyfanshop.adapter.NotificationAdapter;
import com.example.onlyfanshop.api.ApiClient;
import com.example.onlyfanshop.api.NotificationApi;

import com.example.onlyfanshop.model.NotificationDTO;
import com.example.onlyfanshop.model.response.ApiResponse;
import com.example.onlyfanshop.ui.login.LoginActivity;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class NotificationListActivity extends AppCompatActivity {

    private RecyclerView rvNotifications;
    private ProgressBar progressBar;
    private NotificationAdapter adapter;
    private List<NotificationDTO> notificationList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification_list);

        rvNotifications = findViewById(R.id.rvNotifications);
        progressBar = findViewById(R.id.progressBar);

        rvNotifications.setLayoutManager(new LinearLayoutManager(this));
        adapter = new NotificationAdapter(this,notificationList);
        rvNotifications.setAdapter(adapter);

        // Lấy userId từ Intent (hoặc SharedPreferences nếu cần)
        int userId = getIntent().getIntExtra("userId", -1);
        if (userId == -1) {
            SharedPreferences prefs = getSharedPreferences("MyAppPrefs", MODE_PRIVATE);
            userId = prefs.getInt("userId", -1);
        }

        if (userId == -1) {
            // Nếu chưa đăng nhập
            Toast.makeText(this, "Vui lòng đăng nhập để xem thông báo", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        } else {
            fetchNotifications(userId);
        }
    }

    private void fetchNotifications(int userId) {
        progressBar.setVisibility(View.VISIBLE);

        NotificationApi apiService = ApiClient.getPrivateClient(this).create(NotificationApi.class);
        apiService.getUserNotifications(userId).enqueue(new Callback<ApiResponse<List<NotificationDTO>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<NotificationDTO>>> call, Response<ApiResponse<List<NotificationDTO>>> response) {
                progressBar.setVisibility(View.GONE);

                if (response.isSuccessful() && response.body() != null) {
                    List<NotificationDTO> notifications = response.body().getData(); // ✅ lấy danh sách từ ApiResponse

                    if (notifications != null && !notifications.isEmpty()) {
                        notificationList.clear();
                        notificationList.addAll(notifications);
                        adapter.notifyDataSetChanged();
                    } else {
                        Toast.makeText(NotificationListActivity.this, "Không có thông báo nào", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(NotificationListActivity.this, "Không có thông báo nào", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<List<NotificationDTO>>> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(NotificationListActivity.this, "Lỗi tải thông báo: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

}
