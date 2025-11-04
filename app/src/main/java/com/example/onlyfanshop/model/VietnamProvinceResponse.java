package com.example.onlyfanshop.model;

import com.google.gson.Gson;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Response từ VietnamProvince API
 * Format: Map<String, List<String>> 
 * Ví dụ: { "Hà Nội": ["Phường 1", "Phường 2", ...], "Hồ Chí Minh": [...] }
 */
@JsonAdapter(VietnamProvinceResponse.VietnamProvinceResponseDeserializer.class)
public class VietnamProvinceResponse {
    
    private Map<String, List<String>> data;
    
    public VietnamProvinceResponse() {
        this.data = new HashMap<>();
    }
    
    public VietnamProvinceResponse(Map<String, List<String>> data) {
        this.data = data != null ? data : new HashMap<>();
    }
    
    public Map<String, List<String>> getData() {
        return data;
    }
    
    public void setData(Map<String, List<String>> data) {
        this.data = data != null ? data : new HashMap<>();
    }
    
    /**
     * Lấy danh sách tên tỉnh
     */
    public List<String> getProvinceNames() {
        if (data != null && !data.isEmpty()) {
            return new ArrayList<>(data.keySet());
        }
        return new ArrayList<>();
    }
    
    /**
     * Lấy danh sách phường/xã của một tỉnh
     */
    public List<String> getWardsForProvince(String provinceName) {
        if (data != null && data.containsKey(provinceName)) {
            List<String> wards = data.get(provinceName);
            return wards != null ? wards : new ArrayList<>();
        }
        return new ArrayList<>();
    }
    
    /**
     * Custom deserializer để parse JSON response
     * API có thể trả về: 
     * - Map<String, List<String>> trực tiếp
     * - Object với field "data" hoặc "provinces"
     * - Array các object
     */
    public static class VietnamProvinceResponseDeserializer implements JsonDeserializer<VietnamProvinceResponse> {
        @Override
        public VietnamProvinceResponse deserialize(JsonElement json, Type typeOfT,
                                                   JsonDeserializationContext context) throws JsonParseException {
            Map<String, List<String>> result = new HashMap<>();
            
            try {
                android.util.Log.d("ProvinceDeserializer", "=== Deserializing JSON ===");
                android.util.Log.d("ProvinceDeserializer", "JSON type: " + (json.isJsonObject() ? "Object" : json.isJsonArray() ? "Array" : "Primitive"));
                
                if (json.isJsonObject()) {
                    JsonObject jsonObject = json.getAsJsonObject();
                    
                    android.util.Log.d("ProvinceDeserializer", "JSON object keys: " + jsonObject.keySet());
                    android.util.Log.d("ProvinceDeserializer", "JSON object size: " + jsonObject.size());
                    
                    // Kiểm tra xem có field "data" không
                    if (jsonObject.has("data")) {
                        android.util.Log.d("ProvinceDeserializer", "Found 'data' field");
                        JsonElement dataElement = jsonObject.get("data");
                        if (dataElement.isJsonObject()) {
                            parseProvinceMap(dataElement.getAsJsonObject(), result);
                        } else if (dataElement.isJsonArray()) {
                            android.util.Log.d("ProvinceDeserializer", "Data field is array, parsing array");
                            parseProvinceArray(dataElement.getAsJsonArray(), result);
                        }
                    } 
                    // Kiểm tra xem có field "provinces" không
                    else if (jsonObject.has("provinces")) {
                        android.util.Log.d("ProvinceDeserializer", "Found 'provinces' field");
                        JsonElement provincesElement = jsonObject.get("provinces");
                        if (provincesElement.isJsonObject()) {
                            parseProvinceMap(provincesElement.getAsJsonObject(), result);
                        } else {
                            android.util.Log.w("ProvinceDeserializer", "Provinces field is not an object");
                        }
                    }
                    // Nếu không có field wrapper, coi như response trực tiếp là Map
                    else {
                        android.util.Log.d("ProvinceDeserializer", "No wrapper field, parsing root object directly");
                        parseProvinceMap(jsonObject, result);
                    }
                } 
                // Nếu response là array
                else if (json.isJsonArray()) {
                    android.util.Log.d("ProvinceDeserializer", "Response is array, parsing array");
                    parseProvinceArray(json.getAsJsonArray(), result);
                } else {
                    android.util.Log.w("ProvinceDeserializer", "Response is primitive, not handled");
                }
                
                android.util.Log.d("ProvinceDeserializer", "Parsed " + result.size() + " provinces");
                if (!result.isEmpty()) {
                    int count = 0;
                    for (Map.Entry<String, List<String>> entry : result.entrySet()) {
                        if (count < 3) {
                            android.util.Log.d("ProvinceDeserializer", 
                                "  - " + entry.getKey() + ": " + 
                                (entry.getValue() != null ? entry.getValue().size() : 0) + " wards");
                            count++;
                        }
                    }
                }
            } catch (Exception e) {
                android.util.Log.e("ProvinceDeserializer", "Error deserializing JSON", e);
                e.printStackTrace();
                // Fallback: trả về empty map
            }
            
            return new VietnamProvinceResponse(result);
        }
        
