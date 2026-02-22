package com.example.mistcaferestaurantpanglao;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class OrderHistoryAdapter extends RecyclerView.Adapter<OrderHistoryAdapter.OrderHistoryViewHolder> {
    private List<Order> orderHistory;
    private SimpleDateFormat dateTimeFormat;

    public OrderHistoryAdapter(List<Order> orderHistory) {
        this.orderHistory = orderHistory;
        this.dateTimeFormat = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());
    }

    @NonNull
    @Override
    public OrderHistoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_order_history, parent, false);
        return new OrderHistoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull OrderHistoryViewHolder holder, int position) {
        Order order = orderHistory.get(position);
        holder.bind(order);
    }

    @Override
    public int getItemCount() {
        return orderHistory != null ? orderHistory.size() : 0;
    }

    public void updateOrderHistory(List<Order> newOrderHistory) {
        this.orderHistory = newOrderHistory;
        notifyDataSetChanged();
    }

    class OrderHistoryViewHolder extends RecyclerView.ViewHolder {
        private TextView tvOrderId;
        private TextView tvCustomerName;
        private TextView tvTimestamp;
        private TextView tvTotalAmount;
        private TextView tvItemsCount;

        public OrderHistoryViewHolder(@NonNull View itemView) {
            super(itemView);
            tvOrderId = itemView.findViewById(R.id.tv_order_id);
            tvCustomerName = itemView.findViewById(R.id.tv_customer_name);
            tvTimestamp = itemView.findViewById(R.id.tv_timestamp);
            tvTotalAmount = itemView.findViewById(R.id.tv_total_amount);
            tvItemsCount = itemView.findViewById(R.id.tv_items_count);
        }

        public void bind(Order order) {
            String displayOrderId = order.getOrderId();
            if (displayOrderId != null && displayOrderId.length() > 5) {
                displayOrderId = displayOrderId.substring(displayOrderId.length() - 5);
            }
            tvOrderId.setText("Order #" + displayOrderId);

            String customerName = order.getCustomerName();
            if (customerName != null && !customerName.trim().isEmpty()) {
                tvCustomerName.setText(customerName);
                tvCustomerName.setVisibility(View.VISIBLE);
            } else {
                tvCustomerName.setVisibility(View.GONE);
            }

            Date orderDate = new Date(order.getTimestamp());
            tvTimestamp.setText(dateTimeFormat.format(orderDate));

            tvTotalAmount.setText(String.format(Locale.getDefault(), "₱%.2f", order.getTotalAmount()));

            List<OrderItem> items = order.getItems();
            if (items != null && !items.isEmpty()) {
                int itemCount = items.size();
                String itemLabel = itemCount == 1 ? "item" : "items";
                tvItemsCount.setText(itemCount + " " + itemLabel);
                tvItemsCount.setVisibility(View.VISIBLE);
            } else {
                tvItemsCount.setVisibility(View.GONE);
            }
        }
    }
}