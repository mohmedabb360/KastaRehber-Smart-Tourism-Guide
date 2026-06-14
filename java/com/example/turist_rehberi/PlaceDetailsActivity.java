package com.example.turist_rehberi;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.List;

public class PlaceDetailsActivity extends AppCompatActivity {

    private ViewPager2 viewPagerPlaceImages;
    private TabLayout tabLayoutDots;
    private MaterialCardView btnBack;

    private View btnDetailsFavorite;
    private ImageView imgHeartDetails;
    private boolean isFavorite = false;

    private TextView tvCategoryDetail, tvPlaceNameDetail, tvDurationDetail, tvDescription;
    private TextView tvEffortLevel, tvBudget, tvIdealTime;
    private MaterialButton btnNavigate;

    private FirebaseFirestore db;
    private String placeName;

    private List<String> placeImages = new ArrayList<>();
    private ImageSliderAdapter sliderAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_place_details);

        viewPagerPlaceImages = findViewById(R.id.viewPagerPlaceImages);
        tabLayoutDots = findViewById(R.id.tabLayoutDots);
        btnBack = findViewById(R.id.btnBack);

        btnDetailsFavorite = findViewById(R.id.btnDetailsFavorite);
        imgHeartDetails = findViewById(R.id.imgHeartDetails);

        tvCategoryDetail = findViewById(R.id.tvCategoryDetail);
        tvPlaceNameDetail = findViewById(R.id.tvPlaceNameDetail);
        tvDurationDetail = findViewById(R.id.tvDurationDetail);
        tvDescription = findViewById(R.id.tvDescription);
        tvEffortLevel = findViewById(R.id.tvEffortLevel);
        tvBudget = findViewById(R.id.tvBudget);
        tvIdealTime = findViewById(R.id.tvIdealTime);
        btnNavigate = findViewById(R.id.btnNavigate);

        sliderAdapter = new ImageSliderAdapter(this, placeImages);
        viewPagerPlaceImages.setAdapter(sliderAdapter);

        new TabLayoutMediator(tabLayoutDots, viewPagerPlaceImages,
                (tab, position) -> { }
        ).attach();

        db = FirebaseFirestore.getInstance();

        placeName = getIntent().getStringExtra("PLACE_NAME");
        if (placeName != null) {
            tvPlaceNameDetail.setText(placeName);
            fetchPlaceDetails(placeName);
            checkIfFavorite();
        }

        btnBack.setOnClickListener(v -> finish());
        btnDetailsFavorite.setOnClickListener(v -> toggleFavorite());

        btnNavigate.setOnClickListener(v -> {
            Uri gmmIntentUri = Uri.parse("geo:0,0?q=" + Uri.encode(placeName + ", Kastamonu"));
            Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
            mapIntent.setPackage("com.google.android.apps.maps");
            if (mapIntent.resolveActivity(getPackageManager()) != null) startActivity(mapIntent);
        });
    }

    private void checkIfFavorite() {
        if (placeName == null || FirebaseAuth.getInstance().getCurrentUser() == null) return;

        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        db.collection("Users").document(userId)
                .collection("Favorites").document(placeName)
                .get().addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        isFavorite = true;
                        imgHeartDetails.setImageResource(R.drawable.ic_heart_filled);
                        imgHeartDetails.setImageTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#E53935")));
                    } else {
                        isFavorite = false;
                        imgHeartDetails.setImageResource(R.drawable.ic_heart_outline);
                        imgHeartDetails.setImageTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#888888")));
                    }
                });
    }

    // 🚀 دالة تبديل الحفظ مع تصحيح استدعات القلوب المخصصة ومنع ظهور النجوم
    private void toggleFavorite() {
        if (placeName == null || FirebaseAuth.getInstance().getCurrentUser() == null) return;

        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DocumentReference favRef = db.collection("Users").document(userId)
                .collection("Favorites").document(placeName);

        if (isFavorite) {
            // مسح من المفضلة وتلوين للرمادي (استدعاء القلب الفارغ)
            favRef.delete().addOnSuccessListener(aVoid -> {
                isFavorite = false;
                imgHeartDetails.setImageResource(R.drawable.ic_heart_outline);
                imgHeartDetails.setImageTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#888888")));
                Toast.makeText(this, "Favorilerden çıkarıldı", Toast.LENGTH_SHORT).show();
            });
        } else {
            // إضافة للمفضلة وتلوين للأحمر (استدعاء القلب المضلل)
            java.util.Map<String, Object> data = new java.util.HashMap<>();
            data.put("savedAt", System.currentTimeMillis());
            favRef.set(data).addOnSuccessListener(aVoid -> {
                isFavorite = true;
                imgHeartDetails.setImageResource(R.drawable.ic_heart_filled);
                imgHeartDetails.setImageTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#E53935")));
                Toast.makeText(this, "Favorilere eklendi", Toast.LENGTH_SHORT).show();
            });
        }
    }

    private void fetchPlaceDetails(String name) {
        db.collection("Places")
                .whereEqualTo("name", name)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        try {
                            QueryDocumentSnapshot document = (QueryDocumentSnapshot) queryDocumentSnapshots.getDocuments().get(0);
                            PlaceModel place = document.toObject(PlaceModel.class);

                            if (tvCategoryDetail != null && place.getCategory() != null) tvCategoryDetail.setText(place.getCategory().toUpperCase());
                            if (tvDescription != null && place.getDescription() != null) tvDescription.setText(place.getDescription());
                            if (tvDurationDetail != null) tvDurationDetail.setText(place.getDurationMinutes() + " Dak.");
                            if (tvBudget != null && place.getBudget() != null) tvBudget.setText(place.getBudget());

                            if (tvIdealTime != null && place.getIdealTime() != null) {
                                tvIdealTime.setText(place.getIdealTime());
                            }

                            if (tvEffortLevel != null) {
                                String effortText = "Normal Efor";
                                if (place.getEffortLevel() == 1) effortText = "Hafif Efor";
                                else if (place.getEffortLevel() == 3) effortText = "Yüksek Efor";
                                tvEffortLevel.setText(effortText);
                            }

                            placeImages.clear();
                            if (place.getImageUrl1() != null && !place.getImageUrl1().isEmpty()) placeImages.add(place.getImageUrl1());
                            if (place.getImageUrl2() != null && !place.getImageUrl2().isEmpty()) placeImages.add(place.getImageUrl2());
                            if (place.getImageUrl3() != null && !place.getImageUrl3().isEmpty()) placeImages.add(place.getImageUrl3());

                            if (sliderAdapter != null) {
                                sliderAdapter.notifyDataSetChanged();
                            }

                        } catch (Exception e) {
                            Toast.makeText(this, "Veri hatası: " + e.getMessage(), Toast.LENGTH_LONG).show();
                            e.printStackTrace();
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Bağlantı hatası: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}