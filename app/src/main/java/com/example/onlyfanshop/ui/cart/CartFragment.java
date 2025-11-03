package com.example.onlyfanshop.ui.cart;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.graphics.Canvas;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.onlyfanshop.R;
import com.example.onlyfanshop.activity.DashboardActivity;
import com.example.onlyfanshop.adapter.CartAdapter;
import com.example.onlyfanshop.api.ApiClient;
import com.example.onlyfanshop.api.CartItemApi;
import com.example.onlyfanshop.databinding.FragmentCartBinding;
import com.example.onlyfanshop.model.CartItemDTO;
import com.example.onlyfanshop.model.response.ApiResponse;
import com.example.onlyfanshop.ui.payment.ConfirmPaymentActivity;

import java.io.Serializable;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CartFragment extends Fragment {

    private FragmentCartBinding binding;
    private CartAdapter cartAdapter;
    private List<CartItemDTO> cartItems = new ArrayList<>();
    private double totalPrice = 0;
    private String USERNAME;

    public static CartFragment newInstance(String username) {
        CartFragment fragment = new CartFragment();
        Bundle args = new Bundle();
        args.putString("username", username);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentCartBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Kiểm tra token đăng nhập
        String token = com.example.onlyfanshop.api.ApiClient.getToken(requireContext());
        if (token == null || token.trim().isEmpty()) {
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.mainFragmentContainer, new com.example.onlyfanshop.ui.PleaseSignInFragment(), "PLEASE_SIGN_IN")
                    .commit();
            return; // Dừng xử lý tiếp
        }

        if (getArguments() != null) {
            USERNAME = getArguments().getString("username");
        }

        binding.rclViewCart.setLayoutManager(new LinearLayoutManager(requireContext()));
        cartAdapter = new CartAdapter(requireContext(), cartItems, true);
        binding.rclViewCart.setAdapter(cartAdapter);

        // Add swipe to delete functionality
        setupSwipeToDelete();

        // Add wipe out animation
        animateCartEnter();

        // Setup back button
        binding.btnBack.setOnClickListener(v -> {
            if (requireActivity() instanceof DashboardActivity) {
                DashboardActivity dashboardActivity = (DashboardActivity) requireActivity();
                dashboardActivity.getBottomNav().setSelectedItemId(R.id.nav_home);
            }
        });

        cartAdapter.setOnQuantityChangeListener(new CartAdapter.OnQuantityChangeListener() {
            @Override
            public void onIncrease(int productId) {
                addQuantity(USERNAME, productId);
            }

            @Override
            public void onDecrease(int productId) {
                minusQuantity(USERNAME, productId);
            }
        });

        cartAdapter.setOnCheckItem(new CartAdapter.OnCheckItem() {
            @Override
            public void onCheckItem(CartItemDTO cartItem) {
                    onCheck(cartItem);
            }
        });

        getCartItems(USERNAME);

        binding.checkoutBtn.setOnClickListener(v -> {
            if(totalPrice>0){
            confirmCheckout();
            }
        });
        binding.clearAllBtn.setOnClickListener(v -> clear(USERNAME));
    }

    private void onCheck(CartItemDTO cartItem){
        CartItemApi api = ApiClient.getPrivateClient(requireContext()).create(CartItemApi.class);
        api.onCheck(cartItem.getCartItemID()).enqueue(new Callback<ApiResponse<Void>>() {
            @Override
            public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {
                if (response.isSuccessful()){
                    getCartItems(USERNAME);
                } else{
                    Toast.makeText(requireContext(), "Cập nhật thất bại", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                Toast.makeText(requireContext(), "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void confirmCheckout() {
        Intent intent = new Intent(requireContext(), ConfirmPaymentActivity.class);
        intent.putExtra("totalPrice", totalPrice);
        intent.putExtra("cartItems", (Serializable) cartItems);

        startActivity(intent);
    }
    private void clear(String username){
        CartItemApi api = ApiClient.getPrivateClient(requireContext()).create(CartItemApi.class);
        api.clearCart(username).enqueue(new Callback<ApiResponse<Void>>() {

            @Override
            public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {
                if(response.isSuccessful()){
                    getCartItems(USERNAME);
                    Toast.makeText(requireContext(), "Xóa toàn bộ thành công", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(requireContext(), "Xóa thất bại", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Void>> call, Throwable throwable) {
                Toast.makeText(requireContext(), "Lỗi kết nối: " + throwable.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void getCartItems(String username) {
        CartItemApi cartItemApi = ApiClient.getPrivateClient(requireContext()).create(CartItemApi.class);
        cartItemApi.getCartItem(username).enqueue(new Callback<ApiResponse<List<CartItemDTO>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<CartItemDTO>>> call, Response<ApiResponse<List<CartItemDTO>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<CartItemDTO> list = response.body().getData();
                    if (list == null) {
                        Toast.makeText(requireContext(), "Không có dữ liệu giỏ hàng", Toast.LENGTH_SHORT).show();
                    } else {
                        totalPrice = 0;
                        cartItems = new ArrayList<>();
                        cartItems.addAll(list);
                        cartAdapter.setData(cartItems);

                        if (cartAdapter.getItemCount() == 0) {
                            binding.textEmpty.setVisibility(View.VISIBLE);
                            binding.checkoutBtn.setVisibility(View.GONE);
                            binding.clearAllBtn.setVisibility(View.GONE);
                            binding.bottomSummary.setVisibility(View.GONE);
                        } else {
                            binding.textEmpty.setVisibility(View.GONE);
                            binding.checkoutBtn.setVisibility(View.VISIBLE);
                            binding.clearAllBtn.setVisibility(View.VISIBLE);
                        }
                        for (CartItemDTO item : cartItems) {
                            if(item.isChecked()){
                            totalPrice += item.getPrice();
                            }
                        }

                        binding.totalPrice.setText(formatCurrencyVND(totalPrice));

                        // Cập nhật badge ngay sau khi dữ liệu giỏ hàng đã được làm mới
                        if (isAdded() && requireActivity() instanceof DashboardActivity) {
                            ((DashboardActivity) requireActivity()).updateCartBadgeNow();
                        }
                    }
                } else {
                    Log.e("CartItem", "Response not successful or body is null");
                    Toast.makeText(requireContext(), "Failed to load cart items ", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<List<CartItemDTO>>> call, Throwable t) {
                Toast.makeText(requireContext(), "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void addQuantity(String username, int productId) {
        CartItemApi api = ApiClient.getPrivateClient(requireContext()).create(CartItemApi.class);
        api.addQuantity(username, productId).enqueue(new Callback<ApiResponse<Void>>() {
            @Override
            public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {
                if (response.isSuccessful()) {
                    // Sau khi server cập nhật, tải lại giỏ -> onResponse của getCartItems sẽ cập nhật badge
                    getCartItems(USERNAME);
                    ((DashboardActivity) requireActivity()).updateCartBadgeNow();
                    Toast.makeText(requireContext(), "Tăng số lượng thành công", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(requireContext(), "Tăng số lượng thất bại", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                Toast.makeText(requireContext(), "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void minusQuantity(String username, int productId) {
        CartItemApi api = ApiClient.getPrivateClient(requireContext()).create(CartItemApi.class);
        api.minusQuantity(username, productId).enqueue(new Callback<ApiResponse<Void>>() {
            @Override
            public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {
                if (response.isSuccessful()) {
                    // Sau khi server cập nhật, tải lại giỏ -> onResponse của getCartItems sẽ cập nhật badge
                    getCartItems(USERNAME);
                    ((DashboardActivity) requireActivity()).updateCartBadgeNow();
                    Toast.makeText(requireContext(), "Giảm số lượng thành công", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(requireContext(), "Giảm số lượng thất bại", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                Toast.makeText(requireContext(), "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
    
    private String formatCurrencyVND(double value) {
        NumberFormat nf = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
        return nf.format(value).replace("₫", "₫");
    }

    private void setupSwipeToDelete() {
        ItemTouchHelper.SimpleCallback callback = new ItemTouchHelper.SimpleCallback(
                0, // No drag support
                ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT // Swipe both directions
        ) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                if (position >= 0 && position < cartItems.size()) {
                    CartItemDTO item = cartItems.get(position);
                    // Delete by calling minusQuantity until quantity reaches 0
                    removeCartItem(item);
                }
            }

            @Override
            public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
                if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
                    View itemView = viewHolder.itemView;
                    float alpha = 1 - Math.abs(dX) / itemView.getWidth();
                    viewHolder.itemView.setAlpha(alpha);
                    viewHolder.itemView.setTranslationX(dX);
                } else {
                    super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
                }
            }
        };

        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(callback);
        itemTouchHelper.attachToRecyclerView(binding.rclViewCart);
    }

    private void removeCartItem(CartItemDTO item) {
        Toast.makeText(requireContext(), "Đang xóa...", Toast.LENGTH_SHORT).show();
        // Get current quantity and call minusQuantity for each unit
        int quantity = item.getQuantity();
        removeCartItemRecursive(item, quantity);
    }

    private void removeCartItemRecursive(CartItemDTO item, int remainingQuantity) {
        if (remainingQuantity <= 0) {
            // All items removed, refresh cart
            getCartItems(USERNAME);
            Toast.makeText(requireContext(), "Đã xóa sản phẩm", Toast.LENGTH_SHORT).show();
            return;
        }

        CartItemApi api = ApiClient.getPrivateClient(requireContext()).create(CartItemApi.class);
        api.minusQuantity(USERNAME, item.getProductDTO().getProductID()).enqueue(new Callback<ApiResponse<Void>>() {
            @Override
            public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {
                if (response.isSuccessful()) {
                    // Continue removing until quantity is 0
                    removeCartItemRecursive(item, remainingQuantity - 1);
                } else {
                    Toast.makeText(requireContext(), "Xóa thất bại", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                Toast.makeText(requireContext(), "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void animateCartEnter() {
        // Reset initial positions
        binding.topBar.setTranslationX(-1000);
        binding.topBar.setAlpha(0f);
        binding.bottomSummary.setTranslationY(1000);
        binding.bottomSummary.setAlpha(0f);
        binding.clearAllBtn.setTranslationY(1000);
        binding.clearAllBtn.setAlpha(0f);
        binding.checkoutBtn.setTranslationY(1000);
        binding.checkoutBtn.setAlpha(0f);

        // Animate top bar
        ObjectAnimator topBarSlide = ObjectAnimator.ofFloat(binding.topBar, "translationX", -1000, 0);
        ObjectAnimator topBarAlpha = ObjectAnimator.ofFloat(binding.topBar, "alpha", 0f, 1f);
        AnimatorSet topBarAnim = new AnimatorSet();
        topBarAnim.playTogether(topBarSlide, topBarAlpha);
        topBarAnim.setDuration(400);
        topBarAnim.setInterpolator(new DecelerateInterpolator());

        // Animate bottom summary
        ObjectAnimator bottomSummarySlide = ObjectAnimator.ofFloat(binding.bottomSummary, "translationY", 1000, 0);
        ObjectAnimator bottomSummaryAlpha = ObjectAnimator.ofFloat(binding.bottomSummary, "alpha", 0f, 1f);
        AnimatorSet bottomSummaryAnim = new AnimatorSet();
        bottomSummaryAnim.playTogether(bottomSummarySlide, bottomSummaryAlpha);
        bottomSummaryAnim.setDuration(400);
        bottomSummaryAnim.setInterpolator(new OvershootInterpolator());

        // Animate buttons
        ObjectAnimator clearBtnSlide = ObjectAnimator.ofFloat(binding.clearAllBtn, "translationY", 1000, 0);
        ObjectAnimator clearBtnAlpha = ObjectAnimator.ofFloat(binding.clearAllBtn, "alpha", 0f, 1f);
        AnimatorSet clearBtnAnim = new AnimatorSet();
        clearBtnAnim.playTogether(clearBtnSlide, clearBtnAlpha);
        clearBtnAnim.setDuration(400);
        clearBtnAnim.setInterpolator(new OvershootInterpolator());

        ObjectAnimator checkoutBtnSlide = ObjectAnimator.ofFloat(binding.checkoutBtn, "translationY", 1000, 0);
        ObjectAnimator checkoutBtnAlpha = ObjectAnimator.ofFloat(binding.checkoutBtn, "alpha", 0f, 1f);
        AnimatorSet checkoutBtnAnim = new AnimatorSet();
        checkoutBtnAnim.playTogether(checkoutBtnSlide, checkoutBtnAlpha);
        checkoutBtnAnim.setDuration(400);
        checkoutBtnAnim.setInterpolator(new OvershootInterpolator());

        // Play animations
        topBarAnim.start();
        bottomSummaryAnim.setStartDelay(200);
        bottomSummaryAnim.start();
        clearBtnAnim.setStartDelay(150);
        clearBtnAnim.start();
        checkoutBtnAnim.setStartDelay(250);
        checkoutBtnAnim.start();
    }
}