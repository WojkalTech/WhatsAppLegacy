package com.wojkal.wpl;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.json.JSONArray;
import org.json.JSONObject;

public class ChatsActivity extends Activity {

    private static final int TAB_CHATS = 0;
    private static final int TAB_CONTACTS = 1;

    private static final String GREEN = "#075E54";

    private LinearLayout chatsContainer;
    private ScrollView chatsScroll;
    private ProgressBar progressBar;
    private FrameLayout contactsFrame;
    private ListView contactsList;
    private TextView contactsMessage;
    private EditText searchInput;
    private TextView tabChats;
    private TextView tabContacts;
    private View tabChatsLine;
    private View tabContactsLine;

    private String serverIp = "";
    private final String androidVersion = Build.VERSION.RELEASE;
    private int currentTab = TAB_CHATS;

    private JSONArray allChats = new JSONArray();

    private final ArrayList<Contact> allContacts = new ArrayList<Contact>();
    
    private final ArrayList<Object> contactRows = new ArrayList<Object>();
    private ContactsAdapter contactsAdapter;
    private boolean contactsLoaded = false;
    private boolean contactsLoading = false;

    private final ExecutorService picExecutor = Executors.newFixedThreadPool(2);

    private static class Contact {
        final String name;
        final String id;      
        final String number;  

        Contact(String name, String id, String number) {
            this.name = name;
            this.id = id;
            this.number = number;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);

        SharedPreferences prefs = getSharedPreferences("WPLPrefs", MODE_PRIVATE);
        serverIp = prefs.getString("saved_ip", "");

        LinearLayout mainLayout = new LinearLayout(this);
        mainLayout.setOrientation(LinearLayout.VERTICAL);
        mainLayout.setBackgroundColor(Color.WHITE);

        
        LinearLayout topBar = new LinearLayout(this);
        topBar.setOrientation(LinearLayout.HORIZONTAL);
        topBar.setBackgroundColor(Color.parseColor(GREEN));
        topBar.setGravity(Gravity.CENTER_VERTICAL);
        topBar.setPadding(30, 25, 30, 25);

        TextView barTitle = new TextView(this);
        barTitle.setText("WhatsAppLegacy");
        barTitle.setTextColor(Color.WHITE);
        barTitle.setTextSize(20);
        barTitle.setTypeface(Typeface.DEFAULT_BOLD);
        topBar.addView(barTitle);
        mainLayout.addView(topBar, new LinearLayout.LayoutParams(-1, -2));

        
        LinearLayout tabBar = new LinearLayout(this);
        tabBar.setOrientation(LinearLayout.HORIZONTAL);
        tabBar.setBackgroundColor(Color.parseColor(GREEN));

        tabChats = new TextView(this);
        tabChatsLine = new View(this);
        LinearLayout chatsTab = buildTab(tabChats, tabChatsLine, "Chats");
        tabBar.addView(chatsTab, new LinearLayout.LayoutParams(0, -2, 1f));

        tabContacts = new TextView(this);
        tabContactsLine = new View(this);
        LinearLayout contactsTab = buildTab(tabContacts, tabContactsLine, "Contacts");
        tabBar.addView(contactsTab, new LinearLayout.LayoutParams(0, -2, 1f));

