package com.wojkal.wpl;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
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

public class MessageActivity extends Activity {

    private LinearLayout messageContainer;
    private ProgressBar progressBar;
    private EditText inputMessage;
    private Button btnSend;
    private String serverIp = "";
    private String chatId = "";
    private String chatName = "";

    private final String androidVersion = android.os.Build.VERSION.RELEASE;

    private Handler autoRefreshHandler = new Handler();
    private Runnable refreshRunnable;
    private final int REFRESH_INTERVAL = 5000; 

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        chatId = getIntent().getStringExtra("chat_id");
        chatName = getIntent().getStringExtra("chat_name");

        SharedPreferences prefs = getSharedPreferences("WPLPrefs", MODE_PRIVATE);
        serverIp = prefs.getString("saved_ip", "");

        LinearLayout mainLayout = new LinearLayout(this);
        mainLayout.setOrientation(LinearLayout.VERTICAL);
        mainLayout.setBackgroundColor(Color.parseColor("#ECE5DD")); 

        LinearLayout topBar = new LinearLayout(this);
        topBar.setOrientation(LinearLayout.HORIZONTAL);
        topBar.setBackgroundColor(Color.parseColor("#075E54"));
        topBar.setGravity(Gravity.CENTER_VERTICAL);
        topBar.setPadding(30, 20, 30, 20);

        TextView barTitle = new TextView(this);
        barTitle.setText(chatName);
        barTitle.setTextColor(Color.WHITE);
        barTitle.setTextSize(18);
        barTitle.setTypeface(Typeface.DEFAULT_BOLD);
        topBar.addView(barTitle);
        mainLayout.addView(topBar, new LinearLayout.LayoutParams(-1, -2));

        progressBar = new ProgressBar(this);
        LinearLayout.LayoutParams prgParams = new LinearLayout.LayoutParams(-2, -2);
        prgParams.gravity = Gravity.CENTER;
        mainLayout.addView(progressBar, prgParams);

        ScrollView scrollView = new ScrollView(this);
        LinearLayout.LayoutParams scrollParams = new LinearLayout.LayoutParams(-1, 0, 1f); 
        messageContainer = new LinearLayout(this);
        messageContainer.setOrientation(LinearLayout.VERTICAL);
        messageContainer.setPadding(20, 20, 20, 20);
        scrollView.addView(messageContainer, new ScrollView.LayoutParams(-1, -1));
        mainLayout.addView(scrollView, scrollParams);

        LinearLayout bottomBar = new LinearLayout(this);
        bottomBar.setOrientation(LinearLayout.HORIZONTAL);
        bottomBar.setBackgroundColor(Color.WHITE);
        bottomBar.setPadding(15, 15, 15, 15);
        bottomBar.setGravity(Gravity.CENTER_VERTICAL);

        inputMessage = new EditText(this);
        inputMessage.setHint("Type a message");
        inputMessage.setSingleLine(false);
        LinearLayout.LayoutParams inputParams = new LinearLayout.LayoutParams(0, -2, 1f);
        bottomBar.addView(inputMessage, inputParams);

        btnSend = new Button(this);
        btnSend.setText("Send");
        btnSend.setBackgroundColor(Color.parseColor("#128C7E"));
        btnSend.setTextColor(Color.WHITE);
        bottomBar.addView(btnSend, new LinearLayout.LayoutParams(-2, -2));
        
