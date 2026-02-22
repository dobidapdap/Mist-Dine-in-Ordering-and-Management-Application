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

public class ReadyOrdersFragment extends Fragment implements KitchenOrderAdapter.OnOrderActionListener {

    private RecyclerView recyclerView;
    private KitchenOrderAdapter adapter;
    private List<KitchenOrder> readyOrders;

    public static ReadyOrdersFragment newInstance() {
        return new ReadyOrdersFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_list_order, container, false);

        recyclerView = view.findViewById(R.id.recycler_view_orders);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        readyOrders = new ArrayList<>();
        setupAdapter();

        // Load initial data
        loadReadyOrders();

        return view;
    }

    private void setupAdapter() {
        adapter = new KitchenOrderAdapter(getContext(), readyOrders, this);
        recyclerView.setAdapter(adapter);
    }

    private void loadReadyOrders() {
        if (getActivity() instanceof KitchenActivity) {
            KitchenActivity activity = (KitchenActivity) getActivity();
            updateOrders(activity.getOrdersByStatus("ready"));
        }
    }

    public void updateOrders(List<KitchenOrder> newOrders) {
        readyOrders.clear();
        readyOrders.addAll(newOrders);
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onStartCooking(KitchenOrder order) {
        // Not used in ready fragment
    }

    @Override
    public void onMarkReady(KitchenOrder order) {
        // Not used in ready fragment
    }

    @Override
    public void onMarkServed(KitchenOrder order) {
        Toast.makeText(getContext(), "Order #" + order.getOrderId() + " has been completed!", Toast.LENGTH_SHORT).show();

        if (getActivity() instanceof KitchenActivity) {
            ((KitchenActivity) getActivity()).updateOrderStatus(order.getOrderId(), "completed");
        }
    }

    @Override
    public void onViewDetails(KitchenOrder order) {
        Toast.makeText(getContext(), "Viewing details for order #" + order.getOrderId(), Toast.LENGTH_SHORT).show();
        // TODO: Open order details dialog or activity
    }
}