package com.example.turist_rehberi;

import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.List;

public class ExploreFragment extends Fragment {

    private RecyclerView rvExplore;
    private ExploreAdapter adapter;
    private EditText etSearchPlaces;

    // الأزرار المحدثة
    private TextView btnAll, btnHistory, btnNature, btnFood, btnShopping, btnMountain, btnMuseum, btnCave, btnCamping;

    private List<PlaceModel> allPlacesList = new ArrayList<>();
    private List<PlaceModel> filteredList = new ArrayList<>();

    private FirebaseFirestore db;
    private String currentCategory = "Tümü";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_explore, container, false);

        rvExplore = view.findViewById(R.id.rvExplore);
        etSearchPlaces = view.findViewById(R.id.etSearchPlaces);

        // ربط الأزرار المحدثة
        btnAll = view.findViewById(R.id.btnAll);
        btnHistory = view.findViewById(R.id.btnHistory);
        btnNature = view.findViewById(R.id.btnNature);
        btnFood = view.findViewById(R.id.btnFood);
        btnShopping = view.findViewById(R.id.btnShopping);
        btnMountain = view.findViewById(R.id.btnMountain);
        btnMuseum = view.findViewById(R.id.btnMuseum);
        btnCave = view.findViewById(R.id.btnCave);
        btnCamping = view.findViewById(R.id.btnCamping);

        rvExplore.setLayoutManager(new LinearLayoutManager(getContext()));

        adapter = new ExploreAdapter(filteredList);
        rvExplore.setAdapter(adapter);

        db = FirebaseFirestore.getInstance();

        fetchAllPlaces();
        setupSearch();
        setupFilters();

        return view;
    }

    private void fetchAllPlaces() {
        db.collection("Places").get().addOnSuccessListener(queryDocumentSnapshots -> {
            allPlacesList.clear();
            for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                PlaceModel place = document.toObject(PlaceModel.class);
                allPlacesList.add(place);
            }
            filterData(etSearchPlaces.getText().toString());
        });
    }

    private void setupSearch() {
        if (etSearchPlaces != null) {
            etSearchPlaces.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}

                @Override
                public void afterTextChanged(Editable s) {
                    filterData(s.toString());
                }
            });
        }
    }

    private void setupFilters() {
        if (btnAll != null) btnAll.setOnClickListener(v -> updateCategoryFilter(btnAll, "Tümü"));
        if (btnHistory != null) btnHistory.setOnClickListener(v -> updateCategoryFilter(btnHistory, "Tarih"));
        if (btnNature != null) btnNature.setOnClickListener(v -> updateCategoryFilter(btnNature, "Doğa"));
        if (btnFood != null) btnFood.setOnClickListener(v -> updateCategoryFilter(btnFood, "Yemek"));
        if (btnShopping != null) btnShopping.setOnClickListener(v -> updateCategoryFilter(btnShopping, "Alışveriş"));
        if (btnMountain != null) btnMountain.setOnClickListener(v -> updateCategoryFilter(btnMountain, "Dağcılık"));
        if (btnMuseum != null) btnMuseum.setOnClickListener(v -> updateCategoryFilter(btnMuseum, "Müzeler"));
        if (btnCave != null) btnCave.setOnClickListener(v -> updateCategoryFilter(btnCave, "Mağaralar"));
        if (btnCamping != null) btnCamping.setOnClickListener(v -> updateCategoryFilter(btnCamping, "Kampçılık"));
    }

    private void updateCategoryFilter(TextView selectedBtn, String category) {
        currentCategory = category;

        resetButtonUI(btnAll);
        resetButtonUI(btnHistory);
        resetButtonUI(btnNature);
        resetButtonUI(btnFood);
        resetButtonUI(btnShopping);
        resetButtonUI(btnMountain);
        resetButtonUI(btnMuseum);
        resetButtonUI(btnCave);
        resetButtonUI(btnCamping);

        if (selectedBtn != null) {
            selectedBtn.setBackgroundResource(R.drawable.bg_rounded_btn_light);
            selectedBtn.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#052012")));
            selectedBtn.setTextColor(Color.WHITE);
        }

        filterData(etSearchPlaces.getText().toString());
    }

    private void resetButtonUI(TextView btn) {
        if (btn != null) {
            btn.setBackgroundResource(R.drawable.bg_rounded_input);
            btn.setBackgroundTintList(null);
            btn.setTextColor(Color.parseColor("#052012"));
        }
    }

    private void filterData(String text) {
        filteredList.clear();
        String searchText = text.toLowerCase().trim();

        for (PlaceModel place : allPlacesList) {
            boolean matchesSearch = place.getName() != null && place.getName().toLowerCase().contains(searchText);

            boolean matchesCategory = currentCategory.equals("Tümü") ||
                    (place.getCategory() != null && place.getCategory().equalsIgnoreCase(currentCategory));

            if (matchesSearch && matchesCategory) {
                filteredList.add(place);
            }
        }

        if (adapter != null) {
            adapter.filterList(filteredList);
        }
    }
}