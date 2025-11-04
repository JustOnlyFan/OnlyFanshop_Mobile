package com.example.onlyfanshop.ui;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.GridLayoutManager;

import com.example.onlyfanshop.R;
import com.example.onlyfanshop.api.ApiClient;
import com.example.onlyfanshop.api.ProductApi;
import com.example.onlyfanshop.model.BrandDTO;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class BrandSelectDialogFragment extends BottomSheetDialogFragment {
    public interface Listener { void onBrandChosen(Integer brandId); }
    private Listener listener;
    private final List<BrandDTO> brands = new ArrayList<>();
    private SimpleBrandGridAdapter adapter;
    private ProgressBar progressBar;
    private boolean loadFromApi = false;

    public static BrandSelectDialogFragment newInstance() { return new BrandSelectDialogFragment(); }
    public void setListener(Listener l) { this.listener = l; }
    public void setBrands(List<BrandDTO> list) { 
        brands.clear(); 
        if (list!=null && !list.isEmpty()) {
            brands.addAll(list);
            loadFromApi = false; // Đã có data, không cần load từ API
        }
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }
    
    // Load brands từ API
    public void loadBrandsFromApi() {
        loadFromApi = true;
        if (getContext() == null) return;
        
        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);
        
        ProductApi api = ApiClient.getPrivateClient(getContext()).create(ProductApi.class);
        api.getAllBrands().enqueue(new Callback<List<BrandDTO>>() {
            @Override
            public void onResponse(Call<List<BrandDTO>> call, Response<List<BrandDTO>> response) {
                if (progressBar != null) progressBar.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    brands.clear();
                    brands.addAll(response.body());
                    if (adapter != null) {
                        adapter.notifyDataSetChanged();
                        // Force layout update
                        if (getView() != null) {
                            androidx.recyclerview.widget.RecyclerView rv = getView().findViewById(R.id.rvBrandGrid);
                            if (rv != null) {
                                rv.post(() -> {
                                    rv.requestLayout();
                                    adapter.notifyDataSetChanged();
                                });
                            }
                        }
                    }
                    android.util.Log.d("BrandSelectDialog", "Loaded " + brands.size() + " brands");
                } else {
                    Toast.makeText(getContext(), "Không thể tải danh sách thương hiệu", Toast.LENGTH_SHORT).show();
                    android.util.Log.e("BrandSelectDialog", "Failed to load brands: response not successful or empty");
                }
            }

            @Override
            public void onFailure(Call<List<BrandDTO>> call, Throwable t) {
                if (progressBar != null) progressBar.setVisibility(View.GONE);
                Toast.makeText(getContext(), "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @NonNull @Override public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        BottomSheetDialog dialog = new BottomSheetDialog(requireContext(), R.style.BottomSheetTheme);
        return dialog;
    }
    
    @Override
    public void onStart() {
        super.onStart();
        Dialog d = getDialog();
        if (d instanceof BottomSheetDialog) {
            BottomSheetDialog bsd = (BottomSheetDialog) d;
            View bottomSheet = bsd.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (bottomSheet != null) {
                // Loại bỏ padding/margin trên cùng
                bottomSheet.setPadding(
                        bottomSheet.getPaddingLeft(),
                        0,
                        bottomSheet.getPaddingRight(),
                        bottomSheet.getPaddingBottom()
                );
                android.view.ViewGroup.MarginLayoutParams params =
                    (android.view.ViewGroup.MarginLayoutParams) bottomSheet.getLayoutParams();
                if (params != null) {
                    params.topMargin = 0;
                    bottomSheet.setLayoutParams(params);
                }
                
                // Consume window insets để loại bỏ khoảng trống
                ViewCompat.setOnApplyWindowInsetsListener(bottomSheet, (v, insets) -> {
                    return WindowInsetsCompat.CONSUMED;
                });
                
                // Kiểm tra và loại bỏ padding của các container cha
                View parent = (View) bottomSheet.getParent();
                while (parent != null) {
                    if (parent instanceof android.view.ViewGroup) {
                        android.view.ViewGroup vg = (android.view.ViewGroup) parent;
                        vg.setPadding(
                                vg.getPaddingLeft(),
                                0,
                                vg.getPaddingRight(),
                                vg.getPaddingBottom()
                        );
                        // Chỉ cast nếu là MarginLayoutParams
                        android.view.ViewGroup.LayoutParams parentParams = vg.getLayoutParams();
                        if (parentParams instanceof android.view.ViewGroup.MarginLayoutParams) {
                            android.view.ViewGroup.MarginLayoutParams marginParams =
                                (android.view.ViewGroup.MarginLayoutParams) parentParams;
                            marginParams.topMargin = 0;
                            vg.setLayoutParams(marginParams);
                        }
                    }
                    parent = parent.getParent() instanceof View ? (View) parent.getParent() : null;
                }
                
                // Set behavior - tính peek height dựa trên chiều cao nội dung thực tế
                final com.google.android.material.bottomsheet.BottomSheetBehavior<?> behavior =
                    com.google.android.material.bottomsheet.BottomSheetBehavior.from(bottomSheet);
                if (behavior != null) {
                    // Đợi bottomSheet được layout xong
                    bottomSheet.post(() -> {
                        View content = getView();
                        if (content != null) {
                            // Measure content height với width chính xác
                            int width = bottomSheet.getWidth();
                            if (width <= 0) {
                                width = requireContext().getResources().getDisplayMetrics().widthPixels;
                            }
                            int widthSpec = View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY);
                            int heightSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
                            content.measure(widthSpec, heightSpec);
                            int contentHeight = content.getMeasuredHeight();
                            
                            android.util.Log.d("BrandSelectDialog", "Measured contentHeight: " + contentHeight + ", bottomSheet width: " + width);
                            
                            if (contentHeight > 0) {
                                // Set peek height = chiều cao nội dung thực tế (không thêm khoảng trống)
                                // Chỉ set peek height = contentHeight, không thêm padding
                                int peekHeight = contentHeight;
                                int screenHeight = requireContext().getResources().getDisplayMetrics().heightPixels;
                                // Giới hạn tối đa để tránh dialog quá cao
                                int maxHeight = (int) (screenHeight * 0.5); // Tối đa 50% màn hình
                                peekHeight = Math.min(peekHeight, maxHeight);
                                
                                behavior.setPeekHeight(peekHeight);
                                behavior.setSkipCollapsed(false);
                                behavior.setState(com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_COLLAPSED);
                                
                                // Đảm bảo bottomSheet không có padding/margin
                                bottomSheet.setPadding(
                                        bottomSheet.getPaddingLeft(),
                                        0,
                                        bottomSheet.getPaddingRight(),
                                        bottomSheet.getPaddingBottom()
                                );
                                
                                android.util.Log.d("BrandSelectDialog", "Peek height set to: " + peekHeight + ", contentHeight: " + contentHeight);
                            } else {
                                // Fallback: tính dựa trên số lượng brands
                                int rowCount = (int) Math.ceil((brands.isEmpty() ? 18 : brands.size()) / 3.0);
                                int itemHeight = 70; // 60dp height + 10dp margin
                                int headerHeight = 50;
                                int calculatedHeight = headerHeight + (rowCount * itemHeight) + 20;
                                int screenHeight = requireContext().getResources().getDisplayMetrics().heightPixels;
                                int maxHeight = (int) (screenHeight * 0.55);
                                int peekHeight = Math.min(calculatedHeight, maxHeight);
                                
                                behavior.setPeekHeight(peekHeight);
                                behavior.setSkipCollapsed(false);
                                behavior.setState(com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_COLLAPSED);
                            }
                        }
                    });
                }
            }
            
            // Loại bỏ padding của content view
            View content = getView();
            if (content != null) {
                if (content instanceof android.view.ViewGroup) {
                    android.view.ViewGroup vg = (android.view.ViewGroup) content;
                    vg.setPadding(
                            vg.getPaddingLeft(),
                            0,
                            vg.getPaddingRight(),
                            vg.getPaddingBottom()
                    );
                }
                ViewCompat.setOnApplyWindowInsetsListener(content, (v, insets) -> {
                    return WindowInsetsCompat.CONSUMED;
                });
            }
        }
    }

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.dialog_brand_grid, container, false);
        androidx.recyclerview.widget.RecyclerView rv = v.findViewById(R.id.rvBrandGrid);
        progressBar = v.findViewById(R.id.progressBar);
        
        // Setup nút close
        android.widget.ImageView btnClose = v.findViewById(R.id.btnClose);
        if (btnClose != null) {
            btnClose.setOnClickListener(view -> dismiss());
        }
        
        // Hiển thị 3 cột cho grid layout đẹp hơn
        GridLayoutManager gridLayoutManager = new GridLayoutManager(requireContext(), 3);
        rv.setLayoutManager(gridLayoutManager);
        rv.setHasFixedSize(false);
        rv.setItemViewCacheSize(20);
        
        adapter = new SimpleBrandGridAdapter(brands, id -> { 
            if (listener!=null) listener.onBrandChosen(id); 
            dismiss(); 
        });
        rv.setAdapter(adapter);
        
        android.util.Log.d("BrandSelectDialog", "RecyclerView setup: width=" + rv.getWidth() + ", brands.size=" + brands.size());
        
        // Nếu brands rỗng và chưa load từ API, load ngay
        if (brands.isEmpty() && !loadFromApi) {
            loadBrandsFromApi();
        } else if (!brands.isEmpty()) {
            // Nếu đã có data, đảm bảo adapter được notify
            if (adapter != null) {
                adapter.notifyDataSetChanged();
            }
        }
        
        // Force layout sau khi view được attach
        rv.post(() -> {
            if (adapter != null && adapter.getItemCount() > 0) {
                adapter.notifyDataSetChanged();
                android.util.Log.d("BrandSelectDialog", "Post: forcing adapter update, count=" + adapter.getItemCount());
            }
        });
        
        return v;
    }

    // Lightweight adapter for logo grid
    static class SimpleBrandGridAdapter extends androidx.recyclerview.widget.RecyclerView.Adapter<SimpleBrandGridAdapter.VH> {
        interface Click { void onClick(Integer id); }
        private final List<BrandDTO> data; private final Click click;
        SimpleBrandGridAdapter(List<BrandDTO> d, Click c){ data=d; click=c; }
        @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup p, int v){
            android.view.View item = android.view.LayoutInflater.from(p.getContext()).inflate(R.layout.item_brand_chip, p, false);
            return new VH(item);
        }
        @Override public void onBindViewHolder(@NonNull VH h, int pos){
            if (pos < 0 || pos >= data.size()) {
                android.util.Log.e("BrandGridAdapter", "Invalid position: " + pos + ", size: " + data.size());
                return;
            }
            BrandDTO b = data.get(pos);
            
            android.widget.ImageView img = h.itemView.findViewById(R.id.imgLogo);
            
            if (img == null) {
                android.util.Log.e("BrandGridAdapter", "ImageView is null!");
                return;
            }
            
            img.setVisibility(android.view.View.VISIBLE);
            if (b.getImageURL() != null && !b.getImageURL().trim().isEmpty()) {
                com.bumptech.glide.Glide.with(img.getContext())
                        .load(b.getImageURL().trim())
                        .placeholder(R.drawable.ic_launcher_foreground)
                        .error(R.drawable.ic_launcher_foreground)
                        .fitCenter()
                        .into(img);
            } else {
                img.setImageResource(R.drawable.ic_launcher_foreground);
            }
            
            h.itemView.setVisibility(android.view.View.VISIBLE);
            h.itemView.setOnClickListener(v -> {
                if (click != null) click.onClick(b.getBrandID());
            });
        }
        @Override public int getItemCount(){ 
            int count = data==null?0:data.size();
            android.util.Log.d("BrandGridAdapter", "Item count: " + count);
            return count;
        }
        static class VH extends androidx.recyclerview.widget.RecyclerView.ViewHolder { VH(@NonNull View item){ super(item);} }
    }
}


