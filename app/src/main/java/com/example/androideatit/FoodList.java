package com.example.androideatit;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.example.androideatit.Common.Common;
import com.example.androideatit.Database.Database;
import com.example.androideatit.Interface.ItemClickListener;
import com.example.androideatit.Model.Category;
import com.example.androideatit.Model.Food;
import com.example.androideatit.ViewHolder.FoodViewHolder;
import com.example.androideatit.ViewHolder.MenuViewHolder;
import com.facebook.CallbackManager;
import com.facebook.share.model.SharePhoto;
import com.facebook.share.model.SharePhotoContent;
import com.facebook.share.widget.ShareDialog;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.mancj.materialsearchbar.MaterialSearchBar;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import java.util.ArrayList;
import java.util.List;

import uk.co.chrisjenx.calligraphy.CalligraphyConfig;
import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;

public class FoodList extends AppCompatActivity {

    RecyclerView recyclerView;
    RecyclerView.LayoutManager layoutManager;

    FirebaseDatabase database;
    DatabaseReference foodList;

    String categoryId = "";

    FirebaseRecyclerAdapter<Food, FoodViewHolder> adapter;

    //Search Functionality
    FirebaseRecyclerAdapter<Food, FoodViewHolder> searchAdapter;
    List<String> suggestList = new ArrayList<>();
    MaterialSearchBar materialSearchBar;

    //Favorites
    Database localDB;

    //Facebook Share
    CallbackManager callbackManager;
    ShareDialog shareDialog;

    //Create Target from Picaso
    Target target = new Target() {
        @Override
        public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
            //Create Photo from Bitmap
            SharePhoto photo = new SharePhoto.Builder()
                    .setBitmap(bitmap)
                    .build();

            if(ShareDialog.canShow(SharePhotoContent.class)){
                SharePhotoContent content = new SharePhotoContent.Builder()
                        .addPhoto(photo)
                        .build();

                shareDialog.show(content);
            }
        }

        @Override
        public void onBitmapFailed(Exception e, Drawable errorDrawable) {

        }

        @Override
        public void onPrepareLoad(Drawable placeHolderDrawable) {

        }
    };

    SwipeRefreshLayout swipeRefreshLayout;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(CalligraphyContextWrapper.wrap(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Note: Add this before Set Content View
        CalligraphyConfig.initDefault(new CalligraphyConfig.Builder()
                .setDefaultFontPath("fonts/restaurant_font.otf")
                .setFontAttrId(R.attr.fontPath)
                .build());

        setContentView(R.layout.activity_food_list);

        //Init Facebook
        callbackManager = CallbackManager.Factory.create();
        shareDialog = new ShareDialog(this);

        //`Firebase
        database = FirebaseDatabase.getInstance();
        foodList = database.getReference("Food");

        //Local DB
        localDB = new Database(this);

        swipeRefreshLayout = (SwipeRefreshLayout)findViewById(R.id.swipe_foodlist_layout);

        swipeRefreshLayout.setColorSchemeResources(
                R.color.colorPrimary,
                android.R.color.holo_green_dark,
                android.R.color.holo_orange_dark,
                android.R.color.holo_blue_dark);

        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                //Get Intent HEre
                if(getIntent()!=null)
                    categoryId = getIntent().getStringExtra("CategoryId");
                if(!categoryId.isEmpty() && categoryId != null)
                {
                    if(Common.isConnectedToInternet(getBaseContext()))
                        loadListFood(categoryId);
                    else{
                        Toast.makeText(FoodList.this, "Please check your connection!", Toast.LENGTH_SHORT).show();
                        return;
                    }


                }
            }
        });

        swipeRefreshLayout.post(new Runnable() {
            @Override
            public void run() {
                //Get Intent HEre
                if(getIntent()!=null)
                    categoryId = getIntent().getStringExtra("CategoryId");
                if(!categoryId.isEmpty() && categoryId != null)
                {
                    if(Common.isConnectedToInternet(getBaseContext()))
                        loadListFood(categoryId);
                    else{
                        Toast.makeText(FoodList.this, "Please check your connection!", Toast.LENGTH_SHORT).show();
                        return;
                    }


                }
            }
        });

        recyclerView = (RecyclerView)findViewById(R.id.recycler_food);
        recyclerView.setHasFixedSize(true);
        layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);





        //Search
        materialSearchBar = (MaterialSearchBar)findViewById(R.id.searchBar);
        materialSearchBar.setHint("Enter your food");
