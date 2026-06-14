package com.example.turist_rehberi;

import android.content.Intent;
import android.os.Bundle;
import android.widget.NumberPicker;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;
import java.util.Locale;

public class SmartWizardActivity extends AppCompatActivity {

    private NumberPicker npDays;
    private TextView tvDaysResult;
    private MaterialButton btnNextToBudget;
    private int selectedDays = 2; // القيمة الافتراضية

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_smart_wizard);

        npDays = findViewById(R.id.npDays);
        tvDaysResult = findViewById(R.id.tvDaysResult);
        btnNextToBudget = findViewById(R.id.btnNextToBudget);

        // 🚀 1. تحديد الحد الأقصى للأيام (خليناها 14، وتقدر تزيدها مستقبلاً لأي رقم)
        int maxDays = 14;

        npDays.setMinValue(1);
        npDays.setMaxValue(maxDays);

        // 🚀 2. تجهيز نصوص العجلة بتنسيق الأرقام الإنجليزية وإضافة كلمة Gün
        String[] dayValues = new String[maxDays];
        for (int i = 0; i < maxDays; i++) {
            // استخدام Locale.US عشان نضمن إن الأرقام تضل 1, 2, 3 وما تقلب عربي
            dayValues[i] = String.format(Locale.US, "%d Gün", (i + 1));
        }

        // تمرير النصوص الجاهزة للعجلة
        npDays.setDisplayedValues(dayValues);

        // إيقاف الدوران اللانهائي وتحديد القيمة المبدئية
        npDays.setWrapSelectorWheel(false);
        npDays.setValue(selectedDays);

        // عرض النص المبدئي عند فتح الشاشة
        tvDaysResult.setText(String.format(Locale.US, "Seçilen: %d Gün", selectedDays));

        // 🚀 3. تحديث النص عند السحب
        npDays.setOnValueChangedListener((picker, oldVal, newVal) -> {
            selectedDays = newVal;
            // استخدام Locale.US لضمان عدم تحول الرقم للعربي في النص السفلي أيضاً
            tvDaysResult.setText(String.format(Locale.US, "Seçilen: %d Gün", selectedDays));
        });

        // الانتقال للشاشة التالية
        btnNextToBudget.setOnClickListener(v -> {
            Intent intent = new Intent(SmartWizardActivity.this, BudgetWizardActivity.class);
            intent.putExtra("PREF_DAYS", selectedDays);
            startActivity(intent);
        });
    }
}