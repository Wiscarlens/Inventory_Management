package com.example.chezelisma;

/*
 Created by Wiscarlens Lucius on 1 February 2023.
 */

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

public class HomeFragment extends Fragment {
    private FragmentActivity fragmentActivity;
    private Button chargeButton;

    private ArrayList<Items> items_for_display = new ArrayList<>();
    private ArrayList<Drawable> image = new ArrayList<>();

    private Map<Integer, String> itemId_and_name = new HashMap<>();

    private ArrayList<String> selectProductName =  new ArrayList<>();
    private ArrayList<Double> selectProductPrice = new ArrayList<>();

    // Select item total
    private AtomicReference<Double> totalPrice = new AtomicReference<>(0.0);
    private String currentCharge;

    private MyDatabaseHelper myDB;

    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        fragmentActivity = (FragmentActivity) context;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ImageView noDataImage = view.findViewById(R.id.no_data_imageview); // When Database is empty
        TextView noDataText = view.findViewById(R.id.no_data_textview); // When Database is empty
        GridView itemGridview = view.findViewById(R.id.itemList);
        FloatingActionButton scanButton = view.findViewById(R.id.scanButton);
        chargeButton = view.findViewById(R.id.Charge);

        // Local database
        myDB = new MyDatabaseHelper(getContext());

        // Save item data from database to the arraylist
        storeItemsDataInArrays(myDB, items_for_display, itemGridview, noDataImage, noDataText, getResources());

        ItemGridAdapter itemGridAdapter = new ItemGridAdapter(items_for_display);
        itemGridview.setAdapter(itemGridAdapter);


        // When user select an item
        itemGridview.setOnItemClickListener((parent, view1, position, id) -> {
            Double itemSelected = items_for_display.get(position).getPrice();

            // Add selected item price together
            totalPrice.set(totalPrice.get() + itemSelected);

            // Format the double value into currency format
            currentCharge = CurrencyFormat.getCurrencyFormat(totalPrice.get());

            // Set the button text to the current value of price
            chargeButton.setText(currentCharge);

            selectProductName.add(items_for_display.get(position).getName());
            selectProductPrice.add(items_for_display.get(position).getPrice());

        });

        // When user click on charge button
        chargeButton.setOnClickListener(v -> {
            // Open bottom sheet layout
            Dialog dialog = showButtonDialog();
            final double bottomSheetHeight = 0.56; // Initialize to 56% of the screen height
            setBottomSheetHeight(dialog, bottomSheetHeight);
        });

