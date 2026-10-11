package com.wojkal.wpl;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
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
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

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
        topBar.setPadding(20, 15, 20, 15);

        profileImageView = new ImageView(this);
        LinearLayout.LayoutParams profileParams = new LinearLayout.LayoutParams(90, 90);
        profileParams.setMargins(0, 0, 20, 0);
        profileImageView.setLayoutParams(profileParams);
        profileImageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        topBar.addView(profileImageView);

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

        btnFile = new ImageButton(this);
        int resId = getResources().getIdentifier("file_button", "drawable", getPackageName());
        if (resId != 0) {
            btnFile.setImageResource(resId);
        } else {
            btnFile.setImageResource(android.R.drawable.ic_menu_share);
        }
        btnFile.setBackgroundColor(Color.TRANSPARENT);
        btnFile.setScaleType(ImageView.ScaleType.FIT_CENTER);
        LinearLayout.LayoutParams fileBtnParams = new LinearLayout.LayoutParams(80, 80);
        fileBtnParams.setMargins(10, 0, 10, 0);
        bottomBar.addView(btnFile, fileBtnParams);

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
                                        profileImageView.setImageBitmap(bitmap);
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
                    conn.setReadTimeout(10000);

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
                            + URLEncoder.encode(chatId, "UTF-8")
                            + "&v=" + URLEncoder.encode(androidVersion, "UTF-8");

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
                            + "&v=" + URLEncoder.encode(androidVersion, "UTF-8");

                    URL url = new URL(urlString);
                    conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("GET");
                    conn.setConnectTimeout(8000);
                    conn.setReadTimeout(8000);

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
                    InputStream inputStream = conn.getInputStream();

                    final File tempAudio = File.createTempFile("temp_audio", ".aac", getCacheDir());
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

                                mediaPlayer.setDataSource(tempAudio.getAbsolutePath());
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
                                        Toast.makeText(MessageActivity.this, "Audio playback error!", Toast.LENGTH_SHORT).show();
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

    private void playVideo(String videoUrl) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(Uri.parse(videoUrl), "video/*");
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "No video player application found!", Toast.LENGTH_SHORT).show();
        }
    }

    private void renderMessages(JSONArray msgs) {
        try {
            messageContainer.removeAllViews();
            for (int i = 0; i < msgs.length(); i++) {
                JSONObject m = msgs.getJSONObject(i);
                final String msgId = m.getString("id");
                String body = m.optString("body", "");
                boolean fromMe = m.getBoolean("fromMe");
                final String mediaType = m.optString("mediaType", "none");

                LinearLayout bubbleLayout = new LinearLayout(this);
                bubbleLayout.setOrientation(LinearLayout.VERTICAL);
                bubbleLayout.setPadding(20, 15, 20, 15);

                LinearLayout.LayoutParams bubbleParams = new LinearLayout.LayoutParams(-2, -2);
                bubbleParams.setMargins(0, 10, 0, 10);

                final String mediaUrl = "http://" + serverIp + "/get-media?msgId=" + URLEncoder.encode(msgId, "UTF-8");

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
                    playBtn.setText("▶ Play Audio");
                    playBtn.setBackgroundColor(Color.parseColor("#128C7E"));
                    playBtn.setTextColor(Color.WHITE);
                    playBtn.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            playAudio(mediaUrl);
                        }
                    });
                    bubbleLayout.addView(playBtn);
                }
                else if (mediaType.equals("video")) {
                    Button playVideoBtn = new Button(this);
                    playVideoBtn.setText("▶ Play Video");
                    playVideoBtn.setBackgroundColor(Color.parseColor("#128C7E"));
                    playVideoBtn.setTextColor(Color.WHITE);
                    playVideoBtn.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            playVideo(mediaUrl);
                        }
                    });
                    bubbleLayout.addView(playVideoBtn);
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
                    bubbleLayout.addView(textTv);
                }

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

    private static class LoadMediaTask extends AsyncTask<String, Void, Bitmap> {
        private final WeakReference<ImageView> imageViewReference;
        private final String msgId;
        private final WeakReference<Context> contextReference;

        public LoadMediaTask(Context context, ImageView imageView, String msgId) {
            this.contextReference = new WeakReference<>(context);
            this.imageViewReference = new WeakReference<>(imageView);
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
                InputStream in = new URL(urlDisplay).openStream();
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

