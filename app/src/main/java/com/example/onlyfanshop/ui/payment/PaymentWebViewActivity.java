package com.example.onlyfanshop.ui.payment; // Hoặc package phù hợp với bạn

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
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
                android.util.Log.d("VNPay", "Redirecting to: " + url);
                view.loadUrl(url);
                return true;
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

                if (url.contains("vnp_ResponseCode=00")) {
                    Intent intent = new Intent(PaymentWebViewActivity.this, PaymentResultActivity.class);
                    intent.putExtra(PaymentResultActivity.EXTRA_RESULT, "success");
                    startActivity(intent);
                    finish();
                } else if (url.contains("vnp_ResponseCode")) {
                    Intent intent = new Intent(PaymentWebViewActivity.this, PaymentResultActivity.class);
                    intent.putExtra(PaymentResultActivity.EXTRA_RESULT, "failed");
                    startActivity(intent);
                    finish();
                }
            }

            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                android.util.Log.e("VNPay", "Error loading page: " + description);
                Toast.makeText(PaymentWebViewActivity.this, "Lỗi: " + description, Toast.LENGTH_SHORT).show();
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
    