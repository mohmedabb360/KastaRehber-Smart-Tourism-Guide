package com.example.turist_rehberi;

import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class ChatStreamAdapter extends RecyclerView.Adapter<ChatStreamAdapter.ViewHolder> {

    private List<ChatMessage> messageList;

    public ChatStreamAdapter(List<ChatMessage> messageList) {
        this.messageList = messageList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat_stream, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ChatMessage message = messageList.get(position);
        holder.tvMessageText.setText(message.getText());

        if (message.isFromAI()) {
            // 🚀 رد الـ AI: سحب الكرت لليسار، تفعيل نجوم الـ AI، وإخفاء أيقونة المستخدم
            holder.containerLayout.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);

            holder.tvMessageText.setTextColor(Color.parseColor("#C5A059"));
            holder.tvMessageText.setTypeface(null, Typeface.BOLD);

            holder.imgRobotIcon.setVisibility(View.VISIBLE);
            holder.imgUserIcon.setVisibility(View.GONE);
        } else {
            // 🚀 رسالة المستخدم: سحب الكرت لليمين تماماً، تفعيل بروفايل المستخدم، وإخفاء النجوم
            holder.containerLayout.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);

            holder.tvMessageText.setTextColor(Color.parseColor("#052012"));
            holder.tvMessageText.setTypeface(null, Typeface.NORMAL);

            holder.imgRobotIcon.setVisibility(View.GONE);
            holder.imgUserIcon.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public int getItemCount() {
        return messageList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvMessageText;
        ImageView imgRobotIcon, imgUserIcon;
        LinearLayout containerLayout;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMessageText = itemView.findViewById(R.id.tvMessageText);
            imgRobotIcon = itemView.findViewById(R.id.imgRobotIcon);
            imgUserIcon = itemView.findViewById(R.id.imgUserIcon);
            containerLayout = itemView.findViewById(R.id.containerLayout);
        }
    }
}