package com.example.onlyfanshop.ui.order;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.onlyfanshop.R;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.example.onlyfanshop.adapter.OrderAdapter;
import com.example.onlyfanshop.api.ApiClient;
import com.example.onlyfanshop.api.OrderApi;
import com.example.onlyfanshop.model.OrderDTO;
import com.example.onlyfanshop.model.response.ApiResponse;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class UserOrderFragment extends Fragment {

    private static final String ARG_STATUS = "status";

    private RecyclerView rvOrders;
    private LinearLayout layoutEmpty;
    private TextView tvTitle;
    private ImageButton btnBack;
    private OrderAdapter orderAdapter;
    private OrderApi orderApi;
    private Button currentSelectedButton;

    private Button btnPending, btnPicking, btnShipping, btnDelivered;

    public static UserOrderFragment newInstance(String status) {
        UserOrderFragment fragment = new UserOrderFragment();
        Bundle args = new Bundle();
        args.putString(ARG_STATUS, status);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_order, container, false);

        rvOrders = view.findViewById(R.id.rvOrders);
        layoutEmpty = view.findViewById(R.id.layoutEmpty);
        tvTitle = view.findViewById(R.id.tvTitle);
        btnBack = view.findViewById(R.id.btnBack);
        btnPending = view.findViewById(R.id.btnPending);
        btnPicking = view.findViewById(R.id.btnPicking);
        btnShipping = view.findViewById(R.id.btnShipping);
        btnDelivered = view.findViewById(R.id.btnDelivered);
        
        // Setup edge-to-edge insets handling AFTER views are initialized
        setupWindowInsets(view);

        // Hide admin-only buttons (All, Returns/Refunds, Cancelled) for regular users
        Button btnAll = view.findViewById(R.id.btnAll);
        Button btnReturnsRefunds = view.findViewById(R.id.btnReturnsRefunds);
        Button btnCancelled = view.findViewById(R.id.btnCancelled);
        if (btnAll != null) btnAll.setVisibility(View.GONE);
        if (btnReturnsRefunds != null) btnReturnsRefunds.setVisibility(View.GONE);
        if (btnCancelled != null) btnCancelled.setVisibility(View.GONE);

        // Setup back button
        btnBack.setOnClickListener(v -> {
            if (getActivity() != null) {
                getActivity().onBackPressed();
            }
        });

        rvOrders.setLayoutManager(new LinearLayoutManager(getContext()));
        // Tối ưu scroll performance
        rvOrders.setHasFixedSize(true);
        rvOrders.setItemViewCacheSize(10);
        rvOrders.setItemAnimator(null); // Tắt animation khi scroll để mượt hơn
        orderAdapter = new OrderAdapter(null);
        rvOrders.setAdapter(orderAdapter);

        orderApi = ApiClient.getPrivateClient(requireContext()).create(OrderApi.class);

        // Get status from arguments
        String initialStatus = null;
        if (getArguments() != null) {
            String statusArg = getArguments().getString(ARG_STATUS);
            if (statusArg != null) {
                // Map status from ProfileFragment to API status
                initialStatus = mapStatusToApi(statusArg);
            }
        }

        // Setup button click listeners (no "All" button)
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

        // Load orders with initial status and select corresponding button
        if (initialStatus != null) {
            selectButtonByStatus(initialStatus);
            loadOrders(initialStatus);
        } else {
            // Default to Pending
            selectButton(btnPending);
            loadOrdersPending();
        }

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        // Ẩn bottom navigation khi fragment được hiển thị
        hideBottomNavigation();
    }

    @Override
    public void onPause() {
        super.onPause();
        // Hiện lại bottom navigation khi fragment bị pause
        showBottomNavigation();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Đảm bảo hiện lại bottom navigation khi fragment bị destroy
        showBottomNavigation();
    }

    private void hideBottomNavigation() {
        if (getActivity() != null) {
            View bottomNavView = getActivity().findViewById(R.id.bottomNav);
            if (bottomNavView != null) {
                bottomNavView.setVisibility(View.GONE);
            }
        }
    }

    private void showBottomNavigation() {
        if (getActivity() != null) {
            View bottomNavView = getActivity().findViewById(R.id.bottomNav);
            if (bottomNavView != null) {
                bottomNavView.setVisibility(View.VISIBLE);
            }
        }
    }

    private String mapStatusToApi(String statusFromProfile) {
        // Map status from ProfileFragment buttons to API status
        switch (statusFromProfile) {
            case "PENDING":
                return "PENDING";
            case "READY_TO_SHIP":
                return "PICKING"; // Ready to ship = Picking in backend
            case "SHIPPING":
                return "SHIPPING";
            case "DELIVERED":
                return "DELIVERED";
            default:
                return null;
        }
    }

    private void selectButtonByStatus(String status) {
        Button buttonToSelect = null;
        switch (status) {
            case "PENDING":
                buttonToSelect = btnPending;
                break;
            case "PICKING":
                buttonToSelect = btnPicking;
                break;
            case "SHIPPING":
                buttonToSelect = btnShipping;
                break;
            case "DELIVERED":
                buttonToSelect = btnDelivered;
                break;
            default:
                buttonToSelect = btnPending;
        }
        if (buttonToSelect != null) {
            selectButton(buttonToSelect);
        }
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
                Toast.makeText(requireContext(), "Lỗi mạng: " + t.getMessage(), Toast.LENGTH_SHORT).show();
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
                Toast.makeText(requireContext(), "Lỗi mạng: " + t.getMessage(), Toast.LENGTH_SHORT).show();
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
                Toast.makeText(requireContext(), "Lỗi mạng: " + t.getMessage(), Toast.LENGTH_SHORT).show();
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
                Toast.makeText(requireContext(), "Lỗi mạng: " + t.getMessage(), Toast.LENGTH_SHORT).show();
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
                Toast.makeText(requireContext(), "Lỗi mạng: " + t.getMessage(), Toast.LENGTH_SHORT).show();
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
                Toast.makeText(requireContext(), "Lỗi mạng: " + t.getMessage(), Toast.LENGTH_SHORT).show();
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
        // Reset previous button
        if (currentSelectedButton != null) {
            currentSelectedButton.setBackgroundTintList(
                    ContextCompat.getColorStateList(requireContext(), R.color.gray));
            currentSelectedButton.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.black));
        }

        // Set current button
        currentSelectedButton = selectedButton;

        // Highlight current button
        currentSelectedButton.setBackgroundTintList(
                ContextCompat.getColorStateList(requireContext(), R.color.colorPrimary));
        currentSelectedButton.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white));
    }

    private void setupWindowInsets(View root) {
        // Convert dp to pixels for the 12dp padding defined in XML
        float density = getResources().getDisplayMetrics().density;
        int padding12dp = (int) (12 * density);
        
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            
            // Set padding for root view: preserve XML padding (12dp) + add system insets
            int bottomPadding = padding12dp + systemBars.bottom + 32; // Add 32dp extra for safety
            v.setPadding(
                    padding12dp + systemBars.left, 
                    padding12dp + systemBars.top, 
                    padding12dp + systemBars.right, 
                    bottomPadding
            );
            
            return insets;
        });
        
        // Request to apply insets immediately
        ViewCompat.requestApplyInsets(root);
    }
}

