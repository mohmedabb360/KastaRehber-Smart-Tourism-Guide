package com.example.turist_rehberi;

import android.content.Intent;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;
import java.util.Locale;

public class TimelineAdapter extends RecyclerView.Adapter<TimelineAdapter.ViewHolder> {

    private List<PlaceModel> places;
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private String userId;
    private int currentDayIndex = 0;

    private OnProgressChangeListener progressChangeListener;

    public interface OnProgressChangeListener {
        void onProgressUpdated(int dayIndex, String placeName, boolean isVisited);
    }

    public TimelineAdapter(List<PlaceModel> places, OnProgressChangeListener listener) {
        this.places = places;
        this.progressChangeListener = listener;
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        }
    }

    public void setCurrentDayIndex(int dayIndex) {
        this.currentDayIndex = dayIndex;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_timeline_card, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        PlaceModel place = places.get(position);

        if (place.getName() != null) holder.tvPlaceName.setText(place.getName());
        if (place.getCategory() != null) holder.tvCategory.setText(place.getCategory());

        holder.tvTime.setText(calculateStartTime(position));
        holder.tvDuration.setText(place.getDurationMinutes() + " Dakika");

        // 🚀 الإصلاح الحاسم لمنع تداخل الصور وظهور صورة القلعة بشكل عشوائي عند السكرول:
        // نقوم أولاً بمسح الكاش القديم الملتصق بالـ ViewHolder المعاد استخدامه
        Glide.with(holder.itemView.getContext()).clear(holder.imgPlace);

        // 🚀 تنظيف كاش الصورة القديمة لمنع التداخل
        Glide.with(holder.itemView.getContext()).clear(holder.imgPlace);

        if (place.getImageUrl1() != null && !place.getImageUrl1().isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(place.getImageUrl1())
                    .transform(new CenterCrop())
                    .placeholder(new android.graphics.drawable.ColorDrawable(Color.parseColor("#E0E0E0"))) // 🚀 لون رمادي ناعم أثناء التحميل بدلاً من القلعة
                    .error(new android.graphics.drawable.ColorDrawable(Color.parseColor("#E0E0E0")))       // 🚀 لون رمادي ناعم بحال فشل النت
                    .into(holder.imgPlace);
        } else {
            // إذا المعلم لا يملك صورة نهائياً، نضع نفس اللون الرمادي الناعم لمنع التشوه
            holder.imgPlace.setImageDrawable(new android.graphics.drawable.ColorDrawable(Color.parseColor("#E0E0E0")));
        }

        // 1. نظام المفضلات (القلب)
        if (userId != null && place.getName() != null) {
            DocumentReference favRef = db.collection("Users").document(userId)
                    .collection("Favorites").document(place.getName());

            favRef.get().addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    holder.imgTimelineHeart.setImageResource(R.drawable.ic_heart_filled);
                    holder.btnTimelineFavorite.setTag(true);
                } else {
                    holder.imgTimelineHeart.setImageResource(R.drawable.ic_heart_outline);
                    holder.btnTimelineFavorite.setTag(false);
                }
            });

            holder.btnTimelineFavorite.setOnClickListener(v -> {
                Boolean isFav = (Boolean) holder.btnTimelineFavorite.getTag();
                if (isFav != null && isFav) {
                    favRef.delete().addOnSuccessListener(aVoid -> {
                        holder.imgTimelineHeart.setImageResource(R.drawable.ic_heart_outline);
                        holder.btnTimelineFavorite.setTag(false);
                    });
                } else {
                    java.util.Map<String, Object> data = new java.util.HashMap<>();
                    data.put("savedAt", System.currentTimeMillis());
                    favRef.set(data).addOnSuccessListener(aVoid -> {
                        holder.imgTimelineHeart.setImageResource(R.drawable.ic_heart_filled);
                        holder.btnTimelineFavorite.setTag(true);
                    });
                }
            });
        }

        // 2. نظام تتبع الزيارة (الصح) والشفافية
        if (place.isVisited()) {
            holder.imgTimelineVisited.setColorFilter(Color.parseColor("#4CAF50"));
            holder.mainTimelineCard.setAlpha(0.6f);
        } else {
            holder.imgTimelineVisited.setColorFilter(Color.parseColor("#BDBDBD"));
            holder.mainTimelineCard.setAlpha(1.0f);
        }

        holder.btnTimelineVisited.setOnClickListener(v -> {
            v.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY, android.view.HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING);
            boolean newState = !place.isVisited();
            place.setVisited(newState);
            notifyItemChanged(position);

            if (progressChangeListener != null && place.getName() != null) {
                progressChangeListener.onProgressUpdated(currentDayIndex, place.getName(), newState);
            }
        });

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

    private String calculateStartTime(int position) {
        int totalMinutes = 540;
        for (int i = 0; i < position; i++) {
            totalMinutes += places.get(i).getDurationMinutes() + 30;
        }
        int hours = (totalMinutes / 60) % 24;
        int minutes = totalMinutes % 60;
        return String.format(Locale.US, "%02d:%02d", hours, minutes);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvPlaceName, tvCategory, tvTime, tvDuration;
        ImageView imgPlace, imgTimelineHeart, imgTimelineVisited;
        View btnTimelineFavorite, btnTimelineVisited;
        MaterialCardView mainTimelineCard;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvPlaceName = itemView.findViewById(R.id.tvPlaceName);
            tvCategory = itemView.findViewById(R.id.tvCategory);
            tvTime = itemView.findViewById(R.id.tvTime);
            tvDuration = itemView.findViewById(R.id.tvDuration);
            imgPlace = itemView.findViewById(R.id.imgPlace);
            mainTimelineCard = itemView.findViewById(R.id.mainTimelineCard);
            btnTimelineFavorite = itemView.findViewById(R.id.btnTimelineFavorite);
            imgTimelineHeart = itemView.findViewById(R.id.imgTimelineHeart);
            btnTimelineVisited = itemView.findViewById(R.id.btnTimelineVisited);
            imgTimelineVisited = itemView.findViewById(R.id.imgTimelineVisited);
        }
    }
}