package com.example.turist_rehberi;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.List;

public class FavoritesFragment extends Fragment {

    private RecyclerView rvFavorites;
    private View layoutEmptyState;
    private FavoritesAdapter adapter;
    private List<PlaceModel> favoriteList = new ArrayList<>();
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_favorites, container, false);

        rvFavorites = view.findViewById(R.id.rvFavorites);
        layoutEmptyState = view.findViewById(R.id.layoutEmptyState);
        View btnGoToExplore = view.findViewById(R.id.btnGoToExplore);

        // 🚀 استخدام GridLayout لعمل شبكة من عمودين
        rvFavorites.setLayoutManager(new GridLayoutManager(getContext(), 2));
        adapter = new FavoritesAdapter(favoriteList);
        rvFavorites.setAdapter(adapter);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        loadFavorites();

        // زر الذهاب للاستكشاف إذا كانت القائمة فارغة
        btnGoToExplore.setOnClickListener(v -> {
            if (getActivity() instanceof DashboardActivity) {
                // استدعاء زر الاستكشاف في الداشبورد برمجياً
                getActivity().findViewById(R.id.btnNavExplore).performClick();
            }
        });

        return view;
    }

    private void loadFavorites() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        db.collection("Users").document(user.getUid()).collection("Favorites").get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    favoriteList.clear();

                    if (queryDocumentSnapshots.isEmpty()) {
                        layoutEmptyState.setVisibility(View.VISIBLE);
                        rvFavorites.setVisibility(View.GONE);
                    } else {
                        layoutEmptyState.setVisibility(View.GONE);
                        rvFavorites.setVisibility(View.VISIBLE);

                        for (DocumentSnapshot doc : queryDocumentSnapshots) {
                            String placeName = doc.getId();

                            // 🚀 التعديل هنا: البحث باستخدام حقل "name" وليس الـ Document ID
                            db.collection("Places").whereEqualTo("name", placeName).get()
                                    .addOnSuccessListener(placeSnaps -> {
                                        if (!placeSnaps.isEmpty()) {
                                            PlaceModel place = placeSnaps.getDocuments().get(0).toObject(PlaceModel.class);
                                            favoriteList.add(place);
                                            adapter.notifyDataSetChanged();
                                        }
                                    });
                        }
                    }
                });
    }
}