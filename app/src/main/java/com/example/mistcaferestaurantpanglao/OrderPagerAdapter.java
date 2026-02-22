package com.example.mistcaferestaurantpanglao;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import java.util.ArrayList;
import java.util.List;

public class OrderPagerAdapter extends FragmentStateAdapter {
    private static final String TAG = "OrderPagerAdapter";

    private List<Order> newOrders;
    private List<Order> inKitchenOrders;
    private List<Order> servedOrders;

    private OrderListFragment newOrdersFragment;
    private OrderListFragment inKitchenFragment;
    private OrderListFragment servedFragment;

    private OrderAdapter.OnOrderActionListener orderActionListener;

    public OrderPagerAdapter(@NonNull FragmentActivity fragmentActivity,
                             List<Order> newOrders,
                             List<Order> inKitchenOrders,
                             List<Order> servedOrders) {
        super(fragmentActivity);
        this.newOrders = new ArrayList<>(newOrders);
        this.inKitchenOrders = new ArrayList<>(inKitchenOrders);
        this.servedOrders = new ArrayList<>(servedOrders);
    }

    public void setOrderActionListener(OrderAdapter.OnOrderActionListener listener) {
        this.orderActionListener = listener;

        if (newOrdersFragment != null) {
            newOrdersFragment.setOrderActionListener(listener);
        }
        if (inKitchenFragment != null) {
            inKitchenFragment.setOrderActionListener(listener);
        }
        if (servedFragment != null) {
            servedFragment.setOrderActionListener(listener);
        }
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 0:
                newOrdersFragment = OrderListFragment.newInstance(newOrders);
                if (orderActionListener != null) {
                    newOrdersFragment.setOrderActionListener(orderActionListener);
                }
                Log.d(TAG, "Created New Orders fragment with " + newOrders.size() + " orders");
                return newOrdersFragment;

            case 1:
                inKitchenFragment = OrderListFragment.newInstance(inKitchenOrders, false, false);
                if (orderActionListener != null) {
                    inKitchenFragment.setOrderActionListener(orderActionListener);
                }
                Log.d(TAG, "Created Serve to Customer fragment with " + inKitchenOrders.size() + " orders");
                return inKitchenFragment;

            case 2:
                servedFragment = OrderListFragment.newInstance(servedOrders, false, true);
                if (orderActionListener != null) {
                    servedFragment.setOrderActionListener(orderActionListener);
                }
                Log.d(TAG, "Created Served Orders fragment with " + servedOrders.size() + " orders");
                return servedFragment;

            default:
                return OrderListFragment.newInstance(newOrders);
        }
    }

    @Override
    public int getItemCount() {
        return 3;
    }

    public void updateOrders(List<Order> newOrders, List<Order> inKitchenOrders, List<Order> servedOrders) {
        this.newOrders.clear();
        this.newOrders.addAll(newOrders);

        this.inKitchenOrders.clear();
        this.inKitchenOrders.addAll(inKitchenOrders);

        this.servedOrders.clear();
        this.servedOrders.addAll(servedOrders);

        Log.d(TAG, "Updating orders - New: " + newOrders.size() +
                ", Kitchen: " + inKitchenOrders.size() +
                ", Served: " + servedOrders.size());

        if (newOrdersFragment != null) {
            newOrdersFragment.updateOrders(new ArrayList<>(newOrders));
            Log.d(TAG, "✓ Updated New Orders fragment");
        }

        if (inKitchenFragment != null) {
            inKitchenFragment.updateOrders(new ArrayList<>(inKitchenOrders));
            Log.d(TAG, "✓ Updated Serve to Customer fragment");
        }

        if (servedFragment != null) {
            servedFragment.updateOrders(new ArrayList<>(servedOrders));
            Log.d(TAG, "✓ Updated Served Orders fragment with " + servedOrders.size() + " orders");
        } else {
            Log.w(TAG, "⚠ Served fragment not created yet - will use updated data when created");
        }

        notifyDataSetChanged();
    }
}