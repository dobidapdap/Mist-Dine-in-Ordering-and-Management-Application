package com.example.mistcaferestaurantpanglao;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class RatingCommentAdapter extends RecyclerView.Adapter<RatingCommentAdapter.ViewHolder> {

    private List<RatingCommentModel> comments;

    public RatingCommentAdapter(List<RatingCommentModel> comments) {
        this.comments = comments;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_rating_comment, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        RatingCommentModel model = comments.get(position);

        holder.tvName.setText(model.getCustomerName());
        holder.tvRating.setText(model.getRating() + " ★");
        holder.tvComment.setText(model.getComment());
    }

    @Override
    public int getItemCount() {
        return comments.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        TextView tvName, tvRating, tvComment;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            tvName = itemView.findViewById(R.id.tvName);
            tvRating = itemView.findViewById(R.id.tvRating);
            tvComment = itemView.findViewById(R.id.tvComment);
        }
    }
}
