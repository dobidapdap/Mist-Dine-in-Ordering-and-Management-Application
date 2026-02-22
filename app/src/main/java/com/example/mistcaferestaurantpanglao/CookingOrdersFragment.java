package com.example.mistcaferestaurantpanglao;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class CookingOrdersFragment extends Fragment implements KitchenOrderAdapter.OnOrderActionListener {

    private RecyclerView recyclerView;
    private KitchenOrderAdapter adapter;
    private List<KitchenOrder> cookingOrders;

    public static CookingOrdersFragment newInstance() {
        return new CookingOrdersFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_list_order, container, false);

        recyclerView = view.findViewById(R.id.recycler_view_orders);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        cookingOrders = new ArrayList<>();
        setupAdapter();

        // Load initial data
        loadCookingOrders();

        return view;
    }

    private void setupAdapter() {
        adapter = new KitchenOrderAdapter(getContext(), cookingOrders, this);
        recyclerView.setAdapter(adapter);
    }

    private void loadCookingOrders() {
        if (getActivity() instanceof KitchenActivity) {
            KitchenActivity activity = (KitchenActivity) getActivity();
            updateOrders(activity.getOrdersByStatus("cooking"));
        }
    }

    public void updateOrders(List<KitchenOrder> newOrders) {
        cookingOrders.clear();
        cookingOrders.addAll(newOrders);
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onStartCooking(KitchenOrder order) {
        // Not used in cooking fragment
    }

    @Override
    public void onMarkReady(KitchenOrder order) {
        Toast.makeText(getContext(), "Order #" + order.getOrderId() + " is ready to serve!", Toast.LENGTH_SHORT).show();

        if (getActivity() instanceof KitchenActivity) {
            ((KitchenActivity) getActivity()).updateOrderStatus(order.getOrderId(), "ready");
        }
    }

    @Override
    public void onMarkServed(KitchenOrder order) {
        // Not used in cooking fragment
    }

    @Override
    public void onViewDetails(KitchenOrder order) {
        Toast.makeText(getContext(), "Viewing details for order #" + order.getOrderId(), Toast.LENGTH_SHORT).show();
        // TODO: Open order details dialog or activity
    }
}