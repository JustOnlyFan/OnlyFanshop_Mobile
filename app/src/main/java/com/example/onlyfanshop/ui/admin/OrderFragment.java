package com.example.onlyfanshop.ui.admin;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.onlyfanshop.R;
import com.example.onlyfanshop.adapter.OrderAdapter;
import com.example.onlyfanshop.adapter.OrderAdapterAdmin;
import com.example.onlyfanshop.api.ApiClient;
import com.example.onlyfanshop.api.OrderApi;

import com.example.onlyfanshop.model.OrderDTO;
import com.example.onlyfanshop.model.response.ApiResponse;


import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class OrderFragment extends Fragment {

    private RecyclerView rvOrders;
    private LinearLayout layoutEmpty;
    private OrderAdapterAdmin orderAdapter;
    private OrderApi orderApi;
    private Button currentSelectedButton; // lưu nút đang được chọn

    private Button btnAll, btnPending, btnConfirmed, btnShipping, btnCompleted;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_order, container, false);

        rvOrders = view.findViewById(R.id.rvOrders);
        layoutEmpty = view.findViewById(R.id.layoutEmpty);
        btnAll = view.findViewById(R.id.btnAll);
        btnPending = view.findViewById(R.id.btnPending);
        btnConfirmed = view.findViewById(R.id.btnConfirmed);
        btnShipping = view.findViewById(R.id.btnShipping);
        btnCompleted = view.findViewById(R.id.btnCompleted);

        rvOrders.setLayoutManager(new LinearLayoutManager(getContext()));
        orderAdapter = new OrderAdapterAdmin(null);
        rvOrders.setAdapter(orderAdapter);

        orderApi = ApiClient.getPrivateClient(requireContext()).create(OrderApi.class);


        selectButton(btnAll);
        loadOrders(null);

        // Xử lý click nút lọc
        btnAll.setOnClickListener(v -> {
            selectButton(btnAll);
            loadOrders(null);
        });
        btnPending.setOnClickListener(v -> {
            selectButton(btnPending);
            loadOrders("Pending");
        });
        btnConfirmed.setOnClickListener(v -> {
            selectButton(btnConfirmed);
            loadOrders("Confirmed");
        });
        btnShipping.setOnClickListener(v -> {
            selectButton(btnShipping);
            loadOrders("Shipping");
        });
        btnCompleted.setOnClickListener(v -> {
            selectButton(btnCompleted);
            loadOrders("Completed");
        });

        return view;
    }

    private void loadOrders(String status) {
        Call<ApiResponse<List<OrderDTO>>> call = orderApi.getOrders(status);
        call.enqueue(new Callback<ApiResponse<List<OrderDTO>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<OrderDTO>>> call, Response<ApiResponse<List<OrderDTO>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<OrderDTO> orders = response.body().getData();
                    if (orders == null || orders.isEmpty()) {
                        showEmptyState(true);
                    } else {
                        showEmptyState(false);
                        orderAdapter.setOrderList(orders);
                    }
                } else {
                    showEmptyState(true);
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<List<OrderDTO>>> call, Throwable t) {
                showEmptyState(true);
            }
        });
    }

    private void showEmptyState(boolean isEmpty) {
        if (isEmpty) {
            layoutEmpty.setVisibility(View.VISIBLE);
            rvOrders.setVisibility(View.GONE);
        } else {
            layoutEmpty.setVisibility(View.GONE);
            rvOrders.setVisibility(View.VISIBLE);
        }
    }
    private void selectButton(Button selectedButton) {
        // Reset nút trước
        if (currentSelectedButton != null) {
            currentSelectedButton.setBackgroundTintList(
                    ContextCompat.getColorStateList(requireContext(), R.color.gray));
            currentSelectedButton.setTextColor(ContextCompat.getColor(requireContext(), R.color.black));
        }

        // Gán nút hiện tại
        currentSelectedButton = selectedButton;

        // Đổi màu nút hiện tại sang primary
        currentSelectedButton.setBackgroundTintList(
                ContextCompat.getColorStateList(requireContext(), R.color.colorPrimary));
        currentSelectedButton.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white));
    }
}
