package com.example.turist_rehberi;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.List;

public class ExploreAdapter extends RecyclerView.Adapter<ExploreAdapter.ViewHolder> {

    private List<PlaceModel> places;
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private String userId;

    public ExploreAdapter(List<PlaceModel> places) {
        this.places = places;
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        }
    }

    public void filterList(List<PlaceModel> filteredList) {
        this.places = filteredList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_explore_card, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        PlaceModel place = places.get(position);

        if (place.getName() != null) holder.tvName.setText(place.getName());
        if (place.getCategory() != null) holder.tvCategory.setText(place.getCategory());

        if (place.getImageUrl1() != null && !place.getImageUrl1().isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(place.getImageUrl1())
                    .transform(new CenterCrop())
                    .into(holder.imgPlace);
        }

        // 🚀 1. فحص حالة المكان: هل هو بالمفضلة أم لا؟
        if (userId != null && place.getName() != null) {
            DocumentReference favRef = db.collection("Users").document(userId)
                    .collection("Favorites").document(place.getName());

            favRef.get().addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    holder.imgHeart.setImageResource(R.drawable.ic_heart_filled);
                    // يمكنك حفظ الحالة في عنصر الواجهة لتسهيل التبديل
                    holder.btnFav.setTag(true);
                } else {
                    holder.imgHeart.setImageResource(R.drawable.ic_heart_outline);
                    holder.btnFav.setTag(false);
                }
            });

            // 🚀 2. برمجة الضغط على زر القلب
            holder.btnFav.setOnClickListener(v -> {
                Boolean isFav = (Boolean) holder.btnFav.getTag();
                if (isFav != null && isFav) {
                    // إزالة من المفضلة
                    favRef.delete().addOnSuccessListener(aVoid -> {
                        holder.imgHeart.setImageResource(R.drawable.ic_heart_outline);
                        holder.btnFav.setTag(false);
                    });
                } else {
                    // إضافة للمفضلة
                    java.util.Map<String, Object> data = new java.util.HashMap<>();
                    data.put("savedAt", System.currentTimeMillis());
                    favRef.set(data).addOnSuccessListener(aVoid -> {
                        holder.imgHeart.setImageResource(R.drawable.ic_heart_filled);
                        holder.btnFav.setTag(true);
                    });
                }
            });
        }

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(v.getContext(), PlaceDetailsActivity.class);
            intent.putExtra("PLACE_NAME", place.getName());
            v.getContext().startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return places.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvCategory;
        ImageView imgPlace, imgHeart;
        View btnFav;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvNameExplore);
            tvCategory = itemView.findViewById(R.id.tvCategoryExplore);
            imgPlace = itemView.findViewById(R.id.imgPlaceExplore);
            btnFav = itemView.findViewById(R.id.btnExploreFavorite);
            imgHeart = itemView.findViewById(R.id.imgExploreHeart);
        }
    }
}