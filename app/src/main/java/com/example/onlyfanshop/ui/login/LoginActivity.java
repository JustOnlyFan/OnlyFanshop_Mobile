package com.example.onlyfanshop.ui.login;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import androidx.appcompat.app.AppCompatActivity;

import com.example.onlyfanshop.MainActivity;
import com.example.onlyfanshop.R;
import com.example.onlyfanshop.activity.AdminActivity;
import com.example.onlyfanshop.api.ApiClient;
import com.example.onlyfanshop.api.UserApi;
import com.example.onlyfanshop.model.response.ApiResponse;
import com.example.onlyfanshop.model.Request.LoginRequest;
import com.example.onlyfanshop.model.UserDTO;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
// removed FacebookAuthProvider

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;

// removed Facebook SDK imports

import okhttp3.ResponseBody;

import android.util.Base64;

import java.nio.charset.StandardCharsets;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {
    private UserApi userApi;
    private EditText etUsername, etPassword;
    private Button btnLogin, btnLoginGoogle;
    private LinearLayout btnBackMain; // dùng LinearLayout làm container cho icon + text
    private ImageView logoFan;
    private TextView tvForgotPassword, tvSignUp;
    private final Gson gson = new Gson();
    private FirebaseAuth mAuth;
    private GoogleSignInClient mGoogleSignInClient;
    private static final int RC_GOOGLE_SIGN_IN = 9001;
    private ActivityResultLauncher<Intent> googleSignInLauncher;
    private AuthCredential pendingLinkCredential;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();

        initializeGoogleSignIn();

        initializeGoogleSignInLauncher();

        userApi = ApiClient.getPublicClient().create(UserApi.class);
        etUsername = findViewById(R.id.edtUsername);
        etPassword = findViewById(R.id.edtPassword);
        btnLogin = findViewById(R.id.btnLogin);
        btnLoginGoogle = findViewById(R.id.btnLoginGoogle);
        btnBackMain = findViewById(R.id.btnBackMain);
        logoFan = findViewById(R.id.logoFan);
        tvForgotPassword = findViewById(R.id.tvForgotPassword);
        tvSignUp = findViewById(R.id.tvSignUp);

        // Set up back button to return to MainActivity
        btnBackMain.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
            // optional: clear the login activity from back stack
            startActivity(intent);
            finish();
        });

        tvForgotPassword.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, ForgotActivity.class);
            startActivity(intent);
        });
        tvSignUp.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(intent);
        });

        btnLoginGoogle.setOnClickListener(v -> signInWithGoogle());

        TextWatcher watcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                checkInputFields();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        };

        etUsername.addTextChangedListener(watcher);
        etPassword.addTextChangedListener(watcher);

        btnLogin.setOnClickListener(v -> login());
    }

    private void checkInputFields() {
        String username = etUsername.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        btnLogin.setEnabled(!username.isEmpty() && !password.isEmpty());
    }

    private void login() {
        String username = etUsername.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        Log.d("LoginActivityLog", "login() called" + username + password);
        if (username.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập username và password", Toast.LENGTH_SHORT).show();
            return;
        }

        LoginRequest request = new LoginRequest(username, password);

        userApi.login(request).enqueue(new Callback<ApiResponse<UserDTO>>() {
            @Override
            public void onResponse(Call<ApiResponse<UserDTO>> call, Response<ApiResponse<UserDTO>> response) {
                ApiResponse<UserDTO> apiResponse = response.body();

                if (apiResponse == null && response.errorBody() != null) {
                    apiResponse = parseErrorBody(response.errorBody(), UserDTO.class);
                }
                Log.d("LoginActivityLog", "onResponse() called" + apiResponse.getMessage().toString());
                if (apiResponse != null) {
                    if (apiResponse.getStatusCode() == 200) {
                        UserDTO user = apiResponse.getData();
                        String token = user.getToken();
                        Log.d("LoginActivityLog", "Token: " + token);

                        // Lưu token vào SharedPreferences
                        //)
                        SharedPreferences sharedPreferences = getSharedPreferences("MyAppPrefs", MODE_PRIVATE);
                        sharedPreferences.edit().putString("jwt_token", token).apply();
                        sharedPreferences.edit().putString("username", user.getUsername()).apply();
                        sharedPreferences.edit().putString("email", user.getEmail()).apply();
                        sharedPreferences.edit().putString("role", user.getRole()).apply();
                        sharedPreferences.edit().putString("authProvider", user.getAuthProvider()).apply();
                        sharedPreferences.edit().putInt("userId", user.getUserID()).apply();
                        sharedPreferences.edit().putString("address", user.getAddress()).apply();
                        Log.d("role", user.getRole());
                        if (user.getRole().equals("ADMIN")) {
                            Log.d("role", "qua trang admin");
                            Intent intent = new Intent(LoginActivity.this, AdminActivity.class);
                            intent.putExtra("user", user);
                            startActivity(intent);
                            Toast.makeText(LoginActivity.this, "Welcome " + user.getUsername(), Toast.LENGTH_SHORT).show();
                        } else {
                            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                            intent.putExtra("user", user);
                            startActivity(intent);
                            Toast.makeText(LoginActivity.this, "Welcome " + user.getUsername(), Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(LoginActivity.this, apiResponse.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(LoginActivity.this, "Login thất bại", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<UserDTO>> call, Throwable t) {
                Toast.makeText(LoginActivity.this, "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private <T> ApiResponse<T> parseErrorBody(ResponseBody errorBody, Class<T> dataClass) {
        try {
            return gson.fromJson(
                    errorBody.string(),
                    TypeToken.getParameterized(ApiResponse.class, dataClass).getType()
            );
        } catch (Exception e) {
            Log.e("LoginActivity", "parseErrorBody failed", e);
            return null;
        }
    }

    private void initializeGoogleSignIn() {
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken("328191492825-8iket64hs1nr651gn0jnb19js7aimj10.apps.googleusercontent.com") // Web client ID từ Firebase Console
                .requestEmail()
                .requestProfile()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);
        Log.d("GoogleSignIn", "Google Sign-In initialized with client ID: 328191492825-8iket64hs1nr651gn0jnb19js7aimj10.apps.googleusercontent.com");
    }

    // removed initializeFacebookLogin

    private void initializeGoogleSignInLauncher() {
        googleSignInLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    Log.d("GoogleSignIn", "Google Sign-In result: " + result.getResultCode());
                    if (result.getResultCode() == RESULT_OK) {
                        Intent data = result.getData();
                        if (data != null) {
                            Log.d("GoogleSignIn", "Processing Google Sign-In result");
                            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
                            try {
                                GoogleSignInAccount account = task.getResult(ApiException.class);
                                if (account != null) {
                                    Log.d("GoogleSignIn", "Google sign in successful");
                                    firebaseAuthWithGoogle(account.getIdToken());
                                } else {
                                    Log.e("GoogleSignIn", "Google account is null");
                                    Toast.makeText(this, "Google sign in failed: Account is null", Toast.LENGTH_SHORT).show();
                                }
                            } catch (ApiException e) {
                                Log.e("GoogleSignIn", "Google sign in failed: " + e.getMessage() + " (Code: " + e.getStatusCode() + ")");
                                Toast.makeText(this, "Google sign in failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Log.e("GoogleSignIn", "Google Sign-In data is null");
                            Toast.makeText(this, "Google Sign-In data is null", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Log.d("GoogleSignIn", "Google sign in cancelled by user");
                        Toast.makeText(this, "Google sign in cancelled", Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }

    private void signInWithGoogle() {
        Log.d("GoogleSignIn", "Starting Google Sign-In process");
        try {
            mGoogleSignInClient.signOut().addOnCompleteListener(this, task -> {
                Log.d("GoogleSignIn", "Signed out from previous session");

                Intent signInIntent = mGoogleSignInClient.getSignInIntent();
                if (signInIntent != null) {
                    Log.d("GoogleSignIn", "Launching Google Sign-In intent with account picker");
                    googleSignInLauncher.launch(signInIntent);
                } else {
                    Log.e("GoogleSignIn", "Google Sign-In intent is null");
                    Toast.makeText(this, "Google Sign-In không khả dụng", Toast.LENGTH_SHORT).show();
                }
            });
        } catch (Exception e) {
            Log.e("GoogleSignIn", "Error starting Google Sign-In: " + e.getMessage());
            Toast.makeText(this, "Lỗi Google Sign-In: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    // removed initializeFacebookLoginLauncher

    // removed signInWithFacebook

    // removed onActivityResult for Facebook

    private void firebaseAuthWithGoogle(String idToken) {
        if (idToken == null || idToken.isEmpty()) {
            Log.e("GoogleAuth", "ID token is null or empty");
            Toast.makeText(this, "Google authentication failed: Invalid token", Toast.LENGTH_SHORT).show();
            return;
        }

        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        Log.d("GoogleAuth", "Firebase authentication with Google successful");
                        FirebaseUser user = mAuth.getCurrentUser();
                        Log.d("GoogleAuth", "About to call handleSuccessfulLogin");
                        if (pendingLinkCredential != null && user != null) {
                            user.linkWithCredential(pendingLinkCredential)
                                    .addOnCompleteListener(this, linkTask -> {
                                        if (linkTask.isSuccessful()) {
                                            Log.d("GoogleAuth", "Linked pending credential to existing account");
                                            pendingLinkCredential = null;
                                            handleSuccessfulLogin(user);
                                        } else {
                                            String err = linkTask.getException() != null ? linkTask.getException().getMessage() : "Unknown";
                                            Log.e("GoogleAuth", "Linking failed: " + err);
                                            Toast.makeText(LoginActivity.this, "Liên kết tài khoản thất bại: " + err, Toast.LENGTH_LONG).show();
                                        }
                                    });
                        } else {
                            handleSuccessfulLogin(user);
                        }
                    } else {
                        Log.e("GoogleAuth", "Firebase authentication with Google failed: " + task.getException().getMessage());
                        Toast.makeText(LoginActivity.this, "Google authentication failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // removed handleFacebookAccessToken

    // removed callFacebookLoginApi

    private void handleSuccessfulLogin(FirebaseUser user) {
        Log.d("GoogleAuth", "handleSuccessfulLogin called");
        if (user != null) {
            Log.d("GoogleAuth", "Processing successful login for user: " + user.getEmail());
            Log.d("GoogleAuth", "Firebase User - Username: " + user.getDisplayName());
            Log.d("GoogleAuth", "Firebase User - Email: " + user.getEmail());
            Log.d("GoogleAuth", "Firebase User - UID: " + user.getUid());

            // Log Firebase ID token (useful for debugging 403 from backend)
            try {
                user.getIdToken(true).addOnSuccessListener(result -> {
                    String firebaseIdToken = result != null ? result.getToken() : null;
                    Log.d("GoogleAuth", "Firebase ID Token: " + firebaseIdToken);
                }).addOnFailureListener(e -> Log.e("GoogleAuth", "Failed to get Firebase ID token: " + e.getMessage()));
            } catch (Exception ignored) {
            }

            callGoogleLoginApi(user.getEmail(), user.getDisplayName());
        } else {
            Log.e("GoogleAuth", "FirebaseUser is null in handleSuccessfulLogin");
        }
    }

    // Call Google login API to save user data to backend
    private void callGoogleLoginApi(String email, String username) {
        Log.d("GoogleAuth", "Calling Google login API for: " + email);
        Log.d("GoogleAuth", "Username: " + username);
        Log.d("GoogleAuth", "Email: " + email);

        UserApi.GoogleLoginRequest request = new UserApi.GoogleLoginRequest(email, username);

        // Build a Base64URL token to keep header ASCII-safe
        String role = "CUSTOMER"; // default; backend sets final role
        String payloadJson = "{\"role\":\"" + role + "\",\"email\":\"" + email + "\",\"username\":\"" + (username == null ? "" : username) + "\"}";
        String customToken = Base64.encodeToString(payloadJson.getBytes(StandardCharsets.UTF_8), Base64.NO_WRAP | Base64.URL_SAFE);

        Log.d("GoogleAuth", "X-Custom-Token (b64url): " + customToken);
        try {
            String decoded = new String(android.util.Base64.decode(customToken, android.util.Base64.URL_SAFE | android.util.Base64.NO_WRAP));
            Log.d("GoogleAuth", "X-Custom-Token decoded: " + decoded);
        } catch (Exception ignored) {
        }

        userApi.googleLogin(request, customToken).enqueue(new Callback<ApiResponse<UserDTO>>() {
            @Override
            public void onResponse(Call<ApiResponse<UserDTO>> call, Response<ApiResponse<UserDTO>> response) {
                Log.d("GoogleAuth", "Response code: " + response.code());

                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<UserDTO> apiResponse = response.body();
                    Log.d("GoogleAuth", "API Response status: " + apiResponse.getStatusCode());
                    Log.d("GoogleAuth", "API Response message: " + apiResponse.getMessage());

                    if (apiResponse.getStatusCode() == 200) {
                        Log.d("GoogleAuth", "Google login API successful");
                        UserDTO userDTO = apiResponse.getData();
                        String token = userDTO != null ? userDTO.getToken() : null;
                        if (token != null && !token.isEmpty()) {
                            Log.d("GoogleAuth", "JWT from backend: " + token);
                            SharedPreferences prefs = getSharedPreferences("MyAppPrefs", MODE_PRIVATE);
                            prefs.edit().putString("jwt_token", token).apply();
                            Log.d("GoogleAuth", "Saved JWT token from Google login");
                        }

                        // Log user information from backend
                        Log.d("GoogleAuth", "Backend User - Username: " + userDTO.getUsername());
                        Log.d("GoogleAuth", "Backend User - Email: " + userDTO.getEmail());
                        Log.d("GoogleAuth", "Backend User - Role: " + userDTO.getRole());
                        Log.d("GoogleAuth", "Backend User - AuthProvider: " + userDTO.getAuthProvider());

                        Toast.makeText(LoginActivity.this, "Welcome " + userDTO.getUsername(), Toast.LENGTH_SHORT).show();

                        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                        intent.putExtra("user", userDTO);
                        startActivity(intent);
                        finish();
                    } else if (apiResponse.getStatusCode() == 400) {
                        // Email conflict - hiển thị Toast
                        Log.e("GoogleAuth", "Email conflict: " + apiResponse.getMessage());
                        Toast.makeText(LoginActivity.this, "Email đã tồn tại. Vui lòng sử dụng phương thức đăng nhập khác.", Toast.LENGTH_LONG).show();
                    } else {
                        Log.e("GoogleAuth", "Google login API failed: " + apiResponse.getMessage());
                        showErrorDialog("Lỗi đăng nhập", apiResponse.getMessage());
                    }
                } else {
                    Log.e("GoogleAuth", "Google login API response failed: " + response.code());
                    try {
                        String err = response.errorBody() != null ? response.errorBody().string() : "<no body>";
                        Log.e("GoogleAuth", "Error body: " + err);
                    } catch (Exception ignored) {
                    }
                    String errorMessage = "Không thể kết nối đến server";
                    if (response.code() == 400) {
                        // Email conflict - hiển thị Toast
                        errorMessage = "Email already exists. Please use another login method.";
                        Toast.makeText(LoginActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                    } else if (response.code() == 500) {
                        errorMessage = "Server error. Please try again later.";
                        showErrorDialog("Connection Error", errorMessage);
                    } else if (response.code() == 404) {
                        errorMessage = "Service not found. Please check your connection.";
                        showErrorDialog("Connection Error", errorMessage);
                    } else {
                        Toast.makeText(LoginActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                    }
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<UserDTO>> call, Throwable t) {
                Log.e("GoogleAuth", "Google login API call failed: " + t.getMessage());
                showErrorDialog("Lỗi kết nối", "Không thể kết nối đến server. Vui lòng kiểm tra kết nối internet và thử lại.");
            }
        });
    }

    private void showErrorDialog(String title, String message) {
        new android.app.AlertDialog.Builder(this)
                .setTitle("⚠️ " + title)
                .setMessage(message)
                .setPositiveButton("Đóng", (dialog, which) -> {
                    dialog.dismiss();
                })
                .setNegativeButton("Thử lại", (dialog, which) -> {
                    dialog.dismiss();
                })
                .setCancelable(false)
                .show();
    }
}