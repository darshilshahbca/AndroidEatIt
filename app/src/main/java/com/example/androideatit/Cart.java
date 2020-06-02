package com.example.androideatit;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.androideatit.Common.Common;
import com.example.androideatit.Database.Database;
import com.example.androideatit.Helper.RecyclerItemTouchHelper;
import com.example.androideatit.Interface.RecyclerItemTouchHelperListener;
import com.example.androideatit.Model.Order;
import com.example.androideatit.Model.Request;
import com.example.androideatit.ViewHolder.CartAdapter;
import com.example.androideatit.ViewHolder.CartViewHolder;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.rengwuxian.materialedittext.MaterialEditText;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import info.hoang8f.widget.FButton;

public class Cart extends AppCompatActivity implements RecyclerItemTouchHelperListener {

    RecyclerView recyclerView;
    RecyclerView.LayoutManager layoutManager;

    FirebaseDatabase database;
    DatabaseReference requests;

    public TextView txtTotalPrice;
    FButton btnPlace;

    List<Order> cart = new ArrayList<>();

    CartAdapter adapter;

    RelativeLayout rootLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cart);

        //Firebase
        database = FirebaseDatabase.getInstance();
        requests = database.getReference("Requests");

        //Init
        recyclerView = (RecyclerView)findViewById(R.id.listCart);
        recyclerView.setHasFixedSize(true);
        layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);

        rootLayout = (RelativeLayout)findViewById(R.id.rootCartLayout);

        //Swipe to Delete
        ItemTouchHelper.SimpleCallback itemTouchHelperCallback =
                new RecyclerItemTouchHelper(0, ItemTouchHelper.LEFT,this);

        new ItemTouchHelper(itemTouchHelperCallback).attachToRecyclerView(recyclerView);


        txtTotalPrice = (TextView)findViewById(R.id.total);
        btnPlace = (FButton)findViewById(R.id.btnPlaceOrder);

        btnPlace.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(cart.size() > 0 )
                    showAlertDialog();
                else
                    Toast.makeText(Cart.this, "Your Cart is Empty!", Toast.LENGTH_SHORT).show();

            }
        });

        loadListFood();


    }

    private void showAlertDialog() {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(Cart.this);
        alertDialog.setTitle("One More Step!");
        alertDialog.setMessage("Enter Your Address: ");

        LayoutInflater inflater = this.getLayoutInflater();
        View order_address_comment = inflater.inflate(R.layout.order_address_comment, null);

        final MaterialEditText edtAddress = (MaterialEditText)order_address_comment.findViewById(R.id.edtAddress);
        final MaterialEditText edtComment = (MaterialEditText)order_address_comment.findViewById(R.id.edtComment);

        alertDialog.setView(order_address_comment);
        alertDialog.setIcon(R.drawable.ic_shopping_cart_black_24dp);

        alertDialog.setPositiveButton("YES", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

                //Create new Request
                Request request = new Request(
                        Common.currentUser.getPhone(),
                        Common.currentUser.getName(),
                        edtAddress.getText().toString(),
                        txtTotalPrice.getText().toString(),
                        "0",
                        edtComment.getText().toString(),
                        cart
                );

                //Submit to Firebase
                requests.child(String.valueOf(System.currentTimeMillis()))
                        .setValue(request);
                //Delete Cart
                new Database(getBaseContext()).cleanCart(Common.currentUser.getPhone());
                Toast.makeText(Cart.this, "Thank you, Order Place", Toast.LENGTH_SHORT).show();
                finish();

            }
        });

        alertDialog.setNegativeButton("NO", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        alertDialog.show();

    }

    private void loadListFood() {

        cart = new Database(this).getCarts(Common.currentUser.getPhone());
        adapter = new CartAdapter(cart, this);
        adapter.notifyDataSetChanged();
        recyclerView.setAdapter(adapter);

        //Calculate Total Price
        int total = 0;
        for(Order order: cart)
            total+=(Integer.parseInt(order.getPrice())) * (Integer.parseInt(order.getQuantity()));
        Locale locale = new Locale("en", "US");
        NumberFormat fmt = NumberFormat.getCurrencyInstance(locale);

        txtTotalPrice.setText(fmt.format(total));

    }

    @Override
    public boolean onContextItemSelected(@NonNull MenuItem item) {
        if(item.getTitle().equals(Common.DELETE))
            deleteCart(item.getOrder());
        return true;
    }

    private void deleteCart(int position) {
        cart.remove(position);

        new Database(this).cleanCart(Common.currentUser.getPhone());

        for(Order item: cart)
            new Database(this).addToCart(item);

        //Refresh Food
        loadListFood();
    }

    @Override
    public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction, int position) {
        if(viewHolder instanceof CartViewHolder){
            String name = ((CartAdapter)recyclerView.getAdapter()).getItem(viewHolder.getAdapterPosition()).getProductName();

            final Order deleteItem = ((CartAdapter)recyclerView.getAdapter()).getItem(viewHolder.getAdapterPosition());
            final int deleteIndex = viewHolder.getAdapterPosition();

            adapter.removeItem(deleteIndex);

            new Database(getBaseContext()).removeFromCart(deleteItem.getProductId(), Common.currentUser.getPhone());


            //Update Total
            int total = 0;
            List<Order> orders = new Database(getBaseContext()).getCarts(Common.currentUser.getPhone());

            for(Order item: orders)
                total+=(Integer.parseInt(item.getPrice())) * (Integer.parseInt(item.getQuantity()));
            Locale locale = new Locale("en", "US");
            NumberFormat fmt = NumberFormat.getCurrencyInstance(locale);

            txtTotalPrice.setText(fmt.format(total));

            //Make Snackbar
            Snackbar snackbar = Snackbar.make(rootLayout, name + " removed from Cart!", Snackbar.LENGTH_LONG);
            snackbar.setAction("UNDO", new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    adapter.restoreItem(deleteItem,deleteIndex);
                    new Database(getBaseContext()).addToCart(deleteItem);

                    //Update Total
                    int total = 0;
                    List<Order> orders = new Database(getBaseContext()).getCarts(Common.currentUser.getPhone());

                    for(Order item: orders)
                        total+=(Integer.parseInt(item.getPrice())) * (Integer.parseInt(item.getQuantity()));
                    Locale locale = new Locale("en", "US");
                    NumberFormat fmt = NumberFormat.getCurrencyInstance(locale);

                    txtTotalPrice.setText(fmt.format(total));
                }
            });
            snackbar.setActionTextColor(Color.YELLOW);
            snackbar.show();

        }
    }
}
