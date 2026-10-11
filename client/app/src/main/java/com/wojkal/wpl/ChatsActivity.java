package com.wojkal.wpl;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import org.json.JSONArray;
import org.json.JSONObject;

public class ChatsActivity extends Activity {

    private LinearLayout chatsContainer;
    private ProgressBar progressBar;
    private String serverIp = "";
    private final String androidVersion = android.os.Build.VERSION.RELEASE;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        SharedPreferences prefs = getSharedPreferences("WPLPrefs", MODE_PRIVATE);
        serverIp = prefs.getString("saved_ip", "");

        LinearLayout mainLayout = new LinearLayout(this);
        mainLayout.setOrientation(LinearLayout.VERTICAL);
        mainLayout.setBackgroundColor(Color.parseColor("#FFFFFF"));

        LinearLayout topBar = new LinearLayout(this);
        topBar.setOrientation(LinearLayout.HORIZONTAL);
        topBar.setBackgroundColor(Color.parseColor("#075E54"));
        topBar.setGravity(Gravity.CENTER_VERTICAL);
        topBar.setPadding(30, 25, 30, 25);

        TextView barTitle = new TextView(this);
        barTitle.setText("WhatsApp Legacy");
        barTitle.setTextColor(Color.WHITE);
        barTitle.setTextSize(20);
        barTitle.setTypeface(Typeface.DEFAULT_BOLD);
        topBar.addView(barTitle);
        mainLayout.addView(topBar, new LinearLayout.LayoutParams(-1, -2));

        progressBar = new ProgressBar(this);
        LinearLayout.LayoutParams progressParams = new LinearLayout.LayoutParams(-2, -2);
        progressParams.gravity = Gravity.CENTER;
        progressParams.setMargins(0, 60, 0, 0);
        mainLayout.addView(progressBar, progressParams);

        ScrollView scrollView = new ScrollView(this);
        chatsContainer = new LinearLayout(this);
        chatsContainer.setOrientation(LinearLayout.VERTICAL);
        
        scrollView.addView(chatsContainer, new ScrollView.LayoutParams(-1, -1));
        mainLayout.addView(scrollView, new LinearLayout.LayoutParams(-1, -1));

        setContentView(mainLayout);

        fetchChatsFromServer();
    }

    private void fetchChatsFromServer() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                HttpURLConnection conn = null;
                try {
                    String urlString = "http://" + serverIp + "/get-chats?v=" 
                            + java.net.URLEncoder.encode(androidVersion, "UTF-8");

                    URL url = new URL(urlString);
                    conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("GET");
                    conn.setConnectTimeout(30000);
                    conn.setReadTimeout(30000);

                    if (conn.getResponseCode() == 200) {
                        BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
                        StringBuilder response = new StringBuilder();
                        String line;
                        while ((line = in.readLine()) != null) {
                            response.append(line);
                        }
                        in.close();

                        final String rawJson = response.toString();
                        
                        if (rawJson.contains("\"success\":true")) {
                            JSONObject jsonResponse = new JSONObject(rawJson);
                            final JSONArray chatsArray = jsonResponse.getJSONArray("chats");

                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    progressBar.setVisibility(View.GONE);
                                    renderNativeChats(chatsArray); 
                                }
                            });
                            return;
                        }
                    }
                    throw new Exception("Server communication error");
                } catch (final Exception e) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            progressBar.setVisibility(View.GONE);
                            Toast.makeText(ChatsActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });
                } finally {
                    if (conn != null) conn.disconnect();
                }
            }
        }).start();
    }

    private void renderNativeChats(JSONArray chats) {
        try {
            chatsContainer.removeAllViews();

            if (chats.length() == 0) {
                TextView emptyTv = new TextView(this);
                emptyTv.setText("No active chats available.");
                emptyTv.setGravity(Gravity.CENTER);
                emptyTv.setPadding(0, 60, 0, 0);
                chatsContainer.addView(emptyTv);
                return;
            }

            for (int i = 0; i < chats.length(); i++) {
                JSONObject chat = chats.getJSONObject(i);
                final String chatId = chat.getString("id");
                final String chatName = chat.getString("name");
                String lastMessage = chat.optString("lastMessage", ""); 

                LinearLayout rowLayout = new LinearLayout(this);
                rowLayout.setOrientation(LinearLayout.VERTICAL);
                rowLayout.setPadding(35, 25, 35, 25);
                rowLayout.setBackgroundColor(Color.WHITE);

                TextView nameTv = new TextView(this);
                nameTv.setText(chatName);
                nameTv.setTextSize(17);
                nameTv.setTextColor(Color.parseColor("#000000"));
                nameTv.setTypeface(Typeface.DEFAULT_BOLD);

                TextView msgTv = new TextView(this);
                msgTv.setText(lastMessage.isEmpty() ? "No messages" : lastMessage);
                msgTv.setTextSize(14);
                msgTv.setTextColor(Color.parseColor("#757575"));
                msgTv.setSingleLine(true);
                msgTv.setPadding(0, 6, 0, 0);

                rowLayout.addView(nameTv);
                rowLayout.addView(msgTv);

                View divider = new View(this);
                divider.setBackgroundColor(Color.parseColor("#EEEEEE"));
                LinearLayout.LayoutParams lineParams = new LinearLayout.LayoutParams(-1, 2);

                rowLayout.setClickable(true);
                rowLayout.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        android.content.Intent intent = new android.content.Intent(ChatsActivity.this, MessageActivity.class);
                        intent.putExtra("chat_id", chatId);
                        intent.putExtra("chat_name", chatName);
                        startActivity(intent);
                    }
                });

                chatsContainer.addView(rowLayout);
                chatsContainer.addView(divider, lineParams);
            }
        } catch (Exception e) {
            Toast.makeText(this, "UI Render Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
}

