package com.example.turist_rehberi;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import androidx.appcompat.app.AppCompatActivity;

public class LoadingActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_loading); // تأكد إن اسم ملف التصميم عندك هيك

        // محاكاة وقت التحميل (مثلاً ثانيتين) عشان الخوارزمية تبين إنها عم تفكر
        new Handler(Looper.getMainLooper()).postDelayed(() -> {

            // الانتقال للداشبورد
            Intent nextIntent = new Intent(LoadingActivity.this, DashboardActivity.class);

            // 🚀 السطر السحري: استنساخ كل البيانات اللي إجت من الويزارد ورميها للداشبورد
            if (getIntent().getExtras() != null) {
                nextIntent.putExtras(getIntent().getExtras());
            }

            startActivity(nextIntent);
            finish(); // إنهاء شاشة التحميل عشان ما يرجع لها بالسهم

        }, 2000); // 2000 ميلي ثانية = ثانيتين
    }
}