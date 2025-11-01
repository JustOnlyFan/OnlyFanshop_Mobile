package com.example.onlyfanshop.ui.order;


import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.onlyfanshop.R;
import com.example.onlyfanshop.adapter.OrderAdapter;
import com.example.onlyfanshop.api.ApiClient;
import com.example.onlyfanshop.api.OrderApi;
import com.example.onlyfanshop.model.response.ApiResponse;
import com.example.onlyfanshop.model.OrderDTO;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class OrderHistoryActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private OrderAdapter orderAdapter;

    private LinearLayout btnBackMain;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_history);

        recyclerView = findViewById(R.id.rvOrders);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        btnBackMain= findViewById(R.id.btnBackMain);
        btnBackMain.setOnClickListener(v -> finish());

        loadOrders();
    }

    private void loadOrders() {
        OrderApi api = ApiClient.getPrivateClient(this).create(OrderApi.class);
        String status = getIntent().getStringExtra("status");

        RecyclerView recyclerView = findViewById(R.id.rvOrders);
        LinearLayout layoutEmpty = findViewById(R.id.layoutEmpty);

        api.getOrders(status).enqueue(new Callback<ApiResponse<List<OrderDTO>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<OrderDTO>>> call, Response<ApiResponse<List<OrderDTO>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<OrderDTO> orders = response.body().getData();
                    if (orders == null || orders.isEmpty()) {
                        layoutEmpty.setVisibility(View.VISIBLE);
                        recyclerView.setVisibility(View.GONE);
                    } else {
                        layoutEmpty.setVisibility(View.GONE);
                        recyclerView.setVisibility(View.VISIBLE);

                        orderAdapter = new OrderAdapter(orders);
                        recyclerView.setAdapter(orderAdapter);
                    }
                } else {
                    Toast.makeText(OrderHistoryActivity.this, "Không có lịch sử đơn hàng", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<List<OrderDTO>>> call, Throwable t) {
                Toast.makeText(OrderHistoryActivity.this, "Lỗi mạng: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

}
