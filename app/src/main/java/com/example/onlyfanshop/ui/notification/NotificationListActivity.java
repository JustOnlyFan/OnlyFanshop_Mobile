package com.example.onlyfanshop.ui.notification;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
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
import com.google.android.material.floatingactionbutton.FloatingActionButton;

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
    private List<NotificationDTO> allNotifications = new ArrayList<>();
    
    private TextView tabAll, tabPayment, tabContract;
    private ImageButton btnBack, btnSettings;
    
    private String currentFilter = "ALL"; // ALL, PAYMENT, CONTRACT

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification_list);

        initViews();
        setupTabs();
        
        rvNotifications.setLayoutManager(new LinearLayoutManager(this));
        adapter = new NotificationAdapter(this, notificationList);
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
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        } else {
            fetchNotifications(userId);
        }
    }

    private void initViews() {
        rvNotifications = findViewById(R.id.rvNotifications);
        progressBar = findViewById(R.id.progressBar);
        tabAll = findViewById(R.id.tabAll);
        tabPayment = findViewById(R.id.tabPayment);
        tabContract = findViewById(R.id.tabContract);
        btnBack = findViewById(R.id.btnBack);
        btnSettings = findViewById(R.id.btnSettings);
        
        btnBack.setOnClickListener(v -> {
            finish();
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
        });
        btnSettings.setOnClickListener(v -> {
            // TODO: Open notification settings
            Toast.makeText(this, "Cài đặt thông báo", Toast.LENGTH_SHORT).show();
        });
    }

    private void setupTabs() {
        tabAll.setOnClickListener(v -> {
            currentFilter = "ALL";
            selectTab(tabAll);
            filterNotifications();
        });

        tabPayment.setOnClickListener(v -> {
            currentFilter = "PAYMENT";
            selectTab(tabPayment);
            filterNotifications();
        });

        tabContract.setOnClickListener(v -> {
            currentFilter = "CONTRACT";
            selectTab(tabContract);
            filterNotifications();
        });
        
        // Default select "All" tab
        selectTab(tabAll);
    }

    private void selectTab(TextView selectedTab) {
        // Reset all tabs
        tabAll.setTextColor(getResources().getColor(android.R.color.darker_gray));
        tabAll.setBackground(null);
        tabAll.setTypeface(null, android.graphics.Typeface.NORMAL);
        tabPayment.setTextColor(getResources().getColor(android.R.color.darker_gray));
        tabPayment.setBackground(null);
        tabPayment.setTypeface(null, android.graphics.Typeface.NORMAL);
        tabContract.setTextColor(getResources().getColor(android.R.color.darker_gray));
        tabContract.setBackground(null);
        tabContract.setTypeface(null, android.graphics.Typeface.NORMAL);
        
        // Select current tab
        selectedTab.setTextColor(getResources().getColor(android.R.color.black));
        selectedTab.setBackground(getResources().getDrawable(R.drawable.bg_tab_selected));
        selectedTab.setTypeface(null, android.graphics.Typeface.BOLD);
    }

    private void filterNotifications() {
        notificationList.clear();
        
        if ("ALL".equals(currentFilter)) {
            notificationList.addAll(allNotifications);
        } else {
            for (NotificationDTO notification : allNotifications) {
                String message = notification.getMessage().toLowerCase();
                if ("PAYMENT".equals(currentFilter) && (message.contains("thanh toán") || message.contains("payment"))) {
                    notificationList.add(notification);
                } else if ("CONTRACT".equals(currentFilter) && (message.contains("hợp đồng") || message.contains("contract"))) {
                    notificationList.add(notification);
                }
            }
        }
        
        adapter.notifyDataSetChanged();
    }


    private void fetchNotifications(int userId) {
        progressBar.setVisibility(View.VISIBLE);

        NotificationApi apiService = ApiClient.getPrivateClient(this).create(NotificationApi.class);
        apiService.getUserNotifications(userId).enqueue(new Callback<ApiResponse<List<NotificationDTO>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<NotificationDTO>>> call, Response<ApiResponse<List<NotificationDTO>>> response) {
                progressBar.setVisibility(View.GONE);

                if (response.isSuccessful() && response.body() != null) {
                    List<NotificationDTO> notifications = response.body().getData();

                    if (notifications != null && !notifications.isEmpty()) {
                        allNotifications.clear();
                        allNotifications.addAll(notifications);
                        filterNotifications();
                        updateTabBadges();
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

    private void updateTabBadges() {
        // No badges needed for the new design
    }

}
