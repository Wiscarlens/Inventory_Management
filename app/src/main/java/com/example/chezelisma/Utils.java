package com.example.chezelisma;

import static java.security.AccessController.getContext;

import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.View;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Utility class containing methods that are use in different classes.
 */
public class Utils {
    /**
     * Populates an ArrayList with item data from the database and updates the UI accordingly.
     *
     * @param myDB The database helper instance for querying item data.
     * @param items_for_display The ArrayList to store Items objects for display.
     * @param itemGridview The GridView UI element to display items.
     * @param noDataImage The ImageView UI element to show when no data is available.
     * @param noDataText The TextView UI element to show when no data is available.
     * @param resources The Resources instance to access app resources.
     */
    protected static void storeItemsDataInArrays(MyDatabaseHelper myDB, ArrayList<Items> items_for_display,
                                                 GridView itemGridview, ImageView noDataImage,
                                                 TextView noDataText, Resources resources) {
        // Get a cursor to the item data in the database
        Cursor cursor = myDB.readAllItemsData();

        // Check if the database is empty
        if (cursor.getCount() == 0){
            // If the database is empty, hide the item grid view and show the no data message
            itemGridview.setVisibility(View.GONE);
            noDataImage.setVisibility(View.VISIBLE);
            noDataText.setVisibility(View.VISIBLE);

        } else{
            // If the database is not empty, populate the arrays with the item data
            while (cursor.moveToNext()){
                // Retrieve item image as byte array
                byte[] imageData = cursor.getBlob(1);

                // Convert the image byte array to a Bitmap
                Bitmap itemImageBitmap = BitmapFactory.decodeByteArray(imageData, 0, imageData.length);

                // Convert the Bitmap to a Drawable if needed
                Drawable itemImageDrawable = new BitmapDrawable(resources, itemImageBitmap);

                Items item = new Items(
                        cursor.getLong(0),    // id
                        cursor.getString(2),    // name
                        itemImageDrawable,                 // imageData
                        cursor.getDouble(3),    // price
                        cursor.getString(6),    // unitType
                        // TODO: Default Background Color
                        R.color.white                      // backgroundColor
                );

                items_for_display.add(item); // Add the item to the ArrayList

                // TODO: to be remove
                //itemId_and_name.put(cursor.getInt(0), cursor.getString(2));

            }

            // Show the item grid view and hide the no data message
            itemGridview.setVisibility(View.VISIBLE);
            noDataImage.setVisibility(View.GONE);
            noDataText.setVisibility(View.GONE);
        }
    }

    protected static void storeOrdersDataInArrays(MyDatabaseHelper myDB, ArrayList<Orders> ordersArrayList,
                                                  RecyclerView recyclerView, ImageView noDataImage, TextView noDataText,
                                                  Resources resources) {
        // Get a cursor to the order data in the database
        Cursor cursor = myDB.readAllOrdersData();

        // Check if the database is empty
        if (cursor.getCount() == 0){
            // If the database is empty, hide the item grid view and show the no data message
            recyclerView.setVisibility(View.GONE);
            noDataImage.setVisibility(View.VISIBLE);
            noDataText.setVisibility(View.VISIBLE);

        } else {
            // If the database is not empty, populate the ArrayList with order data
            while (cursor.moveToNext()) {
                long orderNumber = cursor.getLong(0);
                String orderDate = cursor.getString(2);
                String orderTime = cursor.getString(3);
                String orderStatus = cursor.getString(6);
                double totalAmount = cursor.getDouble(4);

                ArrayList<Items> selectedItemsArrayList = myDB.getOrderItems(orderNumber, resources);

                int totalItems = 1 ;

                for (int i= 0; i < selectedItemsArrayList.size(); i++) {
                    totalItems += selectedItemsArrayList.get(i).getFrequency();
                }

                Orders order = new Orders(
                        orderNumber,
                        orderDate,
                        orderTime,
                        orderStatus,
                        totalItems,
                        totalAmount,
                        selectedItemsArrayList
                );

                ordersArrayList.add(order); // Add the order to the ArrayList
            }
            // Show the item grid view and hide the no data message
            recyclerView.setVisibility(View.VISIBLE);
            noDataImage.setVisibility(View.GONE);
            noDataText.setVisibility(View.GONE);
        }
    }


    /**
     * Processes a list of selected items to remove duplicates and increment the frequency of each unique item.
     * This method takes a list of selected items and processes it to remove duplicates based on the item name.
     * It increments the frequency of each unique item in the list and returns a new ArrayList containing the processed items.
     *
     * @param selectedItems The list of selected items to be processed.
     * @return A new ArrayList containing the processed items with duplicates removed and frequencies incremented.
     */
    public static ArrayList<Items> processItems(ArrayList<Items> selectedItems) {
        // Create a map to store items by name and their frequencies
        Map<String, Items> itemMap = new HashMap<>();

        // Process each selected item
        for (Items item : selectedItems) {
            String itemName = item.getName();

            // Check if the item is already in the map
            if (itemMap.containsKey(itemName)) {
                Items existingItem = itemMap.get(itemName);
                assert existingItem != null;
                existingItem.incrementFrequency();
            } else {
                itemMap.put(itemName, item); // If item is not in the map, add it
            }
        }

        // Create a new ArrayList from the values of the map (processed items)
        return new ArrayList<>(itemMap.values());
    }

    /**
     * Converts a byte array to a Drawable.
     *
     * @param imageData The image data in byte array format.
     * @param resources The resources object.
     * @return The Drawable object.
     */
    public static Drawable byteArrayToDrawable(byte[] imageData, Resources resources) {
        // Convert the image byte array to a Bitmap
        Bitmap itemImageBitmap = BitmapFactory.decodeByteArray(imageData, 0, imageData.length);

        // Convert the Bitmap to a Drawable if needed
        Drawable itemImageDrawable = new BitmapDrawable(resources, itemImageBitmap);

        return itemImageDrawable;
    }


}