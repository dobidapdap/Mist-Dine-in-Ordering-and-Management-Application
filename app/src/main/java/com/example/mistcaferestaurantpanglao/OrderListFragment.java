package com.example.mistcaferestaurantpanglao;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class OrderListFragment extends Fragment implements OrderAdapter.OnOrderActionListener {
    private static final String ARG_ORDERS = "orders";
    private static final String ARG_DISPLAY_ONLY = "display_only";
    private static final String ARG_PAYMENT_MODE = "payment_mode";
    private static final String TAG = "OrderListFragment";

    private List<Order> orders;
    private OrderAdapter orderAdapter;
    private RecyclerView recyclerView;
    private boolean isDisplayOnly = false;
    private boolean isPaymentMode = false;

    private OrderAdapter.OnOrderActionListener orderActionListener;
    private FirebaseFirestore db;
    private PaymentHandler paymentHandler;

    public static OrderListFragment newInstance(List<Order> orders) {
        OrderListFragment fragment = new OrderListFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_ORDERS, new ArrayList<>(orders));
        fragment.setArguments(args);
        return fragment;
    }

    public static OrderListFragment newInstance(List<Order> orders, boolean displayOnly) {
        OrderListFragment fragment = new OrderListFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_ORDERS, new ArrayList<>(orders));
        args.putBoolean(ARG_DISPLAY_ONLY, displayOnly);
        fragment.setArguments(args);
        return fragment;
    }

    public static OrderListFragment newInstance(List<Order> orders, boolean displayOnly, boolean paymentMode) {
        OrderListFragment fragment = new OrderListFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_ORDERS, new ArrayList<>(orders));
        args.putBoolean(ARG_DISPLAY_ONLY, displayOnly);
        args.putBoolean(ARG_PAYMENT_MODE, paymentMode);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        db = FirebaseFirestore.getInstance();
        paymentHandler = new PaymentHandler();

        if (getArguments() != null) {
            orders = (List<Order>) getArguments().getSerializable(ARG_ORDERS);
            isDisplayOnly = getArguments().getBoolean(ARG_DISPLAY_ONLY, false);
            isPaymentMode = getArguments().getBoolean(ARG_PAYMENT_MODE, false);
        } else {
            orders = new ArrayList<>();
        }

        if (orders == null) {
            orders = new ArrayList<>();
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_order_list, container, false);
        recyclerView = view.findViewById(R.id.recycler_orders);

        setupRecyclerView();

        return view;
    }

    private void setupRecyclerView() {
        orderAdapter = new OrderAdapter(orders);
        orderAdapter.setDisplayOnly(isDisplayOnly);
        orderAdapter.setPaymentMode(isPaymentMode);

        if (!isDisplayOnly || isPaymentMode) {
            if (orderActionListener != null) {
                orderAdapter.setOnOrderActionListener(orderActionListener);
            } else {
                orderAdapter.setOnOrderActionListener(this);
            }
        }

        recyclerView.setAdapter(orderAdapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
    }

    public void setDisplayOnly(boolean displayOnly) {
        this.isDisplayOnly = displayOnly;
        if (orderAdapter != null) {
            orderAdapter.setDisplayOnly(displayOnly);
        }
    }

    public void setPaymentMode(boolean paymentMode) {
        this.isPaymentMode = paymentMode;
        if (orderAdapter != null) {
            orderAdapter.setPaymentMode(paymentMode);
        }
    }

    public void setOrderActionListener(OrderAdapter.OnOrderActionListener listener) {
        if ((!isDisplayOnly || isPaymentMode) && orderAdapter != null) {
            orderAdapter.setOnOrderActionListener(listener);
        }
        if (!isDisplayOnly || isPaymentMode) {
            this.orderActionListener = listener;
        }
    }

    public void updateOrders(List<Order> newOrders) {
        if (orders != null && orderAdapter != null) {
            orders.clear();
            orders.addAll(newOrders);
            orderAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onMoveToKitchen(Order order) {
        if (isDisplayOnly && !isPaymentMode) return;

        if (orderActionListener != null) {
            orderActionListener.onMoveToKitchen(order);
        } else {
            updateOrderStatusInFirebase(order, "preparing");
            Toast.makeText(getContext(), "Order sent to kitchen", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onMarkReady(Order order) {
        if (isDisplayOnly && !isPaymentMode) return;

        if (orderActionListener != null) {
            orderActionListener.onMarkReady(order);
        } else {
            updateOrderStatusInFirebase(order, "ready");
            Toast.makeText(getContext(), "Order marked as ready", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onCompleteOrder(Order order) {
        if (isDisplayOnly && !isPaymentMode) return;

        if (orderActionListener != null) {
            orderActionListener.onCompleteOrder(order);
        } else {
            updateOrderStatusInFirebase(order, "completed");
            Toast.makeText(getContext(), "Order completed", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onProcessPayment(Order order) {
        if (orderActionListener != null) {
            orderActionListener.onProcessPayment(order);
        } else {
            processPaymentAndArchive(order);
        }
    }

    @Override
    public void onShowReceipt(Order order) {
        if (orderActionListener != null) {
            orderActionListener.onShowReceipt(order);
        } else {
            Toast.makeText(getContext(), "Showing receipt for Order #" + order.getOrderId().substring(5), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onDiscardOrder(Order order) {
        if (isDisplayOnly && !isPaymentMode) return;

        if (orderActionListener != null) {
            orderActionListener.onDiscardOrder(order);
        } else {
            showDiscardConfirmationDialog(order);
        }
    }

    @Override
    public void onServeToCustomer(Order order) {
        if (isDisplayOnly && !isPaymentMode) return;

        if (orderActionListener != null) {
            orderActionListener.onServeToCustomer(order);
        } else {
            updateOrderStatusInFirebase(order, "served");
            Toast.makeText(getContext(), "Order served to customer", Toast.LENGTH_SHORT).show();
        }
    }

    private void processPaymentAndArchive(Order order) {
        Toast.makeText(getContext(), "Processing payment...", Toast.LENGTH_SHORT).show();

        paymentHandler.processPayment(order.getOrderId(), new PaymentHandler.PaymentCallback() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "Payment processed and order archived: " + order.getOrderId());
                Toast.makeText(getContext(), "Payment completed! Order archived.", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(String error) {
                Log.e(TAG, "Payment processing failed: " + error);
                Toast.makeText(getContext(), "Payment failed: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showDiscardConfirmationDialog(Order order) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Discard Order")
                .setMessage("Are you sure you want to discard Order #" + order.getOrderId() + "?\n\n" +
                        "This action cannot be undone. The order will be permanently deleted from the system.")
                .setPositiveButton("Discard", (dialog, which) -> {
                    discardOrder(order);
                })
                .setNegativeButton("Cancel", null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    private void discardOrder(Order order) {
        Toast.makeText(getContext(), "Discarding order...", Toast.LENGTH_SHORT).show();

        db.collection("orders")
                .document(order.getOrderId())
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Order " + order.getOrderId() + " successfully deleted");
                    Toast.makeText(getContext(), "Order #" + order.getOrderId() + " has been discarded", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error deleting order " + order.getOrderId(), e);
                    Toast.makeText(getContext(), "Failed to discard order. Please try again.", Toast.LENGTH_SHORT).show();
                });
    }
    private void updateOrderStatusInFirebase(Order order, String newStatus) {
        if (isDisplayOnly && !isPaymentMode) return;

        db.collection("orders")
                .document(order.getOrderId())
                .update("status", newStatus)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Order " + order.getOrderId() + " status updated to " + newStatus);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error updating order status", e);
                    Toast.makeText(getContext(), "Failed to update order status", Toast.LENGTH_SHORT).show();
                });
    }
}