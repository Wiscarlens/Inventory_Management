package com.module.dot.data.remote;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageException;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.module.dot.model.Item;
import com.module.dot.view.MainActivity;
import com.module.dot.model.Order;
import com.module.dot.model.Transaction;
import com.module.dot.model.User;
import com.module.dot.data.local.ItemDatabase;
import com.module.dot.data.local.OrderDatabase;
import com.module.dot.data.local.OrderItemsDatabase;
import com.module.dot.data.local.TransactionDatabase;
import com.module.dot.data.local.UserDatabase;
import com.module.dot.utils.FileManager;
import com.module.dot.utils.Utils;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class FirebaseHandler {
    private final FirebaseAuth mAuth = FirebaseAuth.getInstance();
    private static final FirebaseStorage storage = FirebaseStorage.getInstance();

    public static String getCurrentUserOnlineID(FirebaseAuth mAuth) {
        FirebaseUser firebaseUser = mAuth.getCurrentUser();
        assert firebaseUser != null;
        return firebaseUser.getUid();
    }


    public void createUser(User newUser, Drawable profileImage, Context context){
        if (newUser.getEmail() == null || newUser.getEmail().isEmpty() || newUser.getPassword_hash() == null || newUser.getPassword_hash().isEmpty()) {
            Log.e("FirebaseHandler", "Email or password cannot be empty");
            return;
        }

        mAuth.createUserWithEmailAndPassword(newUser.getEmail(), newUser.getPassword_hash())
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        try {

                            String globalID = Objects.requireNonNull(mAuth.getCurrentUser()).getUid();
                            newUser.setGlobalID(globalID);

                            if (MainActivity.currentUser == null){
                                newUser.setCreatorID(globalID); // Create user from log in page
                            } else {
                                newUser.setCreatorID(MainActivity.currentUser.getGlobalID());
                            }

                            if (profileImage != null){
                                newUser.setProfileImagePath(globalID);
                                uploadFile(profileImage, "Profiles/" + globalID);
                            }

                            DatabaseReference currentUserDb = FirebaseDatabase.getInstance().getReference();
                            currentUserDb.child("users").child(globalID).setValue(newUser);

                            Log.i("Firebase", "User Added Successfully!");


                        } catch (Exception e) {
                            Log.e("Firebase", "Error while adding user to online database", e);
                        }

                        // Sign in success, update UI with the signed-in user's information
                        Log.d("Firebase", "createUserWithEmail:success");
                        Toast.makeText(context, "Account created!", Toast.LENGTH_SHORT).show();
                    } else {
                        // If sign in fails, display a message to the user.
                        Log.w("Firebase", "createUserWithEmail:failure", task.getException());
                        // Inside your Fragment class
                        Toast.makeText(context, "Authentication failed.", Toast.LENGTH_SHORT).show();
                    }
                });

    }


    public static void uploadFile(Drawable image, String imagePath) {
        Log.d("FirebaseHandler", "uploadFile: " + imagePath);
        Log.d("FirebaseHandler", "uploadFile: " + image);

        Bitmap bitmap = Utils.drawableToBitmap(image);

        // Compress the Bitmap into a ByteArrayOutputStream with 25% quality
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 25, baos);

        // Convert the ByteArrayOutputStream to a byte array
        byte[] imageData = baos.toByteArray();

        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference storageReference = storage.getReference(imagePath);

        UploadTask uploadTask = storageReference.putBytes(imageData);
        uploadTask.addOnFailureListener(exception -> {
            // Handle unsuccessful uploads
            Log.e("FirebaseHandler", "Error while uploading image to Firebase Storage", exception);
        }).addOnSuccessListener(taskSnapshot -> Log.i("FirebaseHandler", "Image uploaded successfully"));
    }

    public static void readItem(String tableName, Context context){
        DatabaseReference firebaseRef = FirebaseDatabase.getInstance().getReference(tableName);
        firebaseRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                try (ItemDatabase itemDatabase = new ItemDatabase(context)) {
                    // Iterate through Firebase data
                    for (DataSnapshot userSnapshot : dataSnapshot.getChildren()) {

                        Item item = userSnapshot.getValue(Item.class);

                        // Create item in the local database
                        assert item != null;

                        if (Objects.equals(MainActivity.currentUser.getCreatorID(), item.getCreatorID())){
                            itemDatabase.createItem(item);

                            if (item.getImagePath() != null){
                                downloadAndSaveImagesLocally("Items", item.getImagePath(), context);
                            }

                        }

                    }

                } catch (Exception e) {
                    e.printStackTrace();
                    Log.e("FirebaseItemDatabase", "Failed to sync item data from Firebase: " + e.getMessage());
                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("UserDatabase", "Firebase data fetch cancelled: " + error.getMessage());

            }
        });

    }


    // Synchronize user data from Firebase to SQLite
    public static void readUser(String tableName, Context context){
        DatabaseReference firebaseRef = FirebaseDatabase.getInstance().getReference(tableName);

        // Fetch data from Firebase
        firebaseRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                try (UserDatabase userDatabase = new UserDatabase(context)) {
                    // Iterate through Firebase data
                    for (DataSnapshot userSnapshot : dataSnapshot.getChildren()) {
                        User firebaseUser = userSnapshot.getValue(User.class);

                        // Create user in the local database
                        assert firebaseUser != null;

                        // Do not show current user in the list
                        if (Objects.equals(firebaseUser.getGlobalID(), MainActivity.currentUser.getGlobalID())) {
                            Log.i("FirebaseUserDatabase", "Skipping current user: " + firebaseUser.getFullName());
                        } else {
                            if (Objects.equals(MainActivity.currentUser.getCreatorID(), firebaseUser.getCreatorID())){
                                userDatabase.createUser(firebaseUser);

                                if (firebaseUser.getProfileImagePath() != null){
                                    downloadAndSaveImagesLocally("Profiles", firebaseUser.getProfileImagePath(), context);
                                }

                                Log.i("FirebaseUserDatabase", "User with email " + firebaseUser.getEmail() + " added to SQLite.");
                            }
                        }

                    }

                } catch (Exception e) {
                    e.printStackTrace();
                    Log.e("FirebaseUserDatabase", "Failed to sync user data from Firebase: " + e.getMessage());
                }
            }



            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e("UserDatabase", "Firebase data fetch cancelled: " + databaseError.getMessage());
            }


        });
    }

    public static void downloadAndSaveImagesLocally(String folderName, String fileName, Context context) {
        final long FIVE_MEGABYTES = 1024 * 1024 * 5;

        String imagePath = folderName + "/" + fileName;
        StorageReference imageRef = storage.getReference(imagePath);

        try {
            imageRef.getBytes(FIVE_MEGABYTES).addOnSuccessListener(bytes -> {
                Drawable image = Utils.byteArrayToDrawable(bytes, context.getResources());

                FileManager.saveImageLocally(context, image, folderName, fileName);
            }).addOnFailureListener(exception -> {
                if (exception instanceof StorageException) {
                    int errorCode = ((StorageException) exception).getErrorCode();
                    if (errorCode == StorageException.ERROR_OBJECT_NOT_FOUND || errorCode == StorageException.ERROR_RETRY_LIMIT_EXCEEDED) {
                        // If the error is due to object not found or retry limit exceeded, retry after 5 seconds
                        new Handler().postDelayed(() -> downloadAndSaveImagesLocally(folderName, fileName, context), 5000);
                    }
                } else {
                    Log.e("Firebase", "Error downloading item image", exception);
                }
            });

        } catch (IndexOutOfBoundsException e){
            Log.e("Firebase", "The maximum allowed buffer size was exceeded.", e);
        }
    }