        chatsTab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectTab(TAB_CHATS);
            }
        });
        contactsTab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectTab(TAB_CONTACTS);
            }
        });
        mainLayout.addView(tabBar, new LinearLayout.LayoutParams(-1, -2));

        
		
        LinearLayout searchBar = new LinearLayout(this);
        searchBar.setOrientation(LinearLayout.HORIZONTAL);
        searchBar.setBackgroundColor(Color.parseColor("#F2F2F2"));
        searchBar.setPadding(UiUtil.dp(this, 8), UiUtil.dp(this, 6), UiUtil.dp(this, 8), UiUtil.dp(this, 6));

        searchInput = new EditText(this);
        searchInput.setSingleLine(true);
        searchInput.setTextSize(15);
        searchInput.setTextColor(Color.BLACK);
        searchInput.setHint("Search chats");
        searchInput.setBackgroundDrawable(UiUtil.roundRect(Color.WHITE, UiUtil.dp(this, 18),
                Color.parseColor("#DDDDDD"), 1));
        searchInput.setPadding(UiUtil.dp(this, 12), UiUtil.dp(this, 7), UiUtil.dp(this, 12), UiUtil.dp(this, 7));
        searchInput.setCompoundDrawablesWithIntrinsicBounds(
                new BitmapDrawable(getResources(), UiUtil.searchIcon(UiUtil.dp(this, 18), Color.parseColor("#888888"))),
                null, null, null);
        searchInput.setCompoundDrawablePadding(UiUtil.dp(this, 8));
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                applyFilter(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
        searchBar.addView(searchInput, new LinearLayout.LayoutParams(0, -2, 1f));
        mainLayout.addView(searchBar, new LinearLayout.LayoutParams(-1, -2));

        
        FrameLayout body = new FrameLayout(this);

        chatsScroll = new ScrollView(this);
        chatsContainer = new LinearLayout(this);
        chatsContainer.setOrientation(LinearLayout.VERTICAL);
        chatsScroll.addView(chatsContainer, new ScrollView.LayoutParams(-1, -2));
        body.addView(chatsScroll, new FrameLayout.LayoutParams(-1, -1));

        contactsFrame = new FrameLayout(this);
        contactsList = new ListView(this);
        contactsList.setCacheColorHint(Color.TRANSPARENT);
        contactsList.setDivider(new ColorDrawable(Color.parseColor("#EEEEEE")));
        contactsList.setDividerHeight(1);
        contactsAdapter = new ContactsAdapter();
        contactsList.setAdapter(contactsAdapter);
        contactsList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Object item = contactRows.get(position);
                if (item instanceof Contact) {
                    Contact c = (Contact) item;
                    openChat(c.id, c.name);
                }
            }
        });
        contactsFrame.addView(contactsList, new FrameLayout.LayoutParams(-1, -1));

        contactsMessage = new TextView(this);
        contactsMessage.setGravity(Gravity.CENTER);
        contactsMessage.setTextColor(Color.parseColor("#757575"));
        contactsMessage.setTextSize(15);
        contactsMessage.setPadding(UiUtil.dp(this, 24), UiUtil.dp(this, 40), UiUtil.dp(this, 24), 0);
        contactsMessage.setVisibility(View.GONE);
        contactsFrame.addView(contactsMessage, new FrameLayout.LayoutParams(-1, -2, Gravity.TOP));

        contactsFrame.setVisibility(View.GONE);
        body.addView(contactsFrame, new FrameLayout.LayoutParams(-1, -1));

        progressBar = new ProgressBar(this);
        FrameLayout.LayoutParams progressParams =
                new FrameLayout.LayoutParams(-2, -2, Gravity.CENTER_HORIZONTAL | Gravity.TOP);
        progressParams.setMargins(0, UiUtil.dp(this, 40), 0, 0);
        body.addView(progressBar, progressParams);

        mainLayout.addView(body, new LinearLayout.LayoutParams(-1, 0, 1f));

        setContentView(mainLayout);

        updateTabStyles();
        fetchChatsFromServer();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        picExecutor.shutdownNow();
    }

    
    
    

    private LinearLayout buildTab(TextView label, View line, String text) {
        label.setText(text);
        label.setGravity(Gravity.CENTER);
        label.setTextSize(15);
        label.setTypeface(Typeface.DEFAULT_BOLD);
        label.setPadding(0, UiUtil.dp(this, 10), 0, UiUtil.dp(this, 10));

        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setClickable(true);
        box.addView(label, new LinearLayout.LayoutParams(-1, -2));
        box.addView(line, new LinearLayout.LayoutParams(-1, UiUtil.dp(this, 3)));
        return box;
    }

    private void updateTabStyles() {
        int on = Color.WHITE;
        int off = Color.parseColor("#9FD3CB");
        tabChats.setTextColor(currentTab == TAB_CHATS ? on : off);
        tabChatsLine.setBackgroundColor(currentTab == TAB_CHATS ? Color.WHITE : Color.TRANSPARENT);
        tabContacts.setTextColor(currentTab == TAB_CONTACTS ? on : off);
        tabContactsLine.setBackgroundColor(currentTab == TAB_CONTACTS ? Color.WHITE : Color.TRANSPARENT);
    }

    private void selectTab(int tab) {
        if (tab == currentTab) return;
        currentTab = tab;
        updateTabStyles();
        hideKeyboard();

        if (tab == TAB_CHATS) {
            searchInput.setHint("Search chats");
            chatsScroll.setVisibility(View.VISIBLE);
            contactsFrame.setVisibility(View.GONE);
        } else {
            searchInput.setHint("Search contacts");
            chatsScroll.setVisibility(View.GONE);
            contactsFrame.setVisibility(View.VISIBLE);
        }

        searchInput.setText(""); 

        if (tab == TAB_CONTACTS) {
            ensureContactsLoaded();
        }
    }

    private void applyFilter(String query) {
        if (currentTab == TAB_CHATS) {
            renderChats(query);
        } else {
            rebuildContactRows(query);
        }
    }

    private void hideKeyboard() {
        try {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(searchInput.getWindowToken(), 0);
        } catch (Exception e) {
            
        }
    }

    
    
    

    private void fetchChatsFromServer() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                HttpURLConnection conn = null;
                try {
                    String urlString = "http://" + serverIp + "/get-chats?v="
                            + URLEncoder.encode(androidVersion, "UTF-8");

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
                                    allChats = chatsArray;
                                    if (currentTab == TAB_CHATS) {
                                        renderChats(searchInput.getText().toString());
                                    }
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

    private void renderChats(String filter) {
        try {
            chatsContainer.removeAllViews();

            String q = filter == null ? "" : filter.trim().toLowerCase(Locale.getDefault());
            int shown = 0;

            for (int i = 0; i < allChats.length(); i++) {
                JSONObject chat = allChats.getJSONObject(i);
                final String chatId = chat.getString("id");
                final String chatName = chat.getString("name");
                String lastMessage = chat.optString("lastMessage", "");

                if (q.length() > 0 && !chatName.toLowerCase(Locale.getDefault()).contains(q)) {
                    continue;
                }
                shown++;

                chatsContainer.addView(buildChatRow(chatId, chatName, lastMessage));

                View divider = new View(this);
                divider.setBackgroundColor(Color.parseColor("#EEEEEE"));
                chatsContainer.addView(divider, new LinearLayout.LayoutParams(-1, 2));
            }

            if (shown == 0) {
                TextView emptyTv = new TextView(this);
                emptyTv.setText(q.length() > 0 ? "No chats found." : "No active chats available.");
                emptyTv.setGravity(Gravity.CENTER);
                emptyTv.setTextColor(Color.parseColor("#757575"));
                emptyTv.setPadding(0, UiUtil.dp(this, 40), 0, 0);
                chatsContainer.addView(emptyTv);
            }
        } catch (Exception e) {
            Toast.makeText(this, "UI Render Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private View buildChatRow(final String chatId, final String chatName, String lastMessage) {
        int avatarSize = UiUtil.dp(this, 50);

        LinearLayout rowLayout = new LinearLayout(this);
        rowLayout.setOrientation(LinearLayout.HORIZONTAL);
        rowLayout.setGravity(Gravity.CENTER_VERTICAL);
        rowLayout.setPadding(UiUtil.dp(this, 12), UiUtil.dp(this, 10), UiUtil.dp(this, 12), UiUtil.dp(this, 10));
        rowLayout.setBackgroundDrawable(pressedRowBackground());

        ImageView avatar = new ImageView(this);
        LinearLayout.LayoutParams avatarParams = new LinearLayout.LayoutParams(avatarSize, avatarSize);
        avatarParams.setMargins(0, 0, UiUtil.dp(this, 12), 0);
        avatar.setLayoutParams(avatarParams);
        avatar.setScaleType(ImageView.ScaleType.CENTER_CROP);
        avatar.setImageBitmap(UiUtil.placeholder(chatName, avatarSize));
        rowLayout.addView(avatar);
        loadAvatar(chatId, avatar, avatarSize);

        LinearLayout textColumn = new LinearLayout(this);
        textColumn.setOrientation(LinearLayout.VERTICAL);
        rowLayout.addView(textColumn, new LinearLayout.LayoutParams(0, -2, 1f));

        TextView nameTv = new TextView(this);
        nameTv.setText(chatName);
        nameTv.setTextSize(17);
        nameTv.setTextColor(Color.parseColor("#000000"));
        nameTv.setTypeface(Typeface.DEFAULT_BOLD);
        nameTv.setSingleLine(true);
        nameTv.setEllipsize(TextUtils.TruncateAt.END);

        TextView msgTv = new TextView(this);
        msgTv.setText(lastMessage.length() == 0 ? "No messages" : lastMessage);
        msgTv.setTextSize(14);
        msgTv.setTextColor(Color.parseColor("#757575"));
        msgTv.setSingleLine(true);
        msgTv.setEllipsize(TextUtils.TruncateAt.END);
        msgTv.setPadding(0, 6, 0, 0);

        textColumn.addView(nameTv);
        textColumn.addView(msgTv);

        rowLayout.setClickable(true);
        rowLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openChat(chatId, chatName);
            }
        });
        return rowLayout;
    }

    private StateListDrawable pressedRowBackground() {
        StateListDrawable s = new StateListDrawable();
        s.addState(new int[]{android.R.attr.state_pressed}, new ColorDrawable(Color.parseColor("#E6E6E6")));
        s.addState(new int[]{}, new ColorDrawable(Color.WHITE));
        return s;
    }

    private void openChat(String chatId, String chatName) {
        hideKeyboard();
        Intent intent = new Intent(ChatsActivity.this, MessageActivity.class);
        intent.putExtra("chat_id", chatId);
        intent.putExtra("chat_name", chatName);
        startActivity(intent);
    }

    private void loadAvatar(final String chatId, final ImageView target, final int size) {
        picExecutor.execute(new Runnable() {
            @Override
            public void run() {
                HttpURLConnection conn = null;
                try {
                    File cacheFile = new File(getCacheDir(), "pp_" + Math.abs(chatId.hashCode()) + ".jpg");
                    byte[] data = null;

                    
                    if (cacheFile.exists() && System.currentTimeMillis() - cacheFile.lastModified() < 24L * 3600 * 1000) {
                        data = readFully(new FileInputStream(cacheFile));
                    }

                    if (data == null) {
                        String urlString = "http://" + serverIp + "/profile-pic-image?id="
                                + URLEncoder.encode(chatId, "UTF-8");
                        conn = (HttpURLConnection) new URL(urlString).openConnection();
                        conn.setConnectTimeout(15000);
                        conn.setReadTimeout(15000);
                        if (conn.getResponseCode() != 200) return; 
                        data = readFully(conn.getInputStream());

                        FileOutputStream fos = new FileOutputStream(cacheFile);
                        fos.write(data);
                        fos.close();
                    }

                    Bitmap bmp = UiUtil.decodeSampled(data, size);
                    if (bmp == null) return;
                    final Bitmap round = UiUtil.circle(bmp);

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            target.setImageBitmap(round);
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    if (conn != null) conn.disconnect();
                }
            }
        });
    }

    private byte[] readFully(InputStream in) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[4096];
        int n;
        while ((n = in.read(buf)) != -1) {
            out.write(buf, 0, n);
        }
        in.close();
        return out.toByteArray();
    }

    
    
    

    private void ensureContactsLoaded() {
        if (contactsLoaded) {
            rebuildContactRows(searchInput.getText().toString());
            return;
        }
        if (contactsLoading) return;
        loadContacts();
    }

    private void loadContacts() {
        contactsLoading = true;
        contactsMessage.setText("Loading contacts...");
        contactsMessage.setVisibility(View.VISIBLE);

        new Thread(new Runnable() {
            @Override
            public void run() {
                HttpURLConnection conn = null;
                final ArrayList<Contact> result = new ArrayList<Contact>();
                String error = null;
                try {
                    String urlString = "http://" + serverIp + "/get-contacts?v="
                            + URLEncoder.encode(androidVersion, "UTF-8");
                    conn = (HttpURLConnection) new URL(urlString).openConnection();
                    conn.setRequestMethod("GET");
                    conn.setConnectTimeout(20000);
                    conn.setReadTimeout(90000);

                    if (conn.getResponseCode() == 200) {
                        BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
                        StringBuilder response = new StringBuilder();
                        String line;
                        while ((line = in.readLine()) != null) {
                            response.append(line);
                        }
                        in.close();

                        JSONObject json = new JSONObject(response.toString());
                        if (json.optBoolean("success")) {
                            JSONArray arr = json.getJSONArray("contacts");
                            for (int i = 0; i < arr.length(); i++) {
                                JSONObject c = arr.getJSONObject(i);
                                String name = c.isNull("name") ? "" : c.optString("name", "").trim();
                                String id = c.isNull("id") ? "" : c.optString("id", "");
                                String number = c.isNull("number") ? "" : c.optString("number", "");
                                if (name.length() == 0 || id.length() == 0) continue;
                                result.add(new Contact(name, id, number));
                            }
                        } else {
                            error = "Could not load contacts.";
                        }
                    } else {
                        error = "Could not load contacts.";
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    error = "Could not load contacts.";
                } finally {
                    if (conn != null) conn.disconnect();
                }

                
                final Collator collator = Collator.getInstance();
                Collections.sort(result, new Comparator<Contact>() {
                    @Override
                    public int compare(Contact a, Contact b) {
                        boolean aLetter = Character.isLetter(a.name.charAt(0));
                        boolean bLetter = Character.isLetter(b.name.charAt(0));
                        if (aLetter != bLetter) return aLetter ? -1 : 1;
                        return collator.compare(a.name, b.name);
                    }
                });

                final String finalError = error;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        contactsLoading = false;
                        if (finalError != null) {
                            contactsMessage.setText(finalError + " Switch tabs to try again.");
                            contactsMessage.setVisibility(View.VISIBLE);
                            return;
                        }
                        allContacts.clear();
                        allContacts.addAll(result);
                        contactsLoaded = true;
                        rebuildContactRows(searchInput.getText().toString());
                    }
                });
            }
        }).start();
    }

    private static String digitsOf(String s) {
        return s.replaceAll("[^0-9]", "");
    }

    private static String headerLetter(String name) {
        if (name.length() > 0 && Character.isLetter(name.charAt(0))) {
            return name.substring(0, 1).toUpperCase(Locale.getDefault());
        }
        return "#";
    }

    private void rebuildContactRows(String filter) {
        contactRows.clear();

        String q = filter == null ? "" : filter.trim().toLowerCase(Locale.getDefault());
        String qDigits = digitsOf(q);
        String lastLetter = "";

        for (Contact c : allContacts) {
            boolean match = q.length() == 0
                    || c.name.toLowerCase(Locale.getDefault()).contains(q)
                    || (qDigits.length() > 0 && digitsOf(c.number).contains(qDigits));
            if (!match) continue;

            String letter = headerLetter(c.name);
            if (!letter.equals(lastLetter)) {
                contactRows.add(letter);
                lastLetter = letter;
            }
            contactRows.add(c);
        }

        contactsAdapter.notifyDataSetChanged();

        if (!contactsLoaded) {
            return; 
        }
        if (contactRows.isEmpty()) {
            contactsMessage.setText(allContacts.isEmpty() ? "No contacts found on this phone." : "No contacts found.");
            contactsMessage.setVisibility(View.VISIBLE);
        } else {
            contactsMessage.setVisibility(View.GONE);
        }
    }

    private class ContactsAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return contactRows.size();
        }

        @Override
        public Object getItem(int position) {
            return contactRows.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public int getViewTypeCount() {
            return 2;
        }

        @Override
        public int getItemViewType(int position) {
            return contactRows.get(position) instanceof String ? 0 : 1;
        }

        @Override
        public boolean areAllItemsEnabled() {
            return false;
        }

        @Override
        public boolean isEnabled(int position) {
            return contactRows.get(position) instanceof Contact;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            Object item = contactRows.get(position);

            if (item instanceof String) {
                TextView header;
                if (convertView instanceof TextView) {
                    header = (TextView) convertView;
                } else {
                    header = new TextView(ChatsActivity.this);
                    header.setTextSize(14);
                    header.setTypeface(Typeface.DEFAULT_BOLD);
                    header.setTextColor(Color.parseColor(GREEN));
                    header.setBackgroundColor(Color.parseColor("#F7F7F7"));
                    header.setPadding(UiUtil.dp(ChatsActivity.this, 16), UiUtil.dp(ChatsActivity.this, 6),
                            UiUtil.dp(ChatsActivity.this, 16), UiUtil.dp(ChatsActivity.this, 6));
                }
                header.setText((String) item);
                return header;
            }

            Contact contact = (Contact) item;
            Holder holder;
            View row = convertView;
            if (row == null) {
                int avatarSize = UiUtil.dp(ChatsActivity.this, 42);

                LinearLayout layout = new LinearLayout(ChatsActivity.this);
                layout.setOrientation(LinearLayout.HORIZONTAL);
                layout.setGravity(Gravity.CENTER_VERTICAL);
                layout.setPadding(UiUtil.dp(ChatsActivity.this, 12), UiUtil.dp(ChatsActivity.this, 8),
                        UiUtil.dp(ChatsActivity.this, 12), UiUtil.dp(ChatsActivity.this, 8));

                holder = new Holder();
                holder.avatar = new ImageView(ChatsActivity.this);
                LinearLayout.LayoutParams avatarParams = new LinearLayout.LayoutParams(avatarSize, avatarSize);
                avatarParams.setMargins(0, 0, UiUtil.dp(ChatsActivity.this, 12), 0);
                layout.addView(holder.avatar, avatarParams);

                LinearLayout column = new LinearLayout(ChatsActivity.this);
                column.setOrientation(LinearLayout.VERTICAL);
                layout.addView(column, new LinearLayout.LayoutParams(0, -2, 1f));

                holder.name = new TextView(ChatsActivity.this);
                holder.name.setTextSize(16);
                holder.name.setTextColor(Color.BLACK);
                holder.name.setSingleLine(true);
                holder.name.setEllipsize(TextUtils.TruncateAt.END);
                column.addView(holder.name);

                holder.number = new TextView(ChatsActivity.this);
                holder.number.setTextSize(13);
                holder.number.setTextColor(Color.parseColor("#757575"));
                holder.number.setSingleLine(true);
                column.addView(holder.number);

                layout.setTag(holder);
                row = layout;
            } else {
                holder = (Holder) row.getTag();
            }

            holder.name.setText(contact.name);
            holder.number.setText(contact.number.length() > 0 ? "+" + contact.number : "");
            holder.avatar.setImageBitmap(UiUtil.placeholder(contact.name, UiUtil.dp(ChatsActivity.this, 42)));
            return row;
        }
    }

    private static class Holder {
        ImageView avatar;
        TextView name;
        TextView number;
    }
}
