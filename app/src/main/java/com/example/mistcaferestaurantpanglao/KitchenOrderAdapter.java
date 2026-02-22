package com.example.mistcaferestaurantpanglao;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class KitchenOrderAdapter extends RecyclerView.Adapter<KitchenOrderAdapter.KitchenOrderViewHolder> {

    private Context context;
    private List<KitchenOrder> orders;
    private OnOrderActionListener listener;

    public interface OnOrderActionListener {
        void onStartCooking(KitchenOrder order);
        void onMarkReady(KitchenOrder order);
        void onMarkServed(KitchenOrder order);
        void onViewDetails(KitchenOrder order);
    }

    public KitchenOrderAdapter(Context context, List<KitchenOrder> orders, OnOrderActionListener listener) {
        this.context = context;
        this.orders = orders;
        this.listener = listener;
    }

    @NonNull
    @Override
    public KitchenOrderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_kitchen_order, parent, false);
        return new KitchenOrderViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull KitchenOrderViewHolder holder, int position) {
        KitchenOrder order = orders.get(position);
        holder.bind(order);
    }

    @Override
    public int getItemCount() {
        return orders.size();
    }

    public void updateOrders(List<KitchenOrder> newOrders) {
        this.orders = newOrders;
        notifyDataSetChanged();
    }

    class KitchenOrderViewHolder extends RecyclerView.ViewHolder {
        private TextView tvOrderId;
        private TextView tvCustomerName;
        private TextView tvTableNumber;
        private TextView tvItems;
        private TextView tvOrderTime;
        private TextView tvPrepTime;
        private TextView tvSpecialInstructions;
        private TextView tvUrgentLabel;
        private TextView tvOrderAge;
        private TextView tvTotalAmount;
        private Button btnAction;
        private View urgentIndicator;

        public KitchenOrderViewHolder(@NonNull View itemView) {
            super(itemView);
            tvOrderId = itemView.findViewById(R.id.tv_order_id);
            tvCustomerName = itemView.findViewById(R.id.tv_customer_name);
            tvTableNumber = itemView.findViewById(R.id.tv_table_number);
            tvItems = itemView.findViewById(R.id.tv_items);
            tvOrderTime = itemView.findViewById(R.id.tv_order_time);
            tvPrepTime = itemView.findViewById(R.id.tv_prep_time);
            tvSpecialInstructions = itemView.findViewById(R.id.tv_special_instructions);
            tvUrgentLabel = itemView.findViewById(R.id.tv_urgent_label);
            tvTotalAmount = itemView.findViewById(R.id.tv_total_amount);
            btnAction = itemView.findViewById(R.id.btn_action);
            urgentIndicator = itemView.findViewById(R.id.urgent_indicator);
        }

        public void bind(KitchenOrder order) {
            // Basic information
            tvOrderId.setText("#" + order.getOrderId());
            tvCustomerName.setText(order.getCustomerName());
            tvTableNumber.setText("Table " + order.getTableNumber());
            tvOrderTime.setText(order.getOrderTime());
            tvPrepTime.setText(order.getPreparationTime() + " min");

            // Display items - use detailed view if available
            if (order.getOrderItems() != null && !order.getOrderItems().isEmpty()) {
                tvItems.setText(order.getDetailedItems());
            } else {
                tvItems.setText(order.getItems());
            }

            // Display total amount
            if (tvTotalAmount != null) {
                tvTotalAmount.setText("₱" + String.format("%.2f", order.getTotalAmount()));
            }

            // Display order age
            if (tvOrderAge != null) {
                String orderAge = order.getOrderAge();
                if (!orderAge.isEmpty()) {
                    tvOrderAge.setText(orderAge);
                    tvOrderAge.setVisibility(View.VISIBLE);
                } else {
                    tvOrderAge.setVisibility(View.GONE);
                }
            }

            // Handle special instructions
            if (order.getSpecialInstructions() != null && !order.getSpecialInstructions().isEmpty()) {
                tvSpecialInstructions.setText("Note: " + order.getSpecialInstructions());
                tvSpecialInstructions.setVisibility(View.VISIBLE);
            } else {
                tvSpecialInstructions.setVisibility(View.GONE);
            }

            // Handle urgent orders with enhanced styling
            if (order.isUrgent()) {
                tvUrgentLabel.setVisibility(View.VISIBLE);
                urgentIndicator.setVisibility(View.VISIBLE);

                // Different urgency colors based on status
                int urgentColor;
                switch (order.getStatus()) {
                    case "in-progress":
                        urgentColor = Color.parseColor("#e74c3c"); // Red for urgent in-progress
                        break;
                    case "cooking":
                        urgentColor = Color.parseColor("#f39c12"); // Orange for urgent cooking
                        break;
                    case "ready":
                        urgentColor = Color.parseColor("#e67e22"); // Dark orange for urgent ready
                        break;
                    default:
                        urgentColor = Color.parseColor("#e74c3c");
                }

                urgentIndicator.setBackgroundColor(urgentColor);

                // Make the entire card slightly tinted for urgent orders
                itemView.setBackgroundColor(Color.parseColor("#fff5f5"));
            } else {
                tvUrgentLabel.setVisibility(View.GONE);
                urgentIndicator.setVisibility(View.GONE);
                itemView.setBackgroundColor(Color.WHITE);
            }

            // Set button text and action based on order status
            setupActionButton(order);

            // Set click listener for viewing details
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onViewDetails(order);
                }
            });
        }

        private void setupActionButton(KitchenOrder order) {
            switch (order.getStatus()) {
                case "in-progress":
                    btnAction.setText("Start Cooking");
                    btnAction.setBackgroundColor(Color.parseColor("#9b59b6"));
                    btnAction.setTextColor(Color.WHITE);
                    btnAction.setOnClickListener(v -> {
                        if (listener != null) {
                            listener.onStartCooking(order);
                        }
                    });
                    break;
                case "cooking":
                    btnAction.setText("Mark Ready");
                    btnAction.setBackgroundColor(Color.parseColor("#f39c12"));
                    btnAction.setTextColor(Color.WHITE);
                    btnAction.setOnClickListener(v -> {
                        if (listener != null) {
                            listener.onMarkReady(order);
                        }
                    });
                    break;
                case "ready":
                    btnAction.setText("Serve");
                    btnAction.setBackgroundColor(Color.parseColor("#27ae60"));
                    btnAction.setTextColor(Color.WHITE);
                    btnAction.setOnClickListener(v -> {
                        if (listener != null) {
                            listener.onMarkServed(order);
                        }
                    });
                    break;
                default:
                    btnAction.setText("View Details");
                    btnAction.setBackgroundColor(Color.parseColor("#34495e"));
                    btnAction.setTextColor(Color.WHITE);
                    btnAction.setOnClickListener(v -> {
                        if (listener != null) {
                            listener.onViewDetails(order);
                        }
                    });
                    break;
            }
        }
    }
}