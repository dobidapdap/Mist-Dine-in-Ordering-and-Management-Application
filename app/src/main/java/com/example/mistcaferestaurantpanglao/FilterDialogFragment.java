package com.example.mistcaferestaurantpanglao;

import android.app.Dialog;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class FilterDialogFragment extends DialogFragment {
    private static final String TAG = "FilterDialogFragment";

    private RadioGroup sortRadioGroup;
    private EditText etMinAmount;
    private EditText etMaxAmount;
    private Button btnApplyFilter;
    private Button btnReset;
    private FilterListener filterListener;

    public interface FilterListener {
        void onFilterApplied(FilterOptions filterOptions);
    }

    public static FilterDialogFragment newInstance(FilterListener listener) {
        FilterDialogFragment fragment = new FilterDialogFragment();
        fragment.filterListener = listener;
        return fragment;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());

        // Create custom view
        android.view.View view = android.view.LayoutInflater.from(getActivity())
                .inflate(R.layout.dialog_filter_orders, null);

        initializeViews(view);
        setupClickListeners();

        builder.setView(view)
                .setTitle("Filter Orders")
                .setCancelable(true);

        return builder.create();
    }

    private void initializeViews(android.view.View view) {
        sortRadioGroup = view.findViewById(R.id.sort_radio_group);
        etMinAmount = view.findViewById(R.id.et_min_amount);
        etMaxAmount = view.findViewById(R.id.et_max_amount);
        btnApplyFilter = view.findViewById(R.id.btn_apply_filter);
        btnReset = view.findViewById(R.id.btn_reset_filter);

        // Set default sort option
        sortRadioGroup.check(R.id.radio_newest);
    }

    private void setupClickListeners() {
        btnApplyFilter.setOnClickListener(v -> applyFilter());
        btnReset.setOnClickListener(v -> resetFilter());
    }

    private void applyFilter() {
        FilterOptions filterOptions = new FilterOptions();

        // Get sort option
        int selectedSortId = sortRadioGroup.getCheckedRadioButtonId();
        if (selectedSortId == R.id.radio_newest) {
            filterOptions.setSortBy("newest");
        } else if (selectedSortId == R.id.radio_oldest) {
            filterOptions.setSortBy("oldest");
        } else if (selectedSortId == R.id.radio_highest_price) {
            filterOptions.setSortBy("highest_price");
        } else if (selectedSortId == R.id.radio_lowest_price) {
            filterOptions.setSortBy("lowest_price");
        }

        // Get price range
        String minAmountText = etMinAmount.getText().toString().trim();
        String maxAmountText = etMaxAmount.getText().toString().trim();

        if (!minAmountText.isEmpty()) {
            try {
                filterOptions.setMinAmount(Double.parseDouble(minAmountText));
            } catch (NumberFormatException e) {
                etMinAmount.setError("Invalid amount");
                return;
            }
        }

        if (!maxAmountText.isEmpty()) {
            try {
                filterOptions.setMaxAmount(Double.parseDouble(maxAmountText));
            } catch (NumberFormatException e) {
                etMaxAmount.setError("Invalid amount");
                return;
            }
        }

        if (filterListener != null) {
            filterListener.onFilterApplied(filterOptions);
        }

        dismiss();
    }

    private void resetFilter() {
        etMinAmount.setText("");
        etMaxAmount.setText("");
        sortRadioGroup.check(R.id.radio_newest);
    }

    // Filter options class
    public static class FilterOptions {
        private String sortBy = "newest";
        private double minAmount = 0;
        private double maxAmount = Double.MAX_VALUE;

        public String getSortBy() {
            return sortBy;
        }

        public void setSortBy(String sortBy) {
            this.sortBy = sortBy;
        }

        public double getMinAmount() {
            return minAmount;
        }

        public void setMinAmount(double minAmount) {
            this.minAmount = minAmount;
        }

        public double getMaxAmount() {
            return maxAmount;
        }

        public void setMaxAmount(double maxAmount) {
            this.maxAmount = maxAmount;
        }
    }
}