package com.example.turist_rehberi;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class WizardActivity extends AppCompatActivity {

    private MaterialCardView dropdownCard, countersCard, infoBox;
    private View childRow;
    private TextView tvSelectedGroup, tvAdultCount, tvChildCount, tvInfoTitle, tvInfoDesc;
    private MaterialCardView btnMinusAdult, btnPlusAdult, btnMinusChild, btnPlusChild;
    private MaterialButton btnGenerate;

    private int adultCount = 2;
    private int childCount = 1;
    private String selectedGroup = "Ailece"; // القيمة البرمجية النظيفة اللي بتنحفظ بالفايربيس

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wizard);

        initViews();
        setupListeners();
    }
    @Override
    protected void onResume() {
        super.onResume();
        // 🚀 إعادة تفعيل زر إنشاء الخطة وترجيع النص الأصلي لو رجع المستخدم للخلف
        if (btnGenerate != null) {
            btnGenerate.setEnabled(true);
            btnGenerate.setText("Akıllı Plan Oluştur →");
        }
    }

    private void initViews() {
        dropdownCard = findViewById(R.id.dropdownCard);
        countersCard = findViewById(R.id.countersCard);
        infoBox = findViewById(R.id.infoBox);
        childRow = findViewById(R.id.childRow);

        tvSelectedGroup = findViewById(R.id.tvSelectedGroup);
        tvAdultCount = findViewById(R.id.tvAdultCount);
        tvChildCount = findViewById(R.id.tvChildCount);
        tvInfoTitle = findViewById(R.id.tvInfoTitle);
        tvInfoDesc = findViewById(R.id.tvInfoDesc);

        btnMinusAdult = findViewById(R.id.btnMinusAdult);
        btnPlusAdult = findViewById(R.id.btnPlusAdult);
        btnMinusChild = findViewById(R.id.btnMinusChild);
        btnPlusChild = findViewById(R.id.btnPlusChild);
        btnGenerate = findViewById(R.id.btnGenerate);
    }

    private void setupListeners() {
        dropdownCard.setOnClickListener(v -> showGroupDialog());

        btnPlusAdult.setOnClickListener(v -> { adultCount++; tvAdultCount.setText(String.valueOf(adultCount)); });
        btnMinusAdult.setOnClickListener(v -> { if (adultCount > 1) { adultCount--; tvAdultCount.setText(String.valueOf(adultCount)); } });
        btnPlusChild.setOnClickListener(v -> { childCount++; tvChildCount.setText(String.valueOf(childCount)); });
        btnMinusChild.setOnClickListener(v -> { if (childCount > 0) { childCount--; tvChildCount.setText(String.valueOf(childCount)); } });

        btnGenerate.setOnClickListener(v -> {
            btnGenerate.setEnabled(false);
            btnGenerate.setText("Plan Hazırlanıyor...");

            ArrayList<String> previousInterests = getIntent().getStringArrayListExtra("SELECTED_INTERESTS");

            Map<String, Object> userProfileData = new HashMap<>();
            userProfileData.put("groupType", selectedGroup);
            userProfileData.put("adultCount", adultCount);
            userProfileData.put("childCount", childCount);
            userProfileData.put("preferredDays", getIntent().getIntExtra("PREF_DAYS", 3));
            userProfileData.put("budgetLimit", getIntent().getStringExtra("PREF_BUDGET"));

            if (previousInterests != null) {
                userProfileData.put("interests", previousInterests);
            }

            FirebaseAuth mAuth = FirebaseAuth.getInstance();
            FirebaseFirestore db = FirebaseFirestore.getInstance();

            if (mAuth.getCurrentUser() != null) {
                String userId = mAuth.getCurrentUser().getUid();

                // 🚀 التعديل الجوهري: الحفظ في Users بدلاً من UsersProfile لتوحيد البيانات
                db.collection("Users").document(userId)
                        .update(userProfileData)
                        .addOnSuccessListener(aVoid -> navigateToLoading())
                        .addOnFailureListener(e -> {
                            // إذا فشل التحديث (ربما المستند غير موجود لسبب ما)، نستخدم set مع merge كخطة بديلة آمنة
                            db.collection("Users").document(userId)
                                    .set(userProfileData, com.google.firebase.firestore.SetOptions.merge())
                                    .addOnSuccessListener(aVoid2 -> navigateToLoading())
                                    .addOnFailureListener(e2 -> {
                                        Toast.makeText(WizardActivity.this, "Hata oluştu: " + e2.getMessage(), Toast.LENGTH_SHORT).show();
                                        btnGenerate.setEnabled(true);
                                        btnGenerate.setText("Akıllı Plan Oluştur →");
                                    });
                        });
            } else {
                navigateToLoading();
            }
        });
    }

    private void navigateToLoading() {
        Intent intent = new Intent(WizardActivity.this, LoadingActivity.class);

        // 🚀 تمرير بيانات العائلة
        intent.putExtra("ADULT_COUNT", adultCount);
        intent.putExtra("CHILD_COUNT", childCount);
        intent.putExtra("GROUP_TYPE", selectedGroup);

        // 🚀 تمرير الأيام والميزانية (هاد اللي كان ناقص ويخرب الخوارزمية)
        intent.putExtra("PREF_DAYS", getIntent().getIntExtra("PREF_DAYS", 3));
        intent.putExtra("PREF_BUDGET", getIntent().getStringExtra("PREF_BUDGET"));

        // 🚀 تمرير الاهتمامات
        ArrayList<String> interests = getIntent().getStringArrayListExtra("SELECTED_INTERESTS");
        if (interests != null) {
            intent.putStringArrayListExtra("SELECTED_INTERESTS", interests);
        }

        startActivity(intent);
        
    }

    private void showGroupDialog() {
        String[] displayGroups = {"🚶 Yalnız", "💑 Çift", "👨‍👩‍👧‍👦 Ailece", "🏕️ Arkadaş Grubu"};
        String[] logicGroups = {"Yalnız", "Çift", "Ailece", "Arkadaş Grubu"};

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Kiminle seyahat ediyorsun ?");
        builder.setItems(displayGroups, (dialog, which) -> {
            selectedGroup = logicGroups[which];
            tvSelectedGroup.setText(displayGroups[which]);
            updateUIBasedOnGroup(selectedGroup);
        });
        builder.show();
    }

    private void updateUIBasedOnGroup(String group) {
        switch (group) {
            case "Yalnız":
                countersCard.setVisibility(View.GONE);
                infoBox.setVisibility(View.VISIBLE);
                adultCount = 1; childCount = 0;
                tvInfoTitle.setText("Kendi temponu belirle!");
                tvInfoDesc.setText("Tek başına seyahat etmek, en gizli yerleri keşfetmek için harika bir fırsat.");
                break;
            case "Çift":
                countersCard.setVisibility(View.GONE);
                infoBox.setVisibility(View.VISIBLE);
                adultCount = 2; childCount = 0;
                tvInfoTitle.setText("Romantik bir rota sizi bekliyor.");
                tvInfoDesc.setText("Kastamonu'nun manzara noktaları ve butik konakları tam size göre.");
                break;
            case "Ailece":
                countersCard.setVisibility(View.VISIBLE);
                childRow.setVisibility(View.VISIBLE);
                infoBox.setVisibility(View.VISIBLE);
                adultCount = 2; childCount = 1;
                tvAdultCount.setText("2"); tvChildCount.setText("1");
                tvInfoTitle.setText("Aileniz için en güvenli rotalar.");
                tvInfoDesc.setText("Çocuk dostu parkurlar ve rahat ulaşım seçeneklerini öncelikli hale getirdik.");
                break;
            case "Arkadaş Grubu":
                countersCard.setVisibility(View.VISIBLE);
                childRow.setVisibility(View.GONE);
                infoBox.setVisibility(View.VISIBLE);
                adultCount = 3; childCount = 0;
                tvAdultCount.setText("3");
                tvInfoTitle.setText("Ekip hazır, macera başlıyor!");
                tvInfoDesc.setText("Grup indirimleri ve geniş masalı restoranları listemize ekledik.");
                break;
        }
    }
}