package com.wojkal.wpl;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class MainActivity extends Activity {

    private LinearLayout loadingLayout;
    private LinearLayout errorLayout;
    private ScrollView setupLayout;
    private FrameLayout contentLayout;
    
    private EditText inputServerIp;
    private EditText inputPhoneNumber;
    private Button btnStart;
    
    private SharedPreferences prefs;
    private boolean isShowingCode = false;

    private Handler handler = new Handler();
    private Runnable checkStatusRunnable;
    private String currentServerIp = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        prefs = getSharedPreferences("WPLPrefs", MODE_PRIVATE);
        currentServerIp = prefs.getString("saved_ip", "");

        LinearLayout mainLayout = new LinearLayout(this);
        mainLayout.setOrientation(LinearLayout.VERTICAL);
        mainLayout.setBackgroundColor(Color.parseColor("#ECE5DD")); 

        LinearLayout topBar = new LinearLayout(this);
        topBar.setOrientation(LinearLayout.HORIZONTAL);
        topBar.setBackgroundColor(Color.parseColor("#075E54"));
        topBar.setGravity(Gravity.CENTER_VERTICAL);
        topBar.setPadding(25, 20, 25, 20);

        TextView barTitle = new TextView(this);
        barTitle.setText("WhatsAppLegacy"); 
        barTitle.setTextColor(Color.WHITE);
        barTitle.setTextSize(20);
        barTitle.setTypeface(Typeface.DEFAULT_BOLD);
        topBar.addView(barTitle);
        mainLayout.addView(topBar, new LinearLayout.LayoutParams(-1, -2));

        contentLayout = new FrameLayout(this);
        LinearLayout.LayoutParams wrapParams = new LinearLayout.LayoutParams(-2, -2);
        wrapParams.gravity = Gravity.CENTER;

        loadingLayout = new LinearLayout(this);
        loadingLayout.setOrientation(LinearLayout.VERTICAL);
        loadingLayout.setGravity(Gravity.CENTER);
        ProgressBar pb = new ProgressBar(this);
        TextView lt = new TextView(this);
        lt.setText("Checking Session Status...");
        lt.setTextColor(Color.BLACK);
        lt.setPadding(0, 15, 0, 0);
        loadingLayout.addView(pb, wrapParams);
        loadingLayout.addView(lt, wrapParams);
        contentLayout.addView(loadingLayout, new FrameLayout.LayoutParams(-1, -1));

        errorLayout = new LinearLayout(this);
        errorLayout.setOrientation(LinearLayout.VERTICAL);
        errorLayout.setGravity(Gravity.CENTER);
        errorLayout.setVisibility(View.GONE);
        
        ImageView errorImg = new ImageView(this);
        errorImg.setImageResource(R.drawable.no_internet); 
        LinearLayout.LayoutParams imgParams = new LinearLayout.LayoutParams(128, 128);
        imgParams.gravity = Gravity.CENTER;
        
        TextView errorText = new TextView(this);
        errorText.setText("No Internet Connection");
        errorText.setTextSize(18);
        errorText.setTextColor(Color.BLACK);
        errorText.setPadding(0, 20, 0, 20);
        
        Button retryBtn = new Button(this);
        retryBtn.setText("Try Again");
        errorLayout.addView(errorImg, imgParams);
        errorLayout.addView(errorText, wrapParams);
        errorLayout.addView(retryBtn, wrapParams);
        contentLayout.addView(errorLayout, new FrameLayout.LayoutParams(-1, -1));

        setupLayout = new ScrollView(this);
        setupLayout.setVisibility(View.GONE);
        
        LinearLayout formLayout = new LinearLayout(this);
        formLayout.setOrientation(LinearLayout.VERTICAL);
        formLayout.setPadding(40, 40, 40, 40);
        formLayout.setGravity(Gravity.CENTER_HORIZONTAL);

        TextView lblServer = new TextView(this);
        lblServer.setText("1. Server Address (IP:Port)");
        lblServer.setTextColor(Color.parseColor("#075E54"));
        lblServer.setTypeface(Typeface.DEFAULT_BOLD);
        lblServer.setPadding(0, 20, 0, 10);
        formLayout.addView(lblServer);

                
        inputServerIp = new EditText(this);
        inputServerIp.setHint("e.g., 192.168.1.50:3000");
        inputServerIp.setSingleLine(true);
        inputServerIp.setText(currentServerIp); 
        inputServerIp.setPadding(20, 20, 20, 20); 

        android.graphics.drawable.GradientDrawable gdServer = new android.graphics.drawable.GradientDrawable();
        gdServer.setColor(Color.WHITE); 
        gdServer.setCornerRadius(12f);  
        gdServer.setStroke(2, Color.parseColor("#CCCCCC")); 
        inputServerIp.setBackgroundDrawable(gdServer);

        formLayout.addView(inputServerIp);

        TextView lblPhone = new TextView(this);
        lblPhone.setText("2. WhatsApp Phone Number");
        lblPhone.setTextColor(Color.parseColor("#075E54"));
        lblPhone.setTypeface(Typeface.DEFAULT_BOLD);
        lblPhone.setPadding(0, 30, 0, 10);
        formLayout.addView(lblPhone);

        inputPhoneNumber = new EditText(this);
        inputPhoneNumber.setHint("e.g., +905xxxxxxxxx");
        inputPhoneNumber.setSingleLine(true);
        inputPhoneNumber.setText(prefs.getString("saved_phone", "")); 
        inputPhoneNumber.setPadding(20, 20, 20, 20); 

        android.graphics.drawable.GradientDrawable gdPhone = new android.graphics.drawable.GradientDrawable();
        gdPhone.setColor(Color.WHITE); 
        gdPhone.setCornerRadius(12f);  
        gdPhone.setStroke(2, Color.parseColor("#CCCCCC")); 
        inputPhoneNumber.setBackgroundDrawable(gdPhone);

        formLayout.addView(inputPhoneNumber);

        View spacer = new View(this);
        formLayout.addView(spacer, new LinearLayout.LayoutParams(-1, 40));

		        btnStart = new Button(this);
        btnStart.setText("Let's go!");
        btnStart.setTextColor(Color.WHITE);
        btnStart.setTypeface(Typeface.DEFAULT_BOLD);
        btnStart.setPadding(30, 20, 30, 20);

        android.graphics.drawable.GradientDrawable gdButton = new android.graphics.drawable.GradientDrawable();
        gdButton.setColor(Color.parseColor("#25D366")); 
        gdButton.setCornerRadius(12f); 
        btnStart.setBackgroundDrawable(gdButton);

        formLayout.addView(btnStart, new LinearLayout.LayoutParams(-1, -2));

				
        btnStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                currentServerIp = inputServerIp.getText().toString().trim();
                String phone = inputPhoneNumber.getText().toString().trim();
                
                if (currentServerIp.isEmpty() || phone.isEmpty()) {
                    Toast.makeText(MainActivity.this, "Fields cannot be blank.", Toast.LENGTH_LONG).show();
                    return; 
                }
                
                SharedPreferences.Editor editor = prefs.edit();
                editor.putString("saved_ip", currentServerIp);
                editor.putString("saved_phone", phone);
                editor.commit();

                startWhatsAppConnection(currentServerIp, phone);
            }
        });

        setupLayout.addView(formLayout, new ScrollView.LayoutParams(-1, -1));
        contentLayout.addView(setupLayout, new FrameLayout.LayoutParams(-1, -1));
        mainLayout.addView(contentLayout, new LinearLayout.LayoutParams(-1, -1));
        setContentView(mainLayout);

        retryBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkAutoLoginOrShowForm();
            }
        });

        checkAutoLoginOrShowForm();
    }

    private void checkAutoLoginOrShowForm() {
        if (!isOnline()) {
            loadingLayout.setVisibility(View.GONE);
            setupLayout.setVisibility(View.GONE);
            errorLayout.setVisibility(View.VISIBLE);
            return;
        }

        if (currentServerIp.isEmpty()) {
            loadingLayout.setVisibility(View.GONE);
            setupLayout.setVisibility(View.VISIBLE);
            return;
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                HttpURLConnection conn = null;
                try {
                    URL url = new URL("http://" + currentServerIp + "/status");
                    conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("GET");
                    conn.setConnectTimeout(4000); 
                    conn.setReadTimeout(4000);

                    BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = in.readLine()) != null) {
                        response.append(line);
                    }
                    in.close();

                    if (response.toString().contains("\"loggedIn\":true")) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Intent intent = new Intent(MainActivity.this, ChatsActivity.class);
                                startActivity(intent);
                                finish();
                            }
                        });
                        return;
                    }
                } catch (Exception e) {
                } finally {
                    if (conn != null) conn.disconnect();
                }

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        loadingLayout.setVisibility(View.GONE);
                        setupLayout.setVisibility(View.VISIBLE);
                    }
                });
            }
        }).start();
    }

    private void startWhatsAppConnection(final String ip, final String phoneNumber) {
        setupLayout.setVisibility(View.GONE);
        loadingLayout.setVisibility(View.VISIBLE);
        
        new Thread(new Runnable() {
            @Override
            public void run() {
                java.net.HttpURLConnection conn = null;
                try {
                    String urlString = "http://" + ip + "/get-code?phone=" + java.net.URLEncoder.encode(phoneNumber, "UTF-8");
                    java.net.URL url = new java.net.URL(urlString);
                    
                    conn = (java.net.HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("GET");
                    conn.setConnectTimeout(120000); 
                    conn.setReadTimeout(120000);
                    
                    if (conn.getResponseCode() == 200) { 
                        java.io.BufferedReader in = new java.io.BufferedReader(new java.io.InputStreamReader(conn.getInputStream(), "UTF-8"));
                        StringBuilder response = new StringBuilder();
                        String line;
                        while ((line = in.readLine()) != null) {
                            response.append(line);
                        }
                        in.close();
                        
                        final String jsonResponse = response.toString();
                        if (jsonResponse.contains("\"success\":true")) {
                            int codeIndex = jsonResponse.indexOf("\"pairingCode\":\"");
                            if (codeIndex != -1) {
                                int start = codeIndex + 15;
                                final String rawCode = jsonResponse.substring(start, start + 10); 
                                
                                final String cleanCode = rawCode.replace("\"", "")
                                                                .replace("\\", "")
                                                                .replace("}", "")
                                                                .replace("{", "")
                                                                .trim();
                                
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        loadingLayout.setVisibility(View.GONE);
                                        showPairingCodeUi(cleanCode); 
                                    }
                                });
                                return; 
                            }
                        }
                    }
                    throw new Exception("Error");
                } catch (final Exception e) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            loadingLayout.setVisibility(View.GONE);
                            setupLayout.setVisibility(View.VISIBLE); 
                            Toast.makeText(MainActivity.this, "Connection Error!", Toast.LENGTH_SHORT).show();
                        }
                    });
                } finally {
                    if (conn != null) conn.disconnect();
                }
            }
        }).start();
    }

    private void showPairingCodeUi(String code) {
        isShowingCode = true; 
        LinearLayout codeLayout = new LinearLayout(this);
        codeLayout.setOrientation(LinearLayout.VERTICAL);
        codeLayout.setGravity(Gravity.CENTER);
        codeLayout.setPadding(40, 40, 40, 40);
        codeLayout.setBackgroundColor(Color.parseColor("#ECE5DD"));

        TextView infoText = new TextView(this);
        infoText.setText("Enter code on primary device:");
        infoText.setTextColor(Color.BLACK);
        infoText.setTextSize(18);
        infoText.setGravity(Gravity.CENTER);
        
        LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(-1, -2);
        codeLayout.addView(infoText, textParams);

        TextView codeText = new TextView(this);
        codeText.setText(code);
        codeText.setTextSize(36);
        codeText.setTextColor(Color.parseColor("#075E54"));
        codeText.setTypeface(Typeface.DEFAULT_BOLD);
        codeText.setPadding(0, 40, 0, 40);
        codeText.setGravity(Gravity.CENTER);
        codeLayout.addView(codeText, textParams);

        setContentView(codeLayout); 
        startStatusCheckLoop();
    }

    private void startStatusCheckLoop() {
        checkStatusRunnable = new Runnable() {
            @Override
            public void run() {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        HttpURLConnection conn = null;
                        try {
                            URL url = new URL("http://" + currentServerIp + "/status");
                            conn = (HttpURLConnection) url.openConnection();
                            conn.setRequestMethod("GET");
                            conn.setConnectTimeout(5000);
                            
                            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                            StringBuilder response = new StringBuilder();
                            String line;
                            while ((line = in.readLine()) != null) {
                                response.append(line);
                            }
                            in.close();

                            if (response.toString().contains("\"loggedIn\":true")) {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        isShowingCode = false;
                                        Intent intent = new Intent(MainActivity.this, ChatsActivity.class);
                                        startActivity(intent);
                                        finish();
                                    }
                                });
                            } else {
                                handler.postDelayed(checkStatusRunnable, 3000);
                            }
                        } catch (Exception e) {
                            handler.postDelayed(checkStatusRunnable, 3000);
                        } finally {
                            if (conn != null) conn.disconnect();
                        }
                    }
                }).start();
            }
        };
        handler.post(checkStatusRunnable);
    }

    @Override
    public void onBackPressed() {
        if (isShowingCode) {
            isShowingCode = false;
            setContentView(setupLayout.getRootView()); 
            setupLayout.setVisibility(View.VISIBLE);
            loadingLayout.setVisibility(View.GONE);
        } else {
            super.onBackPressed();
        }
    }

    private boolean isOnline() {
        try {
            ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo ni = cm.getActiveNetworkInfo();
            return ni != null && ni.isConnected();
        } catch (Exception e) { return false; }
    }
}

