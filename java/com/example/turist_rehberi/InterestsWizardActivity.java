package com.example.turist_rehberi;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import java.util.ArrayList;
import java.util.List;

public class InterestsWizardActivity extends AppCompatActivity {

    private MaterialButton btnFinishWizard;
    private int days;
    private String budget;
    private List<String> selectedInterests = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_interests_wizard);

        // استلام البيانات من الشاشات اللي قبلها (الأيام والميزانية)
        days = getIntent().getIntExtra("PREF_DAYS", 3);
        budget = getIntent().getStringExtra("PREF_BUDGET");

        btnFinishWizard = findViewById(R.id.btnFinishWizard);

        setupCard(R.id.cardTarih, "Tarih");
        setupCard(R.id.cardDoga, "Doğa");
        setupCard(R.id.cardYemek, "Yemek");
        setupCard(R.id.cardAlisveris, "Alışveriş");
        setupCard(R.id.cardDagcilik, "Dağcılık");
        setupCard(R.id.cardMuzeler, "Müzeler");
        setupCard(R.id.cardMagaralar, "Mağaralar");
        setupCard(R.id.cardKampcilik, "Kampçılık");

        // لما يضغط التالي، بيروح لدالة الانتقال
        btnFinishWizard.setOnClickListener(v -> navigateToNextWizard());
    }

    private void setupCard(int cardId, String interestName) {
        MaterialCardView card = findViewById(cardId);
        LinearLayout layout = (LinearLayout) card.getChildAt(0);
        ImageView icon = (ImageView) layout.getChildAt(0);
        TextView text = (TextView) layout.getChildAt(1);

        card.setOnClickListener(v -> {
            if (selectedInterests.contains(interestName)) {
                selectedInterests.remove(interestName);
                card.setCardBackgroundColor(Color.WHITE);
                icon.setColorFilter(Color.parseColor("#052012"));
                text.setTextColor(Color.parseColor("#052012"));
            } else {
                selectedInterests.add(interestName);
                card.setCardBackgroundColor(Color.parseColor("#052012"));
                icon.setColorFilter(Color.parseColor("#C5A059"));
                text.setTextColor(Color.WHITE);
            }
        });
    }

    private void navigateToNextWizard() {
        if (selectedInterests.isEmpty()) {
            Toast.makeText(this, "Lütfen en az bir ilgi alanı seçin", Toast.LENGTH_SHORT).show();
            return;
        }

        // الانتقال لشاشة "مع مين رح تسافر؟" (WizardActivity)
        Intent intent = new Intent(InterestsWizardActivity.this, WizardActivity.class);

        // تعبئة كل البيانات بالطرد عشان توصل للشاشة الأخيرة
        intent.putExtra("PREF_DAYS", days);
        intent.putExtra("PREF_BUDGET", budget);
        intent.putStringArrayListExtra("SELECTED_INTERESTS", new ArrayList<>(selectedInterests));

        startActivity(intent);
    }
}