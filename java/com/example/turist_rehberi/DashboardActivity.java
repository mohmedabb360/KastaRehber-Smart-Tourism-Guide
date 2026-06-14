package com.example.turist_rehberi;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.SetOptions;

import nl.dionsegijn.konfetti.KonfettiView;
import nl.dionsegijn.konfetti.models.Shape;
import nl.dionsegijn.konfetti.models.Size;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class DashboardActivity extends AppCompatActivity implements OnMapReadyCallback {

    private View topBar, planBottomSheet, mapView;
    private FrameLayout fragmentContainer;

    private LinearLayout btnNavExplore, btnNavPlan, btnNavAI, btnNavFavorites, btnNavProfile;
    private ImageView icNavExplore, icNavPlan, icNavAI, icNavFavorites, icNavProfile;
    private TextView tvNavExplore, tvNavPlan, tvNavAI, tvNavFavorites, tvNavProfile;

    private TextView tvProgressPercent;
    private LinearProgressIndicator progressBarTrip;
    private KonfettiView konfettiView;

    private RecyclerView rvTimeline;
    private TimelineAdapter adapter;
    private FirebaseFirestore db;
    private GoogleMap mMap;

    private LinearLayout layoutDaysTabs;

    private List<List<PlaceModel>> allDaysPlan = new ArrayList<>();
    private List<PlaceModel> currentDisplayList = new ArrayList<>();

    private ArrayList<String> userInterests = new ArrayList<>();
    private int prefDays = 3;
    private final int MAX_MINUTES_PER_DAY = 420;

    // 🚀 تعديل الميزانية: الاعتماد على الميزانية الشاملة المستمرة وإلغاء ليميت اليوم الواحد
    private double totalBudget = 0.0;
    private double remainingGlobalBudget = 0.0;

    private int adultCount = 2;
    private int childCount = 0;
    private String groupType = "Ailece";
    private int maxDailyEffort = 5;

    private boolean isNewPlanRequested = false;
    private boolean isAnimationRunning = false;
    private int currentActiveDayIndex = 0;

    private List<Boolean> animatedDaysTracker = new ArrayList<>();
    private boolean shouldAnimateMap = false;

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.activity_dashboard);

        topBar = findViewById(R.id.topBar);
        planBottomSheet = findViewById(R.id.planBottomSheet);
        mapView = findViewById(R.id.map);
        fragmentContainer = findViewById(R.id.fragmentContainer);

        btnNavExplore = findViewById(R.id.btnNavExplore);
        btnNavPlan = findViewById(R.id.btnNavPlan);
        btnNavAI = findViewById(R.id.btnNavAI);
        btnNavFavorites = findViewById(R.id.btnNavFavorites);
        btnNavProfile = findViewById(R.id.btnNavProfile);

        icNavExplore = findViewById(R.id.icNavExplore);
        icNavPlan = findViewById(R.id.icNavPlan);
        icNavAI = findViewById(R.id.icNavAI);
        icNavFavorites = findViewById(R.id.icNavFavorites);
        icNavProfile = findViewById(R.id.icNavProfile);

        tvNavExplore = findViewById(R.id.tvNavExplore);
        tvNavPlan = findViewById(R.id.tvNavPlan);
        tvNavAI = findViewById(R.id.tvNavAI);
        tvNavFavorites = findViewById(R.id.tvNavFavorites);
        tvNavProfile = findViewById(R.id.tvNavProfile);

        tvProgressPercent = findViewById(R.id.tvProgressPercent);
        progressBarTrip = findViewById(R.id.progressBarTrip);
        konfettiView = findViewById(R.id.konfettiView);

        layoutDaysTabs = findViewById(R.id.layoutDaysTabs);

        db = FirebaseFirestore.getInstance();

        setupBottomNavigation();
        receiveData();
        setupLimits();

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) mapFragment.getMapAsync(this);

        rvTimeline = findViewById(R.id.rvTimeline);
        rvTimeline.setLayoutManager(new LinearLayoutManager(this));
        rvTimeline.setNestedScrollingEnabled(false);

        adapter = new TimelineAdapter(currentDisplayList, new TimelineAdapter.OnProgressChangeListener() {
            @Override
            public void onProgressUpdated(int dayIndex, String placeName, boolean isVisited) {
                if (currentActiveDayIndex >= 0 && currentActiveDayIndex < allDaysPlan.size()) {
                    for (PlaceModel p : allDaysPlan.get(currentActiveDayIndex)) {
                        if (p.getName() != null && p.getName().equals(placeName)) {
                            p.setVisited(isVisited);
                            break;
                        }
                    }
                }
                calculateTotalProgress();
                if (!isAnimationRunning) {
                    savePlanToFirebaseSilently();
                }
            }
        });
        rvTimeline.setAdapter(adapter);

        if (isNewPlanRequested) {
            loadAndProcessPlaces();
            getIntent().removeExtra("SELECTED_INTERESTS");
            isNewPlanRequested = false;
        } else {
            checkIfPlanExistsInFirebase();
        }
    }

    private void checkIfPlanExistsInFirebase() {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            loadAndProcessPlaces();
            return;
        }

        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        db.collection("Users").document(userId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists() && documentSnapshot.get("daysData") != null) {
                        try {
                            List<Map<String, Object>> daysData = (List<Map<String, Object>>) documentSnapshot.get("daysData");

                            if (daysData.isEmpty()) {
                                loadAndProcessPlaces();
                                return;
                            }

                            allDaysPlan.clear();
                            for (Map<String, Object> dayMap : daysData) {
                                List<Map<String, Object>> placesListMap = (List<Map<String, Object>>) dayMap.get("places");
                                List<PlaceModel> singleDayPlan = new ArrayList<>();
                                if (placesListMap != null) {
                                    for (Map<String, Object> pMap : placesListMap) {
                                        PlaceModel place = new PlaceModel();
                                        place.setName((String) pMap.get("name"));
                                        place.setCategory((String) pMap.get("category"));
                                        place.setDescription((String) pMap.get("description"));
                                        place.setBudget((String) pMap.get("budget"));
                                        place.setImageUrl1((String) pMap.get("imageUrl1"));

                                        if (pMap.get("lat") != null) place.setLat(((Number) pMap.get("lat")).doubleValue());
                                        if (pMap.get("lng") != null) place.setLng(((Number) pMap.get("lng")).doubleValue());
                                        if (pMap.get("durationMinutes") != null) place.setDurationMinutes(((Number) pMap.get("durationMinutes")).intValue());
                                        if (pMap.get("effortLevel") != null) place.setEffortLevel(((Number) pMap.get("effortLevel")).intValue());

                                        if (pMap.containsKey("visited") && pMap.get("visited") != null) {
                                            place.setVisited((Boolean) pMap.get("visited"));
                                        } else {
                                            place.setVisited(false);
                                        }
                                        singleDayPlan.add(place);
                                    }
                                }
                                allDaysPlan.add(singleDayPlan);
                            }

                            shouldAnimateMap = false;
                            animatedDaysTracker.clear();
                            for (int i = 0; i < allDaysPlan.size(); i++) {
                                animatedDaysTracker.add(true);
                            }

                            setupDynamicDayTabs();
                            calculateTotalProgress();

                            if (!allDaysPlan.isEmpty()) {
                                currentActiveDayIndex = 0;
                                showDayPlan(0);
                            }

                        } catch (Exception e) {
                            loadAndProcessPlaces();
                        }
                    } else {
                        loadAndProcessPlaces();
                    }
                })
                .addOnFailureListener(e -> loadAndProcessPlaces());
    }

    private void savePlanToFirebaseSilently() {
        if (FirebaseAuth.getInstance().getCurrentUser() == null || allDaysPlan.isEmpty()) return;
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        List<Map<String, Object>> daysData = new ArrayList<>();
        for (int i = 0; i < allDaysPlan.size(); i++) {
            Map<String, Object> dayMap = new HashMap<>();
            List<Map<String, Object>> placesMapList = new ArrayList<>();
            for (PlaceModel p : allDaysPlan.get(i)) {
                Map<String, Object> pMap = new HashMap<>();
                pMap.put("name", p.getName());
                pMap.put("category", p.getCategory());
                pMap.put("description", p.getDescription());
                pMap.put("budget", p.getBudget());
                pMap.put("lat", p.getLat());
                pMap.put("lng", p.getLng());
                pMap.put("durationMinutes", p.getDurationMinutes());
                pMap.put("effortLevel", p.getEffortLevel());
                pMap.put("visited", p.isVisited());
                pMap.put("imageUrl1", p.getImageUrl1());
                placesMapList.add(pMap);
            }
            dayMap.put("dayNumber", i + 1);
            dayMap.put("places", placesMapList);
            daysData.add(dayMap);
        }

        Map<String, Object> updateMap = new HashMap<>();
        updateMap.put("daysData", daysData);

        db.collection("Users").document(userId).set(updateMap, SetOptions.merge());
    }

    private void calculateTotalProgress() {
        int totalPlaces = 0;
        int visitedCount = 0;

        for (List<PlaceModel> dayPlan : allDaysPlan) {
            totalPlaces += dayPlan.size();
            for (PlaceModel p : dayPlan) {
                if (p.isVisited()) visitedCount++;
            }
        }

        if (totalPlaces == 0) return;

        int progressPercentage = (int) (((float) visitedCount / totalPlaces) * 100);
        progressBarTrip.setProgress(progressPercentage);
        tvProgressPercent.setText("%" + progressPercentage);

        if (progressPercentage == 100) {
            tvProgressPercent.performHapticFeedback(android.view.HapticFeedbackConstants.CONFIRM);
            Toast.makeText(this, "Tebrikler! Tüm rotayı başarıyla tamamladınız! 🎉", Toast.LENGTH_LONG).show();

            konfettiView.build()
                    .addColors(Color.parseColor("#C5A059"), Color.parseColor("#052012"), Color.WHITE)
                    .setDirection(0.0, 359.0)
                    .setSpeed(1f, 5f)
                    .setFadeOutEnabled(true)
                    .setTimeToLive(2000L)
                    .addShapes(Shape.Square.INSTANCE, Shape.Circle.INSTANCE)
                    .addSizes(new Size(12, 5f))
                    .setPosition(-50f, konfettiView.getWidth() + 50f, -50f, -50f)
                    .streamFor(300, 3000L);
        }
    }

    private void setupBottomNavigation() {
        btnNavExplore.setOnClickListener(v -> {
            updateNavColors(1);
            topBar.setVisibility(View.GONE);
            planBottomSheet.setVisibility(View.GONE);
            mapView.setVisibility(View.GONE);
            fragmentContainer.setVisibility(View.VISIBLE);
            getSupportFragmentManager().beginTransaction().replace(R.id.fragmentContainer, new ExploreFragment()).commit();
        });

        btnNavPlan.setOnClickListener(v -> {
            updateNavColors(2);
            fragmentContainer.setVisibility(View.GONE);
            topBar.setVisibility(View.VISIBLE);
            planBottomSheet.setVisibility(View.VISIBLE);
            mapView.setVisibility(View.VISIBLE);
        });

        btnNavAI.setOnClickListener(v -> {
            updateNavColors(3);
            topBar.setVisibility(View.GONE);
            planBottomSheet.setVisibility(View.GONE);
            mapView.setVisibility(View.GONE);
            fragmentContainer.setVisibility(View.VISIBLE);
            getSupportFragmentManager().beginTransaction().replace(R.id.fragmentContainer, new AiFragment()).commit();
        });

        btnNavFavorites.setOnClickListener(v -> {
            updateNavColors(4);
            topBar.setVisibility(View.GONE);
            planBottomSheet.setVisibility(View.GONE);
            mapView.setVisibility(View.GONE);
            fragmentContainer.setVisibility(View.VISIBLE);
            getSupportFragmentManager().beginTransaction().replace(R.id.fragmentContainer, new FavoritesFragment()).commit();
        });

        btnNavProfile.setOnClickListener(v -> {
            updateNavColors(5);
            topBar.setVisibility(View.GONE);
            planBottomSheet.setVisibility(View.GONE);
            mapView.setVisibility(View.GONE);
            fragmentContainer.setVisibility(View.VISIBLE);
            getSupportFragmentManager().beginTransaction().replace(R.id.fragmentContainer, new ProfileFragment()).commit();
        });
    }

    private void updateNavColors(int selectedTab) {
        int colorGold = Color.parseColor("#C5A059");
        int colorGrey = Color.parseColor("#9E9E9E");

        icNavExplore.setColorFilter(colorGrey); tvNavExplore.setTextColor(colorGrey);
        icNavPlan.setColorFilter(colorGrey); tvNavPlan.setTextColor(colorGrey);
        icNavAI.setColorFilter(colorGrey); tvNavAI.setTextColor(colorGrey);
        icNavFavorites.setColorFilter(colorGrey); tvNavFavorites.setTextColor(colorGrey);
        icNavProfile.setColorFilter(colorGrey); tvNavProfile.setTextColor(colorGrey);

        if (selectedTab == 1) { icNavExplore.setColorFilter(colorGold); tvNavExplore.setTextColor(colorGold); }
        else if (selectedTab == 2) { icNavPlan.setColorFilter(colorGold); tvNavPlan.setTextColor(colorGold); }
        else if (selectedTab == 3) { icNavAI.setColorFilter(colorGold); tvNavAI.setTextColor(colorGold); }
        else if (selectedTab == 4) { icNavFavorites.setColorFilter(colorGold); tvNavFavorites.setTextColor(colorGold); }
        else if (selectedTab == 5) { icNavProfile.setColorFilter(colorGold); tvNavProfile.setTextColor(colorGold); }
    }

    private void receiveData() {
        Intent intent = getIntent();
        if (intent == null) return;

        prefDays = intent.getIntExtra("PREF_DAYS", 3);
        adultCount = intent.getIntExtra("ADULT_COUNT", 2);
        childCount = intent.getIntExtra("CHILD_COUNT", 0);
        groupType = intent.getStringExtra("GROUP_TYPE");
        if (groupType == null) groupType = "Ailece";

        isNewPlanRequested = intent.hasExtra("SELECTED_INTERESTS");
        if (isNewPlanRequested) {
            shouldAnimateMap = true;
        }

        ArrayList<String> interests = intent.getStringArrayListExtra("SELECTED_INTERESTS");
        if (interests != null) {
            userInterests.clear();
            userInterests.addAll(interests);
        }

        String budgetStr = intent.getStringExtra("PREF_BUDGET");
        if (budgetStr != null) {
            String cleanBudget = budgetStr.replaceAll("[^0-9]", "");
            if (!cleanBudget.isEmpty()) {
                totalBudget = Double.parseDouble(cleanBudget);
                remainingGlobalBudget = totalBudget; // تهيئة الميزانية المستمرة
            }
        }
    }

    private void setupLimits() {
        if (childCount > 0 || groupType.equals("Ailece")) maxDailyEffort = 4;
        else if (groupType.equals("Arkadaş Grubu") || groupType.equals("Yalnız")) maxDailyEffort = 7;
        else maxDailyEffort = 5;
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        LatLng kastamonu = new LatLng(41.3766, 33.7765);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(kastamonu, 13f));
    }

    private double getEstimatedPrice(String budgetLevel) {
        if (budgetLevel == null) return 0.0;
        switch (budgetLevel) {
            case "Ücretsiz": return 0.0;
            case "Hafif": return 150.0;
            case "Orta": return 400.0;
            case "Lüks": return 1200.0;
            default: return 0.0;
        }
    }

    private double calculateGroupCost(PlaceModel place) {
        double adultPrice = getEstimatedPrice(place.getBudget());
        if (adultPrice == 0) return 0.0;
        return (adultPrice * adultCount) + ((adultPrice * 0.5) * childCount);
    }

    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        if (lat1 == 0 || lon1 == 0 || lat2 == 0 || lon2 == 0) return 0;
        final int R = 6371;
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        return R * (2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a)));
    }

    private void loadAndProcessPlaces() {
        db.collection("Places").get().addOnSuccessListener(queryDocumentSnapshots -> {
            List<PlaceModel> allPlaces = new ArrayList<>();
            for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                allPlaces.add(document.toObject(PlaceModel.class));
            }
            if (!allPlaces.isEmpty()) {
                java.util.Collections.shuffle(allPlaces);
                List<PlaceModel> filteredPlaces = phaseOneFilter(allPlaces);
                distributePlacesSmartly(filteredPlaces);
            }
        });
    }

    private List<PlaceModel> phaseOneFilter(List<PlaceModel> allPlaces) {
        if (userInterests.isEmpty()) return allPlaces;
        List<PlaceModel> validPlaces = new ArrayList<>();
        for (PlaceModel place : allPlaces) {
            if (userInterests.contains(place.getCategory())) {
                validPlaces.add(place);
            }
        }
        return validPlaces.isEmpty() ? allPlaces : validPlaces;
    }

    private void distributePlacesSmartly(List<PlaceModel> filteredPlaces) {
        List<PlaceModel> unvisited = new ArrayList<>(filteredPlaces);
        allDaysPlan.clear();

        int actualCreatedDays = 0;

        for (int i = 0; i < prefDays; i++) {
            if (unvisited.isEmpty()) break;

            List<PlaceModel> dailyPlan = buildGeographicalDayPlan(unvisited);

            // 🚀 آلية الاختصار الذكي: إذا انتهت الميزانية المستمرة أو فرغت الأماكن ولم يستطع اليوم استيعاب أي مكان، ننهي الحلقة فوراً
            if (!dailyPlan.isEmpty()) {
                allDaysPlan.add(dailyPlan);
                actualCreatedDays++;
            } else {
                break;
            }
        }

        // 🚀 فحص الأمان الشامل: لو انتهت الحسابات بـ 0 مكان للرحلة كاملة بسبب تضارب القيود، نطلق الـ Dialog التوضيحي وننهي الواجهة
        if (allDaysPlan.isEmpty()) {
            showPlanFailedDialog();
            return;
        }

        // 🚀 إخطار المستخدم بـ Toast احترافي بالتركي إذا تم اختصار عدد الأيام الفعلي لعدم كفاية الأماكن أو الميزانية الشاملة
        if (actualCreatedDays < prefDays) {
            Toast.makeText(this, "Tercihlerinize göre en optimize plan " + actualCreatedDays + " gün olarak oluşturuldu.", Toast.LENGTH_LONG).show();
        }

        animatedDaysTracker.clear();
        for (int i = 0; i < allDaysPlan.size(); i++) {
            animatedDaysTracker.add(false);
        }

        setupDynamicDayTabs();
        calculateTotalProgress();

        if (!allDaysPlan.isEmpty()) {
            currentActiveDayIndex = 0;
            showDayPlan(0);
        }

        savePlanToFirebaseSilently();
    }

    // 🚀 رسالة الفشل الشاملة والمنسقة هندسياً لتوجيه المستخدم بلغة النظام LTR
    private void showPlanFailedDialog() {
        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle("Plan Oluşturulamadı")
                .setMessage("Girdiğiniz bütçe, seçtiğiniz kategoriler veya efor kısıtlamalarınız birbiriyle uyuşmamaktadır. Lütfen tercihlerinizi değiştirerek tekrar deneyiniz.")
                .setCancelable(false)
                .setPositiveButton("Tamam", (d, which) -> {
                    d.dismiss();
                    Intent intent = new Intent(DashboardActivity.this, SmartWizardActivity.class);
                    startActivity(intent);
                    finish();
                })
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().getDecorView().setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
        }
        dialog.show();
    }

    private void setupDynamicDayTabs() {
        layoutDaysTabs.removeAllViews();

        int marginPx = (int) (8 * getResources().getDisplayMetrics().density);
        int paddingVPx = (int) (10 * getResources().getDisplayMetrics().density);
        int paddingHPx = (int) (16 * getResources().getDisplayMetrics().density);

        int totalDays = allDaysPlan.size();
        boolean stretchTabs = totalDays <= 3;

        for (int i = 0; i < totalDays; i++) {
            TextView tab = new TextView(this);
            LinearLayout.LayoutParams params;

            if (stretchTabs) {
                params = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
            } else {
                params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            }

            if (i < totalDays - 1) {
                params.setMargins(0, 0, marginPx, 0);
            } else {
                params.setMargins(0, 0, 0, 0);
            }

            tab.setLayoutParams(params);
            tab.setPadding(paddingHPx, paddingVPx, paddingHPx, paddingVPx);
            tab.setText((i + 1) + ". Gün");
            tab.setTextSize(14f);
            tab.setTypeface(null, android.graphics.Typeface.BOLD);
            tab.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);

            tab.setBackgroundColor(Color.parseColor("#F2F5F3"));
            tab.setTextColor(Color.parseColor("#052012"));

            final int index = i;
            tab.setOnClickListener(v -> showDayPlan(index));

            layoutDaysTabs.addView(tab);
        }
    }

    private void showDayPlan(int index) {
        currentActiveDayIndex = index;

        for (int i = 0; i < layoutDaysTabs.getChildCount(); i++) {
            TextView tab = (TextView) layoutDaysTabs.getChildAt(i);
            if (i == index) {
                tab.setBackgroundColor(Color.parseColor("#052012"));
                tab.setTextColor(Color.WHITE);
            } else {
                tab.setBackgroundColor(Color.parseColor("#F2F5F3"));
                tab.setTextColor(Color.parseColor("#052012"));
            }
        }

        if (adapter != null) {
            adapter.setCurrentDayIndex(index);
        }

        List<PlaceModel> targetList = allDaysPlan.get(index);

        if (index < animatedDaysTracker.size() && animatedDaysTracker.get(index)) {
            shouldAnimateMap = false;
        } else {
            shouldAnimateMap = true;
            if (index < animatedDaysTracker.size()) {
                animatedDaysTracker.set(index, true);
            }
        }

        currentDisplayList.clear();
        if (!shouldAnimateMap) {
            currentDisplayList.addAll(targetList);
        }

        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }

        updateMapAndAnimate(targetList);
    }

    private List<PlaceModel> buildGeographicalDayPlan(List<PlaceModel> unvisited) {
        List<PlaceModel> dayPlan = new ArrayList<>();
        int remainingTime = MAX_MINUTES_PER_DAY;
        int currentDayEffort = 0;

        if (unvisited.isEmpty()) return dayPlan;

        // 🚀 التعديل المعتمد: إخضاع المكان الأول (Anchor) لفحص قيد الميزانية المستمرة والتناقضات كأي مكان آخر
        PlaceModel anchor = getAnchorPlace(unvisited);
        if (anchor != null) {
            dayPlan.add(anchor);
            unvisited.remove(anchor);

            remainingTime -= (anchor.getDurationMinutes() + 30);
            remainingGlobalBudget -= calculateGroupCost(anchor); // الخصم الفعلي من الميزانية الشاملة
            currentDayEffort += anchor.getEffortLevel();

            PlaceModel currentLocation = anchor;

            while (remainingTime > 60 && !unvisited.isEmpty()) {
                // نمرر الميزانية الكلية المستمرة كمحدد مالي مباشر للدوران والتحقق
                PlaceModel nextPlace = findNearestBestPlace(currentLocation, unvisited, remainingTime, remainingGlobalBudget, currentDayEffort);

                if (nextPlace == null) {
                    nextPlace = findNearestFreeFiller(currentLocation, unvisited, remainingTime, currentDayEffort);
                    if (nextPlace == null) break;
                }

                dayPlan.add(nextPlace);
                unvisited.remove(nextPlace);

                remainingTime -= (nextPlace.getDurationMinutes() + 30);
                remainingGlobalBudget -= calculateGroupCost(nextPlace); // خصم مستمر ومتصل
                currentDayEffort += nextPlace.getEffortLevel();

                currentLocation = nextPlace;
            }
        }
        return dayPlan;
    }

    private PlaceModel getAnchorPlace(List<PlaceModel> unvisited) {
        PlaceModel bestAnchor = null;
        for (PlaceModel p : unvisited) {
            if (childCount > 0 && p.getEffortLevel() == 3) continue;

            // 🚀 الإصلاح الحاسم: فحص الميزانية الشاملة المستمرة للمكان الأول قبل اعتماده لمنع تخطي جيب المستخدم
            if (calculateGroupCost(p) > remainingGlobalBudget) continue;

            if (bestAnchor == null) { bestAnchor = p; continue; }

            boolean isMorning = "Sabah".equals(p.getIdealTime());
            boolean isCurrentMorning = "Sabah".equals(bestAnchor.getIdealTime());

            if (isMorning && !isCurrentMorning) {
                bestAnchor = p;
            } else if (isMorning && isCurrentMorning && p.getEffortLevel() > bestAnchor.getEffortLevel()) {
                bestAnchor = p;
            }
        }
        return bestAnchor;
    }

    private PlaceModel findNearestBestPlace(PlaceModel currentLoc, List<PlaceModel> unvisited, int remTime, double remGlobalBudget, int currentEffort) {
        PlaceModel bestPlace = null;
        double minDistance = Double.MAX_VALUE;

        for (PlaceModel p : unvisited) {
            int timeNeeded = p.getDurationMinutes() + 30;
            double groupCost = calculateGroupCost(p);

            boolean fitsTime = timeNeeded <= remTime;
            // 🚀 مقارنة المجموعة بالميزانية الشاملة المتبقية مباشرة
            boolean fitsBudget = groupCost <= remGlobalBudget;
            boolean fitsEffort = (currentEffort + p.getEffortLevel()) <= maxDailyEffort;
            boolean safeForKids = (childCount == 0) || (p.getEffortLevel() < 3);

            if (fitsTime && fitsBudget && fitsEffort && safeForKids) {
                double dist = calculateDistance(currentLoc.getLat(), currentLoc.getLng(), p.getLat(), p.getLng());
                if (dist < minDistance) {
                    minDistance = dist;
                    bestPlace = p;
                }
            }
        }
        return bestPlace;
    }

    private PlaceModel findNearestFreeFiller(PlaceModel currentLoc, List<PlaceModel> unvisited, int remTime, int currentEffort) {
        PlaceModel bestFiller = null;
        double minDistance = Double.MAX_VALUE;

        for (PlaceModel p : unvisited) {
            int timeNeeded = p.getDurationMinutes() + 30;
            boolean isFree = getEstimatedPrice(p.getBudget()) == 0;
            boolean fitsEffort = (currentEffort + p.getEffortLevel()) <= maxDailyEffort;
            boolean safeForKids = (childCount == 0) || (p.getEffortLevel() < 3);

            if (timeNeeded <= remTime && isFree && fitsEffort && safeForKids) {
                double dist = calculateDistance(currentLoc.getLat(), currentLoc.getLng(), p.getLat(), p.getLng());
                if (dist < minDistance) {
                    minDistance = dist;
                    bestFiller = p;
                }
            }
        }
        return bestFiller;
    }

    private void updateMapAndAnimate(List<PlaceModel> targetList) {
        if (mMap == null || targetList.isEmpty()) return;
        mMap.clear();
        mMap.setPadding(0, 250, 0, 850);

        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        com.google.android.gms.maps.model.PolylineOptions routeLine = new com.google.android.gms.maps.model.PolylineOptions()
                .width(12).color(Color.parseColor("#C5A059")).geodesic(false);

        for (int i = 0; i < targetList.size(); i++) {
            PlaceModel p = targetList.get(i);
            if (p.getLat() != 0 && p.getLng() != 0) {
                LatLng pos = new LatLng(p.getLat(), p.getLng());
                mMap.addMarker(new MarkerOptions().position(pos).anchor(0.5f, 0.5f)
                        .icon(getMarkerIconWithNumber(i + 1)).title((i + 1) + ". " + p.getName()));
                routeLine.add(pos);
                builder.include(pos);
            }
        }
        mMap.addPolyline(routeLine);

        if (shouldAnimateMap) {
            startSmartCameraAnimation(targetList, builder.build());
        } else {
            try { mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), 250)); }
            catch (Exception e) { Log.e("MapError", e.getMessage()); }
        }
    }

    private com.google.android.gms.maps.model.BitmapDescriptor getMarkerIconWithNumber(int number) {
        android.graphics.Paint paint = new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);
        paint.setTextSize(35f);
        paint.setColor(android.graphics.Color.WHITE);
        paint.setTextAlign(android.graphics.Paint.Align.CENTER);

        android.graphics.Bitmap.Config conf = android.graphics.Bitmap.Config.ARGB_8888;
        android.graphics.Bitmap bmp = android.graphics.Bitmap.createBitmap(80, 80, conf);
        android.graphics.Canvas canvas = new android.graphics.Canvas(bmp);

        android.graphics.Paint circlePaint = new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);
        circlePaint.setColor(android.graphics.Color.parseColor("#C5A059"));
        canvas.drawCircle(40, 40, 35, circlePaint);

        canvas.drawText(String.valueOf(number), 40, 52, paint);

        return com.google.android.gms.maps.model.BitmapDescriptorFactory.fromBitmap(bmp);
    }

    private void startSmartCameraAnimation(List<PlaceModel> places, LatLngBounds finalBounds) {
        if (places == null || places.isEmpty()) return;

        isAnimationRunning = true;

        LatLng firstPos = new LatLng(places.get(0).getLat(), places.get(0).getLng());
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(firstPos, 16.5f), 1500, new GoogleMap.CancelableCallback() {
            @Override
            public void onFinish() {
                currentDisplayList.add(places.get(0));
                adapter.notifyItemInserted(0);
                rvTimeline.smoothScrollToPosition(0);

                calculateTotalProgress();

                if (places.size() > 1) {
                    new android.os.Handler().postDelayed(() -> animateToNextStep(1, places, finalBounds), 800);
                } else {
                    zoomOutToViewAll(finalBounds);
                    isAnimationRunning = false;
                }
            }
            @Override public void onCancel() {}
        });
    }

    private void animateToNextStep(int index, List<PlaceModel> places, LatLngBounds finalBounds) {
        if (index >= places.size()) {
            zoomOutToViewAll(finalBounds);
            calculateTotalProgress();
            isAnimationRunning = false;
            return;
        }

        LatLng nextPos = new LatLng(places.get(index).getLat(), places.get(index).getLng());
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(nextPos, 16.5f), 2000, new GoogleMap.CancelableCallback() {
            @Override
            public void onFinish() {
                currentDisplayList.add(places.get(index));
                adapter.notifyItemInserted(currentDisplayList.size() - 1);
                rvTimeline.smoothScrollToPosition(currentDisplayList.size() - 1);

                calculateTotalProgress();

                new android.os.Handler().postDelayed(() -> animateToNextStep(index + 1, places, finalBounds), 800);
            }
            @Override public void onCancel() {}
        });
    }

    private void zoomOutToViewAll(LatLngBounds finalBounds) {
        try { mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(finalBounds, 250), 2000, null); }
        catch (Exception e) { Log.e("MapError", e.getMessage()); }
    }
}