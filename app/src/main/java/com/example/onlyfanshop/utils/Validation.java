package com.example.onlyfanshop.utils;

import android.util.Patterns;

public class Validation {
    public static boolean isValidEmail(String email) {
        return email != null && Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }
    public static boolean isValidPassword(String password) {
        // Ít nhất 8 ký tự, có chữ cái, số và ký tự đặc biệt
        String passwordPattern = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[@$!%*#?&])[A-Za-z\\d@$!%*#?&]{8,}$";
        return password != null && password.matches(passwordPattern);
    }

}
