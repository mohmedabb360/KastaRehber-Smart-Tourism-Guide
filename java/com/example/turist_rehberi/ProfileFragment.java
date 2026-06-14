package com.example.turist_rehberi;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProfileFragment extends Fragment {

    private FirebaseAuth mAuth;
    private FirebaseStorage storage;
    private StorageReference storageReference;

    // 🚀 تحديث: إضافة الـ TextView الجديد لعدد الأماكن المزارة
    private TextView tvUserName, tvUserEmail, tvFavCount, tvVisitedCount;
    private ImageView imgProfile;

    private View btnSettings, btnShare, btnLogout, btnEditPhoto, btnEditProfile, btnNewPlan;

    private static final int PICK_IMAGE_REQUEST = 1;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        mAuth = FirebaseAuth.getInstance();
        storage = FirebaseStorage.getInstance();
        storageReference = storage.getReference();

        imgProfile = view.findViewById(R.id.imgProfile);
        tvUserName = view.findViewById(R.id.tvUserName);
        tvUserEmail = view.findViewById(R.id.tvUserEmail);
        tvFavCount = view.findViewById(R.id.tvFavCount);

        // 🚀 ربط الـ TextView الجديد الخاص بالعداد بالـ Java
        tvVisitedCount = view.findViewById(R.id.tvVisitedCount);

        btnSettings = view.findViewById(R.id.btnSettings);
        btnShare = view.findViewById(R.id.btnShare);
        btnLogout = view.findViewById(R.id.btnLogout);
        btnEditPhoto = view.findViewById(R.id.btnEditPhoto);
        btnEditProfile = view.findViewById(R.id.btnEditProfile);
        btnNewPlan = view.findViewById(R.id.btnNewPlan);

        loadUserInfo();

        btnEditPhoto.setOnClickListener(v -> openGallery());
        btnEditProfile.setOnClickListener(v -> showEditNameDialog());
        btnNewPlan.setOnClickListener(v -> showDeletePlanConfirmationDialog());
        btnLogout.setOnClickListener(v -> showLogoutConfirmationDialog());

        btnShare.setOnClickListener(v -> {
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Kastamonu Gezi Rehberi");
            shareIntent.putExtra(Intent.EXTRA_TEXT, "Kastamonu'yu benimle keşfet! Uygulamayı buradan indir: [https://bit.ly/4urH2Ud]");
            startActivity(Intent.createChooser(shareIntent, "Paylaş"));
        });

        loadFavoritesCount();
        // 🚀 استدعاء دالة جلب عدد الأماكن المزارة فور فتح الواجهة
        loadVisitedPlacesCount();

        return view;
    }

    // 🚀 دالة ذكية لحساب عدد معالم الجولة المزارة فعلياً من الـ daysData المدمجة في مستند الـ Users
    private void loadVisitedPlacesCount() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        FirebaseFirestore.getInstance().collection("Users").document(user.getUid()).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists() && documentSnapshot.get("daysData") != null) {
                        try {
                            List<Map<String, Object>> daysData = (List<Map<String, Object>>) documentSnapshot.get("daysData");
                            int visitedCounter = 0;

                            if (daysData != null) {
                                for (Map<String, Object> dayMap : daysData) {
                                    List<Map<String, Object>> placesListMap = (List<Map<String, Object>>) dayMap.get("places");
                                    if (placesListMap != null) {
                                        for (Map<String, Object> pMap : placesListMap) {
                                            if (pMap.containsKey("visited") && pMap.get("visited") != null) {
                                                if ((Boolean) pMap.get("visited")) {
                                                    visitedCounter++; // زيادة العداد مع كل معلم تم إثبات زيارته
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            // إسناد المجموع للواجهة على اليسار
                            tvVisitedCount.setText(String.valueOf(visitedCounter));

                        } catch (Exception e) {
                            tvVisitedCount.setText("0");
                        }
                    } else {
                        tvVisitedCount.setText("0");
                    }
                })
                .addOnFailureListener(e -> tvVisitedCount.setText("0"));
    }

    private void showDeletePlanConfirmationDialog() {
        androidx.appcompat.app.AlertDialog dialog = new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                .setTitle("Yeni Rota Oluştur")
                .setMessage("Mevcut gezi planınız tamamen silinecektir. Yeni bir plan oluşturmak istediğinize emin misiniz?")
                .setCancelable(false)
                .setPositiveButton("Evet, Sil ve Başla", (d, which) -> deleteCurrentPlanAndRedirect())
                .setNegativeButton("İptal", (d, which) -> d.dismiss())
                .create();

        dialog.setOnShowListener(dialogInterface -> {
            android.widget.Button posButton = dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE);
            if (posButton != null) {
                posButton.setTextColor(Color.parseColor("#2E7D32"));
                posButton.setTypeface(null, android.graphics.Typeface.BOLD);
            }

            android.widget.Button realNegButton = dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_NEGATIVE);
            if (realNegButton != null) {
                realNegButton.setTextColor(Color.parseColor("#FF5252"));
                realNegButton.setTypeface(null, android.graphics.Typeface.BOLD);
            }
        });

        if (dialog.getWindow() != null) {
            dialog.getWindow().getDecorView().setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
        }

        dialog.show();
    }

    private void deleteCurrentPlanAndRedirect() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        String userId = user.getUid();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        Map<String, Object> updates = new HashMap<>();
        updates.put("preferredDays", com.google.firebase.firestore.FieldValue.delete());
        updates.put("daysData", com.google.firebase.firestore.FieldValue.delete());

        db.collection("Users").document(userId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "Eski plan silindi. Yeni rota ekranına yönlendiriliyorsunuz.", Toast.LENGTH_SHORT).show();

                    Intent intent = new Intent(getActivity(), SmartWizardActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    if (getActivity() != null) {
                        getActivity().finish();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Hata: Plan silinemedi!", Toast.LENGTH_SHORT).show();
                });
    }

    private void loadFavoritesCount() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            FirebaseFirestore.getInstance().collection("Users").document(user.getUid())
                    .collection("Favorites").get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        int count = queryDocumentSnapshots.size();
                        tvFavCount.setText(String.valueOf(count));
                    });
        }
    }

    private void loadUserInfo() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            String name = user.getDisplayName();
            if (name == null || name.isEmpty()) {
                name = user.getEmail().split("@")[0];
            }
            tvUserName.setText(name);
            tvUserEmail.setText(user.getEmail());

            if (user.getPhotoUrl() != null) {
                Glide.with(this)
                        .load(user.getPhotoUrl())
                        .apply(new RequestOptions()
                                .placeholder(R.drawable.account_balance)
                                .error(R.drawable.account_balance))
                        .into(imgProfile);
            }
        }
    }

    private void showEditNameDialog() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        com.google.android.material.dialog.MaterialAlertDialogBuilder builder =
                new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext());

        builder.setTitle("İsmi Değiştir");

        final EditText input = new EditText(requireContext());
        input.setText(tvUserName.getText().toString());

        input.setTextColor(Color.WHITE);
        input.setTextDirection(View.TEXT_DIRECTION_LTR);
        input.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);

        LinearLayout container = new LinearLayout(requireContext());
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(60, 20, 60, 20);
        container.addView(input);

        builder.setView(container);

        builder.setPositiveButton("Kaydet", (dialog, which) -> {
            String newName = input.getText().toString().trim();
            if (!newName.isEmpty()) {
                updateUserName(newName);
            }
        });
        builder.setNegativeButton("İptal", (dialog, which) -> dialog.cancel());

        androidx.appcompat.app.AlertDialog dialog = builder.create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().getDecorView().setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
        }

        dialog.show();
    }

    private void updateUserName(String newName) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                    .setDisplayName(newName)
                    .build();

            user.updateProfile(profileUpdates).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    tvUserName.setText(newName);
                    Toast.makeText(getContext(), "İsim başarıyla güncellendi", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getContext(), "Hata oluştu!", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void openGallery() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Fotoğraf Seç"), PICK_IMAGE_REQUEST);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == getActivity().RESULT_OK && data != null && data.getData() != null) {
            Uri imageUri = data.getData();
            uploadImageToFirebase(imageUri);
        }
    }

    private void uploadImageToFirebase(Uri imageUri) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        ProgressDialog progressDialog = new ProgressDialog(getContext());
        progressDialog.setMessage("Fotoğraf yükleniyor...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        StorageReference fileRef = storageReference.child("profile_images/" + user.getUid() + ".jpg");

        fileRef.putFile(imageUri).addOnSuccessListener(taskSnapshot -> {
            fileRef.getDownloadUrl().addOnSuccessListener(uri -> {
                updateUserPhotoUrl(uri, progressDialog);
            });
        }).addOnFailureListener(e -> {
            progressDialog.dismiss();
            Toast.makeText(getContext(), "Yükleme başarısız: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }

    private void updateUserPhotoUrl(Uri uri, ProgressDialog progressDialog) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                    .setPhotoUri(uri)
                    .build();

            user.updateProfile(profileUpdates).addOnCompleteListener(task -> {
                progressDialog.dismiss();
                if (task.isSuccessful()) {
                    Glide.with(this).load(uri).into(imgProfile);
                    Toast.makeText(getContext(), "Fotoğraf güncellendi", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getContext(), "Fotoğraf güncellenemedi", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void showLogoutConfirmationDialog() {
        com.google.android.material.dialog.MaterialAlertDialogBuilder builder =
                new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext());

        builder.setTitle("Çıkış Yap");
        builder.setMessage("Gerçekten çıkış yapmak istiyor musunuz?");

        builder.setPositiveButton("Evet", (dialog, which) -> {
            mAuth.signOut();
            Intent intent = new Intent(getActivity(), LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });

        builder.setNegativeButton("Hayır", (dialog, which) -> dialog.dismiss());

        androidx.appcompat.app.AlertDialog dialog = builder.create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().getDecorView().setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
        }

        dialog.show();
    }
}