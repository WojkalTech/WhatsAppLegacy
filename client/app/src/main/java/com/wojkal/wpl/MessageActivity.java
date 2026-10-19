package com.wojkal.wpl;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.OpenableColumns;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import org.json.JSONArray;
import org.json.JSONObject;

public class MessageActivity extends Activity {

    private static final int PICK_FILE_REQUEST = 1001;

    private LinearLayout messageContainer;
    private ProgressBar progressBar;
    private EditText inputMessage;
    private Button btnSend;
    private ImageButton btnFile;
    private ImageView profileImageView;

    private String serverIp = "";
    private String chatId = "";
    private String chatName = "";
    private String lastLoadedMessagesJson = "";

    private final String androidVersion = Build.VERSION.RELEASE;

    private Handler autoRefreshHandler = new Handler();
    private Runnable refreshRunnable;
    private final int REFRESH_INTERVAL = 5000;

    private MediaPlayer mediaPlayer;
    private ScrollView scrollView;
    private boolean firstRender = true;
    private int messageLimit = 25;       
    private boolean loadingOlder = false;
    private int olderPrevHeight = 0;
    private int olderPrevScrollY = 0;

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
        topBar.setPadding(UiUtil.dp(this, 4), UiUtil.dp(this, 6), UiUtil.dp(this, 12), UiUtil.dp(this, 6));

        ImageView backBtn = new ImageView(this);
        backBtn.setImageBitmap(UiUtil.backArrow(UiUtil.dp(this, 24), Color.WHITE));
        backBtn.setScaleType(ImageView.ScaleType.CENTER);
        backBtn.setClickable(true);
        backBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        topBar.addView(backBtn, new LinearLayout.LayoutParams(UiUtil.dp(this, 40), UiUtil.dp(this, 40)));

        int avatarSize = UiUtil.dp(this, 40);
        profileImageView = new ImageView(this);
        LinearLayout.LayoutParams profileParams = new LinearLayout.LayoutParams(avatarSize, avatarSize);
        profileParams.setMargins(UiUtil.dp(this, 2), 0, UiUtil.dp(this, 12), 0);
        profileImageView.setLayoutParams(profileParams);
        profileImageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        profileImageView.setImageBitmap(UiUtil.placeholder(chatName, avatarSize));
        topBar.addView(profileImageView);

        TextView barTitle = new TextView(this);
        barTitle.setText(chatName);
        barTitle.setTextColor(Color.WHITE);
        barTitle.setTextSize(18);
        barTitle.setTypeface(Typeface.DEFAULT_BOLD);
        barTitle.setSingleLine(true);
        barTitle.setEllipsize(TextUtils.TruncateAt.END);
        topBar.addView(barTitle, new LinearLayout.LayoutParams(0, -2, 1f));
        mainLayout.addView(topBar, new LinearLayout.LayoutParams(-1, -2));

        progressBar = new ProgressBar(this);
        LinearLayout.LayoutParams prgParams = new LinearLayout.LayoutParams(-2, -2);
        prgParams.gravity = Gravity.CENTER;
        mainLayout.addView(progressBar, prgParams);

        scrollView = new ScrollView(this);
        LinearLayout.LayoutParams scrollParams = new LinearLayout.LayoutParams(-1, 0, 1f);
        messageContainer = new LinearLayout(this);
        messageContainer.setOrientation(LinearLayout.VERTICAL);
        messageContainer.setPadding(UiUtil.dp(this, 8), UiUtil.dp(this, 8), UiUtil.dp(this, 8), UiUtil.dp(this, 8));
        scrollView.addView(messageContainer, new ScrollView.LayoutParams(-1, -1));
        mainLayout.addView(scrollView, scrollParams);

        
        int fieldHeight = UiUtil.dp(this, 44);
        int radius = UiUtil.dp(this, 22);

