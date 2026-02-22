package com.example.mistcaferestaurantpanglao;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.Target;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

public class MenuItemAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final String TAG = "MenuItemAdapter";
    private static final int VIEW_TYPE_NORMAL = 0;
    private static final int VIEW_TYPE_EDIT = 1;

    private final Context context;
    private List<MenuItem> menuItemList;
    private OnMenuItemClickListener listener;
    private boolean isEditMode = false;
    private FirebaseFirestore db;

    private Dialog currentDialog;
    private int currentItemPosition = -1;

    public interface OnMenuItemClickListener {
        void onEditClick(MenuItem item);
        void onDeleteClick(MenuItem item);
        void onAvailabilityToggle(MenuItem item, boolean isAvailable);
    }

    public MenuItemAdapter(Context context, List<MenuItem> menuItemList) {
        this.context = context;
        this.menuItemList = menuItemList;
        this.isEditMode = false;
        this.db = FirebaseFirestore.getInstance();
    }

    public MenuItemAdapter(List<MenuItem> menuItemList, OnMenuItemClickListener listener) {
        this.context = null;
        this.menuItemList = menuItemList;
        this.listener = listener;
        this.isEditMode = true;
        this.db = FirebaseFirestore.getInstance();
    }

    @Override
    public int getItemViewType(int position) {
        return isEditMode ? VIEW_TYPE_EDIT : VIEW_TYPE_NORMAL;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_EDIT) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_edit_menu, parent, false);
            return new EditMenuItemViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_menu_dish, parent, false);
            return new MenuItemViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        MenuItem item = menuItemList.get(position);

        if (holder instanceof EditMenuItemViewHolder) {
            bindEditViewHolder((EditMenuItemViewHolder) holder, item, position);
        } else if (holder instanceof MenuItemViewHolder) {
            bindNormalViewHolder((MenuItemViewHolder) holder, item);
        }
    }

    private void bindEditViewHolder(EditMenuItemViewHolder holder, MenuItem item, int position) {
        holder.tvItemName.setText(item.getName());
        holder.tvDescription.setText(item.getDescription());
        holder.tvPrice.setText(item.getPrice().startsWith("₱") ? item.getPrice() : "₱" + item.getPrice());
        holder.tvCategory.setText(item.getCategory());

        holder.switchAvailable.setOnCheckedChangeListener(null);
        holder.switchAvailable.setChecked(item.isAvailable());
        holder.switchAvailable.setOnCheckedChangeListener((buttonView, isChecked) -> {
            item.setAvailable(isChecked);
            if (listener != null) {
                listener.onAvailabilityToggle(item, isChecked);
            }
        });

        holder.btnEdit.setOnClickListener(v -> {
            if (listener != null) {
                listener.onEditClick(item);
            }
        });

        holder.btnDelete.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDeleteClick(item);
            }
        });
    }

    private void bindNormalViewHolder(MenuItemViewHolder holder, MenuItem item) {
        holder.tvDishName.setText(item.getDishName());
        holder.tvDescription.setText(item.getDescription());
        holder.tvPrice.setText(item.getPrice().startsWith("₱") ? item.getPrice() : "₱" + item.getPrice());

        holder.itemView.setAlpha(1.0f);

        loadImage(item.getImageUrl(), holder.ivDishImage, false);

        holder.itemView.setOnClickListener(v -> {
            showMenuItemDialog(item);
        });
        holder.tvAddToCart.setOnClickListener(v -> {
            v.setOnClickListener(null);
            v.setOnClickListener(clickView -> addToCartDirectly(item));
            addToCartDirectly(item);
        });
        if (item.isAvailable()) {
            holder.tvAddToCart.setEnabled(true);
            holder.tvAddToCart.setAlpha(1.0f);
            holder.tvAddToCart.setTextColor(Color.parseColor("#4CAF50"));
        } else {
            holder.tvAddToCart.setEnabled(false);
            holder.tvAddToCart.setAlpha(0.5f);
            holder.tvAddToCart.setTextColor(Color.parseColor("#9E9E9E"));
        }
    }

    private void addToCartDirectly(MenuItem item) {
        Context ctx = context != null ? context : null;
        if (ctx == null) return;

        if (!item.isAvailable()) {
            Toast.makeText(ctx, "This item is currently unavailable", Toast.LENGTH_SHORT).show();
            return;
        }

        // Check dishes left before adding to cart
        checkDishesLeftAndAddToCart(ctx, item, 1, null);
    }

    private void checkDishesLeftAndAddToCart(Context ctx, MenuItem item, int quantity, Dialog dialog) {
        db.collection("menuItems").document(item.getId()).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Long dishesLeft = documentSnapshot.getLong("dishesLeft");

                        if (dishesLeft != null && dishesLeft > 0) {
                            // Check if there's enough stock
                            if (dishesLeft < quantity) {
                                Toast.makeText(ctx, "Only " + dishesLeft + " left in stock", Toast.LENGTH_SHORT).show();
                                return;
                            }

                            // Deduct the quantity
                            long newDishesLeft = dishesLeft - quantity;
                            db.collection("menuItems").document(item.getId())
                                    .update("dishesLeft", newDishesLeft,
                                            "available", newDishesLeft > 0)
                                    .addOnSuccessListener(aVoid -> {
                                        addItemToCart(ctx, item, quantity, dialog);

                                        // Update local item availability
                                        if (newDishesLeft == 0) {
                                            item.setAvailable(false);
                                            notifyDataSetChanged();
                                        }
                                    })
                                    .addOnFailureListener(e -> {
                                        Toast.makeText(ctx, "Error updating stock", Toast.LENGTH_SHORT).show();
                                    });
                        } else {
                            // No stock limit, add normally
                            addItemToCart(ctx, item, quantity, dialog);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(ctx, "Error checking stock", Toast.LENGTH_SHORT).show();
                });
    }

    private void addItemToCart(Context ctx, MenuItem item, int quantity, Dialog dialog) {
        try {
            String priceStr = item.getPrice().replace("₱", "").trim();
            double price = Double.parseDouble(priceStr);
            double total = price * quantity;

            CartItem cartItem = new CartItem(
                    item.getDishName(),
                    price,
                    quantity,
                    total,
                    item.getImageUrl(),
                    item.getDescription()
            );

            ShoppingCartActivity.addToStaticCart(cartItem);

            Toast.makeText(ctx, item.getDishName() + " added to cart", Toast.LENGTH_SHORT).show();

            if (dialog != null) {
                dialog.dismiss();
            }

        } catch (NumberFormatException e) {
            Log.e(TAG, "Error parsing price: " + e.getMessage());
            Toast.makeText(ctx, "Error adding item to cart", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadImage(String imageUrl, ImageView imageView, boolean isDialog) {
        if (imageUrl == null || imageUrl.isEmpty()) {
            imageView.setImageResource(R.drawable.mistlogo);
            return;
        }

        RequestOptions options = new RequestOptions()
                .placeholder(R.drawable.mistlogo)
                .error(R.drawable.mistlogo)
                .fallback(R.drawable.mistlogo)
                .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                .timeout(15000);

        if (isDialog) {
            options = options
                    .override(800, 600)
                    .centerCrop()
                    .format(com.bumptech.glide.load.DecodeFormat.PREFER_ARGB_8888)
                    .skipMemoryCache(false)
                    .diskCacheStrategy(DiskCacheStrategy.ALL);
        } else {
            options = options.centerCrop();
        }

        Context ctx = context != null ? context : imageView.getContext();
        Glide.with(ctx)
                .load(imageUrl)
                .apply(options)
                .listener(new RequestListener<Drawable>() {
                    @Override
                    public boolean onLoadFailed(@Nullable GlideException e, Object model,
                                                Target<Drawable> target, boolean isFirstResource) {
                        Log.e(TAG, "Image load failed: " + (e != null ? e.getMessage() : "Unknown error"));
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(Drawable resource, Object model,
                                                   Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                        return false;
                    }
                })
                .into(imageView);
    }

    private void showMenuItemDialog(MenuItem item) {
        Context ctx = context != null ? context : null;
        if (ctx == null) return;

        currentItemPosition = menuItemList.indexOf(item);
        if (currentItemPosition == -1) return;

        currentDialog = new Dialog(ctx);
        currentDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        currentDialog.setContentView(R.layout.dialog_menu_detail);

        Window window = currentDialog.getWindow();
        if (window != null) {
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.setGravity(Gravity.CENTER);
            WindowManager.LayoutParams lp = window.getAttributes();
            lp.horizontalMargin = 0.1f;
            window.setAttributes(lp);
        }

        setupSwipeGesture(currentDialog);

        loadDialogContent(currentDialog, menuItemList.get(currentItemPosition));

        currentDialog.show();
    }

    private void setupSwipeGesture(Dialog dialog) {
        final GestureDetector gestureDetector = new GestureDetector(dialog.getContext(),
                new GestureDetector.SimpleOnGestureListener() {
                    private static final int SWIPE_THRESHOLD = 100;
                    private static final int SWIPE_VELOCITY_THRESHOLD = 100;

                    @Override
                    public boolean onDown(MotionEvent e) {
                        return true;
                    }

                    @Override
                    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                        if (e1 == null || e2 == null) {
                            return false;
                        }

                        try {
                            float diffX = e2.getX() - e1.getX();
                            float diffY = e2.getY() - e1.getY();

                            Log.d(TAG, "Swipe detected - diffX: " + diffX + ", diffY: " + diffY +
                                    ", velocityX: " + velocityX);

                            if (Math.abs(diffX) > Math.abs(diffY)) {
                                if (Math.abs(diffX) > SWIPE_THRESHOLD && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                                    if (diffX > 0) {
                                        Log.d(TAG, "Swipe RIGHT detected - navigating to previous");
                                        navigateToPreviousItem();
                                    } else {
                                        Log.d(TAG, "Swipe LEFT detected - navigating to next");
                                        navigateToNextItem();
                                    }
                                    return true;
                                }
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error in swipe gesture: " + e.getMessage());
                        }
                        return false;
                    }
                });

        View.OnTouchListener swipeTouchListener = new View.OnTouchListener() {
            private float startX = 0;
            private float startY = 0;
            private boolean isSwiping = false;
            private boolean handledByGesture = false;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                boolean gestureResult = gestureDetector.onTouchEvent(event);

                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        startX = event.getX();
                        startY = event.getY();
                        isSwiping = false;
                        handledByGesture = false;
                        Log.d(TAG, "Touch DOWN at: " + startX + ", " + startY);
                        break;

                    case MotionEvent.ACTION_MOVE:
                        float moveX = event.getX();
                        float moveY = event.getY();
                        float diffX = moveX - startX;
                        float diffY = moveY - startY;

                        if (Math.abs(diffX) > 30 && Math.abs(diffX) > Math.abs(diffY)) {
                            isSwiping = true;
                        }
                        break;

                    case MotionEvent.ACTION_UP:
                        if (gestureResult) {
                            handledByGesture = true;
                            Log.d(TAG, "Gesture detector handled the swipe");
                            return true;
                        }

                        if (!handledByGesture) {
                            float endX = event.getX();
                            float endY = event.getY();
                            float totalDiffX = endX - startX;
                            float totalDiffY = endY - startY;

                            Log.d(TAG, "Touch UP - diffX: " + totalDiffX + ", diffY: " + totalDiffY);

                            if (Math.abs(totalDiffX) > Math.abs(totalDiffY) && Math.abs(totalDiffX) > 100) {
                                if (totalDiffX > 0) {
                                    Log.d(TAG, "Manual swipe RIGHT detected");
                                    navigateToPreviousItem();
                                } else {
                                    Log.d(TAG, "Manual swipe LEFT detected");
                                    navigateToNextItem();
                                }
                                return true;
                            }
                        }
                        break;
                }

                return isSwiping || gestureResult;
            }
        };

        dialog.getWindow().getDecorView().setOnTouchListener(swipeTouchListener);

        View scrollView = dialog.findViewById(R.id.ivDishImageDetail);
        if (scrollView != null) {
            View parent = (View) scrollView.getParent();
            while (parent != null && !(parent instanceof android.widget.ScrollView)) {
                parent = (View) parent.getParent();
            }
            if (parent != null) {
                parent.setOnTouchListener(swipeTouchListener);
                Log.d(TAG, "Applied touch listener to ScrollView");
            }
        }

        View contentLayout = dialog.findViewById(R.id.ivDishImageDetail);
        if (contentLayout != null) {
            View parent = (View) contentLayout.getParent();
            if (parent != null) {
                parent.setOnTouchListener(swipeTouchListener);
                Log.d(TAG, "Applied touch listener to content layout");
            }
        }
    }

    private void navigateToNextItem() {
        Log.d(TAG, "Navigate to next - Current position: " + currentItemPosition +
                ", List size: " + menuItemList.size());

        if (currentItemPosition < menuItemList.size() - 1) {
            currentItemPosition++;
            MenuItem nextItem = menuItemList.get(currentItemPosition);
            Log.d(TAG, "Moving to next item: " + nextItem.getDishName() +
                    " (position " + currentItemPosition + ")");
            loadDialogContent(currentDialog, nextItem);
        } else {
            Context ctx = context != null ? context : currentDialog.getContext();
            Toast.makeText(ctx, "This is the last item (" + (currentItemPosition + 1) +
                    " of " + menuItemList.size() + ")", Toast.LENGTH_SHORT).show();
        }
    }

    private void navigateToPreviousItem() {
        Log.d(TAG, "Navigate to previous - Current position: " + currentItemPosition +
                ", List size: " + menuItemList.size());

        if (currentItemPosition > 0) {
            currentItemPosition--;
            MenuItem prevItem = menuItemList.get(currentItemPosition);
            Log.d(TAG, "Moving to previous item: " + prevItem.getDishName() +
                    " (position " + currentItemPosition + ")");
            loadDialogContent(currentDialog, prevItem);
        } else {
            Context ctx = context != null ? context : currentDialog.getContext();
            Toast.makeText(ctx, "This is the first item (1 of " + menuItemList.size() + ")",
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void showFullImageDialog(String imageUrl) {
        Context ctx = context != null ? context : null;
        if (ctx == null) return;

        Dialog fullImageDialog = new Dialog(ctx);
        fullImageDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        fullImageDialog.setContentView(R.layout.dialog_full_image);

        Window window = fullImageDialog.getWindow();
        if (window != null) {
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            window.setBackgroundDrawable(new ColorDrawable(Color.BLACK));
        }

        ImageView ivFullImage = fullImageDialog.findViewById(R.id.ivFullImage);
        ImageButton btnCloseFullImage = fullImageDialog.findViewById(R.id.btnCloseFullImage);

        RequestOptions options = new RequestOptions()
                .placeholder(R.drawable.mistlogo)
                .error(R.drawable.mistlogo)
                .fallback(R.drawable.mistlogo)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .format(com.bumptech.glide.load.DecodeFormat.PREFER_ARGB_8888)
                .timeout(15000);

        Glide.with(ctx)
                .load(imageUrl)
                .apply(options)
                .into(ivFullImage);

        btnCloseFullImage.setOnClickListener(v -> fullImageDialog.dismiss());

        ivFullImage.setOnClickListener(v -> fullImageDialog.dismiss());

        fullImageDialog.show();
    }

    private void loadDialogContent(Dialog dialog, MenuItem item) {
        Context ctx = context != null ? context : dialog.getContext();

        ImageView ivImage = dialog.findViewById(R.id.ivDishImageDetail);
        TextView tvName = dialog.findViewById(R.id.tvDishNameDetail);
        TextView tvPrice = dialog.findViewById(R.id.tvPriceDetail);
        TextView tvDesc = dialog.findViewById(R.id.tvDescriptionDetail);
        TextView tvAvailability = dialog.findViewById(R.id.tvAvailability);
        TextView tvDishesLeft = dialog.findViewById(R.id.tvDishesLeft);
        ImageButton btnClose = dialog.findViewById(R.id.btnClose);
        Button btnAdd = dialog.findViewById(R.id.btnAddToCart);
        Button btnViewMore = dialog.findViewById(R.id.btnViewMore);
        TextView btnDec = dialog.findViewById(R.id.btnDecreaseQuantity);
        TextView tvQty = dialog.findViewById(R.id.tvQuantity);
        TextView btnInc = dialog.findViewById(R.id.btnIncreaseQuantity);

        final int[] qty = {1};
        tvQty.setText("1");

        tvName.setText(item.getDishName());
        tvPrice.setText(item.getPrice().startsWith("₱") ? item.getPrice() : "₱" + item.getPrice());
        tvDesc.setText(item.getDescription());

        db.collection("menuItems").document(item.getId()).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Long dishesLeft = documentSnapshot.getLong("dishesLeft");

                        if (dishesLeft != null && dishesLeft >= 0) {
                            tvDishesLeft.setVisibility(View.VISIBLE);
                            if (dishesLeft == 0) {
                                tvDishesLeft.setText("Out of Stock");
                                tvDishesLeft.setTextColor(Color.parseColor("#F44336"));
                            } else if (dishesLeft <= 5) {
                                tvDishesLeft.setText("Only " + dishesLeft + " left!");
                                tvDishesLeft.setTextColor(Color.parseColor("#FF9800"));
                            } else {
                                tvDishesLeft.setText(dishesLeft + " available");
                                tvDishesLeft.setTextColor(Color.parseColor("#4CAF50"));
                            }
                        } else {
                            tvDishesLeft.setVisibility(View.GONE);
                        }

                        // Update availability based on current stock
                        Boolean available = documentSnapshot.getBoolean("available");
                        boolean isAvailable = available != null ? available : true;
                        item.setAvailable(isAvailable);

                        updateAvailabilityUI(ctx, item, tvAvailability, btnDec, btnInc, btnAdd, dishesLeft);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading dishes left", e);
                    tvDishesLeft.setVisibility(View.GONE);
                    updateAvailabilityUI(ctx, item, tvAvailability, btnDec, btnInc, btnAdd, null);
                });

        ivImage.setScaleType(ImageView.ScaleType.CENTER_CROP);
        loadImage(item.getImageUrl(), ivImage, true);

        ivImage.setOnClickListener(v -> showFullImageDialog(item.getImageUrl()));

        btnDec.setOnClickListener(null);
        btnInc.setOnClickListener(null);
        btnAdd.setOnClickListener(null);
        btnClose.setOnClickListener(null);
        btnViewMore.setOnClickListener(null);

        btnDec.setOnClickListener(v -> {
            if (item.isAvailable() && qty[0] > 1) {
                qty[0]--;
                tvQty.setText(String.valueOf(qty[0]));
            }
        });

        btnInc.setOnClickListener(v -> {
            if (item.isAvailable()) {
                // Check dishes left before allowing increase
                db.collection("menuItems").document(item.getId()).get()
                        .addOnSuccessListener(documentSnapshot -> {
                            if (documentSnapshot.exists()) {
                                Long dishesLeft = documentSnapshot.getLong("dishesLeft");
                                if (dishesLeft != null && dishesLeft > 0) {
                                    if (qty[0] < dishesLeft && qty[0] < 99) {
                                        qty[0]++;
                                        tvQty.setText(String.valueOf(qty[0]));
                                    } else if (qty[0] >= dishesLeft) {
                                        Toast.makeText(ctx, "Only " + dishesLeft + " available", Toast.LENGTH_SHORT).show();
                                    }
                                } else if (qty[0] < 99) {
                                    qty[0]++;
                                    tvQty.setText(String.valueOf(qty[0]));
                                }
                            }
                        });
            }
        });

        btnAdd.setOnClickListener(v -> {
            if (!item.isAvailable()) {
                Toast.makeText(ctx, "This item is currently unavailable", Toast.LENGTH_SHORT).show();
                return;
            }

            checkDishesLeftAndAddToCart(ctx, item, qty[0], dialog);
        });

        btnClose.setOnClickListener(v -> dialog.dismiss());
        btnViewMore.setOnClickListener(v -> dialog.dismiss());
    }

    private void updateAvailabilityUI(Context ctx, MenuItem item, TextView tvAvailability,
                                      TextView btnDec, TextView btnInc, Button btnAdd, Long dishesLeft) {
        if (item.isAvailable() && (dishesLeft == null || dishesLeft > 0)) {
            tvAvailability.setText("Available");
            try {
                tvAvailability.setTextColor(ctx.getResources().getColor(R.color.success_color));
            } catch (Exception e) {
                tvAvailability.setTextColor(Color.parseColor("#4CAF50"));
            }
            btnDec.setEnabled(true);
            btnInc.setEnabled(true);
            btnAdd.setEnabled(true);
            btnDec.setAlpha(1.0f);
            btnInc.setAlpha(1.0f);
            btnAdd.setAlpha(1.0f);
            btnAdd.setText("Add to Cart");
        } else {
            tvAvailability.setText("Unavailable");
            try {
                tvAvailability.setTextColor(ctx.getResources().getColor(R.color.error_color));
            } catch (Exception e) {
                tvAvailability.setTextColor(Color.parseColor("#F44336"));
            }
            btnDec.setEnabled(false);
            btnInc.setEnabled(false);
            btnAdd.setEnabled(false);
            btnDec.setAlpha(0.5f);
            btnInc.setAlpha(0.5f);
            btnAdd.setAlpha(0.5f);
            btnAdd.setText("Currently Unavailable");
        }
    }

    @Override
    public int getItemCount() {
        return menuItemList.size();
    }

    public void updateMenuItems(List<MenuItem> items) {
        this.menuItemList = items;
        notifyDataSetChanged();
    }

    static class MenuItemViewHolder extends RecyclerView.ViewHolder {
        ImageView ivDishImage;
        TextView tvDishName, tvDescription, tvPrice, tvAddToCart;

        public MenuItemViewHolder(@NonNull View itemView) {
            super(itemView);
            ivDishImage = itemView.findViewById(R.id.ivDishImage);
            tvDishName = itemView.findViewById(R.id.tvDishName);
            tvDescription = itemView.findViewById(R.id.tvDescription);
            tvPrice = itemView.findViewById(R.id.tvPrice);
            tvAddToCart = itemView.findViewById(R.id.tvAddToCart);
        }
    }

    static class EditMenuItemViewHolder extends RecyclerView.ViewHolder {
        TextView tvItemName, tvDescription, tvPrice, tvCategory;
        com.google.android.material.materialswitch.MaterialSwitch switchAvailable;
        ImageButton btnEdit, btnDelete;

        public EditMenuItemViewHolder(@NonNull View itemView) {
            super(itemView);
            tvItemName = itemView.findViewById(R.id.tvItemName);
            tvDescription = itemView.findViewById(R.id.tvDescription);
            tvPrice = itemView.findViewById(R.id.tvPrice);
            tvCategory = itemView.findViewById(R.id.tvCategory);
            switchAvailable = itemView.findViewById(R.id.switchAvailable);
            btnEdit = itemView.findViewById(R.id.btnEdit);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }
}