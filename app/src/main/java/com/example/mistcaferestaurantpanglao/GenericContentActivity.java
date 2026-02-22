package com.example.mistcaferestaurantpanglao;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

public class GenericContentActivity extends AppCompatActivity {

    public static final String EXTRA_LAYOUT_ID = "layout_id";
    public static final String EXTRA_TITLE = "title";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        int layoutId = getIntent().getIntExtra(EXTRA_LAYOUT_ID, 0);
        String title = getIntent().getStringExtra(EXTRA_TITLE);

        if (layoutId != 0) {
            setContentView(layoutId);
        }

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            if (title != null) {
                getSupportActionBar().setTitle(title);
            }
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}