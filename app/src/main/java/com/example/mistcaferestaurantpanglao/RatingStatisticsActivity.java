package com.example.mistcaferestaurantpanglao;

import android.os.Bundle;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class RatingStatisticsActivity extends AppCompatActivity {

    private TextView tvAverageRating, tvTotalRatings;
    private LinearLayout starDisplay;
    private ProgressBar progress5, progress4, progress3, progress2, progress1;
    private RecyclerView recyclerComments;

    private FirebaseFirestore db;
    private List<RatingCommentModel> commentList;
    private RatingCommentAdapter adapter;

    private static final String TAG = "RatingStats";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rating_statistics);

        db = FirebaseFirestore.getInstance();

        // Back Button
        ImageButton btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> onBackPressed());

        tvAverageRating = findViewById(R.id.tvAverageRating);
        tvTotalRatings = findViewById(R.id.tvTotalRatings);
        starDisplay = findViewById(R.id.starDisplay);

        progress5 = findViewById(R.id.progress5);
        progress4 = findViewById(R.id.progress4);
        progress3 = findViewById(R.id.progress3);
        progress2 = findViewById(R.id.progress2);
        progress1 = findViewById(R.id.progress1);

        recyclerComments = findViewById(R.id.recyclerComments);
        recyclerComments.setLayoutManager(new LinearLayoutManager(this));

        commentList = new ArrayList<>();
        adapter = new RatingCommentAdapter(commentList);
        recyclerComments.setAdapter(adapter);

        loadRatingStatistics();
    }

    private void loadRatingStatistics() {
        db.collection("ratings")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {

                    int total = 0;
                    int sum = 0;

                    int count1 = 0, count2 = 0, count3 = 0, count4 = 0, count5 = 0;

                    commentList.clear();

                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {

                        Long ratingVal = doc.getLong("rating");
                        String comment = doc.getString("comment");
                        String name = doc.getString("customerName");
                        String table = doc.getString("tableNumber");

                        if (ratingVal != null) {
                            int rating = ratingVal.intValue();
                            total++;
                            sum += rating;

                            switch (rating) {
                                case 5: count5++; break;
                                case 4: count4++; break;
                                case 3: count3++; break;
                                case 2: count2++; break;
                                case 1: count1++; break;
                            }

                            if (comment != null && !comment.trim().isEmpty()) {
                                commentList.add(new RatingCommentModel(
                                        name,
                                        table,
                                        rating,
                                        comment
                                ));
                            }
                        }
                    }

                    adapter.notifyDataSetChanged();

                    if (total == 0) {
                        tvAverageRating.setText("0.0");
                        tvTotalRatings.setText("0 ratings");
                        return;
                    }

                    float avg = (float) sum / total;
                    tvAverageRating.setText(String.format("%.1f", avg));
                    tvTotalRatings.setText(total + " ratings");

                    updateStarDisplay(avg);

                    progress5.setProgress((int) ((count5 / (float) total) * 100));
                    progress4.setProgress((int) ((count4 / (float) total) * 100));
                    progress3.setProgress((int) ((count3 / (float) total) * 100));
                    progress2.setProgress((int) ((count2 / (float) total) * 100));
                    progress1.setProgress((int) ((count1 / (float) total) * 100));
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error loading statistics", e));
    }

    private void updateStarDisplay(float rating) {

        starDisplay.removeAllViews();

        int fullStars = (int) rating;
        boolean hasHalf = (rating - fullStars) >= 0.5;

        for (int i = 0; i < 5; i++) {
            TextView star = new TextView(this);
            star.setTextSize(35);

            if (i < fullStars) {
                star.setText("★");
                star.setTextColor(getResources().getColor(android.R.color.holo_orange_light));
            } else if (i == fullStars && hasHalf) {
                star.setText("★");
                star.setTextColor(getResources().getColor(android.R.color.darker_gray));
            } else {
                star.setText("☆");
                star.setTextColor(getResources().getColor(android.R.color.darker_gray));
            }

            starDisplay.addView(star);
        }
    }
}
