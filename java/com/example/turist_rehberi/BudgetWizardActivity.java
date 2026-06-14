package com.example.turist_rehberi;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.slider.LabelFormatter;
import com.google.android.material.slider.Slider;
import java.util.Locale;

public class BudgetWizardActivity extends AppCompatActivity {

    private Slider sliderBudget;
    private TextView tvBudgetResult;
    private TextView tvMaxBudget;
    private ImageView btnEditBudget;
    private MaterialButton btnNextToInterests;

    private int days;
    private String selectedBudget = "5.000 ₺"; // القيمة التي ستُرسل للـ Intent/Firebase

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_budget_wizard);

        days = getIntent().getIntExtra("PREF_DAYS", 3);

        sliderBudget = findViewById(R.id.sliderBudget);
        tvBudgetResult = findViewById(R.id.tvBudgetResult);
        tvMaxBudget = findViewById(R.id.tvMaxBudget);
        btnEditBudget = findViewById(R.id.btnEditBudget);
        btnNextToInterests = findViewById(R.id.btnNextToInterests);

        // 1. منسق المؤشر الصغير فوق السلايدر
        sliderBudget.setLabelFormatter(new LabelFormatter() {
            @NonNull
            @Override
            public String getFormattedValue(float value) {
                int intValue = (int) value;
                return String.format(Locale.US, "%,d ₺", intValue).replace(",", ".");
            }
        });

        // 2. مستمع التغيير عند سحب السلايدر باليد
        sliderBudget.addOnChangeListener(new Slider.OnChangeListener() {
            @Override
            public void onValueChange(@NonNull Slider slider, float value, boolean fromUser) {
                if (fromUser) {
                    int intValue = (int) value;
                    selectedBudget = String.format(Locale.US, "%,d ₺", intValue).replace(",", ".");
                    tvBudgetResult.setText(selectedBudget);
                }
            }
        });

        // 🚀 3. الحل الاحترافي الفخم: تصميم واجهة منبثقة مودرن (BottomSheet) مدمجة برمجياً لحل مشكلة التصميم البشع نهائياً
        btnEditBudget.setOnClickListener(v -> {
            BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(BudgetWizardActivity.this);

            // الحاوية الأساسية للنافذة (بلون بيج/كريمي نفس خلفية تطبيقك)
            LinearLayout sheetLayout = new LinearLayout(BudgetWizardActivity.this);
            sheetLayout.setOrientation(LinearLayout.VERTICAL);
            sheetLayout.setPadding(64, 64, 64, 80);
            sheetLayout.setBackgroundColor(Color.parseColor("#F8F6F0"));

            // عنوان النافذة باللغة التركية وبخط عريض بالأخضر الداكن
            TextView tvTitle = new TextView(BudgetWizardActivity.this);
            tvTitle.setText("Manuel Bütçe Girişi");
            tvTitle.setTextColor(Color.parseColor("#052012"));
            tvTitle.setTextSize(20);
            tvTitle.setTypeface(null, Typeface.BOLD);
            tvTitle.setGravity(Gravity.CENTER);
            sheetLayout.addView(tvTitle);

            // حقل الإدخال الرقمي الأنيق في المنتصف بدون حواف مشوهة
            EditText etInput = new EditText(BudgetWizardActivity.this);
            etInput.setInputType(InputType.TYPE_CLASS_NUMBER);
            etInput.setTextSize(26);
            etInput.setTextColor(Color.parseColor("#052012"));
            etInput.setTypeface(null, Typeface.BOLD);
            etInput.setGravity(Gravity.CENTER);
            etInput.setHint("5000");

            LinearLayout.LayoutParams lpInput = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            lpInput.setMargins(0, 48, 0, 48);
            etInput.setLayoutParams(lpInput);

            // جلب الرقم الحالي بدون علامات لوضعه داخل الحقل تلقائياً
            String currentNumber = tvBudgetResult.getText().toString()
                    .replace(".", "")
                    .replace(" ₺", "")
                    .trim();
            etInput.setText(currentNumber);
            sheetLayout.addView(etInput);

            // زر الحفظ الفخم المطور (مطابق لأزرار تطبيقك تماماً)
            MaterialButton btnConfirm = new MaterialButton(BudgetWizardActivity.this);
            btnConfirm.setText("Güncelle");
            btnConfirm.setTextSize(16);
            btnConfirm.setTypeface(null, Typeface.BOLD);
            btnConfirm.setTextColor(Color.WHITE);
            btnConfirm.setBackgroundColor(Color.parseColor("#052012")); // لون تطبيقك الأخضر
            btnConfirm.setCornerRadius(20); // حواف دائرية فخمة

            LinearLayout.LayoutParams lpBtn = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, 140); // ارتفاع مريح للضغط
            btnConfirm.setLayoutParams(lpBtn);
            sheetLayout.addView(btnConfirm);

// منطق الحفظ والتأكيد الذكي والآمن عند الضغط على الزر
            btnConfirm.setOnClickListener(v1 -> {
                String inputText = etInput.getText().toString().trim();
                if (!inputText.isEmpty()) {
                    int enteredValue = Integer.parseInt(inputText);

                    // 1. حد الأمان الأدنى
                    if (enteredValue < 400) enteredValue = 400;

                    // 🚀 2. الحل السحري لمنع الكراش: تقريب الرقم لأقرب مضاعف للـ 200 (الـ stepSize)
                    enteredValue = Math.round(enteredValue / 200.0f) * 200;

                    // 3. حركة التمدد الديناميكي الذكية: لو الرقم أكبر من حد السلايدر الحالي
                    if (enteredValue > sliderBudget.getValueTo()) {
                        sliderBudget.setValueTo((float) enteredValue);
                        tvMaxBudget.setText(String.format(Locale.US, "%,d ₺", enteredValue).replace(",", "."));
                    }

                    // 4. تحديث المتغير والنصوص بالشاشة والفايربيس بدقة بالرقم المقرب الآمن
                    selectedBudget = String.format(Locale.US, "%,d ₺", enteredValue).replace(",", ".");
                    tvBudgetResult.setText(selectedBudget);

                    // 5. تحريك مؤشر السلايدر للموقع الجديد برمجياً بدون أي تعليق أو كراش
                    sliderBudget.setValue((float) enteredValue);
                }
                bottomSheetDialog.dismiss(); // إغلاق النافذة بنعومة
            });

            // عرض التصميم المطور بالكامل داخل الـ BottomSheet
            bottomSheetDialog.setContentView(sheetLayout);
            bottomSheetDialog.show();

            // فتح الكيبورد تلقائياً وتحديد النص بالكامل لتجربة مستخدم سريعة
            etInput.requestFocus();
            etInput.selectAll();
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.showSoftInput(etInput, InputMethodManager.SHOW_IMPLICIT);
            }
        });

        btnNextToInterests.setOnClickListener(v -> {
            Intent intent = new Intent(BudgetWizardActivity.this, InterestsWizardActivity.class);
            intent.putExtra("PREF_DAYS", days);
            intent.putExtra("PREF_BUDGET", selectedBudget); // القيمة المحدثة بالكامل تذهب هنا
            startActivity(intent);
        });
    }
}