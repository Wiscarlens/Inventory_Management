package com.module.dot.Activities.Orders;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.module.dot.Database.MyDatabaseHelper;
import com.module.dot.Activities.Orders.Orders;
import com.module.dot.Activities.Orders.OrdersAdapter;
import com.module.dot.R;

import java.util.ArrayList;

public class OrdersFragment extends Fragment {
    private final ArrayList<Orders> orders_for_display = new ArrayList<>();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_orders, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        LinearLayout noOrder = view.findViewById(R.id.noOrderFragmentLL); // When Database is empty
        RecyclerView OrderList_RecyclerView = view.findViewById(R.id.ordersList);  // Connect to Recyclerview in fragment_orders

        MyDatabaseHelper myDB = new MyDatabaseHelper(getContext()); // Local database

        MyDatabaseHelper.getOrders(
                myDB, // Local database
                orders_for_display, // ArrayList to store Orders objects for display
                OrderList_RecyclerView, // RecyclerView UI element to display orders
                noOrder,
                getResources() // Resources instance to access app resources
        );

        OrdersAdapter ordersAdapter = new OrdersAdapter(orders_for_display, getContext());
        OrderList_RecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        OrderList_RecyclerView.setAdapter(ordersAdapter);
    }
}