        LinearLayout bottomBar = new LinearLayout(this);
        bottomBar.setOrientation(LinearLayout.HORIZONTAL);
        bottomBar.setBackgroundColor(Color.parseColor("#F0F0F0"));
        bottomBar.setPadding(UiUtil.dp(this, 8), UiUtil.dp(this, 6), UiUtil.dp(this, 8), UiUtil.dp(this, 6));
        bottomBar.setGravity(Gravity.CENTER_VERTICAL);

        inputMessage = new EditText(this);
        inputMessage.setHint("Type a message");
        inputMessage.setSingleLine(false);
        inputMessage.setMaxLines(4);
        inputMessage.setTextSize(16);
        inputMessage.setTextColor(Color.BLACK);
        inputMessage.setGravity(Gravity.CENTER_VERTICAL);
        inputMessage.setMinHeight(fieldHeight);
        inputMessage.setBackgroundDrawable(UiUtil.roundRect(Color.WHITE, radius, Color.parseColor("#D0D0D0"), 1));
        inputMessage.setPadding(UiUtil.dp(this, 16), UiUtil.dp(this, 6), UiUtil.dp(this, 16), UiUtil.dp(this, 6));
        bottomBar.addView(inputMessage, new LinearLayout.LayoutParams(0, -2, 1f));

        btnFile = new ImageButton(this);
        int resId = getResources().getIdentifier("file_button", "drawable", getPackageName());
        if (resId != 0) {
            btnFile.setImageResource(resId);
        } else {
            btnFile.setImageResource(android.R.drawable.ic_menu_share);
        }
        btnFile.setBackgroundColor(Color.TRANSPARENT);
        btnFile.setScaleType(ImageView.ScaleType.FIT_CENTER);
        LinearLayout.LayoutParams fileBtnParams = new LinearLayout.LayoutParams(UiUtil.dp(this, 40), UiUtil.dp(this, 40));
        fileBtnParams.setMargins(UiUtil.dp(this, 4), 0, UiUtil.dp(this, 4), 0);
        bottomBar.addView(btnFile, fileBtnParams);

        btnSend = new Button(this);
        btnSend.setText("Send");
        btnSend.setTextSize(15);
        btnSend.setTextColor(Color.WHITE);
        btnSend.setBackgroundDrawable(UiUtil.pressableRoundRect(
                Color.parseColor("#128C7E"), Color.parseColor("#0B6B5F"), radius));
        btnSend.setPadding(UiUtil.dp(this, 20), 0, UiUtil.dp(this, 20), 0);
        btnSend.setMinHeight(0);
        btnSend.setMinimumHeight(0);
        btnSend.setMinWidth(0);
        btnSend.setMinimumWidth(0);
        bottomBar.addView(btnSend, new LinearLayout.LayoutParams(-2, fieldHeight));

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

