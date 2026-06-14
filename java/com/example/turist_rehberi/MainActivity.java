package com.example.turist_rehberi;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private boolean isRedirecting = false; // لمنع حدوث انتقال مزدوج بالخطأ

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // ربط الفايربيس
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // 🚀 1. فحص الدخول التلقائي في الخلفية فوراً عند فتح التطبيق
        if (mAuth.getCurrentUser() != null) {
            checkUserAndRedirect();
        }

        // 2. تحديد الشاشة بالكامل كزر (للمستخدم الجديد أو بحال لم يتم التوجيه التلقائي)
        View rootView = findViewById(android.R.id.content);
        rootView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // إذا كان النظام عم ينقله تلقائياً هلق، بنوقف الضغط اليدوي عشان ما يفتح شاشتين فوق بعض
                if (isRedirecting) return;

                navigateToLogin();
            }
        });
    }

    private void checkUserAndRedirect() {
        isRedirecting = true;
        String userId = mAuth.getCurrentUser().getUid();

        db.collection("Users").document(userId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists() && documentSnapshot.contains("preferredDays")) {
                        Intent intent = new Intent(MainActivity.this, DashboardActivity.class);

                        Long adultCount = documentSnapshot.getLong("adultCount");
                        Long childCount = documentSnapshot.getLong("childCount");
                        String groupType = documentSnapshot.getString("groupType");
                        Long prefDays = documentSnapshot.getLong("preferredDays");
                        String prefBudget = documentSnapshot.getString("budgetLimit");

                        intent.putExtra("ADULT_COUNT", adultCount != null ? adultCount.intValue() : 2);
                        intent.putExtra("CHILD_COUNT", childCount != null ? childCount.intValue() : 0);
                        intent.putExtra("GROUP_TYPE", groupType != null ? groupType : "Ailece");
                        intent.putExtra("PREF_DAYS", prefDays != null ? prefDays.intValue() : 3);
                        intent.putExtra("PREF_BUDGET", prefBudget != null ? prefBudget : "Orta");

                        // 🚀 الإصلاح الحاسم: عند الفتح التلقائي لا نمرر SELECTED_INTERESTS أبداً!
                        // هذا يمنع الـ Dashboard من تفعيل شرط (isNewPlanRequested) وإعادة حساب الخوارزمية.

                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();

                    } else {
                        Intent intent = new Intent(MainActivity.this, SmartWizardActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    isRedirecting = false;
                });
    }

    private void navigateToLogin() {
        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
        startActivity(intent);
        finish();
    }
}