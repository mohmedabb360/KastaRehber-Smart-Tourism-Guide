package com.example.turist_rehberi;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import java.util.List;

public class ImageSliderAdapter extends RecyclerView.Adapter<ImageSliderAdapter.SliderViewHolder> {

    private List<String> imageUrls;
    private Context context;

    // Constructor
    public ImageSliderAdapter(Context context, List<String> imageUrls) {
        this.context = context;
        this.imageUrls = imageUrls;
    }

    @NonNull
    @Override
    public SliderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // إنشاء عرض بسيط للصورة
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_image_slider, parent, false);
        return new SliderViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SliderViewHolder holder, int position) {
        // تحميل الصورة باستخدام Glide بالرابط الموجود في الليستة
        String currentUrl = imageUrls.get(position);

        // 🚀 تنظيف الكاش لمنع تداخل الصور القديمة أثناء السكرول السريع
        Glide.with(context).clear(holder.imageView);

        Glide.with(context)
                .load(currentUrl)
                .transform(new CenterCrop()) // قص الصورة لتعبي المكان بدون تمطيط
                .placeholder(new ColorDrawable(Color.parseColor("#E0E0E0"))) // 🚀 الإصلاح: لون رمادي ناعم أثناء التحميل
                .error(new ColorDrawable(Color.parseColor("#E0E0E0")))       // 🚀 لون رمادي ناعم في حال فشل تحميل الرابط
                .into(holder.imageView);
    }

    @Override
    public int getItemCount() {
        return imageUrls.size();
    }

    // ViewHolder بسيط
    static class SliderViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;

        public SliderViewHolder(@NonNull View itemView) {
            super(itemView);
            // تأكد من وجود ImageView بملف item_image_slider.xml
            imageView = itemView.findViewById(R.id.imageViewSlider);
        }
    }
}