        btnFile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("*/*");
                startActivityForResult(Intent.createChooser(intent, "Choose a file"), PICK_FILE_REQUEST);
            }
        });

        refreshRunnable = new Runnable() {
            @Override
            public void run() {
                loadMessagesSilently();
                autoRefreshHandler.postDelayed(this, REFRESH_INTERVAL);
            }
        };

        loadProfilePic();
        loadMessages();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_FILE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri fileUri = data.getData();
            uploadFile(fileUri);
        }
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
        stopAudio();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopAudio();
    }

    private void stopAudio() {
        if (mediaPlayer != null) {
            try {
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.stop();
                }
                mediaPlayer.release();
            } catch (Exception e) { e.printStackTrace(); }
            mediaPlayer = null;
        }
    }

    private void loadProfilePic() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                HttpURLConnection conn = null;
                try {
                    String urlString = "http://" + serverIp + "/get-profile-pic?id=" + URLEncoder.encode(chatId, "UTF-8");
                    URL url = new URL(urlString);
                    conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("GET");
                    conn.setConnectTimeout(10000);
                    conn.setReadTimeout(10000);

                    if (conn.getResponseCode() == 200) {
                        BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
                        StringBuilder response = new StringBuilder();
                        String line;
                        while ((line = in.readLine()) != null) { response.append(line); }
                        in.close();

                        JSONObject json = new JSONObject(response.toString());
                        if (json.optBoolean("success") && !json.isNull("url")) {
                            String imgUrl = json.getString("url");
                            InputStream inStream = new URL(imgUrl).openStream();
                            final Bitmap bitmap = BitmapFactory.decodeStream(inStream);
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    if (bitmap != null) {
                                        profileImageView.setImageBitmap(UiUtil.circle(bitmap));
                                    }
                                }
                            });
                        }
                    }
                } catch (Exception e) { e.printStackTrace(); }
                finally {
                    if (conn != null) conn.disconnect();
                }
            }
        }).start();
    }

    private void uploadFile(final Uri fileUri) {
        Toast.makeText(this, "File is uploading...", Toast.LENGTH_SHORT).show();

        String fileName = "file_" + System.currentTimeMillis();
        String mimeType = getContentResolver().getType(fileUri);
        if (mimeType == null) {
            mimeType = "application/octet-stream";
        }

        Cursor cursor = getContentResolver().query(fileUri, null, null, null, null);
        if (cursor != null) {
            int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
            if (nameIndex != -1 && cursor.moveToFirst()) {
                fileName = cursor.getString(nameIndex);
            }
            cursor.close();
        }

        final String finalFileName = fileName;
        final String finalMimeType = mimeType;

        new Thread(new Runnable() {
            @Override
            public void run() {
                HttpURLConnection conn = null;
                try {
                    InputStream inputStream = getContentResolver().openInputStream(fileUri);
                    ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();
                    byte[] buffer = new byte[2048];
                    int len;
                    while ((len = inputStream.read(buffer)) != -1) {
                        byteBuffer.write(buffer, 0, len);
                    }
                    byte[] fileBytes = byteBuffer.toByteArray();

                    String boundary = "*****" + System.currentTimeMillis() + "*****";
                    URL url = new URL("http://" + serverIp + "/send-file");
                    conn = (HttpURLConnection) url.openConnection();
                    conn.setDoOutput(true);
                    conn.setDoInput(true);
                    conn.setUseCaches(false);
                    conn.setRequestMethod("POST");
                    conn.setConnectTimeout(20000);
                    conn.setReadTimeout(20000);
                    conn.setRequestProperty("Connection", "Keep-Alive");
                    conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

                    DataOutputStream request = new DataOutputStream(conn.getOutputStream());

                    request.writeBytes("--" + boundary + "\r\n");
                    request.writeBytes("Content-Disposition: form-data; name=\"id\"\r\n\r\n");
                    request.writeBytes(chatId + "\r\n");

                    request.writeBytes("--" + boundary + "\r\n");
                    request.writeBytes("Content-Disposition: form-data; name=\"file\"; filename=\"" + finalFileName + "\"\r\n");
                    request.writeBytes("Content-Type: " + finalMimeType + "\r\n\r\n");
                    request.write(fileBytes);
                    request.writeBytes("\r\n");
                    request.writeBytes("--" + boundary + "--\r\n");

                    request.flush();
                    request.close();

                    int responseCode = conn.getResponseCode();
                    if (responseCode == 200) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(MessageActivity.this, "File sent successfully!", Toast.LENGTH_SHORT).show();
                                loadMessagesSilently();
                            }
                        });
                    } else {
                        throw new Exception("Server response code: " + responseCode);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(MessageActivity.this, "File could not be sent", Toast.LENGTH_SHORT).show();
                        }
                    });
                } finally {
                    if (conn != null) conn.disconnect();
                }
            }
        }).start();
    }

    private void sendMessage(final String messageText) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                HttpURLConnection conn = null;
                try {
                    String urlString = "http://" + serverIp + "/send-message?id="
                            + URLEncoder.encode(chatId, "UTF-8")
                            + "&text=" + URLEncoder.encode(messageText, "UTF-8")
                            + "&v=" + URLEncoder.encode(androidVersion, "UTF-8");

                    URL url = new URL(urlString);
                    conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("GET");
                    conn.setConnectTimeout(10000);
                    conn.setReadTimeout(35000);

                    
                    boolean ok = false;
                    if (conn.getResponseCode() == 200) {
                        BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
                        StringBuilder sb = new StringBuilder();
                        String ln;
                        while ((ln = in.readLine()) != null) { sb.append(ln); }
                        in.close();
                        ok = sb.toString().contains("\"success\":true");
                    }

                    if (ok) {
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
                            + URLEncoder.encode(chatId, "UTF-8")
                            + "&v=" + URLEncoder.encode(androidVersion, "UTF-8")
                            + "&limit=" + messageLimit;

                    URL url = new URL(urlString);
                    conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("GET");
                    conn.setConnectTimeout(10000);
                    conn.setReadTimeout(90000);

                    if (conn.getResponseCode() == 200) {
                        BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
                        StringBuilder response = new StringBuilder();
                        String line;
                        while ((line = in.readLine()) != null) { response.append(line); }
                        in.close();

                        final String responseStr = response.toString();
                        JSONObject jsonResponse = new JSONObject(responseStr);
                        if (jsonResponse.getBoolean("success")) {
                            lastLoadedMessagesJson = responseStr;
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
                            + URLEncoder.encode(chatId, "UTF-8")
                            + "&v=" + URLEncoder.encode(androidVersion, "UTF-8")
                            + "&limit=" + messageLimit;

                    URL url = new URL(urlString);
                    conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("GET");
                    conn.setConnectTimeout(8000);
                    conn.setReadTimeout(60000);

                    if (conn.getResponseCode() == 200) {
                        BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
                        StringBuilder response = new StringBuilder();
                        String line;
                        while ((line = in.readLine()) != null) { response.append(line); }
                        in.close();

                        final String responseStr = response.toString();
                        if (responseStr.equals(lastLoadedMessagesJson)) {
                            return;
                        }

                        JSONObject jsonResponse = new JSONObject(responseStr);
                        if (jsonResponse.getBoolean("success")) {
                            lastLoadedMessagesJson = responseStr;
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

    private void playAudio(final String audioUrl) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    URL url = new URL(audioUrl);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setConnectTimeout(30000);
                    conn.setReadTimeout(300000); 
                    InputStream inputStream = conn.getInputStream();

                    String ctype = conn.getContentType();
                    String ext = ".m4a";
                    if (ctype != null) {
                        ctype = ctype.toLowerCase();
                        if (ctype.indexOf("ogg") >= 0) ext = ".ogg";
                        else if (ctype.indexOf("mpeg") >= 0) ext = ".mp3";
                        else if (ctype.indexOf("amr") >= 0) ext = ".amr";
                        else if (ctype.indexOf("aac") >= 0 && ctype.indexOf("mp4") < 0) ext = ".aac";
                    }
                    final File tempAudio = File.createTempFile("temp_audio", ext, getCacheDir());
                    FileOutputStream outputStream = new FileOutputStream(tempAudio);

                    byte[] buffer = new byte[1024];
                    int len;
                    while ((len = inputStream.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, len);
                    }
                    outputStream.close();
                    inputStream.close();

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                stopAudio();
                                mediaPlayer = new MediaPlayer();

                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                    mediaPlayer.setAudioAttributes(
                                        new AudioAttributes.Builder()
                                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                                            .setUsage(AudioAttributes.USAGE_MEDIA)
                                            .build()
                                    );
                                } else {
                                    mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
                                }

                                
                                
                                FileInputStream audioIn = new FileInputStream(tempAudio);
                                try {
                                    mediaPlayer.setDataSource(audioIn.getFD());
                                } finally {
                                    audioIn.close();
                                }
                                mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                                    @Override
                                    public void onPrepared(MediaPlayer mp) {
                                        mp.start();
                                        Toast.makeText(MessageActivity.this, "Playing audio...", Toast.LENGTH_SHORT).show();
                                    }
                                });
                                mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                                    @Override
                                    public void onCompletion(MediaPlayer mp) {
                                        Toast.makeText(MessageActivity.this, "Audio finished.", Toast.LENGTH_SHORT).show();
                                        stopAudio();
                                        tempAudio.delete();
                                    }
                                });
                                mediaPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
                                    @Override
                                    public boolean onError(MediaPlayer mp, int what, int extra) {
                                        Toast.makeText(MessageActivity.this, "Audio playback error (" + what + "/" + extra + ")", Toast.LENGTH_LONG).show();
                                        stopAudio();
                                        tempAudio.delete();
                                        return true;
                                    }
                                });

                                mediaPlayer.prepareAsync();
                                Toast.makeText(MessageActivity.this, "Loading audio...", Toast.LENGTH_SHORT).show();

                            } catch (Exception e) {
                                e.printStackTrace();
                                Toast.makeText(MessageActivity.this, "Cannot play audio: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        }
                    });

                } catch (Exception e) {
                    e.printStackTrace();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(MessageActivity.this, "Audio loading error!", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        }).start();
    }

    private boolean videoPreparing = false;

    private void playVideo(final String videoUrl) {
        if (videoPreparing) {
            Toast.makeText(this, "Video is still being prepared...", Toast.LENGTH_SHORT).show();
            return;
        }
        videoPreparing = true;
        Toast.makeText(this, "Preparing video, please wait...", Toast.LENGTH_LONG).show();

        new Thread(new Runnable() {
            @Override
            public void run() {
                String error = null;
                HttpURLConnection conn = null;
                try {
                    
                    
                    conn = (HttpURLConnection) new URL(videoUrl + "&prepare=1").openConnection();
                    conn.setConnectTimeout(30000);
                    conn.setReadTimeout(900000); 
                    if (conn.getResponseCode() != 200) {
                        error = "Video could not be prepared.";
                    }
                } catch (Exception e) {
                    error = "Video could not be prepared.";
                } finally {
                    if (conn != null) conn.disconnect();
                }

                final String finalError = error;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        videoPreparing = false;
                        if (finalError != null) {
                            Toast.makeText(MessageActivity.this, finalError, Toast.LENGTH_LONG).show();
                            return;
                        }
                        try {
                            Intent intent = new Intent(Intent.ACTION_VIEW);
                            intent.setDataAndType(Uri.parse(videoUrl), "video/mp4");
                            startActivity(intent);
                        } catch (Exception e) {
                            Toast.makeText(MessageActivity.this, "No video player application found!", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        }).start();
    }

    private void renderMessages(JSONArray msgs) {
        try {
            
            boolean stickToBottom = firstRender;
            if (!stickToBottom && scrollView != null) {
                int contentH = messageContainer.getHeight();
                stickToBottom = scrollView.getScrollY() + scrollView.getHeight() >= contentH - UiUtil.dp(this, 120);
            }
            if (loadingOlder) stickToBottom = false;
            messageContainer.removeAllViews();

            
            if (msgs.length() >= messageLimit) {
                TextView olderBtn = new TextView(this);
                olderBtn.setText("Load older messages");
                olderBtn.setTextSize(14);
                olderBtn.setTextColor(Color.parseColor("#075E54"));
                olderBtn.setGravity(Gravity.CENTER);
                olderBtn.setPadding(UiUtil.dp(this, 16), UiUtil.dp(this, 8), UiUtil.dp(this, 16), UiUtil.dp(this, 8));
                olderBtn.setBackgroundDrawable(UiUtil.pressableRoundRect(
                        Color.parseColor("#E1F3FB"), Color.parseColor("#C5E6F5"), UiUtil.dp(this, 16)));
                olderBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        olderPrevHeight = messageContainer.getHeight();
                        olderPrevScrollY = scrollView.getScrollY();
                        loadingOlder = true;
                        messageLimit += 50;
                        progressBar.setVisibility(View.VISIBLE);
                        loadMessages();
                    }
                });
                LinearLayout.LayoutParams olderParams = new LinearLayout.LayoutParams(-2, -2);
                olderParams.gravity = Gravity.CENTER_HORIZONTAL;
                olderParams.setMargins(0, UiUtil.dp(this, 4), 0, UiUtil.dp(this, 8));
                messageContainer.addView(olderBtn, olderParams);
            }

            int bubbleRadius = UiUtil.dp(this, 12);
            int sideMargin = UiUtil.dp(this, 48);
            SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
            
            int screenW = getResources().getDisplayMetrics().widthPixels;
            int maxTextWidth = screenW - UiUtil.dp(this, 16) - sideMargin - UiUtil.dp(this, 20);
            if (maxTextWidth < UiUtil.dp(this, 120)) maxTextWidth = UiUtil.dp(this, 120);

            for (int i = 0; i < msgs.length(); i++) {
                JSONObject m = msgs.getJSONObject(i);
                final String msgId = m.getString("id");
                String body = m.optString("body", "");
                boolean fromMe = m.getBoolean("fromMe");
                final String mediaType = m.optString("mediaType", "none");
                long timestamp = m.optLong("timestamp", 0);
                int ack = m.optInt("ack", 0);

                LinearLayout bubbleLayout = new LinearLayout(this);
                bubbleLayout.setOrientation(LinearLayout.VERTICAL);

                
                bubbleLayout.setBackgroundDrawable(UiUtil.roundRect(
                        fromMe ? Color.parseColor("#DCF8C6") : Color.WHITE, bubbleRadius, 0, 0));
                bubbleLayout.setPadding(UiUtil.dp(this, 10), UiUtil.dp(this, 6), UiUtil.dp(this, 10), UiUtil.dp(this, 4));

                LinearLayout.LayoutParams bubbleParams = new LinearLayout.LayoutParams(-2, -2);

                final String mediaUrl = "http://" + serverIp + "/get-media?msgId=" + URLEncoder.encode(msgId, "UTF-8")
                        + "&v=" + URLEncoder.encode(androidVersion, "UTF-8");

                if (mediaType.equals("image")) {
                    ImageView imageView = new ImageView(this);
                    LinearLayout.LayoutParams imgParams = new LinearLayout.LayoutParams(400, 300);
                    imgParams.setMargins(0, 0, 0, 10);
                    imageView.setLayoutParams(imgParams);
                    imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
                    bubbleLayout.addView(imageView);

                    new LoadMediaTask(this, imageView, msgId).execute(mediaUrl);
                }
                else if (mediaType.equals("sticker") || mediaType.equals("gif")) {
                    body = "[sticker]";
                }
                else if (mediaType.equals("audio")) {
                    body = "";
                    Button playBtn = new Button(this);
                    playBtn.setText("Play Audio");
                    playBtn.setBackgroundDrawable(UiUtil.pressableRoundRect(Color.parseColor("#128C7E"), Color.parseColor("#0B6B5F"), UiUtil.dp(this, 20)));
                    playBtn.setMinWidth(0);
                    playBtn.setMinHeight(0);
                    playBtn.setPadding(UiUtil.dp(this, 16), 0, UiUtil.dp(this, 16), 0);
                    playBtn.setTextColor(Color.WHITE);
                    playBtn.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            playAudio(mediaUrl);
                        }
                    });
                    bubbleLayout.addView(playBtn, new LinearLayout.LayoutParams(UiUtil.dp(this, 150), UiUtil.dp(this, 40)));
                }
                else if (mediaType.equals("video")) {
                    Button playVideoBtn = new Button(this);
                    playVideoBtn.setText("Play Video");
                    playVideoBtn.setBackgroundDrawable(UiUtil.pressableRoundRect(Color.parseColor("#128C7E"), Color.parseColor("#0B6B5F"), UiUtil.dp(this, 20)));
                    playVideoBtn.setMinWidth(0);
                    playVideoBtn.setMinHeight(0);
                    playVideoBtn.setPadding(UiUtil.dp(this, 16), 0, UiUtil.dp(this, 16), 0);
                    playVideoBtn.setTextColor(Color.WHITE);
                    playVideoBtn.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            playVideo(mediaUrl);
                        }
                    });
                    bubbleLayout.addView(playVideoBtn, new LinearLayout.LayoutParams(UiUtil.dp(this, 150), UiUtil.dp(this, 40)));
                }

                if (body != null) {
                    String lower = body.toLowerCase().trim();
                    if (lower.equals("[ses kaydı]") || lower.equals("[audio]") || lower.equals("[video]") || lower.equals("∅")) {
                        body = "";
                    }
                }

                if (body != null && !body.trim().isEmpty()) {
                    TextView textTv = new TextView(this);
                    textTv.setText(body);
                    textTv.setTextSize(16);
                    textTv.setTextColor(Color.BLACK);
                    
                    textTv.setSingleLine(false);
                    textTv.setHorizontallyScrolling(false);
                    
                    
                    
                    float widest = 0;
                    String[] lines = body.split("\n", -1);
                    for (int li = 0; li < lines.length; li++) {
                        float lw = textTv.getPaint().measureText(lines[li]);
                        if (lw > widest) widest = lw;
                    }
                    int textWidth = (int) Math.ceil(widest) + UiUtil.dp(this, 4);
                    if (textWidth > maxTextWidth) textWidth = maxTextWidth;
                    bubbleLayout.addView(textTv, new LinearLayout.LayoutParams(textWidth, -2));
                }

                
                LinearLayout metaRow = new LinearLayout(this);
                metaRow.setOrientation(LinearLayout.HORIZONTAL);
                metaRow.setGravity(Gravity.CENTER_VERTICAL);

                if (timestamp > 0) {
                    TextView timeTv = new TextView(this);
                    timeTv.setText(timeFormat.format(new Date(timestamp * 1000L)));
                    timeTv.setTextSize(11);
                    timeTv.setTextColor(Color.parseColor("#8A8A8A"));
                    metaRow.addView(timeTv);
                }
                if (fromMe) {
                    TickView tick = new TickView(this, ack);
                    LinearLayout.LayoutParams tickParams = new LinearLayout.LayoutParams(tick.widthPx(), tick.heightPx());
                    tickParams.setMargins(UiUtil.dp(this, 4), 0, 0, 0);
                    metaRow.addView(tick, tickParams);
                }

                LinearLayout.LayoutParams metaParams = new LinearLayout.LayoutParams(-2, -2);
                metaParams.gravity = Gravity.RIGHT;
                metaParams.setMargins(0, UiUtil.dp(this, 2), 0, 0);
                bubbleLayout.addView(metaRow, metaParams);

                if (fromMe) {
                    bubbleParams.gravity = Gravity.RIGHT;
                    bubbleParams.setMargins(sideMargin, UiUtil.dp(this, 3), 0, UiUtil.dp(this, 3));
                } else {
                    bubbleParams.gravity = Gravity.LEFT;
                    bubbleParams.setMargins(0, UiUtil.dp(this, 3), sideMargin, UiUtil.dp(this, 3));
                }

                messageContainer.addView(bubbleLayout, bubbleParams);
            }

            if (loadingOlder && scrollView != null) {
                loadingOlder = false;
                scrollView.post(new Runnable() {
                    @Override
                    public void run() {
                        int added = messageContainer.getHeight() - olderPrevHeight;
                        scrollView.scrollTo(0, olderPrevScrollY + (added > 0 ? added : 0));
                    }
                });
            }

            if (stickToBottom && scrollView != null) {
                firstRender = false;
                scrollView.post(new Runnable() {
                    @Override
                    public void run() {
                        scrollView.fullScroll(View.FOCUS_DOWN);
                    }
                });
                
                scrollView.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        scrollView.fullScroll(View.FOCUS_DOWN);
                    }
                }, 400);
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    
    private static class TickView extends View {
        private final int ack;
        private final int w;
        private final int h;
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

        TickView(Context context, int ack) {
            super(context);
            this.ack = ack;
            float d = context.getResources().getDisplayMetrics().density;
            this.h = (int) (11 * d + 0.5f);
            this.w = (int) (17 * d + 0.5f);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeCap(Paint.Cap.ROUND);
            paint.setStrokeJoin(Paint.Join.ROUND);
            paint.setStrokeWidth(1.6f * d);
        }

        int widthPx() {
            return w;
        }

        int heightPx() {
            return h;
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            setMeasuredDimension(w, h);
        }

        private void drawTick(Canvas c, float x0, float tw) {
            float y1 = h * 0.55f;
            float y2 = h * 0.88f;
            float y3 = h * 0.10f;
            c.drawLine(x0 + tw * 0.06f, y1, x0 + tw * 0.38f, y2, paint);
            c.drawLine(x0 + tw * 0.38f, y2, x0 + tw * 0.96f, y3, paint);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            float tw = h; 
            if (ack < 0) {
                
                paint.setColor(Color.parseColor("#E53935"));
                float r = h * 0.42f;
                float cx = w - h / 2f;
                float cy = h / 2f;
                canvas.drawCircle(cx, cy, r, paint);
                canvas.drawLine(cx, cy - r * 0.5f, cx, cy + r * 0.1f, paint);
                canvas.drawPoint(cx, cy + r * 0.5f, paint);
            } else if (ack == 0) {
                
                paint.setColor(Color.parseColor("#8A9AA3"));
                float r = h * 0.42f;
                float cx = w - h / 2f;
                float cy = h / 2f;
                canvas.drawCircle(cx, cy, r, paint);
                canvas.drawLine(cx, cy, cx, cy - r * 0.6f, paint);
                canvas.drawLine(cx, cy, cx + r * 0.45f, cy, paint);
            } else if (ack == 1) {
                paint.setColor(Color.parseColor("#8A9AA3"));
                drawTick(canvas, w - tw, tw);
            } else {
                paint.setColor(ack >= 3 ? Color.parseColor("#34B7F1") : Color.parseColor("#8A9AA3"));
                drawTick(canvas, w - tw - tw * 0.4f, tw);
                drawTick(canvas, w - tw, tw);
            }
        }
    }

    private static class LoadMediaTask extends AsyncTask<String, Void, Bitmap> {
        private final WeakReference<ImageView> imageViewReference;
        private final String msgId;
        private final WeakReference<Context> contextReference;

        public LoadMediaTask(Context context, ImageView imageView, String msgId) {
            this.contextReference = new WeakReference<Context>(context);
            this.imageViewReference = new WeakReference<ImageView>(imageView);
            this.msgId = msgId;
        }

        @Override
        protected Bitmap doInBackground(String... urls) {
            String urlDisplay = urls[0];
            Context context = contextReference.get();
            if (context == null) return null;

            try {
                File file = new File(context.getCacheDir(), "persist_" + msgId.replaceAll("[^a-zA-Z0-9]", "_") + ".jpg");
                if (file.exists()) {
                    return BitmapFactory.decodeFile(file.getAbsolutePath());
                }
                HttpURLConnection mconn = (HttpURLConnection) new URL(urlDisplay).openConnection();
                mconn.setConnectTimeout(30000);
                mconn.setReadTimeout(300000);
                InputStream in = mconn.getInputStream();
                Bitmap bitmap = BitmapFactory.decodeStream(in);
                if (bitmap != null) {
                    FileOutputStream out = new FileOutputStream(file);
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out);
                    out.flush();
                    out.close();
                }
                return bitmap;
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Bitmap result) {
            if (isCancelled()) {
                result = null;
            }
            ImageView imageView = imageViewReference.get();
            if (imageView != null && result != null) {
                imageView.setImageBitmap(result);
            }
        }
    }
}
