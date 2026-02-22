package com.example.mistcaferestaurantpanglao;

import android.app.Dialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.Locale;

public class PaymentDialogFragment extends DialogFragment {

    private static final String ARG_ORDER_ID = "order_id";
    private static final String ARG_TOTAL_AMOUNT = "total_amount";

    private String orderId;
    private double totalAmount;
    private OnPaymentConfirmListener listener;

    private TextInputEditText etCashGiven;
    private TextInputLayout tilCashGiven;
    private TextView tvPaymentTotal;
    private TextView tvPaymentSubtotal;
    private TextView tvPaymentCashGiven;
    private TextView tvPaymentChange;
    private TextView tvPaymentStatus;
    private MaterialButton btnConfirm;
    private MaterialButton btnCancel;

    public interface OnPaymentConfirmListener {
        void onPaymentConfirmed(String orderId, double totalAmount, double cashGiven, double change);
    }

    public static PaymentDialogFragment newInstance(String orderId, double totalAmount) {
        PaymentDialogFragment fragment = new PaymentDialogFragment();
        Bundle args = new Bundle();
        args.putString(ARG_ORDER_ID, orderId);
        args.putDouble(ARG_TOTAL_AMOUNT, totalAmount);
        fragment.setArguments(args);
        return fragment;
    }

    public void setOnPaymentConfirmListener(OnPaymentConfirmListener listener) {
        this.listener = listener;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            orderId = getArguments().getString(ARG_ORDER_ID);
            totalAmount = getArguments().getDouble(ARG_TOTAL_AMOUNT);
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        View view = LayoutInflater.from(getContext()).inflate(R.layout.dialog_payment_input, null);

        initializeViews(view);
        setupTotalAmount();
        setupTextWatcher();
        setupClickListeners();

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext());
        builder.setView(view)
                .setCancelable(false);

        return builder.create();
    }

    private void initializeViews(View view) {
        etCashGiven = view.findViewById(R.id.et_cash_given);
        tilCashGiven = view.findViewById(R.id.til_cash_given);
        tvPaymentTotal = view.findViewById(R.id.tv_payment_total);
        tvPaymentSubtotal = view.findViewById(R.id.tv_payment_subtotal);
        tvPaymentCashGiven = view.findViewById(R.id.tv_payment_cash_given);
        tvPaymentChange = view.findViewById(R.id.tv_payment_change);
        tvPaymentStatus = view.findViewById(R.id.tv_payment_status);
        btnConfirm = view.findViewById(R.id.btn_payment_confirm);
        btnCancel = view.findViewById(R.id.btn_payment_cancel);
    }

    private void setupTotalAmount() {
        tvPaymentTotal.setText(String.format(Locale.getDefault(), "₱%.2f", totalAmount));
        tvPaymentSubtotal.setText(String.format(Locale.getDefault(), "₱%.2f", totalAmount));
        tvPaymentCashGiven.setText("₱0.00");
        tvPaymentChange.setText("₱0.00");
    }

    private void setupTextWatcher() {
        etCashGiven.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                calculateChange();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void calculateChange() {
        String cashInput = etCashGiven.getText().toString().trim();

        if (TextUtils.isEmpty(cashInput)) {
            tvPaymentCashGiven.setText("₱0.00");
            tvPaymentChange.setText("₱0.00");
            tvPaymentStatus.setText("Enter cash amount to calculate change");
            btnConfirm.setEnabled(false);
            tilCashGiven.setError(null);
            return;
        }

        try {
            double cashGiven = Double.parseDouble(cashInput);

            if (cashGiven < 0) {
                tilCashGiven.setError("Cash cannot be negative");
                btnConfirm.setEnabled(false);
                tvPaymentCashGiven.setText("₱0.00");
                tvPaymentChange.setText("₱0.00");
                tvPaymentStatus.setText("Invalid cash amount");
                return;
            }

            double change = cashGiven - totalAmount;
            tvPaymentCashGiven.setText(String.format(Locale.getDefault(), "₱%.2f", cashGiven));
            tvPaymentChange.setText(String.format(Locale.getDefault(), "₱%.2f", change));
            tilCashGiven.setError(null);

            if (cashGiven < totalAmount) {
                tvPaymentStatus.setText("Insufficient payment!");
                tvPaymentChange.setTextColor(getResources().getColor(android.R.color.holo_red_light));
                btnConfirm.setEnabled(false);
            } else if (cashGiven == totalAmount) {
                tvPaymentStatus.setText("✓ Exact payment");
                tvPaymentChange.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
                btnConfirm.setEnabled(true);
            } else {
                tvPaymentStatus.setText("✓ Payment accepted");
                tvPaymentChange.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
                btnConfirm.setEnabled(true);
            }

        } catch (NumberFormatException e) {
            tilCashGiven.setError("Invalid amount");
            btnConfirm.setEnabled(false);
            tvPaymentStatus.setText("Enter a valid number");
            tvPaymentChange.setText("₱0.00");
        }
    }

    private void setupClickListeners() {
        btnConfirm.setOnClickListener(v -> {
            String cashInput = etCashGiven.getText().toString().trim();
            if (!TextUtils.isEmpty(cashInput)) {
                double cashGiven = Double.parseDouble(cashInput);
                double change = cashGiven - totalAmount;

                if (listener != null) {
                    listener.onPaymentConfirmed(orderId, totalAmount, cashGiven, change);
                }
                dismiss();
            }
        });

        btnCancel.setOnClickListener(v -> dismiss());
    }
}