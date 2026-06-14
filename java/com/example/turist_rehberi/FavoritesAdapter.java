package com.example.turist_rehberi;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.List;

public class FavoritesAdapter extends RecyclerView.Adapter<FavoritesAdapter.ViewHolder> {

    private List<PlaceModel> places;

    public FavoritesAdapter(List<PlaceModel> places) {
        this.places = places;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_favorite_card, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        PlaceModel place = places.get(position);

        holder.tvName.setText(place.getName());
        holder.tvCategory.setText(place.getCategory());

        if (place.getImageUrl1() != null) {
            Glide.with(holder.itemView.getContext())
                    .load(place.getImageUrl1())
                    .transform(new CenterCrop())
                    .into(holder.imgPlace);
        }

        // الذهاب للتفاصيل
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(v.getContext(), PlaceDetailsActivity.class);
            intent.putExtra("PLACE_NAME", place.getName());
            v.getContext().startActivity(intent);
        });

        // 🚀 حذف من المفضلة فوراً عند الضغط على القلب
        holder.btnRemoveFav.setOnClickListener(v -> {
            String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
            FirebaseFirestore.getInstance().collection("Users").document(userId)
                    .collection("Favorites").document(place.getName())
                    .delete().addOnSuccessListener(aVoid -> {
                        places.remove(position);
                        notifyItemRemoved(position);
                        notifyItemRangeChanged(position, places.size());
                    });
        });
    }

    @Override
    public int getItemCount() { return places.size(); }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvCategory;
        ImageView imgPlace;
        View btnRemoveFav;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvFavName);
            tvCategory = itemView.findViewById(R.id.tvFavCategory);
            imgPlace = itemView.findViewById(R.id.imgFavPlace);
            btnRemoveFav = itemView.findViewById(R.id.btnRemoveFav);
        }
    }
}