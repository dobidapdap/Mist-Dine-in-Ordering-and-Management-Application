package com.example.mistcaferestaurantpanglao;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ReceiptDialogFragment extends DialogFragment {
    private static final String ARG_ORDER = "order";
    private static final String ARG_CASH_GIVEN = "cash_given";
    private static final String ARG_CHANGE = "change";

    private Order order;
    private double cashGiven;
    private double change;
    private OnPaymentConfirmListener listener;

    public interface OnPaymentConfirmListener {
        void onPaymentConfirmed(Order order);
    }

    public static ReceiptDialogFragment newInstance(Order order, double cashGiven, double change) {
        ReceiptDialogFragment fragment = new ReceiptDialogFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_ORDER, order);
        args.putDouble(ARG_CASH_GIVEN, cashGiven);
        args.putDouble(ARG_CHANGE, change);
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
            order = (Order) getArguments().getSerializable(ARG_ORDER);
            cashGiven = getArguments().getDouble(ARG_CASH_GIVEN);
            change = getArguments().getDouble(ARG_CHANGE);
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        View view = LayoutInflater.from(getContext()).inflate(R.layout.dialog_receipt, null);

        setupReceiptContent(view);

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext());
        builder.setView(view)
                .setPositiveButton("Confirm Payment", (dialog, which) -> {
                    if (listener != null) {
                        listener.onPaymentConfirmed(order);
                    }
                })
                .setNegativeButton("Cancel", (dialog, which) -> dismiss());

        return builder.create();
    }

    private void setupReceiptContent(View view) {
        TextView tvReceiptHeader = view.findViewById(R.id.tv_receipt_header);
        TextView tvOrderId = view.findViewById(R.id.tv_receipt_order_id);
        TextView tvTableNumber = view.findViewById(R.id.tv_receipt_table);
        TextView tvCustomerName = view.findViewById(R.id.tv_receipt_customer);
        TextView tvOrderTime = view.findViewById(R.id.tv_receipt_time);
        TextView tvOrderItems = view.findViewById(R.id.tv_receipt_items);
        TextView tvSubtotal = view.findViewById(R.id.tv_receipt_subtotal);
        TextView tvTax = view.findViewById(R.id.tv_receipt_tax);
        TextView tvTotal = view.findViewById(R.id.tv_receipt_total);

        tvReceiptHeader.setText("MIST CAFE RESTAURANT\nPanglao, Bohol");

        tvOrderId.setText("Order #" + order.getOrderId().substring(5));
        tvTableNumber.setText("Table: " + order.getTableNumber());

        if (order.getCustomerName() != null && !order.getCustomerName().isEmpty()) {
            tvCustomerName.setText("Customer: " + order.getCustomerName());
            tvCustomerName.setVisibility(View.VISIBLE);
        } else {
            tvCustomerName.setVisibility(View.GONE);
        }

        SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());
        tvOrderTime.setText("Date: " + dateFormat.format(new Date(order.getTimestamp())));

        StringBuilder itemsText = new StringBuilder();
        List<OrderItem> items = order.getItems();
        double subtotal = 0;

        if (items != null && !items.isEmpty()) {
            for (OrderItem item : items) {
                double itemTotal = item.getPrice() * item.getQuantity();
                subtotal += itemTotal;

                itemsText.append(String.format("%-2d %-20s %8s\n", item.getQuantity(), item.getItemName() != null ? item.getItemName() : "Unknown Item", "₱" + String.format("%.2f", itemTotal)));

                if (item.getNotes() != null && !item.getNotes().trim().isEmpty()) {
                    itemsText.append("   Note: ").append(item.getNotes()).append("\n");
                }
            }
        } else {
            itemsText.append("No items found");
        }

        tvOrderItems.setText(itemsText.toString());

        // Calculate VAT for display only (do NOT add to total)
        double taxRate = 0.12;
        double taxAmount = subtotal * taxRate;
        double total = subtotal;

        tvSubtotal.setText("₱" + String.format("%.2f", subtotal));
        tvTax.setText("₱" + String.format("%.2f", taxAmount));
        tvTotal.setText("₱" + String.format("%.2f", total));

        // Add payment information section
        addPaymentSection(view, subtotal);
    }

    private void addPaymentSection(View view, double subtotal) {
        // Find the footer view
        TextView tvFooter = view.findViewById(R.id.tv_receipt_footer);
        if (tvFooter == null) return;

        // Create a LinearLayout for payment info if it doesn't exist
        ViewGroup parent = (ViewGroup) tvFooter.getParent();
        if (parent != null) {
            // Find position of footer
            int footerPosition = parent.indexOfChild(tvFooter);

            // Remove footer temporarily
            parent.removeView(tvFooter);

            // Create payment section
            android.widget.LinearLayout paymentLayout = new android.widget.LinearLayout(view.getContext());
            paymentLayout.setOrientation(android.widget.LinearLayout.VERTICAL);
            paymentLayout.setPadding(0, 16, 0, 16);

            // Divider
            View divider = new View(view.getContext());
            divider.setLayoutParams(new android.widget.LinearLayout.LayoutParams(
                    android.widget.LinearLayout.LayoutParams.MATCH_PARENT, 1));
            divider.setBackgroundColor(view.getResources().getColor(android.R.color.darker_gray));
            paymentLayout.addView(divider);

            // Cash Given
            android.widget.LinearLayout cashLayout = new android.widget.LinearLayout(view.getContext());
            cashLayout.setOrientation(android.widget.LinearLayout.HORIZONTAL);
            cashLayout.setPadding(0, 8, 0, 4);

            TextView tvCashLabel = new TextView(view.getContext());
            tvCashLabel.setText("Cash Given:");
            tvCashLabel.setLayoutParams(new android.widget.LinearLayout.LayoutParams(0, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1));
            tvCashLabel.setTextSize(14);

            TextView tvCashValue = new TextView(view.getContext());
            tvCashValue.setText("₱" + String.format(Locale.getDefault(), "%.2f", cashGiven));
            tvCashValue.setTextSize(14);
            tvCashValue.setLayoutParams(new android.widget.LinearLayout.LayoutParams(
                    android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                    android.widget.LinearLayout.LayoutParams.WRAP_CONTENT));

            cashLayout.addView(tvCashLabel);
            cashLayout.addView(tvCashValue);
            paymentLayout.addView(cashLayout);

            // Change
            android.widget.LinearLayout changeLayout = new android.widget.LinearLayout(view.getContext());
            changeLayout.setOrientation(android.widget.LinearLayout.HORIZONTAL);
            changeLayout.setPadding(0, 4, 0, 8);

            TextView tvChangeLabel = new TextView(view.getContext());
            tvChangeLabel.setText("Change:");
            tvChangeLabel.setLayoutParams(new android.widget.LinearLayout.LayoutParams(0, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1));
            tvChangeLabel.setTextSize(16);
            tvChangeLabel.setTypeface(null, android.graphics.Typeface.BOLD);

            TextView tvChangeValue = new TextView(view.getContext());
            tvChangeValue.setText("₱" + String.format(Locale.getDefault(), "%.2f", change));
            tvChangeValue.setTextSize(16);
            tvChangeValue.setTypeface(null, android.graphics.Typeface.BOLD);
            tvChangeValue.setTextColor(view.getResources().getColor(android.R.color.holo_green_dark));
            tvChangeValue.setLayoutParams(new android.widget.LinearLayout.LayoutParams(
                    android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                    android.widget.LinearLayout.LayoutParams.WRAP_CONTENT));

            changeLayout.addView(tvChangeLabel);
            changeLayout.addView(tvChangeValue);
            paymentLayout.addView(changeLayout);

            // Add payment layout before footer
            parent.addView(paymentLayout, footerPosition);

            // Add footer back
            parent.addView(tvFooter);
        }
    }
}