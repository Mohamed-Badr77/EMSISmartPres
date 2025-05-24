package com.example.emsimarkpresence;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.emsimarkpresence.BuildConfig;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class Assistant_virtuel extends AppCompatActivity {
    private final String API_KEY = BuildConfig.AI_ASSISTANT_API_KEY;
    private static final MediaType JSON = MediaType.parse("application/json");
    private final String API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=" + API_KEY;


    private EditText editMessage;
    private Button btnSend;
    private RecyclerView chatRecycler;
    private ChatAdapter chatAdapter;
    private List<ChatMessage> messages = new ArrayList<>();
    private OkHttpClient client = new OkHttpClient();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_assistant_virtuel);

        // Initialize views
        editMessage = findViewById(R.id.prompt);
        btnSend = findViewById(R.id.btnSend);
        chatRecycler = findViewById(R.id.chat_recycler);

        // Setup RecyclerView
        chatAdapter = new ChatAdapter(messages);
        chatRecycler.setLayoutManager(new LinearLayoutManager(this));
        chatRecycler.setAdapter(chatAdapter);

        btnSend.setOnClickListener(v -> {
            String userMessage = editMessage.getText().toString().trim();
            if (!userMessage.isEmpty()) {
                // Add user message to chat
                addMessage(userMessage, true);
                editMessage.setText("");
                // Send to Gemini
                sendMessageToGemini(userMessage);
            }
        });
    }

    private void addMessage(String message, boolean isUser) {
        runOnUiThread(() -> {
            messages.add(new ChatMessage(message, isUser));
            chatAdapter.notifyItemInserted(messages.size() - 1);
            chatRecycler.smoothScrollToPosition(messages.size() - 1);
        });
    }

    private void sendMessageToGemini(String message) {
        try {
            JSONObject json = new JSONObject();
            JSONArray contents = new JSONArray();
            JSONObject contentItem = new JSONObject();
            JSONArray parts = new JSONArray();
            JSONObject part = new JSONObject();

            part.put("text", message);
            parts.put(part);
            contentItem.put("parts", parts);
            contents.put(contentItem);
            json.put("contents", contents);

            RequestBody body = RequestBody.create(json.toString(), JSON);
            Request request = new Request.Builder()
                    .url(API_URL)
                    .post(body)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    addMessage("Error: " + e.getMessage(), false);
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    if (response.isSuccessful()) {
                        try {
                            String responseData = response.body().string();
                            JSONObject jsonResponse = new JSONObject(responseData);
                            JSONArray candidates = jsonResponse.getJSONArray("candidates");
                            String text = candidates.getJSONObject(0)
                                    .getJSONObject("content")
                                    .getJSONArray("parts")
                                    .getJSONObject(0)
                                    .getString("text");
                            addMessage(text, false);
                        } catch (JSONException e) {
                            addMessage("Error parsing response", false);
                        }
                    } else {
                        addMessage("Error: " + response.message(), false);
                    }
                }
            });
        } catch (JSONException e) {
            addMessage("Error creating request", false);
        }
    }

    // ChatAdapter class
    private static class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.MessageViewHolder> {
        private final List<ChatMessage> messages;

        public ChatAdapter(List<ChatMessage> messages) {
            this.messages = messages;
        }

        @NonNull
        @Override
        public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(
                    viewType == 0 ? R.layout.item_user_message : R.layout.item_ai_message,
                    parent, false);
            return new MessageViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
            holder.messageText.setText(messages.get(position).getContent());
        }

        @Override
        public int getItemCount() {
            return messages.size();
        }

        @Override
        public int getItemViewType(int position) {
            return messages.get(position).isUser() ? 0 : 1;
        }

        static class MessageViewHolder extends RecyclerView.ViewHolder {
            TextView messageText;

            public MessageViewHolder(@NonNull View itemView) {
                super(itemView);
                messageText = itemView.findViewById(R.id.message_text);
            }
        }
    }

    // ChatMessage class
    private static class ChatMessage {
        private final String content;
        private final boolean isUser;

        public ChatMessage(String content, boolean isUser) {
            this.content = content;
            this.isUser = isUser;
        }

        public String getContent() {
            return content;
        }

        public boolean isUser() {
            return isUser;
        }
    }
}