        mainLayout.addView(bottomBar, new LinearLayout.LayoutParams(-1, -2));
        setContentView(mainLayout);

        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String msgText = inputMessage.getText().toString().trim();
                if (!msgText.isEmpty()) {
                    sendMessage(msgText);
                }
            }
        });

        refreshRunnable = new Runnable() {
            @Override
            public void run() {
                loadMessagesSilently(); 
                autoRefreshHandler.postDelayed(this, REFRESH_INTERVAL);
            }
        };

        loadMessages(); 
    }

    @Override
    protected void onResume() {
        super.onResume();
        autoRefreshHandler.postDelayed(refreshRunnable, REFRESH_INTERVAL);
    }

    @Override
    protected void onPause() {
        super.onPause();
        autoRefreshHandler.removeCallbacks(refreshRunnable);
    }

    private void sendMessage(final String messageText) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                HttpURLConnection conn = null;
                try {
                    String urlString = "http://" + serverIp + "/send-message?id=" 
                            + java.net.URLEncoder.encode(chatId, "UTF-8") 
                            + "&text=" + java.net.URLEncoder.encode(messageText, "UTF-8")
                            + "&v=" + java.net.URLEncoder.encode(androidVersion, "UTF-8");
                    
                    URL url = new URL(urlString);
                    conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("GET");
                    conn.setConnectTimeout(15000);

                    if (conn.getResponseCode() == 200) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                inputMessage.setText("");
                                loadMessagesSilently(); 
                            }
                        });
                    } else {
                        throw new Exception("Send error");
                    }
                } catch (Exception e) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(MessageActivity.this, "Message could not be sent.", Toast.LENGTH_SHORT).show();
                        }
                    });
                } finally {
                    if (conn != null) conn.disconnect();
                }
            }
        }).start();
    }

    private void loadMessages() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                HttpURLConnection conn = null;
                try {
                    String urlString = "http://" + serverIp + "/get-messages?id=" 
                            + java.net.URLEncoder.encode(chatId, "UTF-8")
                            + "&v=" + java.net.URLEncoder.encode(androidVersion, "UTF-8");
                            
                    URL url = new URL(urlString);
                    conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("GET");
                    conn.setConnectTimeout(15000);

                    if (conn.getResponseCode() == 200) {
                        BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
                        StringBuilder response = new StringBuilder();
                        String line;
                        while ((line = in.readLine()) != null) { response.append(line); }
                        in.close();

                        JSONObject jsonResponse = new JSONObject(response.toString());
                        if (jsonResponse.getBoolean("success")) {
                            final JSONArray msgs = jsonResponse.getJSONArray("messages");
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    progressBar.setVisibility(View.GONE);
                                    renderMessages(msgs);
                                }
                            });
                            return;
                        }
                    }
                    throw new Exception("Error");
                } catch (Exception e) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            progressBar.setVisibility(View.GONE);
                            Toast.makeText(MessageActivity.this, "Messages could not be loaded.", Toast.LENGTH_SHORT).show();
                        }
                    });
                } finally {
                    if (conn != null) conn.disconnect();
                }
            }
        }).start();
    }

    private void loadMessagesSilently() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                HttpURLConnection conn = null;
                try {
                    String urlString = "http://" + serverIp + "/get-messages?id=" 
                            + java.net.URLEncoder.encode(chatId, "UTF-8")
                            + "&v=" + java.net.URLEncoder.encode(androidVersion, "UTF-8");

                    URL url = new URL(urlString);
                    conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("GET");
                    conn.setConnectTimeout(10000);

                    if (conn.getResponseCode() == 200) {
                        BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
                        StringBuilder response = new StringBuilder();
                        String line;
                        while ((line = in.readLine()) != null) { response.append(line); }
                        in.close();

                        JSONObject jsonResponse = new JSONObject(response.toString());
                        if (jsonResponse.getBoolean("success")) {
                            final JSONArray msgs = jsonResponse.getJSONArray("messages");
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    renderMessages(msgs);
                                }
                            });
                        }
                    }
                } catch (Exception e) {
                } finally {
                    if (conn != null) conn.disconnect();
                }
            }
        }).start();
    }

    private void renderMessages(JSONArray msgs) {
        try {
            messageContainer.removeAllViews();
            for (int i = 0; i < msgs.length(); i++) {
                JSONObject m = msgs.getJSONObject(i);
                String body = m.getString("body");
                boolean fromMe = m.getBoolean("fromMe");

                LinearLayout bubbleLayout = new LinearLayout(this);
                bubbleLayout.setOrientation(LinearLayout.VERTICAL);
                bubbleLayout.setPadding(20, 15, 20, 15);
                
                LinearLayout.LayoutParams bubbleParams = new LinearLayout.LayoutParams(-2, -2);
                bubbleParams.setMargins(0, 10, 0, 10);

                TextView textTv = new TextView(this);
                textTv.setText(body);
                textTv.setTextSize(16);
                textTv.setTextColor(Color.BLACK);
                bubbleLayout.addView(textTv);

                if (fromMe) {
                    bubbleParams.gravity = Gravity.RIGHT;
                    bubbleLayout.setBackgroundColor(Color.parseColor("#E1FFC7"));
                } else {
                    bubbleParams.gravity = Gravity.LEFT;
                    bubbleLayout.setBackgroundColor(Color.WHITE);
                }

                messageContainer.addView(bubbleLayout, bubbleParams);
            }
        } catch (Exception e) { e.printStackTrace(); }
    }
}

