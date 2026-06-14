package com.example.turist_rehberi;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class LoginActivity extends AppCompatActivity {

    private TextInputEditText etEmail, etPassword;
    private MaterialButton btnGiris, btnKayit;
    private ProgressBar progressBar;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private static final String PREFS_NAME = "LoginPrefs";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnGiris = findViewById(R.id.btnGiris);
        btnKayit = findViewById(R.id.btnKayit);
        progressBar = findViewById(R.id.progressBar);

        // سحب البيانات المحفوظة من الذاكرة وتعبئة الخانات تلقائياً لراحة المستخدم
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String savedEmail = prefs.getString("email", "");
        String savedPassword = prefs.getString("password", "");

        etEmail.setText(savedEmail);
        etPassword.setText(savedPassword);

        // 🚀 تم مسح كود الفحص التلقائي المتكرر من هنا (لأنه أصبح يُعالج بكفاءة داخل MainActivity)

        btnGiris.setOnClickListener(v -> loginUser());

        btnKayit.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
        });
    }

    private void loginUser() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Lütfen geçerli bir e-posta giriniz");
            etEmail.requestFocus();
            return;
        }
        if (password.isEmpty() || password.length() < 6) {
            etPassword.setError("Şifre en az 6 karakter olmalıdır");
            etPassword.requestFocus();
            return;
        }

        setLoading(true);

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(LoginActivity.this, "Giriş Başarılı!", Toast.LENGTH_SHORT).show();

                        // حفظ الإيميل والباسوورد في الذاكرة بعد نجاح تسجيل الدخول اليدوي
                        SharedPreferences.Editor editor = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit();
                        editor.putString("email", email);
                        editor.putString("password", password);
                        editor.apply();

                        // 🚀 نقله وتوجيهه بناءً على بيانات مستنده في الفايربيس
                        checkUserAndRedirect();
                    } else {
                        setLoading(false);
                        Toast.makeText(LoginActivity.this, "Hata: Bilgilerinizi kontrol ediniz.", Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void checkUserAndRedirect() {
        setLoading(true);
        String userId = mAuth.getCurrentUser().getUid();

        db.collection("Users").document(userId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    setLoading(false);
                    if (documentSnapshot.exists() && documentSnapshot.contains("preferredDays")) {

                        Intent intent = new Intent(LoginActivity.this, DashboardActivity.class);

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

                        // 🚀 الإصلاح الحاسم والنهائي:
                        // نمنع تمرير SELECTED_INTERESTS عند تسجيل الدخول العادي لمنع الـ Dashboard
                        // من اعتبارها "طلب خطة جديدة" ومسح الأماكن المزارة المخزنة مسبقاً.

                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();

                    } else {
                        // إذا كان مستخدم جديد أو بياناته غير مكتملة، بنوجهه لـ معالج التخطيط الذكي
                        Intent intent = new Intent(LoginActivity.this, SmartWizardActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Toast.makeText(LoginActivity.this, "Veriler alınamadı, lütfen tekrar giriş yapın.", Toast.LENGTH_SHORT).show();
                    mAuth.signOut();
                });
    }

    private void setLoading(boolean isLoading) {
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        btnGiris.setEnabled(!isLoading);
        btnKayit.setEnabled(!isLoading);
        etEmail.setEnabled(!isLoading);
        etPassword.setEnabled(!isLoading);
    }
}