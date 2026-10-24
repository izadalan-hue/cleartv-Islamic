package com.cleartv.football;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.util.Xml;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;
import org.xmlpull.v1.XmlPullParser;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class FootballActivity extends Activity {
    private static final String API =
        "https://site.api.espn.com/apis/site/v2/sports/soccer/eng.1/scoreboard";
    private static final String AWANI_RSS = "https://www.astroawani.com/rss/latest/public";
    private static final ZoneId MYT = ZoneId.of("Asia/Kuala_Lumpur");
    private final List<JSONObject> fixtures = new ArrayList<>();
    private LinearLayout list;
    private TextView status;
    private TextView tickerText;
    private Button refresh;
    private String latestNewsUrl = "https://www.astroawani.com/";

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().getDecorView().setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        getWindow().setStatusBarColor(Color.rgb(9, 15, 25));
        getWindow().setNavigationBarColor(Color.rgb(9, 15, 25));

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(34), dp(22), dp(34), dp(12));
        root.setBackgroundColor(Color.rgb(9, 15, 25));

        TextView title = text("PREMIER LEAGUE", 28, Color.WHITE, true);
        root.addView(title);
        TextView subtitle = text("FIXTURES  •  MALAYSIA TIME (MYT)", 15,
            Color.rgb(53, 199, 122), true);
        LinearLayout.LayoutParams subParams = new LinearLayout.LayoutParams(-1, -2);
        subParams.topMargin = dp(5);
        root.addView(subtitle, subParams);

        LinearLayout controls = new LinearLayout(this);
        controls.setGravity(Gravity.CENTER_VERTICAL);
        status = text("Loading fixtures…", 14, Color.LTGRAY, false);
        controls.addView(status, new LinearLayout.LayoutParams(0, -2, 1));
        refresh = new Button(this);
        refresh.setText("Refresh");
        refresh.setTextSize(15);
        refresh.setOnClickListener(v -> refreshAll());
        controls.addView(refresh);
        LinearLayout.LayoutParams controlParams = new LinearLayout.LayoutParams(-1, -2);
        controlParams.topMargin = dp(8);
        controlParams.bottomMargin = dp(8);
        root.addView(controls, controlParams);

        ScrollView scroll = new ScrollView(this);
        list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        scroll.setFillViewport(false);
        scroll.addView(list);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));

        LinearLayout newsBar = new LinearLayout(this);
        newsBar.setOrientation(LinearLayout.HORIZONTAL);
        newsBar.setGravity(Gravity.CENTER_VERTICAL);
        newsBar.setPadding(dp(12), dp(9), dp(12), dp(9));
        newsBar.setBackgroundColor(Color.rgb(160, 29, 39));
        TextView newsLabel = text("ASTRO AWANI", 14, Color.WHITE, true);
        newsBar.addView(newsLabel);
        tickerText = text("  •  Loading latest headlines…", 16, Color.WHITE, true);
        tickerText.setSingleLine(true);
        tickerText.setSelected(true);
        tickerText.setFocusable(false);
        tickerText.setEllipsize(android.text.TextUtils.TruncateAt.MARQUEE);
        tickerText.setMarqueeRepeatLimit(-1);
        tickerText.setHorizontallyScrolling(true);
        LinearLayout.LayoutParams tickerParams = new LinearLayout.LayoutParams(0, -2, 1);
        tickerParams.leftMargin = dp(12);
        newsBar.addView(tickerText, tickerParams);
        newsBar.setFocusable(true);
        newsBar.setContentDescription("Astro Awani scrolling news headlines. Press OK to open the latest story.");
        newsBar.setOnClickListener(v -> {
            try {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(latestNewsUrl)));
            } catch (Exception ignored) {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.astroawani.com/")));
            }
        });
        LinearLayout.LayoutParams newsParams = new LinearLayout.LayoutParams(-1, -2);
        newsParams.topMargin = dp(10);
        root.addView(newsBar, newsParams);

        setContentView(root);
        refresh.requestFocus();
        refreshAll();
    }

    private void refreshAll() {
        loadFixtures();
        loadAwaniNews();
    }

    private void loadFixtures() {
        refresh.setEnabled(false);
        status.setText("Loading fixtures…");
        list.removeAllViews();
        new Thread(() -> {
            try {
                LocalDate today = LocalDate.now(MYT);
                String dates = today.format(DateTimeFormatter.BASIC_ISO_DATE) + "-"
                    + today.plusDays(14).format(DateTimeFormatter.BASIC_ISO_DATE);
                URL url = new URL(API + "?dates=" + dates + "&limit=100");
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(12000);
                connection.setReadTimeout(12000);
                connection.setRequestProperty("Accept", "application/json");
                connection.setRequestProperty("User-Agent", "ClearTV-EPL-Fixtures/1.0");
                try {
                    int code = connection.getResponseCode();
                    if (code < 200 || code >= 300) {
                        throw new IllegalStateException("Fixture service returned HTTP " + code);
                    }
                    ByteArrayOutputStream output = new ByteArrayOutputStream();
                    try (BufferedInputStream input = new BufferedInputStream(connection.getInputStream())) {
                        byte[] buffer = new byte[4096];
                        int count;
                        while ((count = input.read(buffer)) != -1) output.write(buffer, 0, count);
                    }
                    JSONObject json = new JSONObject(output.toString(StandardCharsets.UTF_8.name()));
                    JSONArray events = json.optJSONArray("events");
                    List<JSONObject> result = new ArrayList<>();
                    if (events != null) {
                        for (int i = 0; i < events.length(); i++) {
                            JSONObject event = events.optJSONObject(i);
                            if (event != null && event.has("date")) result.add(event);
                        }
                    }
                    result.sort(Comparator.comparing(e -> e.optString("date", "")));
                    runOnUiThread(() -> showFixtures(result, null));
                } finally {
                    connection.disconnect();
                }
            } catch (Exception e) {
                runOnUiThread(() -> showFixtures(new ArrayList<>(),
                    "Could not load fixtures. Check the TV's internet connection and try Refresh."));
            }
        }).start();
    }

    private void loadAwaniNews() {
        new Thread(() -> {
            List<String> headlines = new ArrayList<>();
            String firstUrl = "https://www.astroawani.com/";
            HttpURLConnection connection = null;
            try {
                connection = (HttpURLConnection) new URL(AWANI_RSS).openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(12000);
                connection.setReadTimeout(12000);
                connection.setRequestProperty("Accept", "application/rss+xml, application/xml, text/xml");
                connection.setRequestProperty("User-Agent", "ClearTV-Awani-Ticker/1.0");
                int code = connection.getResponseCode();
                if (code < 200 || code >= 300) throw new IllegalStateException("RSS HTTP " + code);
                try (InputStream input = new BufferedInputStream(connection.getInputStream())) {
                    XmlPullParser parser = Xml.newPullParser();
                    parser.setInput(input, "UTF-8");
                    boolean insideItem = false;
                    String currentTag = "";
                    String itemTitle = "";
                    String itemLink = "";
                    int eventType = parser.getEventType();
                    while (eventType != XmlPullParser.END_DOCUMENT && headlines.size() < 15) {
                        if (eventType == XmlPullParser.START_TAG) {
                            currentTag = parser.getName();
                            if ("item".equalsIgnoreCase(currentTag)) {
                                insideItem = true;
                                itemTitle = "";
                                itemLink = "";
                            }
                        } else if (eventType == XmlPullParser.TEXT && insideItem) {
                            if ("title".equalsIgnoreCase(currentTag)) itemTitle = parser.getText().trim();
                            else if ("link".equalsIgnoreCase(currentTag)) itemLink = parser.getText().trim();
                        } else if (eventType == XmlPullParser.END_TAG) {
                            String tag = parser.getName();
                            if ("item".equalsIgnoreCase(tag) && insideItem) {
                                if (!itemTitle.isEmpty()) headlines.add(itemTitle.replaceAll("\\s+", " "));
                                if (headlines.size() == 1 && itemLink.startsWith("https://")) firstUrl = itemLink;
                                insideItem = false;
                            }
                            currentTag = "";
                        }
                        eventType = parser.next();
                    }
                }
            } catch (Exception ignored) {
                // Keep the ticker useful even when the feed is temporarily unavailable.
            } finally {
                if (connection != null) connection.disconnect();
            }
            final String openUrl = firstUrl;
            final String ticker = headlines.isEmpty()
                ? "  •  Latest headlines unavailable. Visit astroawani.com"
                : "  •  " + String.join("     •     ", headlines) + "     •     ";
            runOnUiThread(() -> {
                latestNewsUrl = openUrl;
                tickerText.setText(ticker);
                tickerText.setSelected(true);
            });
        }).start();
    }

    private void showFixtures(List<JSONObject> result, String error) {
        refresh.setEnabled(true);
        list.removeAllViews();
        if (error != null) {
            status.setText("Unable to update fixtures");
            TextView message = text(error, 18, Color.WHITE, false);
            message.setPadding(dp(16), dp(18), dp(16), dp(18));
            list.addView(message);
            return;
        }
        fixtures.clear();
        fixtures.addAll(result);
        status.setText(result.size() + " matches • next 14 days");
        if (result.isEmpty()) {
            list.addView(text("No fixtures listed for the next 14 days.", 18, Color.WHITE, false));
            return;
        }
        String currentDay = "";
        DateTimeFormatter dayFormat = DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.UK);
        DateTimeFormatter timeFormat = DateTimeFormatter.ofPattern("HH:mm", Locale.UK);
        for (JSONObject event : result) {
            Instant kickoff;
            try {
                kickoff = Instant.parse(event.getString("date"));
            } catch (Exception ex) {
                continue;
            }
            java.time.ZonedDateTime local = kickoff.atZone(MYT);
            String day = local.toLocalDate().format(dayFormat).toUpperCase(Locale.UK);
            if (!day.equals(currentDay)) {
                currentDay = day;
                TextView heading = text(day, 14, Color.rgb(53, 199, 122), true);
                heading.setPadding(dp(4), dp(14), dp(4), dp(7));
                list.addView(heading);
            }
            list.addView(makeMatchCard(event, local.format(timeFormat)));
        }
    }

    private View makeMatchCard(JSONObject event, String time) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(16), dp(11), dp(16), dp(11));
        card.setBackgroundColor(Color.rgb(21, 31, 46));
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(-1, -2);
        cardParams.bottomMargin = dp(7);
        card.setFocusable(true);
        card.setFocusableInTouchMode(true);
        card.setOnFocusChangeListener((v, focused) ->
            v.setBackgroundColor(focused ? Color.rgb(37, 57, 72) : Color.rgb(21, 31, 46)));

        JSONObject competition = event.optJSONArray("competitions") != null
            ? event.optJSONArray("competitions").optJSONObject(0) : null;
        JSONArray competitors = competition == null ? null : competition.optJSONArray("competitors");
        String home = "Home team";
        String away = "Away team";
        String homeScore = "";
        String awayScore = "";
        if (competitors != null) {
            for (int i = 0; i < competitors.length(); i++) {
                JSONObject team = competitors.optJSONObject(i);
                if (team == null) continue;
                String name = team.optJSONObject("team") == null ? "Team"
                    : team.optJSONObject("team").optString("displayName", "Team");
                String score = team.optString("score", "");
                if ("home".equals(team.optString("homeAway"))) {
                    home = name; homeScore = score;
                } else {
                    away = name; awayScore = score;
                }
            }
        }
        JSONObject statusObj = competition == null ? null : competition.optJSONObject("status");
        JSONObject type = statusObj == null ? null : statusObj.optJSONObject("type");
        String state = type == null ? "Scheduled" : type.optString("shortDetail",
            type.optString("detail", "Scheduled"));
        boolean started = type != null && !"STATUS_SCHEDULED".equals(type.optString("name"));
        String matchup = started && !homeScore.isEmpty() && !awayScore.isEmpty()
            ? home + "   " + homeScore + "  -  " + awayScore + "   " + away
            : home + "   vs   " + away;

        card.addView(text(matchup, 19, Color.WHITE, true));
        TextView meta = text((started ? state : time + " MYT") + "   •   " +
            event.optString("name", "Premier League"), 13,
            started ? Color.rgb(255, 205, 96) : Color.LTGRAY, false);
        LinearLayout.LayoutParams metaParams = new LinearLayout.LayoutParams(-1, -2);
        metaParams.topMargin = dp(5);
        card.addView(meta, metaParams);
        card.setContentDescription(matchup + ", " + time + " Malaysia time");
        return card;
    }

    private TextView text(String value, int sp, int color, boolean bold) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(sp);
        view.setTextColor(color);
        if (bold) view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        view.setGravity(Gravity.CENTER_VERTICAL);
        return view;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    @Override public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_R) {
            refreshAll();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }
}
