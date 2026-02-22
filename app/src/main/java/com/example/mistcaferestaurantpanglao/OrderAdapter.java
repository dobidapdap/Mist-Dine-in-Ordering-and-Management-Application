package com.example.mistcaferestaurantpanglao;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class OrderAdapter extends RecyclerView.Adapter<OrderAdapter.OrderViewHolder> {
    private List<Order> orders;
    private OnOrderActionListener listener;
    private SimpleDateFormat timeFormat;
    private boolean isDisplayOnly = false;
    private boolean isPaymentMode = false;

    public interface OnOrderActionListener {
        void onMoveToKitchen(Order order);
        void onMarkReady(Order order);
        void onCompleteOrder(Order order);
        void onProcessPayment(Order order);
        void onShowReceipt(Order order);
        void onDiscardOrder(Order order);
        void onServeToCustomer(Order order);
    }

    public OrderAdapter(List<Order> orders) {
        this.orders = orders;
        this.timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
    }

    public void setOnOrderActionListener(OnOrderActionListener listener) {
        this.listener = listener;
    }

    public void setDisplayOnly(boolean displayOnly) {
        this.isDisplayOnly = displayOnly;
        notifyDataSetChanged();
    }

    public void setPaymentMode(boolean paymentMode) {
        this.isPaymentMode = paymentMode;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public OrderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_order, parent, false);
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
        private TextView tvTableNumber;
        private TextView tvCustomerName;
        private TextView tvOrderTime;
        private TextView tvOrderItems;
        private TextView tvTotalAmount;
        private TextView tvItemCount;
        private Button btnAction;
        private Button btnDiscardOrder;

        public OrderViewHolder(@NonNull View itemView) {
            super(itemView);
            tvOrderId = itemView.findViewById(R.id.tv_order_id);
            tvTableNumber = itemView.findViewById(R.id.tv_table_number);
            tvCustomerName = itemView.findViewById(R.id.tv_customer_name);
            tvOrderTime = itemView.findViewById(R.id.tv_order_time);
            tvOrderItems = itemView.findViewById(R.id.tv_order_items);
            tvTotalAmount = itemView.findViewById(R.id.tv_total_amount);
            tvItemCount = itemView.findViewById(R.id.tv_item_count);
            btnAction = itemView.findViewById(R.id.btn_action);
            btnDiscardOrder = itemView.findViewById(R.id.btn_discard_order);
        }

        public void bind(Order order) {
            tvOrderId.setText("Order #" + order.getOrderId().substring(5));
            tvTableNumber.setText(order.getTableNumber());

            if (order.getCustomerName() != null && !order.getCustomerName().isEmpty()) {
                tvCustomerName.setText(order.getCustomerName());
                tvCustomerName.setVisibility(View.VISIBLE);
            } else {
                tvCustomerName.setVisibility(View.GONE);
            }

            Date orderDate = new Date(order.getTimestamp());
            tvOrderTime.setText(timeFormat.format(orderDate));

            StringBuilder itemsText = new StringBuilder();
            List<OrderItem> items = order.getItems();

            if (items != null && !items.isEmpty()) {
                for (int i = 0; i < items.size(); i++) {
                    OrderItem item = items.get(i);
                    if (i > 0) {
                        itemsText.append("\n");
                    }

                    itemsText.append("• ")
                            .append(item.getQuantity())
                            .append("x ")
                            .append(item.getItemName() != null ? item.getItemName() : "Unknown Item");

                    if (item.getPrice() > 0) {
                        itemsText.append(" (₱")
                                .append(String.format("%.0f", item.getPrice()))
                                .append(" each)");
                    }

                    if (item.getNotes() != null && !item.getNotes().trim().isEmpty()) {
                        itemsText.append("\n  Note: ").append(item.getNotes());
                    }
                }
            } else {
                itemsText.append("No items found");
            }

            tvOrderItems.setText(itemsText.toString());

            tvTotalAmount.setText("₱" + String.format("%.0f", order.getTotalAmount()));

            int totalItems = order.getTotalItemCount();
            tvItemCount.setText(totalItems + " item" + (totalItems != 1 ? "s" : ""));

            setupActionButtons(order);
        }

        private void setupActionButtons(Order order) {
            if (isDisplayOnly && !isPaymentMode) {
                btnAction.setVisibility(View.GONE);
                btnDiscardOrder.setVisibility(View.GONE);
                return;
            }

            if (listener == null) {
                btnAction.setVisibility(View.GONE);
                btnDiscardOrder.setVisibility(View.GONE);
                return;
            }

            String status = order.getStatus();

            if (status.equalsIgnoreCase("pending") || status.equalsIgnoreCase("new")) {
                btnDiscardOrder.setVisibility(View.VISIBLE);
                btnDiscardOrder.setOnClickListener(v -> listener.onDiscardOrder(order));
            } else {
                btnDiscardOrder.setVisibility(View.GONE);
            }

            if (isPaymentMode && status.equalsIgnoreCase("served")) {
                btnAction.setText("Process Payment");
                btnAction.setOnClickListener(v -> {
                    listener.onProcessPayment(order);
                });
                btnAction.setVisibility(View.VISIBLE);
                return;
            }

            switch (status.toLowerCase()) {
                case "pending":
                case "new":
                    btnAction.setText("Send to Kitchen");
                    btnAction.setOnClickListener(v -> listener.onMoveToKitchen(order));
                    btnAction.setVisibility(View.VISIBLE);
                    break;
                case "in-progress":
                case "preparing":
                case "in_kitchen":
                    btnAction.setVisibility(View.GONE);
                    break;
                case "ready":
                    btnAction.setText("Complete Order");
                    btnAction.setOnClickListener(v -> listener.onCompleteOrder(order));
                    btnAction.setVisibility(View.VISIBLE);
                    break;
                case "completed":
                    btnAction.setText("Served");
                    btnAction.setOnClickListener(v -> listener.onServeToCustomer(order));
                    btnAction.setVisibility(View.VISIBLE);
                    break;
                case "served":
                    if (!isPaymentMode) {
                        btnAction.setVisibility(View.GONE);
                    }
                    break;
                default:
                    btnAction.setVisibility(View.GONE);
                    break;
            }
        }
    }

    public void updateOrders(List<Order> newOrders) {
        this.orders = newOrders;
        notifyDataSetChanged();
    }
}