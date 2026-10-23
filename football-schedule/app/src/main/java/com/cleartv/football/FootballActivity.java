package com.cleartv.football;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
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
    private static final ZoneId MYT = ZoneId.of("Asia/Kuala_Lumpur");
    private final List<JSONObject> fixtures = new ArrayList<>();
    private LinearLayout list;
    private TextView status;
    private Button refresh;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().getDecorView().setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        getWindow().setStatusBarColor(Color.rgb(9, 15, 25));
        getWindow().setNavigationBarColor(Color.rgb(9, 15, 25));

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(34), dp(22), dp(34), dp(20));
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
        refresh.setOnClickListener(v -> loadFixtures());
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
        setContentView(root);
        refresh.requestFocus();
        loadFixtures();
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
                int code = connection.getResponseCode();
                if (code < 200 || code >= 300) {
                    throw new IllegalStateException("Fixture service returned HTTP " + code);
                }
                ByteArrayOutputStream output = new ByteArrayOutputStream();
                try (BufferedInputStream input = new BufferedInputStream(connection.getInputStream())) {
                    byte[] buffer = new byte[4096];
                    int count;
                    while ((count = input.read(buffer)) != -1) output.write(buffer, 0, count);
                } finally {
                    connection.disconnect();
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
            } catch (Exception e) {
                runOnUiThread(() -> showFixtures(new ArrayList<>(),
                    "Could not load fixtures. Check the TV's internet connection and try Refresh."));
            }
        }).start();
    }

    private void showFixtures(List<JSONObject> result, String error) {
        refresh.setEnabled(true);
        list.removeAllViews();
        if (error != null) {
            status.setText("Unable to update");
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

        TextView match = text(matchup, 19, Color.WHITE, true);
        card.addView(match);
        TextView meta = text((started ? state : time + " MYT") + "   •   " +
            event.optString("name", "Premier League"), 13,
            started ? Color.rgb(255, 205, 96) : Color.LTGRAY, false);
        LinearLayout.LayoutParams metaParams = new LinearLayout.LayoutParams(-1, -2);
        metaParams.topMargin = dp(5);
        card.addView(meta, metaParams);
        card.setContentDescription(matchup + ", " + time + " Malaysia time");
        card.setOnClickListener(v -> { /* Selectable card for TV remote navigation. */ });
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
            loadFixtures();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }
}
