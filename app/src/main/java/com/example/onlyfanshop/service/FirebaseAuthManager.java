package com.example.onlyfanshop.service;

import android.content.Context;
import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public final class FirebaseAuthManager {

    private static final String TAG = "FirebaseAuthMgr";

    private FirebaseAuthManager() {}

    public static void ensureSignedIn(Context context) {
        try {
            FirebaseAuth auth = FirebaseAuth.getInstance();
            FirebaseUser current = auth.getCurrentUser();
            if (current == null) {
                auth.signInAnonymously()
                        .addOnSuccessListener(result -> Log.d(TAG, "Firebase anonymous sign-in ok: " + result.getUser().getUid()))
                        .addOnFailureListener(e -> Log.e(TAG, "Firebase anonymous sign-in failed: " + e.getMessage()));
            } else {
                Log.d(TAG, "Firebase already signed in: " + current.getUid());
            }
        } catch (Exception e) {
            Log.e(TAG, "ensureSignedIn error: " + e.getMessage());
        }
    }
}


