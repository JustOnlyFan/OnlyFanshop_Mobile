package com.example.onlyfanshop.model.Request;

import com.google.gson.annotations.SerializedName;

/**
 * Body gửi lên theo schema backend yêu cầu.
 * Các field không cho chỉnh sửa sẽ được lấy từ GET /users/getUser và truyền nguyên vẹn.
 */
public class UpdateUserRequest {

    @SerializedName("userID")
    private int userID;

    @SerializedName("username")
    private String username;

    @SerializedName("email")
    private String email;

    @SerializedName("phoneNumber")
    private String phoneNumber;

    @SerializedName("address")
    private String address;

    @SerializedName("role")
    private String role;

    @SerializedName("authProvider")
    private String authProvider;

    @SerializedName("token")
    private String token; // có thể null tùy backend

    public UpdateUserRequest(int userID, String username, String email, String phoneNumber, String address,
                             String role, String authProvider, String token) {
        this.userID = userID;
        this.username = username;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.address = address;
        this.role = role;
        this.authProvider = authProvider;
        this.token = token;
    }

    // Getter/Setter nếu cần
}