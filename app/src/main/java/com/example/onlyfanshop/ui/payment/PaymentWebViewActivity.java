package com.example.onlyfanshop.ui.payment; // Hoặc package phù hợp với bạn

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.example.onlyfanshop.R;

public class PaymentWebViewActivity extends AppCompatActivity {

    public static final String EXTRA_URL = "extra_url";

    private WebView webView;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment_web_view);

        webView = findViewById(R.id.webView);
        progressBar = findViewById(R.id.progressBar);

        String url = getIntent().getStringExtra(EXTRA_URL);

        // Bật JavaScript (rất quan trọng cho các cổng thanh toán)
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);
        webView.getSettings().setUseWideViewPort(true);
        webView.getSettings().setLoadWithOverviewMode(true);
        webView.getSettings().setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);


        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if (url.contains("vnp_ResponseCode=24")) {
                    Toast.makeText(PaymentWebViewActivity.this, "Bạn đã hủy thanh toán", Toast.LENGTH_SHORT).show();

                    // Có thể mở PaymentResultActivity báo “hủy” luôn nếu bạn muốn
                    Intent intent = new Intent(PaymentWebViewActivity.this, PaymentResultActivity.class);
                    intent.putExtra(PaymentResultActivity.EXTRA_RESULT, "cancel");
                    startActivity(intent);
                    finish();
                    return true;
                }
                if (url.contains("localhost:3000/payment-result")) {
                    Uri uri = Uri.parse(url);
                    String status = uri.getQueryParameter("status");
                    String orderId = uri.getQueryParameter("order");// 👈 lấy orderId
                    Log.d("orderID", orderId);
                    Intent intent = new Intent(PaymentWebViewActivity.this, PaymentResultActivity.class);
                    intent.putExtra(PaymentResultActivity.EXTRA_RESULT, status);
                    intent.putExtra("orderId", orderId); // 👈 truyền qua Result
                    startActivity(intent);
                    finish();
                    return true;
                }
                return false;
            }


            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                android.util.Log.d("VNPay", "Page started: " + url);
                progressBar.setVisibility(View.VISIBLE);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                progressBar.setVisibility(View.GONE);
                android.util.Log.d("VNPay", "Page finished: " + url);

            }

            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                android.util.Log.e("VNPay", "Error loading page: " + description);
            }

            @Override
            public void onReceivedSslError(WebView view, android.webkit.SslErrorHandler handler, android.net.http.SslError error) {
                android.util.Log.e("VNPay", "SSL Error: " + error.toString());
                handler.proceed(); // chỉ dùng để test, KHÔNG deploy thật
            }
        });


        if (url != null && !url.isEmpty()) {
            webView.loadUrl(url);
        }
    }
}
    