        private void parseProvinceMap(JsonObject jsonObject, Map<String, List<String>> result) {
            Set<Map.Entry<String, JsonElement>> entries = jsonObject.entrySet();
            
            android.util.Log.d("ProvinceDeserializer", "parseProvinceMap: " + entries.size() + " entries");
            
            for (Map.Entry<String, JsonElement> entry : entries) {
                String provinceName = entry.getKey();
                JsonElement value = entry.getValue();
                
                List<String> wards = new ArrayList<>();
                if (value.isJsonArray()) {
                    for (JsonElement wardElement : value.getAsJsonArray()) {
                        if (wardElement.isJsonPrimitive()) {
                            wards.add(wardElement.getAsString());
                        } else if (wardElement.isJsonObject()) {
                            // Nếu ward là object, lấy tên
                            JsonObject wardObj = wardElement.getAsJsonObject();
                            if (wardObj.has("name")) {
                                wards.add(wardObj.get("name").getAsString());
                            }
                        }
                    }
                } else {
                    android.util.Log.w("ProvinceDeserializer", 
                        "Province " + provinceName + " value is not an array: " + value.getClass().getSimpleName());
                }
                result.put(provinceName, wards);
            }
            
            android.util.Log.d("ProvinceDeserializer", "parseProvinceMap completed: " + result.size() + " provinces parsed");
        }
        
        /**
         * Parse mảng provinces từ API response
         * Mỗi element trong array là một object có:
         * - "name" hoặc "province" hoặc "fullName" (tên tỉnh)
         * - "wards" (mảng các phường/xã)
         */
        private void parseProvinceArray(JsonArray jsonArray, Map<String, List<String>> result) {
            android.util.Log.d("ProvinceDeserializer", "parseProvinceArray: " + jsonArray.size() + " elements");
            
            for (int i = 0; i < jsonArray.size(); i++) {
                try {
                    JsonElement element = jsonArray.get(i);
                    if (!element.isJsonObject()) {
                        android.util.Log.w("ProvinceDeserializer", "Element " + i + " is not an object, skipping");
                        continue;
                    }
                    
                    JsonObject provinceObj = element.getAsJsonObject();
                    
                    // Tìm tên tỉnh từ các field có thể có
                    String provinceName = null;
                    if (provinceObj.has("name")) {
                        provinceName = provinceObj.get("name").getAsString();
                    } else if (provinceObj.has("province")) {
                        provinceName = provinceObj.get("province").getAsString();
                    } else if (provinceObj.has("fullName")) {
                        provinceName = provinceObj.get("fullName").getAsString();
                    } else if (provinceObj.has("full_name")) {
                        provinceName = provinceObj.get("full_name").getAsString();
                    }
                    
                    if (provinceName == null || provinceName.isEmpty()) {
                        android.util.Log.w("ProvinceDeserializer", "Element " + i + " has no province name, skipping");
                        continue;
                    }
                    
                    // Lấy danh sách wards
                    List<String> wards = new ArrayList<>();
                    if (provinceObj.has("wards")) {
                        JsonElement wardsElement = provinceObj.get("wards");
                        if (wardsElement.isJsonArray()) {
                            JsonArray wardsArray = wardsElement.getAsJsonArray();
                            for (JsonElement wardElement : wardsArray) {
                                if (wardElement.isJsonPrimitive()) {
                                    wards.add(wardElement.getAsString());
                                } else if (wardElement.isJsonObject()) {
                                    // Nếu ward là object, lấy tên
                                    JsonObject wardObj = wardElement.getAsJsonObject();
                                    if (wardObj.has("name")) {
                                        wards.add(wardObj.get("name").getAsString());
                                    } else if (wardObj.has("fullName")) {
                                        wards.add(wardObj.get("fullName").getAsString());
                                    } else if (wardObj.has("full_name")) {
                                        wards.add(wardObj.get("full_name").getAsString());
                                    }
                                }
                            }
                        }
                    }
                    
                    result.put(provinceName, wards);
                    android.util.Log.d("ProvinceDeserializer", 
                        "Parsed province: " + provinceName + " with " + wards.size() + " wards");
                    
                } catch (Exception e) {
                    android.util.Log.e("ProvinceDeserializer", "Error parsing element " + i, e);
                }
            }
            
            android.util.Log.d("ProvinceDeserializer", "parseProvinceArray completed: " + result.size() + " provinces parsed");
        }
    }
}
