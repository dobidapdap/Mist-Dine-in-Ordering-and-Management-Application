package com.example.mistcaferestaurantpanglao;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

public class CartAdapter extends RecyclerView.Adapter<CartAdapter.CartViewHolder> {

    private static final String TAG = "CartAdapter";

    private final Context context;
    private final List<CartItem> cartItems;
    private final OnCartUpdateListener updateListener;
    private FirebaseFirestore db;

    public interface OnCartUpdateListener {
        void onCartUpdated();
    }

    public CartAdapter(Context context, List<CartItem> cartItems, OnCartUpdateListener updateListener) {
        this.context = context;
        this.cartItems = cartItems;
        this.updateListener = updateListener;
        this.db = FirebaseFirestore.getInstance();
    }

    @NonNull
    @Override
    public CartViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_cart, parent, false);
        return new CartViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CartViewHolder holder, int position) {
        CartItem cartItem = cartItems.get(position);

        holder.tvCartDishName.setText(cartItem.getDishName());
        holder.tvCartQuantity.setText(String.valueOf(cartItem.getQuantity()));

        holder.tvCartPrice.setText("₱" + formatPrice(cartItem.getPrice()) + " each");
        holder.tvCartTotalPrice.setText("₱" + formatPrice(cartItem.getTotalPrice()));

        loadDishImage(holder.ivCartDishImage, cartItem.getImageUri());

        holder.btnDecreaseQuantity.setEnabled(cartItem.getQuantity() > 1);

        holder.btnDecreaseQuantity.setOnClickListener(v -> {
            int currentQty = cartItem.getQuantity();
            if (currentQty > 1) {
                int newQty = currentQty - 1;
                updateCartItem(cartItem, newQty, holder, position);
            }
        });

        holder.btnIncreaseQuantity.setOnClickListener(v -> {
            db.collection("menuItems")
                    .whereEqualTo("dishName", cartItem.getDishName())
                    .limit(1)
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        if (!queryDocumentSnapshots.isEmpty()) {
                            DocumentSnapshot document = queryDocumentSnapshots.getDocuments().get(0);
                            Long dishesLeft = document.getLong("dishesLeft");
                            Boolean available = document.getBoolean("available");

                            if (available != null && !available) {
                                Toast.makeText(context, cartItem.getDishName() + " is no longer available",
                                        Toast.LENGTH_SHORT).show();
                                return;
                            }

                            if (dishesLeft != null) {
                                int currentCartQty = cartItem.getQuantity();

                                if (dishesLeft <= 0) {
                                    Toast.makeText(context, cartItem.getDishName() + " is out of stock",
                                            Toast.LENGTH_SHORT).show();
                                    return;
                                }

                                if (currentCartQty >= dishesLeft) {
                                    Toast.makeText(context, "Only " + dishesLeft + " available in stock",
                                            Toast.LENGTH_SHORT).show();
                                    return;
                                }

                                int newQty = currentCartQty + 1;
                                updateCartItem(cartItem, newQty, holder, position);
                            } else {
                                int newQty = cartItem.getQuantity() + 1;
                                updateCartItem(cartItem, newQty, holder, position);
                            }
                        }
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(context, "Error checking stock availability", Toast.LENGTH_SHORT).show();
                    });
        });

        holder.btnRemoveCartItem.setOnClickListener(v -> {
            if (context instanceof ShoppingCartActivity) {
                ((ShoppingCartActivity) context).removeCartItem(position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return cartItems.size();
    }

    private void updateCartItem(CartItem item, int newQuantity, CartViewHolder holder, int position) {
        item.setQuantity(newQuantity);
        item.setTotalPrice(item.getPrice() * newQuantity);

        holder.tvCartQuantity.setText(String.valueOf(newQuantity));
        holder.tvCartTotalPrice.setText("₱" + formatPrice(item.getTotalPrice()));

        holder.btnDecreaseQuantity.setEnabled(newQuantity > 1);

        if (context instanceof ShoppingCartActivity) {
            ((ShoppingCartActivity) context).updateCartItemQuantity(position, newQuantity);
        }

        if (updateListener != null) updateListener.onCartUpdated();
    }

    private void loadDishImage(ImageView imageView, String uri) {
        if (uri != null && !uri.trim().isEmpty()) {
            Glide.with(context)
                    .load(uri)
                    .centerCrop()
                    .placeholder(R.drawable.ic_launcher_foreground)
                    .error(R.drawable.ic_launcher_foreground)
                    .into(imageView);
        } else {
            imageView.setImageResource(R.drawable.ic_launcher_foreground);
        }
    }

    private String formatPrice(double price) {
        return String.format("%.2f", price);
    }

    static class CartViewHolder extends RecyclerView.ViewHolder {

        TextView tvCartDishName, tvCartPrice, tvCartTotalPrice, tvCartQuantity;
        ImageView ivCartDishImage;
        ImageButton btnDecreaseQuantity, btnIncreaseQuantity, btnRemoveCartItem;

        public CartViewHolder(@NonNull View itemView) {
            super(itemView);

            tvCartDishName = itemView.findViewById(R.id.tvCartDishName);
            tvCartPrice = itemView.findViewById(R.id.tvCartPrice);
            tvCartTotalPrice = itemView.findViewById(R.id.tvCartTotalPrice);
            tvCartQuantity = itemView.findViewById(R.id.tvCartQuantity);
            ivCartDishImage = itemView.findViewById(R.id.ivCartDishImage);

            btnDecreaseQuantity = itemView.findViewById(R.id.btnDecreaseQuantity);
            btnIncreaseQuantity = itemView.findViewById(R.id.btnIncreaseQuantity);
            btnRemoveCartItem = itemView.findViewById(R.id.btnRemoveCartItem);
        }
    }
}