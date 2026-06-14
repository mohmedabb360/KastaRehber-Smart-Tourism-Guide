package com.example.turist_rehberi;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore; // 🚀 مكتبة الفايرستور

import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {

    private TextInputEditText etName, etEmail, etPassword, etConfirmPassword;
    private MaterialButton btnRegister;
    private LinearLayout llGoToLogin;
    private ImageView backIcon;
    private ProgressBar progressBar;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db; // 🚀 تعريف قاعدة البيانات

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance(); // 🚀 تهيئة قاعدة البيانات

        etName = findViewById(R.id.etName);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        btnRegister = findViewById(R.id.btnRegister);
        llGoToLogin = findViewById(R.id.llGoToLogin);
        backIcon = findViewById(R.id.backIcon);
        progressBar = findViewById(R.id.progressBar);

        backIcon.setOnClickListener(v -> finish());
        btnRegister.setOnClickListener(v -> createAccount());
        llGoToLogin.setOnClickListener(v -> finish());
    }

    private void createAccount() {
        String name = etName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();

        if (name.isEmpty()) { etName.setError("Lütfen adınızı giriniz"); etName.requestFocus(); return; }
        if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) { etEmail.setError("Lütfen geçerli bir e-posta giriniz"); etEmail.requestFocus(); return; }
        if (password.isEmpty() || password.length() < 6) { etPassword.setError("Şifre en az 6 karakter olmalıdır"); etPassword.requestFocus(); return; }
        if (!password.equals(confirmPassword)) { etConfirmPassword.setError("Şifreler eşleşmiyor!"); etConfirmPassword.requestFocus(); return; }

        setLoading(true);

        // 1. إنشاء الحساب في Authentication
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {

                        // 🚀 2. أخذ معرف المستخدم الفريد
                        String userId = mAuth.getCurrentUser().getUid();

                        // 🚀 3. تجهيز بيانات المستخدم لحفظها في القاعدة
                        Map<String, Object> userMap = new HashMap<>();
                        userMap.put("name", name);
                        userMap.put("email", email);

                        // 🚀 4. حفظ البيانات في كولكشن Users
                        db.collection("Users").document(userId).set(userMap)
                                .addOnCompleteListener(dbTask -> {
                                    setLoading(false);
                                    if (dbTask.isSuccessful()) {
                                        Toast.makeText(RegisterActivity.this, "Hesap başarıyla oluşturuldu!", Toast.LENGTH_SHORT).show();
                                        Intent intent = new Intent(RegisterActivity.this, SmartWizardActivity.class);
                                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                        startActivity(intent);
                                        finish();
                                    } else {
                                        Toast.makeText(RegisterActivity.this, "Kayıt hatası (DB): " + dbTask.getException().getMessage(), Toast.LENGTH_LONG).show();
                                    }
                                });
                    } else {
                        setLoading(false);
                        Toast.makeText(RegisterActivity.this, "Kayıt Başarısız: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void setLoading(boolean isLoading) {
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        btnRegister.setEnabled(!isLoading);
        llGoToLogin.setEnabled(!isLoading);
        etName.setEnabled(!isLoading);
        etEmail.setEnabled(!isLoading);
        etPassword.setEnabled(!isLoading);
        etConfirmPassword.setEnabled(!isLoading);
    }
}