//        materialSearchBar.setSpeechMode(false); No Need as already defined in XML
        loadSuggest(); //Write function to load suggest from Firebase
        materialSearchBar.setLastSuggestions(suggestList);
        materialSearchBar.setCardViewElevation(10);
        materialSearchBar.addTextChangeListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

                //When User Type their Text, We will change your Suggest List
                List<String> suggest = new ArrayList<>();
                for(String search: suggestList)
                {
                    if(search.toLowerCase().contains(materialSearchBar.getText().toLowerCase()))
                    {
                        suggest.add(search);
                    }
                }
                materialSearchBar.setLastSuggestions(suggest);
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
        materialSearchBar.setOnSearchActionListener(new MaterialSearchBar.OnSearchActionListener() {
            @Override
            public void onSearchStateChanged(boolean enabled) {
                //When SearchBar is Close
                //Restore Original Suggest Adapter
                if(!enabled){
                    adapter.startListening();
                    recyclerView.setAdapter(adapter);
                }
            }

            @Override
            public void onSearchConfirmed(CharSequence text) {
                //When Search Finished
                //Show Result of Search Adapter
                startSearch(text);
            }

            @Override
            public void onButtonClicked(int buttonCode) {

            }
        });

    }

    private void startSearch(CharSequence text) {
        FirebaseRecyclerOptions<Food> options =
                new FirebaseRecyclerOptions.Builder<Food>()
                        .setQuery(foodList.orderByChild("name").equalTo(text.toString()), Food.class) //Compare Name
                        .build();


        searchAdapter = new FirebaseRecyclerAdapter<Food, FoodViewHolder>(options) {
            @Override
            protected void onBindViewHolder(@NonNull FoodViewHolder viewHolder, int position, @NonNull Food model) {
                viewHolder.food_name.setText(model.getName());
                Picasso.get().load(model.getImage()).into(viewHolder.food_image);

                final Food local = model;

                viewHolder.setItemClickListener(new ItemClickListener() {
                    @Override
                    public void onClick(View view, int position, boolean isLongClick) {
//                        Toast.makeText(FoodList.this, ""+local.getName() , Toast.LENGTH_SHORT).show();
                        Intent foodDetail = new Intent(FoodList.this, FoodDetail.class);
                        foodDetail.putExtra("FoodId", searchAdapter.getRef(position).getKey());
                        startActivity(foodDetail);
                    }
                });

            }

            @NonNull
            @Override
            public FoodViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.food_item, parent, false);
                return new FoodViewHolder(view);
            }
        };
        searchAdapter.startListening();
        recyclerView.setAdapter(searchAdapter); //Set Adapter for Recycler View is Search Result
    }

    private void loadSuggest() {
        foodList.orderByChild("menuId").equalTo(categoryId)
                .addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for(DataSnapshot postSnapshot: dataSnapshot.getChildren()){
                    Food item = postSnapshot.getValue(Food.class);
                    suggestList.add(item.getName()); //Add name of food to suggested list
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void loadListFood(String categoryId) {
        FirebaseRecyclerOptions<Food> options =
                new FirebaseRecyclerOptions.Builder<Food>()
                        .setQuery(foodList.orderByChild("menuId").equalTo(categoryId), Food.class)
                        .build();


        adapter = new FirebaseRecyclerAdapter<Food, FoodViewHolder>(options) {
            @Override
            protected void onBindViewHolder(@NonNull final FoodViewHolder viewHolder, final int position, @NonNull final Food model) {
                viewHolder.food_name.setText(model.getName());
                viewHolder.food_price.setText(String.format("$ %s", model.getPrice()).toString());
                Picasso.get().load(model.getImage()).into(viewHolder.food_image);

                if(localDB.isFavorite(adapter.getRef(position).getKey()))
                    viewHolder.fav_image.setImageResource(R.drawable.ic_favorite_black_24dp);

                //Click to Share
                viewHolder.share_image.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Picasso.get().load(model.getImage()).into(target);
                    }
                });

                //Click to Change State of Favorites
                viewHolder.fav_image.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if(!localDB.isFavorite(adapter.getRef(position).getKey())) {
                            localDB.addToFavorites(adapter.getRef(position).getKey());
                            viewHolder.fav_image.setImageResource(R.drawable.ic_favorite_black_24dp);
                            Toast.makeText(FoodList.this, ""+model.getName()+" was added to Favorites", Toast.LENGTH_SHORT).show();
                        }
                        else
                        {
                            localDB.removeFromFavorites(adapter.getRef(position).getKey());
                            viewHolder.fav_image.setImageResource(R.drawable.ic_favorite_border_black_24dp);
                            Toast.makeText(FoodList.this, ""+model.getName()+" was removed from Favorites", Toast.LENGTH_SHORT).show();
                        }

                    }
                });

                final Food local = model;

                viewHolder.setItemClickListener(new ItemClickListener() {
                    @Override
                    public void onClick(View view, int position, boolean isLongClick) {
//                        Toast.makeText(FoodList.this, ""+local.getName() , Toast.LENGTH_SHORT).show();
                         Intent foodDetail = new Intent(FoodList.this, FoodDetail.class);
                         foodDetail.putExtra("FoodId", adapter.getRef(position).getKey());
                         startActivity(foodDetail);
                    }
                });

            }

            @NonNull
            @Override
            public FoodViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.food_item, parent, false);
                return new FoodViewHolder(view);
            }
        };
        adapter.startListening();
        recyclerView.setAdapter(adapter);

        swipeRefreshLayout.setRefreshing(false);
    }
}
