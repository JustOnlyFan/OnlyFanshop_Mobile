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

    private Button btnAll, btnPending, btnPicking, btnShipping, btnDelivered, btnReturnsRefunds, btnCancelled;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_order, container, false);

        rvOrders = view.findViewById(R.id.rvOrders);
        layoutEmpty = view.findViewById(R.id.layoutEmpty);
        btnAll = view.findViewById(R.id.btnAll);
        btnPending = view.findViewById(R.id.btnPending);
        btnPicking = view.findViewById(R.id.btnPicking);
        btnShipping = view.findViewById(R.id.btnShipping);
        btnDelivered = view.findViewById(R.id.btnDelivered);
        btnReturnsRefunds = view.findViewById(R.id.btnReturnsRefunds);
        btnCancelled = view.findViewById(R.id.btnCancelled);

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
            loadOrdersPending();
        });
        btnPicking.setOnClickListener(v -> {
            selectButton(btnPicking);
            loadOrdersByStatus("PICKING");
        });
        btnShipping.setOnClickListener(v -> {
            selectButton(btnShipping);
            loadOrdersShipping();
        });
        btnDelivered.setOnClickListener(v -> {
            selectButton(btnDelivered);
            loadOrdersByStatus("DELIVERED");
        });
        btnReturnsRefunds.setOnClickListener(v -> {
            selectButton(btnReturnsRefunds);
            loadOrdersByStatus("RETURNS_REFUNDS");
        });
        btnCancelled.setOnClickListener(v -> {
            selectButton(btnCancelled);
            loadOrdersByStatus("CANCELLED");
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

    private void loadOrdersPending() {
        Call<ApiResponse<List<OrderDTO>>> call = orderApi.getOrdersPending();
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

    private void loadOrdersConfirmed() {
        Call<ApiResponse<List<OrderDTO>>> call = orderApi.getOrdersConfirmed();
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

    private void loadOrdersShipping() {
        Call<ApiResponse<List<OrderDTO>>> call = orderApi.getOrdersShipping();
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

    private void loadOrdersCompleted() {
        Call<ApiResponse<List<OrderDTO>>> call = orderApi.getOrdersCompleted();
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

    private void loadOrdersByStatus(String status) {
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
        // Kiểm tra null để tránh crash
        if (selectedButton == null) {
            return;
        }

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
