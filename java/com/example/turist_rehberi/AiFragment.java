package com.example.turist_rehberi;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class AiFragment extends Fragment {

    private RecyclerView rvChatStream;
    private ChatStreamAdapter adapter;
    private List<ChatMessage> messageList = new ArrayList<>();

    private EditText etMessage;
    private ImageView btnSend;
    private LinearLayout containerCapsules;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_ai, container, false);

        rvChatStream = view.findViewById(R.id.rvChatStream);
        etMessage = view.findViewById(R.id.etMessage);
        btnSend = view.findViewById(R.id.btnSend);
        containerCapsules = view.findViewById(R.id.containerCapsules);

        rvChatStream.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new ChatStreamAdapter(messageList);
        rvChatStream.setAdapter(adapter);

        // 🚀 تنظيف كود الترحيب: تم حذف إيموجي القلعة وجعل النص أرقى وأكاديمي متوافق مع اللاما
        if (messageList.isEmpty()) {
            messageList.add(new ChatMessage("Merhaba! Kastamonu Akıllı Rehberine hoş geldiniz. Size nasıl yardımcı olabilirim?", true));
            adapter.notifyDataSetChanged();
        }

        btnSend.setOnClickListener(v -> {
            String userQuery = etMessage.getText().toString().trim();
            if (!userQuery.isEmpty()) {
                sendMessageToAI(userQuery);
            }
        });

        setupQuickReplies();

        return view;
    }

    private void setupQuickReplies() {
        if (containerCapsules == null) return;
        for (int i = 0; i < containerCapsules.getChildCount(); i++) {
            View child = containerCapsules.getChildAt(i);
            if (child instanceof TextView) {
                TextView capsule = (TextView) child;
                capsule.setOnClickListener(v -> {
                    String query = capsule.getText().toString();
                    sendMessageToAI(query);
                });
            }
        }
    }

    private void sendMessageToAI(String userText) {
        final String GROQ_API_KEY = "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";

        messageList.add(new ChatMessage(userText, false));
        adapter.notifyItemInserted(messageList.size() - 1);
        rvChatStream.smoothScrollToPosition(messageList.size() - 1);
        etMessage.setText("");

        messageList.add(new ChatMessage("Kastamonu AI düşünüyor...", true));
        final int loadingIndex = messageList.size() - 1;
        adapter.notifyItemInserted(loadingIndex);
        rvChatStream.smoothScrollToPosition(loadingIndex);

        String prompt = "Sen, Kastamonu Üniversitesi tarafından geliştirilen 'Kastamonu Akıllı Turist Rehberi' adlı uygulamanın resmi yapay zeka asistanısın. " +
                "Görevin: Kullanıcılara Kastamonu'nun tarihi (Kastamonu Kalesi, Mahmut Bey Camii vb.), kültürü, doğası (Valla Kanyonu, Ilıca Şelalesi) ve yöresel lezzetleri (Etli Ekmek, Taşköprü Sarımsağı, Banduma) hakkında %100 doğru, akademik ve turistik bilgiler vermektir. " +
                "Kurallar: 1) Sadece Kastamonu ile ilgili soruları yanıtla, alakasız konuları kibarca reddet. " +
                "2) Cevapların kısa, net, akıcı ve bilgilendirici olsun. " +
                "3) Kullanıcı hangi dilde yazarsa (Türkçe, Arapça veya İngilizce) o dilde profesyonelce cevap ver. " +
                "Kullanıcının sorusu: " + userText;

        final String safePrompt = prompt.replace("\"", "\\\"").replace("\n", " ");

        new Thread(() -> {
            try {
                java.net.URL url = new java.net.URL("https://api.groq.com/openai/v1/chat/completions");
                java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty("Authorization", "Bearer " + GROQ_API_KEY);
                conn.setDoOutput(true);

                String jsonBody = "{"
                        + "\"model\": \"llama-3.3-70b-versatile\","
                        + "\"messages\": [{\"role\": \"user\", \"content\": \"" + safePrompt + "\"}]"
                        + "}";

                java.io.OutputStream os = conn.getOutputStream();
                byte[] input = jsonBody.getBytes("utf-8");
                os.write(input, 0, input.length);
                os.close();

                int responseCode = conn.getResponseCode();
                java.io.InputStream inputStream = (200 <= responseCode && responseCode <= 299)
                        ? conn.getInputStream() : conn.getErrorStream();

                java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(inputStream, "utf-8"));
                StringBuilder response = new StringBuilder();
                String responseLine;
                while ((responseLine = br.readLine()) != null) {
                    response.append(responseLine.trim());
                }
                br.close();

                if (responseCode == 200) {
                    org.json.JSONObject jsonObject = new org.json.JSONObject(response.toString());
                    String aiResponseText = jsonObject.getJSONArray("choices")
                            .getJSONObject(0)
                            .getJSONObject("message")
                            .getString("content");

                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            if (loadingIndex < messageList.size()) {
                                messageList.remove(loadingIndex);
                                messageList.add(new ChatMessage(aiResponseText, true));
                                adapter.notifyItemChanged(loadingIndex);
                                rvChatStream.smoothScrollToPosition(messageList.size() - 1);
                            }
                        });
                    }
                } else {
                    final String errorMsg = "Hata (Code " + responseCode + "): \n" + response.toString();
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            if (loadingIndex < messageList.size()) {
                                messageList.remove(loadingIndex);
                                messageList.add(new ChatMessage(errorMsg, true));
                                adapter.notifyItemChanged(loadingIndex);
                            }
                        });
                    }
                }
            } catch (Exception e) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        if (loadingIndex < messageList.size()) {
                            messageList.remove(loadingIndex);
                            messageList.add(new ChatMessage("Bağlantı hatası: İnternetinizi kontrol edin.", true));
                            adapter.notifyItemChanged(loadingIndex);
                        }
                    });
                }
            }
        }).start();
    }
}