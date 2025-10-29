package com.example.onlyfanshop.utils;

import android.util.Patterns;

public class Validation {
    public static boolean isValidEmail(String email) {
        return email != null && Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }
}