//    public static void downloadAndSaveImagesLocally(String folderName, String fileName, Context context) {
//        final long ONE_MEGABYTE = 1024 * 1024;
//
//        String imagePath = folderName + "/" + fileName;
//        StorageReference imageRef = storage.getReference(imagePath);
//
//        try {
//            imageRef.getBytes(ONE_MEGABYTE).addOnSuccessListener(bytes -> {
//                Drawable image = Utils.byteArrayToDrawable(bytes, context.getResources());
//
//                FileManager.saveImageLocally(context, image, folderName, fileName);
//            }).addOnFailureListener(exception -> {
//                Log.e("Firebase", "Error downloading item image", exception);
//            });
//
//        } catch (IndexOutOfBoundsException e){
//            Log.e("Firebase", "The maximum allowed buffer size was exceeded.", e);
//        }
//
//    }

    public static void createItem(Item newItem, Drawable itemImage){
        DatabaseReference itemsRef = FirebaseDatabase.getInstance().getReference("items");

        // Use push to generate a unique key
        DatabaseReference newItemRef = itemsRef.push();

        String globalID = newItemRef.getKey(); // Get get item global ID

        newItem.setGlobalID(globalID);

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            // Set the creatorID of the item to the uid of the currently authenticated user
            newItem.setCreatorID(currentUser.getUid());

            if(itemImage != null){
                newItem.setImagePath(globalID);
                Log.d("FirebaseHandler", "createItem: " + itemImage);
            } else {
                newItem.setImagePath(null);
                Log.d("FirebaseHandler", "createItem: " + "No image");
            }

            // Set the item with the generated key
            newItemRef.setValue(newItem).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    if (itemImage != null){
                        // Save the image to Firebase Storage with the generated key
                        uploadFile(itemImage, "Items/" + globalID);
                    }

                    Log.i("Firebase", "Item Added Successfully!");
                }
            }).addOnFailureListener(e ->
                    Log.d("Firebase", "createUserWithEmail:failure")
            );
        } else {
            // User is not authenticated
            Log.w("Firebase", "User is not authenticated. Cannot create item.");
        }
    }

    public String getUserAuthenticationStatus() {
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser != null) {
            return "Authenticated";
        } else {
            return null; // or return "Not Authenticated";
        }
    }


    public static String createOrder(Order newOrder) {
        DatabaseReference ordersRef = FirebaseDatabase.getInstance().getReference("orders");

        // Use push to generate a unique key
        DatabaseReference newOrderRef = ordersRef.push();

        String globalID = newOrderRef.getKey(); // Get order global ID

        newOrder.setGlobalID(globalID);

        // Create a map to represent the order data
        Map<String, Object> orderData = new HashMap<>();

        orderData.put("orderDate", newOrder.getOrderDate());
        orderData.put("orderTime", newOrder.getOrderTime());
        orderData.put("orderStatus", newOrder.getOrderStatus());
        orderData.put("orderTotalItems", newOrder.getOrderTotalItems());
        orderData.put("orderTotalAmount", newOrder.getOrderTotalAmount());
        orderData.put("creatorID", newOrder.getCreatorID());

        // Handle the list of items
        Map<String, Map<String, Object>> itemList = new HashMap<>();
        if (newOrder.getSelectedItemList() != null) {
            for (Item item : newOrder.getSelectedItemList()) {
                Map<String, Object> itemData = new HashMap<>();

                // Generator a unique key for selected item
                DatabaseReference selectedItem = newOrderRef.child("selectedItem").push();

                itemData.put("itemGlobalID", item.getGlobalID());
                itemData.put("itemPrice", item.getPrice());
                itemData.put("itemQuantity", item.getQuantity());

                itemList.put(selectedItem.getKey(), itemData);
            }
        }
        orderData.put("selectedItem", itemList);

        // Set the order with the generated key
        newOrderRef.setValue(orderData).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Log.i("Firebase", "Order Added Successfully!");
            }
        }).addOnFailureListener(e ->
                Log.d("Firebase", "Order creation failed")
        );

        return globalID;

    }

    public static void readOrder(String tableName, Context context){
        DatabaseReference firebaseRef = FirebaseDatabase.getInstance().getReference(tableName);
        firebaseRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @SuppressWarnings("unchecked")
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                try (OrderDatabase orderDatabase = new OrderDatabase(context)) {
                    // Iterate through Firebase data
                    for (DataSnapshot orderSnapshot : dataSnapshot.getChildren()) {
                        // Extract order data
                        Map<String, Object> orderData = (Map<String, Object>) orderSnapshot.getValue();

                        if (orderData != null) {

                            Order order = new Order(
                                    orderSnapshot.getKey(),
                                    (String) orderData.get("creatorID"),
                                    (String) orderData.get("orderDate"),
                                    (String) orderData.get("orderTime"),
                                    (Double) orderData.get("orderTotalAmount"),
                                    ((Long) orderData.get("orderTotalItems")),
                                    (String) orderData.get("orderStatus")
                            );

                            if (Objects.equals(MainActivity.currentUser.getCreatorID(), order.getCreatorID())){
                                long newOrderID = orderDatabase.createOrder(order);

                                // Extract list of items
                                DataSnapshot itemListSnapshot = orderSnapshot.child("selectedItem");
                                if (itemListSnapshot.exists()) {
                                    try (OrderItemsDatabase orderItemsDatabase = new OrderItemsDatabase(context)){
                                        for (DataSnapshot itemSnapshot : itemListSnapshot.getChildren()) {
                                            String selectedItemID = itemSnapshot.getKey(); // This is the key for each item
                                            Map<String, Object> itemData = (Map<String, Object>) itemSnapshot.getValue();

                                            assert itemData != null;
                                            orderItemsDatabase.createOrderItems(selectedItemID,order.getGlobalID(), new Item(
                                                    (String) itemData.get("itemGlobalID"),
                                                    (Double) itemData.get("itemPrice"),
                                                    (Long) itemData.get("itemQuantity")
                                            ));
                                        }

                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                }
                            }

                        }
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                    Log.e("FirebaseOrderDatabase", "Failed to sync Order data from Firebase: " + e.getMessage());
                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("UserDatabase", "Firebase data fetch cancelled: " + error.getMessage());

            }
        });

    }


    public static void createTransaction(Transaction newTransaction){
        DatabaseReference transactionsRef = FirebaseDatabase.getInstance().getReference("transactions");

        // Use push to generate a unique key
        DatabaseReference newTransactionRef = transactionsRef.push();

        // Set the item with the generated key
        newTransactionRef.setValue(newTransaction).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Log.i("FirebaseHandler", "Transaction Added Successfully!");
            }
        }).addOnFailureListener(e ->
                Log.d("FirebaseHandler", "Transaction creation failed")
        );
    }

    public static void readTransaction(String tableName, Context context){
        DatabaseReference firebaseRef = FirebaseDatabase.getInstance().getReference(tableName);
        firebaseRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                try (TransactionDatabase transactionDatabase = new TransactionDatabase(context)) {
                    // Iterate through Firebase data
                    for (DataSnapshot transactionSnapshot : dataSnapshot.getChildren()) {
                        Transaction transaction = transactionSnapshot.getValue(Transaction.class);

                        assert transaction != null;

                        if (Objects.equals(MainActivity.currentUser.getCreatorID(), transaction.getCreatorID())){
                            transaction.setGlobalID(transactionSnapshot.getKey());

                            try(OrderDatabase orderDatabase = new OrderDatabase(context)){
                                long orderNumber =  orderDatabase.getOrderId(transaction.getOrderGlobalID());
                                transaction.setOrderNumber(orderNumber);
                            }

                            transactionDatabase.createTransaction(transaction); // Create item in the local database
                        }

                    }

                } catch (Exception e) {
                    e.printStackTrace();
                    Log.e("FirebaseTransactionDatabase", "Failed to sync transaction data from Firebase: " + e.getMessage());
                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("TransactionDatabase", "Firebase data fetch cancelled: " + error.getMessage());

            }
        });

    }


}
