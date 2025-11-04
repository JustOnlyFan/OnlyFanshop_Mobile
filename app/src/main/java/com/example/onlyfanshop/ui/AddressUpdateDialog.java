package com.example.onlyfanshop.ui;

import android.app.Dialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.example.onlyfanshop.R;
import com.example.onlyfanshop.api.VietnamProvinceApi;
import com.example.onlyfanshop.api.VietnamProvinceApiClient;
import com.example.onlyfanshop.model.VietnamProvinceResponse;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AddressUpdateDialog extends DialogFragment {

    public interface OnAddressSavedListener {
        void onAddressSaved(String fullAddress);
    }

    private OnAddressSavedListener listener;
    private String currentAddress = "";

    private AutoCompleteTextView actvProvince, actvWard;
    private TextInputEditText etStreetNumber, etStreetName;
    private MaterialButton btnCancel, btnSaveAddress;
    private TextInputLayout tilProvince, tilWard;
    
    private VietnamProvinceApi provinceApi;
    private List<String> provinceList = new ArrayList<>();
    private List<String> wardList = new ArrayList<>();
    private ArrayAdapter<String> provinceAdapter;
    private ArrayAdapter<String> wardAdapter;
    
    // Cache tất cả provinces và wards để tránh query riêng cho mỗi province
    private Map<String, List<String>> allProvincesDataCache = null;

    public static AddressUpdateDialog newInstance(String currentAddress, OnAddressSavedListener listener) {
        AddressUpdateDialog dialog = new AddressUpdateDialog();
        dialog.listener = listener;
        dialog.currentAddress = currentAddress != null ? currentAddress : "";
        return dialog;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NORMAL, R.style.BottomSheetTheme);
        provinceApi = VietnamProvinceApiClient.getApi();
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        if (dialog.getWindow() != null) {
            dialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            // Đảm bảo dialog hiển thị
            dialog.setCancelable(true);
            dialog.setCanceledOnTouchOutside(true);
        }
        return dialog;
    }
    
    @Override
    public void onStart() {
        super.onStart();
        // Đảm bảo dialog có kích thước phù hợp
        if (getDialog() != null && getDialog().getWindow() != null) {
            getDialog().getWindow().setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            );
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_update_address, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tilProvince = view.findViewById(R.id.tilProvince);
        tilWard = view.findViewById(R.id.tilWard);
        TextInputLayout tilStreetNumber = view.findViewById(R.id.tilStreetNumber);
        TextInputLayout tilStreetName = view.findViewById(R.id.tilStreetName);

        actvProvince = view.findViewById(R.id.actvProvince);
        actvWard = view.findViewById(R.id.actvWard);
        etStreetNumber = view.findViewById(R.id.etStreetNumber);
        etStreetName = view.findViewById(R.id.etStreetName);
        btnCancel = view.findViewById(R.id.btnCancel);
        btnSaveAddress = view.findViewById(R.id.btnSaveAddress);

        // Khởi tạo adapter với danh sách rỗng trước
        provinceAdapter = new ArrayAdapter<>(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            provinceList
        );
        actvProvince.setAdapter(provinceAdapter);
        
        wardAdapter = new ArrayAdapter<>(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            wardList
        );
        actvWard.setAdapter(wardAdapter);

        // Hiển thị loading indicator
        if (tilProvince != null) {
            tilProvince.setHelperText("Đang tải danh sách tỉnh thành...");
        }

        // Load danh sách tỉnh thành từ API - async, không block UI
        // Delay một chút để đảm bảo dialog đã hiển thị
        actvProvince.postDelayed(() -> {
            if (isAdded() && !isDetached()) {
                testAndLoadProvinces();
            }
        }, 100);

        // Khi chọn tỉnh, load phường/xã
        actvProvince.setOnItemClickListener((parent, view1, position, id) -> {
            if (position < provinceList.size()) {
                String selectedProvince = provinceList.get(position);
                loadWardsForProvince(selectedProvince);
            }
        });

        // Parse current address nếu có
        if (!TextUtils.isEmpty(currentAddress)) {
            parseAndFillAddress(currentAddress);
        }

        btnCancel.setOnClickListener(v -> dismiss());
        btnSaveAddress.setOnClickListener(v -> saveAddress());
    }

    /**
     * Test API và load provinces
     */
    private void testAndLoadProvinces() {
        if (!isAdded() || isDetached()) {
            android.util.Log.w("AddressDialog", "Fragment not attached, skip loading");
            return;
        }
        
        if (provinceApi == null) {
            if (tilProvince != null) {
                tilProvince.setHelperText("API không khả dụng");
            }
            android.util.Log.e("AddressDialog", "API is null");
            return;
        }
        
        android.util.Log.d("AddressDialog", "=== Testing API ===");
        android.util.Log.d("AddressDialog", "API URL: https://vietnamlabs.com/api/vietnamprovince");
        
        provinceApi.getAllProvinces().enqueue(new Callback<VietnamProvinceResponse>() {
            @Override
            public void onResponse(@NonNull Call<VietnamProvinceResponse> call,
                                   @NonNull Response<VietnamProvinceResponse> response) {
                if (!isAdded() || isDetached()) return;
                
                android.util.Log.d("AddressDialog", "=== API Response ===");
                android.util.Log.d("AddressDialog", "Response code: " + response.code());
                android.util.Log.d("AddressDialog", "Response successful: " + response.isSuccessful());
                
                if (response.isSuccessful()) {
                    if (response.body() != null) {
                        VietnamProvinceResponse body = response.body();
                        Map<String, List<String>> data = body.getData();
                        
                        android.util.Log.d("AddressDialog", "Response body data: " + 
                            (data != null ? data.size() : 0) + " provinces");
                        
                        if (data != null && !data.isEmpty()) {
                            android.util.Log.d("AddressDialog", "Province keys: " + data.keySet());
                            // Log sample data
                            int count = 0;
                            for (Map.Entry<String, List<String>> entry : data.entrySet()) {
                                if (count < 3) { // Log first 3 provinces
                                    android.util.Log.d("AddressDialog", 
                                        "Province: " + entry.getKey() + 
                                        ", Wards count: " + (entry.getValue() != null ? entry.getValue().size() : 0));
                                    if (entry.getValue() != null && !entry.getValue().isEmpty()) {
                                        android.util.Log.d("AddressDialog", 
                                            "Sample wards: " + entry.getValue().subList(0, 
                                                Math.min(3, entry.getValue().size())));
                                    }
                                    count++;
                                }
                            }
                        }
                        
                        provinceList = body.getProvinceNames();
                        
                        android.util.Log.d("AddressDialog", "Loaded provinces: " + 
                            (provinceList != null ? provinceList.size() : 0));
                        
                        // Cache dữ liệu để dùng cho loadWardsForProvince
                        if (data != null && !data.isEmpty()) {
                            allProvincesDataCache = data;
                            android.util.Log.d("AddressDialog", "✅ Cached " + data.size() + " provinces with wards");
                        }
                        
                        if (provinceList != null && !provinceList.isEmpty()) {
                            provinceAdapter.clear();
                            provinceAdapter.addAll(provinceList);
                            provinceAdapter.notifyDataSetChanged();
                            if (tilProvince != null) {
                                tilProvince.setHelperText(null);
                            }
                            android.util.Log.d("AddressDialog", "✅ Successfully loaded " + provinceList.size() + " provinces");
                        } else {
                            // Nếu data không rỗng nhưng getProvinceNames() trả về rỗng
                            // Có thể do format response khác
                            if (data != null && !data.isEmpty()) {
                                provinceList = new ArrayList<>(data.keySet());
                                provinceAdapter.clear();
                                provinceAdapter.addAll(provinceList);
                                provinceAdapter.notifyDataSetChanged();
                                android.util.Log.d("AddressDialog", "Fixed: Loaded " + provinceList.size() + " provinces from data map");
                                if (tilProvince != null) {
                                    tilProvince.setHelperText(null);
                                }
                            } else {
                                if (tilProvince != null) {
                                    tilProvince.setHelperText("Không có dữ liệu. Vui lòng kiểm tra kết nối mạng.");
                                }
                                android.util.Log.w("AddressDialog", "❌ No province data received - data is empty");
                            }
                        }
                    } else {
                        if (tilProvince != null) {
                            tilProvince.setHelperText("Không nhận được dữ liệu từ server");
                        }
                        android.util.Log.e("AddressDialog", "❌ Response body is null");
                    }
                } else {
                    String errorMsg = "Lỗi: " + response.code();
                    if (tilProvince != null) {
                        tilProvince.setHelperText("Lỗi: " + response.code());
                    }
                    android.util.Log.e("AddressDialog", "❌ " + errorMsg);
                    if (response.errorBody() != null) {
                        try {
                            String errorBody = response.errorBody().string();
                            android.util.Log.e("AddressDialog", "Error body: " + errorBody);
                        } catch (Exception e) {
                            android.util.Log.e("AddressDialog", "Error reading error body", e);
                        }
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<VietnamProvinceResponse> call, @NonNull Throwable t) {
                if (!isAdded() || isDetached()) return;
                String errorMsg = "Lỗi kết nối: " + t.getMessage();
                if (tilProvince != null) {
                    tilProvince.setHelperText("Lỗi kết nối: " + t.getMessage());
                }
                android.util.Log.e("AddressDialog", "❌ " + errorMsg, t);
                t.printStackTrace();
            }
        });
    }

    /**
     * Load wards từ cache (nhanh hơn, không cần query API)
     */
    private void loadWardsFromCache(String provinceName) {
        if (allProvincesDataCache == null || allProvincesDataCache.isEmpty()) {
            android.util.Log.w("AddressDialog", "Cache is empty");
            if (tilWard != null) {
                tilWard.setHelperText("Không có dữ liệu");
            }
            return;
        }
        
        android.util.Log.d("AddressDialog", "Searching cache for: " + provinceName);
        android.util.Log.d("AddressDialog", "Cache contains: " + allProvincesDataCache.keySet());
        
        List<String> wards = null;
        String matchedKey = null;
        
        // Thử exact match trước
        if (allProvincesDataCache.containsKey(provinceName)) {
            matchedKey = provinceName;
            wards = allProvincesDataCache.get(provinceName);
            android.util.Log.d("AddressDialog", "Found exact match: " + provinceName);
        } else {
            // Thử fuzzy match (contains)
            android.util.Log.d("AddressDialog", "No exact match, trying fuzzy match...");
            for (String key : allProvincesDataCache.keySet()) {
                // Kiểm tra nếu key chứa provinceName hoặc ngược lại
                if (key.contains(provinceName) || provinceName.contains(key)) {
                    matchedKey = key;
                    wards = allProvincesDataCache.get(key);
                    android.util.Log.d("AddressDialog", "Found fuzzy match: " + key + " matches " + provinceName);
                    break;
                }
            }
            
            // Nếu vẫn không tìm thấy, thử case-insensitive match
            if (matchedKey == null) {
                String provinceNameLower = provinceName.toLowerCase().trim();
                for (String key : allProvincesDataCache.keySet()) {
                    String keyLower = key.toLowerCase().trim();
                    if (keyLower.equals(provinceNameLower) || 
                        keyLower.contains(provinceNameLower) || 
                        provinceNameLower.contains(keyLower)) {
                        matchedKey = key;
                        wards = allProvincesDataCache.get(key);
                        android.util.Log.d("AddressDialog", "Found case-insensitive match: " + key + " matches " + provinceName);
                        break;
                    }
                }
            }
        }
        
        if (wards != null && !wards.isEmpty()) {
            wardList = wards;
            wardAdapter.clear();
            wardAdapter.addAll(wardList);
            wardAdapter.notifyDataSetChanged();
            if (tilWard != null) {
                tilWard.setHelperText(null);
            }
            android.util.Log.d("AddressDialog", "✅ Successfully loaded " + wardList.size() + " wards from cache for " + matchedKey);
        } else {
            if (tilWard != null) {
                tilWard.setHelperText("Không có dữ liệu phường/xã cho " + provinceName);
            }
            android.util.Log.w("AddressDialog", "❌ No wards found in cache for " + provinceName);
            android.util.Log.w("AddressDialog", "Available provinces: " + allProvincesDataCache.keySet());
        }
    }

    private void loadWardsForProvince(String provinceName) {
        if (TextUtils.isEmpty(provinceName)) {
            Toast.makeText(requireContext(), "Tỉnh thành không hợp lệ", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Clear ward list trước
        wardList.clear();
        wardAdapter.clear();
        wardAdapter.notifyDataSetChanged();
        actvWard.setText("", false);
        
        android.util.Log.d("AddressDialog", "=== Loading wards for: " + provinceName + " ===");
        
        // Ưu tiên lấy từ cache (nhanh hơn, không cần query API)
        if (allProvincesDataCache != null && !allProvincesDataCache.isEmpty()) {
            android.util.Log.d("AddressDialog", "Loading wards from cache...");
            loadWardsFromCache(provinceName);
            return;
        }
        
        // Nếu cache chưa có, query API riêng (fallback)
        android.util.Log.d("AddressDialog", "Cache not available, querying API...");
        if (provinceApi == null) {
            Toast.makeText(requireContext(), "API không khả dụng", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Hiển thị loading
        if (tilWard != null) {
            tilWard.setHelperText("Đang tải...");
        }
        
        provinceApi.getProvinceWards(provinceName).enqueue(new Callback<VietnamProvinceResponse>() {
            @Override
            public void onResponse(@NonNull Call<VietnamProvinceResponse> call,
                                   @NonNull Response<VietnamProvinceResponse> response) {
                if (!isAdded() || isDetached()) return;
                
                if (tilWard != null) {
                    tilWard.setHelperText(null);
                }
                
                android.util.Log.d("AddressDialog", "Wards response code: " + response.code());
                android.util.Log.d("AddressDialog", "Wards response successful: " + response.isSuccessful());
                
                if (response.isSuccessful()) {
                    if (response.body() != null) {
                        VietnamProvinceResponse body = response.body();
                        Map<String, List<String>> data = body.getData();
                        
                        android.util.Log.d("AddressDialog", "Wards data: " + 
                            (data != null ? data.size() : 0) + " entries");
                        
                        // Log tất cả keys trong data để debug
                        if (data != null && !data.isEmpty()) {
                            android.util.Log.d("AddressDialog", "Data keys: " + data.keySet());
                            for (Map.Entry<String, List<String>> entry : data.entrySet()) {
                                android.util.Log.d("AddressDialog", 
                                    "  - Province: " + entry.getKey() + 
                                    ", Wards count: " + (entry.getValue() != null ? entry.getValue().size() : 0));
                            }
                        }
                        
                        wardList = body.getWardsForProvince(provinceName);
                        
                        android.util.Log.d("AddressDialog", "Loaded wards for " + provinceName + ": " + 
                            (wardList != null ? wardList.size() : 0));
                        
                        if (wardList != null && !wardList.isEmpty()) {
                            wardAdapter.clear();
                            wardAdapter.addAll(wardList);
                            wardAdapter.notifyDataSetChanged();
                            android.util.Log.d("AddressDialog", "✅ Successfully loaded " + wardList.size() + " wards");
                        } else {
                            // Thử tìm exact match trước
                            if (data != null && data.containsKey(provinceName)) {
                                wardList = data.get(provinceName);
                                android.util.Log.d("AddressDialog", "Found exact match for: " + provinceName);
                            } else if (data != null && !data.isEmpty()) {
                                // Nếu không có exact match, thử tìm fuzzy match (contains)
                                android.util.Log.d("AddressDialog", "No exact match, trying fuzzy match...");
                                String matchedKey = null;
                                for (String key : data.keySet()) {
                                    // Kiểm tra nếu key chứa provinceName hoặc ngược lại
                                    if (key.contains(provinceName) || provinceName.contains(key)) {
                                        matchedKey = key;
                                        android.util.Log.d("AddressDialog", "Found fuzzy match: " + key + " matches " + provinceName);
                                        break;
                                    }
                                }
                                
                                // Nếu vẫn không tìm thấy, lấy entry đầu tiên (khi query một province cụ thể, thường chỉ có một entry)
                                if (matchedKey == null && data.size() == 1) {
                                    matchedKey = data.keySet().iterator().next();
                                    android.util.Log.d("AddressDialog", "Using first entry (single result): " + matchedKey);
                                }
                                
                                if (matchedKey != null) {
                                    wardList = data.get(matchedKey);
                                    android.util.Log.d("AddressDialog", "Loaded wards from matched key: " + matchedKey);
                                }
                            }
                            
                            if (wardList != null && !wardList.isEmpty()) {
                                wardAdapter.clear();
                                wardAdapter.addAll(wardList);
                                wardAdapter.notifyDataSetChanged();
                                android.util.Log.d("AddressDialog", "✅ Successfully loaded " + wardList.size() + " wards from data map");
                            } else {
                                if (tilWard != null) {
                                    tilWard.setHelperText("Không có dữ liệu phường/xã cho " + provinceName);
                                }
                                android.util.Log.w("AddressDialog", "❌ No wards data for " + provinceName);
                                if (data != null && !data.isEmpty()) {
                                    android.util.Log.w("AddressDialog", "Available provinces: " + data.keySet());
                                }
                            }
                        }
                    } else {
                        if (tilWard != null) {
                            tilWard.setHelperText("Không nhận được dữ liệu");
                        }
                        android.util.Log.e("AddressDialog", "❌ Wards response body is null");
                    }
                } else {
                    String errorMsg = "Lỗi: " + response.code();
                    if (tilWard != null) {
                        tilWard.setHelperText("Lỗi: " + response.code());
                    }
                    android.util.Log.e("AddressDialog", "❌ " + errorMsg);
                    if (response.errorBody() != null) {
                        try {
                            String errorBody = response.errorBody().string();
                            android.util.Log.e("AddressDialog", "Wards error body: " + errorBody);
                        } catch (Exception e) {
                            android.util.Log.e("AddressDialog", "Error reading error body", e);
                        }
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<VietnamProvinceResponse> call, @NonNull Throwable t) {
                if (!isAdded() || isDetached()) return;
                if (tilWard != null) {
                    tilWard.setHelperText(null);
                }
                String errorMsg = "Lỗi kết nối: " + t.getMessage();
                if (tilWard != null) {
                    tilWard.setHelperText("Lỗi kết nối");
                }
                android.util.Log.e("AddressDialog", "❌ " + errorMsg, t);
                t.printStackTrace();
            }
        });
    }

    private void parseAndFillAddress(String address) {
        // Parse địa chỉ hiện tại (logic đơn giản)
        // Tìm tên tỉnh trong danh sách
        if (provinceList != null && !provinceList.isEmpty()) {
            for (String province : provinceList) {
                if (address.contains(province)) {
                    actvProvince.setText(province, false);
                    loadWardsForProvince(province);
                    break;
                }
            }
        }
    }

    private void saveAddress() {
        String province = actvProvince.getText() != null ? actvProvince.getText().toString().trim() : "";
        String ward = actvWard.getText() != null ? actvWard.getText().toString().trim() : "";
        String streetNumber = etStreetNumber.getText() != null ? etStreetNumber.getText().toString().trim() : "";
        String streetName = etStreetName.getText() != null ? etStreetName.getText().toString().trim() : "";

        if (TextUtils.isEmpty(province)) {
            Toast.makeText(requireContext(), "Vui lòng chọn Tỉnh/Thành phố", Toast.LENGTH_SHORT).show();
            return;
        }
        if (TextUtils.isEmpty(ward)) {
            Toast.makeText(requireContext(), "Vui lòng chọn Phường/Xã", Toast.LENGTH_SHORT).show();
            return;
        }
        if (TextUtils.isEmpty(streetNumber) && TextUtils.isEmpty(streetName)) {
            Toast.makeText(requireContext(), "Vui lòng nhập Số nhà hoặc Tên đường", Toast.LENGTH_SHORT).show();
            return;
        }

        // Tạo địa chỉ đầy đủ (không có Quận/Huyện nữa)
        StringBuilder fullAddress = new StringBuilder();
        if (!TextUtils.isEmpty(streetNumber)) {
            fullAddress.append(streetNumber).append(" ");
        }
        if (!TextUtils.isEmpty(streetName)) {
            fullAddress.append(streetName).append(", ");
        }
        if (!TextUtils.isEmpty(ward)) {
            fullAddress.append(ward).append(", ");
        }
        if (!TextUtils.isEmpty(province)) {
            fullAddress.append(province);
        }

        if (listener != null) {
            listener.onAddressSaved(fullAddress.toString());
        }
        dismiss();
    }
}
