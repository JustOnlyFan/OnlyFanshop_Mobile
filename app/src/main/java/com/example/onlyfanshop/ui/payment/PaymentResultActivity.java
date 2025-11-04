package com.example.onlyfanshop.ui.payment;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import com.example.onlyfanshop.R;
import com.example.onlyfanshop.activity.DashboardActivity;
import com.example.onlyfanshop.ui.order.OrderDetailsActivity; // 👈 import activity hiển thị chi tiết đơn hàng

public class PaymentResultActivity extends AppCompatActivity {

    public static final String EXTRA_RESULT = "payment_result"; // "success" hoặc "fail"

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String result = getIntent().getStringExtra(EXTRA_RESULT);
        String orderIdStr = getIntent().getStringExtra("orderId");
        int orderId = -1;

        try {
            orderId = Integer.parseInt(orderIdStr);
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }

        if ("success".equals(result)) {
            setContentView(R.layout.activity_payment_success);

            Button btnViewOrder = findViewById(R.id.btnViewOrder);
            Button btnGoHome = findViewById(R.id.btnGoHome);

            int finalOrderId = orderId;
            btnViewOrder.setOnClickListener(v -> {
                if (finalOrderId != -1) {
                    Intent intent = new Intent(this, OrderDetailsActivity.class);
                    intent.putExtra("orderId", finalOrderId);
                    startActivity(intent);
                    finish();
                } else {
                    Toast.makeText(this, "Order ID not found", Toast.LENGTH_SHORT).show();
                }
            });

            btnGoHome.setOnClickListener(v -> {
                Intent intent = new Intent(this, DashboardActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                finish();
            });

        } else {
            setContentView(R.layout.activity_payment_failed);

            Button btnGoHome = findViewById(R.id.btnGoHome);
            btnGoHome.setOnClickListener(v -> {
                Intent intent = new Intent(this, DashboardActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                finish();
            });
        }
    }

}
