package com.example.mistcaferestaurantpanglao;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
import java.util.Locale;

public class OrderItemAdapter extends RecyclerView.Adapter<OrderItemAdapter.OrderItemViewHolder> {
    private final List<OrderItem> orderItems;

    public OrderItemAdapter(List<OrderItem> orderItems) {
        this.orderItems = orderItems;
    }

    @NonNull
    @Override
    public OrderItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_order_item, parent, false);
        return new OrderItemViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull OrderItemViewHolder holder, int position) {
        OrderItem item = orderItems.get(position);

        holder.tvItemName.setText(item.getItemName() != null ? item.getItemName() : "Unknown Item");
        holder.tvQuantity.setText("x" + item.getQuantity());
        holder.tvPrice.setText(String.format(Locale.getDefault(), "₱%.2f", item.getPrice()));
        holder.tvTotalPrice.setText(String.format(Locale.getDefault(), "₱%.2f", item.getTotalPrice()));

        if (item.getNotes() != null && !item.getNotes().isEmpty()) {
            holder.tvNotes.setText("Note: " + item.getNotes());
            holder.tvNotes.setVisibility(View.VISIBLE);
        } else {
            holder.tvNotes.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return orderItems != null ? orderItems.size() : 0;
    }

    static class OrderItemViewHolder extends RecyclerView.ViewHolder {
        TextView tvItemName, tvQuantity, tvPrice, tvTotalPrice, tvNotes;

        public OrderItemViewHolder(@NonNull View itemView) {
            super(itemView);
            tvItemName = itemView.findViewById(R.id.tv_item_name);
            tvQuantity = itemView.findViewById(R.id.tv_quantity);
            tvPrice = itemView.findViewById(R.id.tv_price);
            tvTotalPrice = itemView.findViewById(R.id.tv_total_price);
            tvNotes = itemView.findViewById(R.id.tv_notes);
        }
    }
}