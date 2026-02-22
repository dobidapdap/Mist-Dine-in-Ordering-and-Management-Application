package com.example.mistcaferestaurantpanglao;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.textfield.TextInputEditText;

public class GenerateQrActivity extends AppCompatActivity {

    private TextInputEditText etTableNumber;
    private TextView tvGeneratedUrl;
    private ImageView ivCopyUrl;
    private Button btnOpenQRGenerator;
    private Toolbar toolbar;

    private static final String QR_GENERATOR_URL = "https://goqr.me/#t=url";
    private static final String BASE_URL = "mistcafe://table?id=";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_generate_qr);

        // Setup toolbar with back button
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        // Set up back button click listener
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        // Initialize views
        etTableNumber = findViewById(R.id.etTableNumber);
        tvGeneratedUrl = findViewById(R.id.tvGeneratedUrl);
        ivCopyUrl = findViewById(R.id.ivCopyUrl);
        btnOpenQRGenerator = findViewById(R.id.btnOpenQRGenerator);

        // Add text watcher to update URL when table number changes
        etTableNumber.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateGeneratedUrl();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        // Copy URL button
        ivCopyUrl.setOnClickListener(v -> copyUrlToClipboard());

        // Open QR generator website button
        btnOpenQRGenerator.setOnClickListener(v -> openQRGeneratorWebsite());
    }

    private void updateGeneratedUrl() {
        String tableNumber = etTableNumber.getText().toString().trim();

        if (tableNumber.isEmpty()) {
            tvGeneratedUrl.setText(BASE_URL + "X");
        } else {
            tvGeneratedUrl.setText(BASE_URL + tableNumber);
        }
    }

    private void copyUrlToClipboard() {
        String tableNumber = etTableNumber.getText().toString().trim();

        if (tableNumber.isEmpty()) {
            Toast.makeText(this, "Please enter a table number first", Toast.LENGTH_SHORT).show();
            return;
        }

        String url = tvGeneratedUrl.getText().toString();

        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("Table QR URL", url);
        clipboard.setPrimaryClip(clip);

        Toast.makeText(this, "URL copied to clipboard!", Toast.LENGTH_SHORT).show();
    }

    private void openQRGeneratorWebsite() {
        String tableNumber = etTableNumber.getText().toString().trim();

        if (tableNumber.isEmpty()) {
            Toast.makeText(this, "Please enter a table number first", Toast.LENGTH_SHORT).show();
            return;
        }

        // Copy URL to clipboard before opening website
        copyUrlToClipboard();

        // Open the QR generator website
        try {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(QR_GENERATOR_URL));
            startActivity(browserIntent);

            Toast.makeText(this, "URL copied! Paste it in the website to generate QR code", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, "Unable to open browser", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }
}