        //When user click on scanner button
        scanButton.setOnClickListener(v -> scanCode());
    }

    public Dialog showButtonDialog(){
        final Dialog bottomSheetDialog = new Dialog(getContext());

        bottomSheetDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        bottomSheetDialog.setContentView(R.layout.bottomsheet_layout);

        TextView transactionTotal = bottomSheetDialog.findViewById(R.id.transactionTotal);
        Button checkoutButton = bottomSheetDialog.findViewById(R.id.checkoutButton);
        // Variable to test bottom sheet
        RecyclerView bottomSheetRecyclerView = bottomSheetDialog.findViewById(R.id.transactionSheetList); // Find the RecyclerView in the layout

        transactionTotal.setText(currentCharge);

        // Bottom sheet recycle view
        bottomSheetRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // Create the adapter and set it to the RecyclerView
        BottomSheetAdapter bottomSheetAdapter = new BottomSheetAdapter(selectProductName, selectProductPrice, getContext());
        bottomSheetRecyclerView.setAdapter(bottomSheetAdapter);

        checkoutButton.setOnClickListener(v -> {
            bottomSheetDialog.dismiss();

            // Check if recycle view is empty before check out
            if (!selectProductName.isEmpty()) {
                // Sending data to next fragment
                Bundle result = new Bundle();
                result.putString("price", currentCharge);
                getParentFragmentManager().setFragmentResult("priceData", result);

                // Confirmation message to check out
                AlertDialog.Builder checkoutConfirmation = new AlertDialog.Builder(getContext());

                checkoutConfirmation.setTitle(getResources().getString(R.string.confirm))
                        .setMessage(getResources().getString(R.string.confirm_checkout))
                        .setNegativeButton(getResources().getString(R.string.no), (dialog, which) -> {
                             // Complete with the value 'false'
                        }).setPositiveButton(getResources().getString(R.string.yes), (dialog, which) -> {
                           // If user click on yes
                            int creatorId = 1;
                            double totalAmount = totalPrice.get();
                            String paymentMethod = "Visa";
                            String paymentStatus = "Completed";
                            // TODO: update to long datatype
                            int newOrderID = (int) myDB.addOrder(creatorId, totalAmount, paymentMethod, paymentStatus);

                            // Find selectProductName ID in the database
                            // Remove duplicates from the itemNamesToFind ArrayList
//                            ArrayList<Items> uniqueItemNamesToFind = new ArrayList<>(new LinkedHashSet<>(items_for_display));

                            Integer[] itemID = getItemIdsFromNames(selectProductName);
                            Integer[] itemFrequency = getFrequencies(selectProductName);

                            for(int i = 0; i < itemID.length; i++){
                                myDB.addOrderItem(newOrderID, itemID[i], itemFrequency[i]);
                            }

                            // TODO: Find how to read data from the database using terminal

                            // Open Confirmation fragment
                            FragmentManager fragmentManager = fragmentActivity.getSupportFragmentManager();
                            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
                            ConfirmationFragment confirmationFragment = new ConfirmationFragment();
                            fragmentTransaction.replace(R.id.fragment_container, confirmationFragment);
                            fragmentTransaction.commit();

                        }).show();

            } else {
                String message = getResources().getString(R.string.empty_cart);
                Toast.makeText(fragmentActivity, message, Toast.LENGTH_SHORT).show();
            }

        });

        bottomSheetDialog.show();

        bottomSheetDialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);
        bottomSheetDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        bottomSheetDialog.getWindow().getAttributes().windowAnimations = R.style.DialogAnimation;
        bottomSheetDialog.getWindow().setGravity(Gravity.BOTTOM);

        return bottomSheetDialog;
    }

    /**
     * Sets the height of a dialog displayed as a bottom sheet to a specified percentage of the screen height.
     *
     * @param dialog The dialog for which to set the height.
     * @param heightPercentage The desired height of the dialog as a percentage of the screen height.
     */
    private void setBottomSheetHeight(Dialog dialog, double heightPercentage){
        // Set the height of the dialog
        Window window = dialog.getWindow();

        if (window != null) {
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, (int) (heightPercentage * getScreenHeight()));
            window.setGravity(Gravity.BOTTOM);
        }
    }

    /**
     * Retrieves the height of the device screen in pixels.
     *
     * @return The height of the screen in pixels.
     */
    private int getScreenHeight() {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        return displayMetrics.heightPixels;
    }

    /**
     * Initiates a barcode scanning process using the specified scan options.
     */
    private void scanCode(){
        ScanOptions options = new ScanOptions();

        options.setPrompt("Press Volume Up to Turn Flash On");
        options.setBeepEnabled(true);
        options.setOrientationLocked(true);
        options.setCaptureActivity(CaptureAct.class);

        scannerLauncher.launch(options);
    }

    /**
     * Result launcher for initiating barcode scanning and handling the scanning result.
     */
    ActivityResultLauncher<ScanOptions>  scannerLauncher = registerForActivityResult(new ScanContract(), result -> {
        /**
         * Handles the result of a barcode scanning operation.
         *
         * @param result The scanning result containing the scanned contents.
         */
        String res = result.getContents();

        // Check if the scanned code corresponds to a specific item
        if(res.equals("096619756803")){
            Toast.makeText(fragmentActivity, "Water added", Toast.LENGTH_SHORT).show();
            // TODO: Additional logic for processing the scanned item, e.g., updating prices
            // TODO: price.set(price.get() + 1.99);
        } else{
            Toast.makeText(fragmentActivity, "Item not found", Toast.LENGTH_SHORT).show();
        }
    });

    /**
     * Stores the item data from the database into arrays.
     *
     * @param myDB The database object.
     * @param itemGridview item grid view
     * @param noDataImage  Data message image
     * @param noDataText Data message text
     */
    private void storeItemsDataInArrays(MyDatabaseHelper myDB, ArrayList<Items> items_for_display,
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
//                itemId_and_name.put(items_for_display.get().getId(), items_for_display.get().getName());
                itemId_and_name.put(cursor.getInt(0), cursor.getString(2));

            }

            // Show the item grid view and hide the no data message
            itemGridview.setVisibility(View.VISIBLE);
            noDataImage.setVisibility(View.GONE);
            noDataText.setVisibility(View.GONE);
        }
    }

    /**
     * Retrieves the item IDs associated with given unique item names from the itemId_and_name map.
     *
     * @param itemNamesToFind The list of unique item names for which the corresponding item IDs are to be retrieved.
     * @return An array of item IDs corresponding to the provided unique item names. If an item name is not found, the corresponding array element will be null.
     */
    private Integer[] getItemIdsFromNames(ArrayList<String> itemNamesToFind) {
        // Remove duplicates from the itemNamesToFind ArrayList
        ArrayList<String> uniqueItemNamesToFind = new ArrayList<>(
                new LinkedHashSet<>(itemNamesToFind)); // Do not copy a value twice

        Integer[] itemIds = new Integer[uniqueItemNamesToFind.size()];

        for (int i = 0; i < uniqueItemNamesToFind.size(); i++) {
            String itemNameToFind = uniqueItemNamesToFind.get(i);

            for (Map.Entry<Integer, String> entry : itemId_and_name.entrySet()) {
                if (entry.getValue().equals(itemNameToFind)) {
                    itemIds[i] = entry.getKey();
                    break; // Move to the next item name
                }
            }

            // Log if an item name is not found in the map
            if (itemIds[i] == null) {
                Log.d("error", "HomeFragment: Item name '" + itemNameToFind + "' is not found in the map");
            }
        }

        return itemIds;
    }

    /**
     * Calculates the frequency of each name in the input ArrayList and returns an array of frequencies.
     *
     * @param selectProductName The ArrayList of strings containing the names to calculate frequencies for.
     * @return An array of integers representing the frequency of each name in the input ArrayList.
     */
    public static Integer[] getFrequencies(ArrayList<String> selectProductName) {
        // Create a HashMap to store the frequency of each name
        Map<String, Integer> nameFrequencyMap = new HashMap<>();

        // Count the frequency of each name
        for (String name : selectProductName) {
            nameFrequencyMap.put(name, nameFrequencyMap.getOrDefault(name, 0) + 1);
        }

        // Create an array to store the frequencies
        Integer[] frequencies = new Integer[selectProductName.size()];

        // Populate the array with frequencies
        for (int i = 0; i < selectProductName.size(); i++) {
            frequencies[i] = nameFrequencyMap.get(selectProductName.get(i));
        }

        return frequencies;
    }

}