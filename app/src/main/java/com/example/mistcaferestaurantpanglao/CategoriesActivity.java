package com.example.mistcaferestaurantpanglao;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class CategoriesActivity extends AppCompatActivity {

    private static final String TAG = "CategoriesActivity";
    private LinearLayout categoriesLayout;
    private LinearLayout chefsSpecialLayout;
    private LinearLayout mistDaySpecialsLayout;
    private LinearLayout bestSellersLayout;
    private LinearLayout addOnsLayout;
    private LinearLayout searchResultsLayout;

    private RecyclerView recyclerViewChefsSpecial;
    private RecyclerView recyclerViewMistDaySpecials;
    private RecyclerView recyclerViewBestSellers;
    private RecyclerView recyclerViewAddOns;
    private RecyclerView recyclerViewSearchResults;

    private TextView tvChefsSpecialTitle;
    private TextView tvMistDaySpecialsTitle;
    private TextView tvBestSellersTitle;
    private TextView tvAddOnsTitle;
    private TextView tvSearchResultsTitle;

    private CardView chipCategories;
    private CardView chipChefsSpecial;
    private CardView chipMistDaySpecials;
    private CardView chipBestSellers;
    private CardView chipAddOns;

    private EditText etSearch;
    private FirebaseFirestore db;

    private MenuItemAdapter chefsSpecialAdapter;
    private MenuItemAdapter mistDaySpecialsAdapter;
    private MenuItemAdapter bestSellersAdapter;
    private MenuItemAdapter addOnsAdapter;
    private MenuItemAdapter searchResultsAdapter;

    private List<MenuItem> chefsSpecialItems;
    private List<MenuItem> mistDaySpecialsItems;
    private List<MenuItem> bestSellersItems;
    private List<MenuItem> addOnsItems;
    private List<MenuItem> allMenuItems;
    private List<MenuItem> searchResultsItems;

    private boolean isShowingChefsSpecial = false;
    private boolean isShowingMistDaySpecials = false;
    private boolean isShowingBestSellers = false;
    private boolean isShowingAddOns = false;
    private boolean isSearching = false;
    private ListenerRegistration addOnsListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_categories);

        db = FirebaseFirestore.getInstance();

        initializeViews();
        setupClickListeners();
        setupRecyclerViews();
        setupSearchListener();

        loadAllMenuItems();
        loadChefsSpecialItems();
        loadMistDaySpecialsItems();
        loadBestSellersItems();
        loadAddOnsItems();
    }

    private void initializeViews() {
        CardView cardPyro = findViewById(R.id.card_pyro_series);
        CardView cardPlatter = findViewById(R.id.card_platter);
        CardView cardSnacks = findViewById(R.id.card_snacks);
        CardView cardDrinks = findViewById(R.id.card_drinks);
        CardView cardDessert = findViewById(R.id.card_dessert);
        CardView cardMainCourse = findViewById(R.id.card_main_course);
        CardView cardSalad = findViewById(R.id.card_salad);
        CardView cardSoup = findViewById(R.id.card_soup);
        CardView cardPasta = findViewById(R.id.card_pasta);

        FloatingActionButton fabPrimary = findViewById(R.id.btnFoodCart);
        FloatingActionButton fabSecondary = findViewById(R.id.btnViewStatus);
        FloatingActionButton fabRating = findViewById(R.id.btnRating);

        etSearch = findViewById(R.id.etSearch);

        chipCategories = findViewById(R.id.chip_categories);
        chipChefsSpecial = findViewById(R.id.chip_chefs_special);
        chipMistDaySpecials = findViewById(R.id.chip_mist_day_specials);
        chipBestSellers = findViewById(R.id.chip_best_sellers);
        chipAddOns = findViewById(R.id.chip_add_ons);

        categoriesLayout = findViewById(R.id.categories_layout);
        chefsSpecialLayout = findViewById(R.id.chefs_special_layout);
        mistDaySpecialsLayout = findViewById(R.id.mist_day_specials_layout);
        bestSellersLayout = findViewById(R.id.best_sellers_layout);
        addOnsLayout = findViewById(R.id.add_ons_layout);

        recyclerViewChefsSpecial = findViewById(R.id.recycler_chefs_special);
        recyclerViewMistDaySpecials = findViewById(R.id.recycler_mist_day_specials);
        recyclerViewBestSellers = findViewById(R.id.recycler_best_sellers);
        recyclerViewAddOns = findViewById(R.id.recycler_add_ons);

        tvChefsSpecialTitle = findViewById(R.id.tv_chefs_special_title);
        tvMistDaySpecialsTitle = findViewById(R.id.tv_mist_day_specials_title);
        tvBestSellersTitle = findViewById(R.id.tv_best_sellers_title);
        tvAddOnsTitle = findViewById(R.id.tv_add_ons_title);

        chefsSpecialItems = new ArrayList<>();
        mistDaySpecialsItems = new ArrayList<>();
        bestSellersItems = new ArrayList<>();
        addOnsItems = new ArrayList<>();
        allMenuItems = new ArrayList<>();
        searchResultsItems = new ArrayList<>();

        createSearchResultsLayout();
        showCategoriesLayout();
    }

    private void createSearchResultsLayout() {
        LinearLayout mainLayout = (LinearLayout) categoriesLayout.getParent();

        searchResultsLayout = new LinearLayout(this);
        searchResultsLayout.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        searchResultsLayout.setOrientation(LinearLayout.VERTICAL);
        searchResultsLayout.setPadding(16, 12, 16, 12);
        searchResultsLayout.setVisibility(View.GONE);

        tvSearchResultsTitle = new TextView(this);
        tvSearchResultsTitle.setText("SEARCH RESULTS");
        tvSearchResultsTitle.setTextSize(18);
        tvSearchResultsTitle.setTypeface(null, android.graphics.Typeface.BOLD);
        tvSearchResultsTitle.setTextColor(getResources().getColor(android.R.color.black));
        tvSearchResultsTitle.setPadding(0, 0, 0, 12);
        searchResultsLayout.addView(tvSearchResultsTitle);

        recyclerViewSearchResults = new RecyclerView(this);
        recyclerViewSearchResults.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        recyclerViewSearchResults.setNestedScrollingEnabled(false);
        searchResultsLayout.addView(recyclerViewSearchResults);

        int index = mainLayout.indexOfChild(categoriesLayout);
        mainLayout.addView(searchResultsLayout, index);
    }

    private void setupSearchListener() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String searchQuery = s.toString().trim().toLowerCase();
                performSearch(searchQuery);
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    private void performSearch(String query) {
        if (query.isEmpty()) {
            isSearching = false;
            searchResultsLayout.setVisibility(View.GONE);
            showCategoriesLayout();
            updateChipStates(true, false, false, false, false);
        } else {
            isSearching = true;
            categoriesLayout.setVisibility(View.GONE);
            chefsSpecialLayout.setVisibility(View.GONE);
            mistDaySpecialsLayout.setVisibility(View.GONE);
            bestSellersLayout.setVisibility(View.GONE);
            addOnsLayout.setVisibility(View.GONE);
            searchResultsLayout.setVisibility(View.VISIBLE);

            searchResultsItems.clear();
            searchResultsItems.addAll(
                    allMenuItems.stream()
                            .filter(item -> matchesSearchQuery(item, query))
                            .collect(Collectors.toList())
            );

            if (searchResultsAdapter != null) {
                searchResultsAdapter.notifyDataSetChanged();
            }

            Log.d(TAG, "Search results found: " + searchResultsItems.size());
        }
    }

    private boolean matchesSearchQuery(MenuItem item, String query) {
        String dishName = item.getDishName() != null ? item.getDishName().toLowerCase() : "";
        String category = item.getCategory() != null ? item.getCategory().toLowerCase() : "";

        return dishName.contains(query) || category.contains(query);
    }

    private void setupClickListeners() {
        CardView cardPyro = findViewById(R.id.card_pyro_series);
        CardView cardPlatter = findViewById(R.id.card_platter);
        CardView cardSnacks = findViewById(R.id.card_snacks);
        CardView cardDrinks = findViewById(R.id.card_drinks);
        CardView cardDessert = findViewById(R.id.card_dessert);
        CardView cardMainCourse = findViewById(R.id.card_main_course);
        CardView cardSalad = findViewById(R.id.card_salad);
        CardView cardSoup = findViewById(R.id.card_soup);
        CardView cardPasta = findViewById(R.id.card_pasta);

        cardPyro.setOnClickListener(v -> startActivity(new Intent(this, PyroSeriesActivity.class)));
        cardPlatter.setOnClickListener(v -> startActivity(new Intent(this, PlatterActivity.class)));
        cardSnacks.setOnClickListener(v -> startActivity(new Intent(this, SnacksActivity.class)));
        cardDrinks.setOnClickListener(v -> startActivity(new Intent(this, DrinksActivity.class)));
        cardDessert.setOnClickListener(v -> startActivity(new Intent(this, DessertActivity.class)));
        cardMainCourse.setOnClickListener(v -> startActivity(new Intent(this, MainCourseActivity.class)));
        cardSalad.setOnClickListener(v -> startActivity(new Intent(this, SaladActivity.class)));
        cardSoup.setOnClickListener(v -> startActivity(new Intent(this, SoupActivity.class)));
        cardPasta.setOnClickListener(v -> startActivity(new Intent(this, PastaActivity.class)));

        FloatingActionButton fabPrimary = findViewById(R.id.btnFoodCart);
        FloatingActionButton fabSecondary = findViewById(R.id.btnViewStatus);
        FloatingActionButton fabRating = findViewById(R.id.btnRating);

        fabPrimary.setOnClickListener(v -> startActivity(new Intent(this, ShoppingCartActivity.class)));
        fabSecondary.setOnClickListener(v -> handleViewStatusClick());
        fabRating.setOnClickListener(v -> showRatingDialog());

        chipCategories.setOnClickListener(v -> {
            if (isSearching) {
                etSearch.setText("");
            } else if (isShowingChefsSpecial || isShowingMistDaySpecials || isShowingBestSellers || isShowingAddOns) {
                showCategoriesLayout();
                updateChipStates(true, false, false, false, false);
            }
        });

        chipChefsSpecial.setOnClickListener(v -> {
            if (!isShowingChefsSpecial && !isSearching) {
                showChefsSpecialLayout();
                updateChipStates(false, true, false, false, false);
            }
        });

        chipMistDaySpecials.setOnClickListener(v -> {
            if (!isShowingMistDaySpecials && !isSearching) {
                showMistDaySpecialsLayout();
                updateChipStates(false, false, true, false, false);
            }
        });

        chipBestSellers.setOnClickListener(v -> {
            if (!isShowingBestSellers && !isSearching) {
                showBestSellersLayout();
                updateChipStates(false, false, false, true, false);
            }
        });

        chipAddOns.setOnClickListener(v -> {
            if (!isShowingAddOns && !isSearching) {
                showAddOnsLayout();
                updateChipStates(false, false, false, false, true);
            }
        });
    }

    private void setupRecyclerViews() {
        setupChefsSpecialRecyclerView();
        setupMistDaySpecialsRecyclerView();
        setupBestSellersRecyclerView();
        setupAddOnsRecyclerView();
        setupSearchResultsRecyclerView();
    }

    private void setupChefsSpecialRecyclerView() {
        chefsSpecialAdapter = new MenuItemAdapter(this, chefsSpecialItems);
        recyclerViewChefsSpecial.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewChefsSpecial.setAdapter(chefsSpecialAdapter);
    }

    private void setupMistDaySpecialsRecyclerView() {
        mistDaySpecialsAdapter = new MenuItemAdapter(this, mistDaySpecialsItems);
        recyclerViewMistDaySpecials.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewMistDaySpecials.setAdapter(mistDaySpecialsAdapter);
    }

    private void setupBestSellersRecyclerView() {
        bestSellersAdapter = new MenuItemAdapter(this, bestSellersItems);
        recyclerViewBestSellers.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewBestSellers.setAdapter(bestSellersAdapter);
    }

    private void setupAddOnsRecyclerView() {
        addOnsAdapter = new MenuItemAdapter(this, addOnsItems);
        recyclerViewAddOns.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewAddOns.setAdapter(addOnsAdapter);
    }

    private void setupSearchResultsRecyclerView() {
        searchResultsAdapter = new MenuItemAdapter(this, searchResultsItems);
        recyclerViewSearchResults.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewSearchResults.setAdapter(searchResultsAdapter);
    }

    private boolean containsSpecialty(String specialties, String targetSpecialty) {
        if (specialties == null || targetSpecialty == null) {
            return false;
        }

        String[] specialtyArray = specialties.split(",");
        for (String specialty : specialtyArray) {
            if (specialty.trim().equalsIgnoreCase(targetSpecialty.trim())) {
                return true;
            }
        }
        return false;
    }

    private void loadAllMenuItems() {
        Log.d(TAG, "Starting to load all menu items for search...");

        db.collection("menuItems")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        allMenuItems.clear();

                        for (DocumentSnapshot document : task.getResult()) {
                            try {
                                Boolean available = document.getBoolean("available");
                                String specialties = document.getString("specialties");

                                MenuItem item = new MenuItem();
                                item.setId(document.getId());
                                item.setName(document.getString("name"));
                                item.setDishName(document.getString("dishName"));
                                item.setDescription(document.getString("description"));
                                item.setPrice(PriceUtils.getPriceFromDocument(document, "price"));
                                item.setCategory(document.getString("category"));
                                item.setImageUrl(document.getString("imageUrl"));
                                item.setAvailable(available != null ? available : true);
                                item.setSpecialties(specialties);

                                allMenuItems.add(item);
                            } catch (Exception e) {
                                Log.e(TAG, "Error parsing menu item: " + document.getId(), e);
                            }
                        }

                        Log.d(TAG, "Loaded " + allMenuItems.size() + " menu items for search");
                    } else {
                        Log.e(TAG, "Error loading all menu items", task.getException());
                    }
                });
    }

    private void loadChefsSpecialItems() {
        Log.d(TAG, "Starting to load chef's special items...");

        db.collection("menuItems")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        chefsSpecialItems.clear();

                        for (DocumentSnapshot document : task.getResult()) {
                            try {
                                String specialties = document.getString("specialties");
                                Boolean available = document.getBoolean("available");

                                if (containsSpecialty(specialties, "recommended")) {
                                    MenuItem item = new MenuItem();
                                    item.setId(document.getId());
                                    item.setName(document.getString("name"));
                                    item.setDishName(document.getString("dishName"));
                                    item.setDescription(document.getString("description"));
                                    item.setPrice(PriceUtils.getPriceFromDocument(document, "price"));
                                    item.setCategory(document.getString("category"));
                                    item.setImageUrl(document.getString("imageUrl"));
                                    item.setAvailable(available != null ? available : true);
                                    item.setSpecialties(specialties);

                                    chefsSpecialItems.add(item);
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Error parsing menu item: " + document.getId(), e);
                            }
                        }

                        Collections.sort(chefsSpecialItems, (item1, item2) -> {
                            String name1 = item1.getName() != null ? item1.getName() : "";
                            String name2 = item2.getName() != null ? item2.getName() : "";
                            return name1.compareToIgnoreCase(name2);
                        });

                        runOnUiThread(() -> {
                            chefsSpecialAdapter.notifyDataSetChanged();
                            Log.d(TAG, "Loaded " + chefsSpecialItems.size() + " chef's special items");
                        });
                    } else {
                        Log.e(TAG, "Error loading chef's special items", task.getException());
                    }
                });
    }

    private void loadMistDaySpecialsItems() {
        Log.d(TAG, "Starting to load Mist Day Specials items...");

        db.collection("menuItems")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        mistDaySpecialsItems.clear();

                        for (DocumentSnapshot document : task.getResult()) {
                            try {
                                String specialties = document.getString("specialties");
                                Boolean available = document.getBoolean("available");

                                if (containsSpecialty(specialties, "mistday")) {
                                    MenuItem item = new MenuItem();
                                    item.setId(document.getId());
                                    item.setName(document.getString("name"));
                                    item.setDishName(document.getString("dishName"));
                                    item.setDescription(document.getString("description"));
                                    item.setPrice(PriceUtils.getPriceFromDocument(document, "price"));
                                    item.setCategory(document.getString("category"));
                                    item.setImageUrl(document.getString("imageUrl"));
                                    item.setAvailable(available != null ? available : true);
                                    item.setSpecialties(specialties);

                                    mistDaySpecialsItems.add(item);
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Error parsing menu item: " + document.getId(), e);
                            }
                        }

                        Collections.sort(mistDaySpecialsItems, (item1, item2) -> {
                            String name1 = item1.getName() != null ? item1.getName() : "";
                            String name2 = item2.getName() != null ? item2.getName() : "";
                            return name1.compareToIgnoreCase(name2);
                        });

                        runOnUiThread(() -> {
                            mistDaySpecialsAdapter.notifyDataSetChanged();
                            Log.d(TAG, "Loaded " + mistDaySpecialsItems.size() + " Mist Day Specials items");
                        });
                    } else {
                        Log.e(TAG, "Error loading Mist Day Specials items", task.getException());
                    }
                });
    }

    private void loadBestSellersItems() {
        Log.d(TAG, "Starting to load Best Sellers items...");

        db.collection("menuItems")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        bestSellersItems.clear();

                        for (DocumentSnapshot document : task.getResult()) {
                            try {
                                String specialties = document.getString("specialties");
                                Boolean available = document.getBoolean("available");

                                if (containsSpecialty(specialties, "bestseller")) {
                                    MenuItem item = new MenuItem();
                                    item.setId(document.getId());
                                    item.setName(document.getString("name"));
                                    item.setDishName(document.getString("dishName"));
                                    item.setDescription(document.getString("description"));
                                    item.setPrice(PriceUtils.getPriceFromDocument(document, "price"));
                                    item.setCategory(document.getString("category"));
                                    item.setImageUrl(document.getString("imageUrl"));
                                    item.setAvailable(available != null ? available : true);
                                    item.setSpecialties(specialties);

                                    bestSellersItems.add(item);
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Error parsing menu item: " + document.getId(), e);
                            }
                        }

                        Collections.sort(bestSellersItems, (item1, item2) -> {
                            String name1 = item1.getName() != null ? item1.getName() : "";
                            String name2 = item2.getName() != null ? item2.getName() : "";
                            return name1.compareToIgnoreCase(name2);
                        });

                        runOnUiThread(() -> {
                            bestSellersAdapter.notifyDataSetChanged();
                            Log.d(TAG, "Loaded " + bestSellersItems.size() + " Best Sellers items");
                        });
                    } else {
                        Log.e(TAG, "Error loading Best Sellers items", task.getException());
                    }
                });
    }

    private void loadAddOnsItems() {
        Log.d(TAG, "Starting to load Add Ons items...");

        if (addOnsListener != null) {
            addOnsListener.remove();
        }

        addOnsListener = db.collection("menuItems")
                .addSnapshotListener((queryDocumentSnapshots, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Listen failed: " + error.getMessage(), error);
                        return;
                    }

                    if (queryDocumentSnapshots != null) {
                        addOnsItems.clear();

                        for (DocumentSnapshot document : queryDocumentSnapshots) {
                            try {
                                String specialties = document.getString("specialties");
                                Boolean available = document.getBoolean("available");

                                if (containsSpecialty(specialties, "addons")) {
                                    MenuItem item = new MenuItem();
                                    item.setId(document.getId());
                                    item.setName(document.getString("name"));
                                    item.setDishName(document.getString("dishName"));
                                    item.setDescription(document.getString("description"));
                                    item.setPrice(PriceUtils.getPriceFromDocument(document, "price"));
                                    item.setCategory(document.getString("category"));
                                    item.setImageUrl(document.getString("imageUrl"));
                                    item.setAvailable(available != null ? available : true);
                                    item.setSpecialties(specialties);

                                    addOnsItems.add(item);
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Error parsing menu item: " + document.getId(), e);
                            }
                        }

                        Collections.sort(addOnsItems, (item1, item2) -> {
                            String name1 = item1.getName() != null ? item1.getName() : "";
                            String name2 = item2.getName() != null ? item2.getName() : "";
                            return name1.compareToIgnoreCase(name2);
                        });

                        runOnUiThread(() -> {
                            addOnsAdapter.notifyDataSetChanged();
                            Log.d(TAG, "Loaded " + addOnsItems.size() + " Add Ons items");
                        });
                    }
                });
    }
    private void showCategoriesLayout() {
        categoriesLayout.setVisibility(View.VISIBLE);
        chefsSpecialLayout.setVisibility(View.GONE);
        mistDaySpecialsLayout.setVisibility(View.GONE);
        bestSellersLayout.setVisibility(View.GONE);
        addOnsLayout.setVisibility(View.GONE);
        searchResultsLayout.setVisibility(View.GONE);

        isShowingChefsSpecial = false;
        isShowingMistDaySpecials = false;
        isShowingBestSellers = false;
        isShowingAddOns = false;
        isSearching = false;
    }

    private void showChefsSpecialLayout() {
        categoriesLayout.setVisibility(View.GONE);
        chefsSpecialLayout.setVisibility(View.VISIBLE);
        mistDaySpecialsLayout.setVisibility(View.GONE);
        bestSellersLayout.setVisibility(View.GONE);
        addOnsLayout.setVisibility(View.GONE);
        searchResultsLayout.setVisibility(View.GONE);

        isShowingChefsSpecial = true;
        isShowingMistDaySpecials = false;
        isShowingBestSellers = false;
        isShowingAddOns = false;
        isSearching = false;
    }

    private void showMistDaySpecialsLayout() {
        categoriesLayout.setVisibility(View.GONE);
        chefsSpecialLayout.setVisibility(View.GONE);
        mistDaySpecialsLayout.setVisibility(View.VISIBLE);
        bestSellersLayout.setVisibility(View.GONE);
        addOnsLayout.setVisibility(View.GONE);
        searchResultsLayout.setVisibility(View.GONE);

        isShowingChefsSpecial = false;
        isShowingMistDaySpecials = true;
        isShowingBestSellers = false;
        isShowingAddOns = false;
        isSearching = false;
    }

    private void showBestSellersLayout() {
        categoriesLayout.setVisibility(View.GONE);
        chefsSpecialLayout.setVisibility(View.GONE);
        mistDaySpecialsLayout.setVisibility(View.GONE);
        bestSellersLayout.setVisibility(View.VISIBLE);
        addOnsLayout.setVisibility(View.GONE);
        searchResultsLayout.setVisibility(View.GONE);

        isShowingChefsSpecial = false;
        isShowingMistDaySpecials = false;
        isShowingBestSellers = true;
        isShowingAddOns = false;
        isSearching = false;
    }

    private void showAddOnsLayout() {
        categoriesLayout.setVisibility(View.GONE);
        chefsSpecialLayout.setVisibility(View.GONE);
        mistDaySpecialsLayout.setVisibility(View.GONE);
        bestSellersLayout.setVisibility(View.GONE);
        addOnsLayout.setVisibility(View.VISIBLE);
        searchResultsLayout.setVisibility(View.GONE);

        isShowingChefsSpecial = false;
        isShowingMistDaySpecials = false;
        isShowingBestSellers = false;
        isShowingAddOns = true;
        isSearching = false;
    }

    private void updateChipStates(boolean categoriesActive, boolean chefsSpecialActive,
                                  boolean mistDaySpecialsActive, boolean bestSellersActive,
                                  boolean addOnsActive) {
        chipCategories.setCardBackgroundColor(categoriesActive ?
                getResources().getColor(android.R.color.darker_gray) :
                getResources().getColor(android.R.color.white));

        chipChefsSpecial.setCardBackgroundColor(chefsSpecialActive ?
                getResources().getColor(android.R.color.darker_gray) :
                getResources().getColor(android.R.color.white));

        chipMistDaySpecials.setCardBackgroundColor(mistDaySpecialsActive ?
                getResources().getColor(android.R.color.darker_gray) :
                getResources().getColor(android.R.color.white));

        chipBestSellers.setCardBackgroundColor(bestSellersActive ?
                getResources().getColor(android.R.color.darker_gray) :
                getResources().getColor(android.R.color.white));

        chipAddOns.setCardBackgroundColor(addOnsActive ?
                getResources().getColor(android.R.color.darker_gray) :
                getResources().getColor(android.R.color.white));
    }

    private void handleViewStatusClick() {
        showOrderStatusDialog();
    }

    private void showOrderStatusDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Track Your Order");
        builder.setMessage("Enter your order details to track status:");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 20, 50, 20);

        LinearLayout tableLayout = new LinearLayout(this);
        tableLayout.setOrientation(LinearLayout.HORIZONTAL);
        tableLayout.setPadding(0, 10, 0, 10);

        TextView tvTable = new TextView(this);
        tvTable.setText("Table ");
        tvTable.setTextSize(16);
        tvTable.setPadding(0, 0, 10, 0);
        tableLayout.addView(tvTable);

        EditText etTableNumber = new EditText(this);
        etTableNumber.setHint("Enter Table number");
        etTableNumber.setInputType(InputType.TYPE_CLASS_NUMBER);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1.0f
        );
        etTableNumber.setLayoutParams(params);
        tableLayout.addView(etTableNumber);

        layout.addView(tableLayout);

        EditText etCustomerName = new EditText(this);
        etCustomerName.setHint("Customer Name");
        layout.addView(etCustomerName);

        builder.setView(layout);

        builder.setPositiveButton("Track Order", (dialog, which) -> {
            String tableNumberInput = etTableNumber.getText().toString().trim();
            String customerName = etCustomerName.getText().toString().trim();

            if (TextUtils.isEmpty(tableNumberInput) || TextUtils.isEmpty(customerName)) {
                Toast.makeText(this, "Please enter both table number and customer name", Toast.LENGTH_SHORT).show();
                return;
            }

            try {
                int tableNum = Integer.parseInt(tableNumberInput);
                if (tableNum < 1) {
                    Toast.makeText(this, "Please enter a valid table number", Toast.LENGTH_SHORT).show();
                    return;
                }
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Please enter a valid number", Toast.LENGTH_SHORT).show();
                return;
            }

            String tableNumber = "Table " + tableNumberInput;
            navigateToOrderStatus(customerName, tableNumber);
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());

        builder.show();
    }

    private void navigateToOrderStatus(String customerName, String tableNumber) {
        Intent intent = new Intent(this, OrderStatusActivity.class);
        intent.putExtra("customerName", customerName);
        intent.putExtra("tableNumber", tableNumber);
        startActivity(intent);
    }

    private void showRatingDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Rate Your Experience");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 20);
        layout.setGravity(android.view.Gravity.CENTER);

        TextView tvMessage = new TextView(this);
        tvMessage.setText("How was your overall experience?");
        tvMessage.setTextSize(16);
        tvMessage.setGravity(android.view.Gravity.CENTER);
        tvMessage.setPadding(0, 0, 0, 30);
        layout.addView(tvMessage);

        LinearLayout starLayout = new LinearLayout(this);
        starLayout.setOrientation(LinearLayout.HORIZONTAL);
        starLayout.setGravity(android.view.Gravity.CENTER);

        final TextView[] stars = new TextView[5];
        final int[] selectedRating = {0};

        for (int i = 0; i < 5; i++) {
            final int starIndex = i;
            stars[i] = new TextView(this);
            stars[i].setText("★");
            stars[i].setTextSize(48);
            stars[i].setTextColor(getResources().getColor(android.R.color.darker_gray));
            stars[i].setPadding(10, 10, 10, 10);

            stars[i].setOnClickListener(v -> {
                selectedRating[0] = starIndex + 1;
                updateStars(stars, starIndex + 1);
            });

            starLayout.addView(stars[i]);
        }

        layout.addView(starLayout);

        TextView tvComment = new TextView(this);
        tvComment.setText("Additional Comments (Optional)");
        tvComment.setTextSize(14);
        tvComment.setPadding(0, 30, 0, 10);
        layout.addView(tvComment);

        EditText etComment = new EditText(this);
        etComment.setHint("Share your thoughts...");
        etComment.setMinLines(3);
        etComment.setGravity(android.view.Gravity.TOP | android.view.Gravity.START);
        layout.addView(etComment);

        builder.setView(layout);

        builder.setPositiveButton("Submit", (dialog, which) -> {
            if (selectedRating[0] == 0) {
                Toast.makeText(this, "Please select a rating", Toast.LENGTH_SHORT).show();
                return;
            }

            String comment = etComment.getText().toString().trim();
            submitRating(selectedRating[0], comment);
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());

        builder.show();
    }

    private void updateStars(TextView[] stars, int rating) {
        for (int i = 0; i < stars.length; i++) {
            if (i < rating) {
                stars[i].setTextColor(getResources().getColor(android.R.color.holo_orange_light));
            } else {
                stars[i].setTextColor(getResources().getColor(android.R.color.darker_gray));
            }
        }
    }

    private void submitRating(int rating, String comment) {
        SharedPreferences prefs = getSharedPreferences("UserSession", MODE_PRIVATE);
        String customerName = prefs.getString("customerName", "Anonymous");
        String tableNumber = prefs.getString("tableNumber", "N/A");

        Map<String, Object> ratingData = new HashMap<>();
        ratingData.put("rating", rating);
        ratingData.put("comment", comment);
        ratingData.put("customerName", customerName);
        ratingData.put("tableNumber", tableNumber);
        ratingData.put("timestamp", Timestamp.now());

        db.collection("ratings")
                .add(ratingData)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(this, "Thank you for your feedback! ⭐", Toast.LENGTH_LONG).show();
                    Log.d(TAG, "Rating submitted: " + rating + " stars");
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to submit rating. Please try again.", Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Error submitting rating", e);
                });
    }

    @Override
    public void onBackPressed() {
        if (isSearching) {
            etSearch.setText("");
        } else if (isShowingChefsSpecial || isShowingMistDaySpecials || isShowingBestSellers || isShowingAddOns) {
            showCategoriesLayout();
            updateChipStates(true, false, false, false, false);
        } else {
            super.onBackPressed();
        }
    }
}