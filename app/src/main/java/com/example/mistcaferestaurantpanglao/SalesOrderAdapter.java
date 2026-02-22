package com.example.mistcaferestaurantpanglao;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.Timestamp;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class SalesOrderAdapter extends RecyclerView.Adapter<SalesOrderAdapter.OrderViewHolder> {

    private Context context;
    private List<Order> orders;
    private SimpleDateFormat dateTimeFormat;

    public SalesOrderAdapter(Context context, List<Order> orders) {
        this.context = context;
        this.orders = orders;
        this.dateTimeFormat = new SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault());
    }

    @NonNull
    @Override
    public OrderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_order_report, parent, false);
        return new OrderViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull OrderViewHolder holder, int position) {
        Order order = orders.get(position);
        holder.bind(order);
    }

    @Override
    public int getItemCount() {
        return orders.size();
    }

    class OrderViewHolder extends RecyclerView.ViewHolder {
        private TextView tvOrderId;
        private TextView tvCustomerName;
        private TextView tvOrderAmount;
        private TextView tvOrderDateTime;
        private TextView tvItemsCount;

        public OrderViewHolder(@NonNull View itemView) {
            super(itemView);
            tvOrderId = itemView.findViewById(R.id.tvOrderId);
            tvCustomerName = itemView.findViewById(R.id.tvCustomerName);
            tvOrderAmount = itemView.findViewById(R.id.tvOrderAmount);
            tvOrderDateTime = itemView.findViewById(R.id.tvOrderDateTime);
            tvItemsCount = itemView.findViewById(R.id.tvItemsCount);
        }

        public void bind(Order order) {
            String orderId = order.getOrderId();
            if (orderId != null && orderId.length() > 8) {
                tvOrderId.setText("Order #" + orderId.substring(orderId.length() - 8));
            } else {
                tvOrderId.setText("Order #" + (orderId != null ? orderId : "N/A"));
            }

            String customerName = order.getCustomerName();
            String tableNumber = order.getTableNumber();

            if (customerName != null && !customerName.isEmpty() && !customerName.equals("Guest")) {
                tvCustomerName.setText(customerName + " • Table " + tableNumber);
            } else {
                tvCustomerName.setText("Table " + tableNumber);
            }

            double amount = order.getTotalAmount();
            tvOrderAmount.setText("₱ " + String.format(Locale.getDefault(), "%.2f", amount));

            long timestamp = order.getTimestamp();
            Date orderDate = new Date(timestamp);
            tvOrderDateTime.setText(dateTimeFormat.format(orderDate));

            int totalItems = order.getTotalItemCount();
            String itemsText = totalItems + " item" + (totalItems != 1 ? "s" : "");
            tvItemsCount.setText(itemsText);
        }
    }

    public void updateOrders(List<Order> newOrders) {
        this.orders = newOrders;
        notifyDataSetChanged();
    }
}