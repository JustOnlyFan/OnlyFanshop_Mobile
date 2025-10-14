package com.example.onlyfanshop.ui.product;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.onlyfanshop.R;
import com.example.onlyfanshop.model.BrandDTO;
import com.example.onlyfanshop.model.CategoryDTO;
import com.example.onlyfanshop.model.ProductDTO;

import java.util.List;

public class UpdateProductActivity extends AppCompatActivity {
    private static final int PICK_IMAGE_REQUEST = 100;

    private EditText edtName, edtBrief, edtFull, edtSpecs, edtPrice;
    private Spinner spinnerCategory, spinnerBrand;
    private ImageView imgPreview;
    private Button btnChooseImage, btnUpdate;

    private Uri selectedImageUri;
    private String uploadedImageUrl;

    private List<CategoryDTO> categoryList;
    private List<BrandDTO> brandList;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_update_product);
        edtName = findViewById(R.id.edtProductName);
        edtBrief = findViewById(R.id.edtBriefDescription);
        edtFull = findViewById(R.id.edtFullDescription);
        edtSpecs = findViewById(R.id.edtTechSpecs);
        edtPrice = findViewById(R.id.edtPrice);
        spinnerCategory = findViewById(R.id.spinnerCategory);
        spinnerBrand = findViewById(R.id.spinnerBrand);
        imgPreview = findViewById(R.id.imgPreview);
        btnChooseImage = findViewById(R.id.btnChooseImage);
        btnUpdate = findViewById(R.id.btnSubmitProduct);

//        // Nhận dữ liệu từ Intent
//        Intent intent = getIntent();
//        if (intent != null && intent.hasExtra("productToEdit")) {
//            ProductDTO product = (ProductDTO) intent.getSerializableExtra("productToEdit");
//            populateFields(product);
//        }
//
//        btnUpdate.setOnClickListener(v -> updateProduct());
    }
}