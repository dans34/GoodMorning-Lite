package com.dans34.goodmorninglite;

import android.app.Activity;
import android.app.TimePickerDialog;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {
    private static final int INK = Color.rgb(57, 58, 70);
    private static final int MUTED = Color.rgb(150, 151, 158);
    private static final int PALE = Color.rgb(249, 249, 250);
    private static final int YELLOW = Color.rgb(255, 224, 0);
    private LinearLayout root;
    private SharedPreferences prefs;
    private int page = 0;
    private String routineType = "Morning routine";
    private String struggle = "I'm not sure";
    private int wakeHour = 6;
    private int wakeMinute = 0;
    private final List<String> struggles = new ArrayList<>();
    private final String[] tasks = {
        "Stretch and drink a glass of water",
        "Tidy up the bed",
        "Check the weather"
    };

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        prefs = getSharedPreferences("goodmorning_local", MODE_PRIVATE);
        if (prefs.getBoolean("setup_complete", false)) {
            showHome();
        } else {
            showPage();
        }
    }

    private int dp(float value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private TextView text(String value, int size, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(size);
        t.setTextColor(color);
        t.setGravity(Gravity.CENTER);
        if (bold) t.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
        return t;
    }

    private LinearLayout column() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        l.setPadding(dp(24), dp(18), dp(24), dp(18));
        return l;
    }

    private void base(String title, String subtitle, int step) {
        root = column();
        root.setBackgroundColor(Color.WHITE);
        setContentView(root);

        LinearLayout progress = new LinearLayout(this);
        progress.setBackgroundColor(Color.rgb(241, 241, 242));
        LinearLayout.LayoutParams pp = new LinearLayout.LayoutParams(-1, dp(8));
        pp.setMargins(dp(4), dp(16), dp(4), dp(34));
        root.addView(progress, pp);
        View fill = new View(this);
        fill.setBackgroundColor(YELLOW);
        progress.addView(fill, new LinearLayout.LayoutParams(0, -1, step / 4.0f));

        LinearLayout bubble = new LinearLayout(this);
        bubble.setOrientation(LinearLayout.VERTICAL);
        bubble.setPadding(dp(22), dp(22), dp(22), dp(22));
        bubble.setBackgroundColor(PALE);
        TextView heading = text(title, 25, INK, true);
        heading.setGravity(Gravity.START);
        TextView sub = text(subtitle, 16, MUTED, false);
        sub.setGravity(Gravity.START);
        LinearLayout.LayoutParams h = new LinearLayout.LayoutParams(-1, -2);
        h.bottomMargin = dp(10);
        bubble.addView(heading, h);
        bubble.addView(sub);
        LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(-1, -2);
        bp.setMargins(dp(16), 0, dp(0), dp(26));
        root.addView(bubble, bp);
    }

    private Button button(String label, boolean selected, Runnable action) {
        Button b = new Button(this);
        b.setText(label);
        b.setTextSize(16);
        b.setAllCaps(false);
        b.setTextColor(selected ? INK : INK);
        b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(selected ? Color.rgb(255, 249, 199) : PALE));
        b.setMinHeight(dp(58));
        b.setOnClickListener(v -> action.run());
        return b;
    }

    private void addChoice(String label, boolean selected, Runnable action) {
        Button b = button(label, selected, action);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
        p.setMargins(0, dp(5), 0, dp(7));
        root.addView(b, p);
    }

    private void nextButton(String label, Runnable action) {
        Button b = new Button(this);
        b.setText(label);
        b.setTextSize(18);
        b.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        b.setAllCaps(false);
        b.setTextColor(Color.WHITE);
        b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(INK));
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, dp(64));
        p.setMargins(0, dp(20), 0, dp(10));
        p.gravity = Gravity.BOTTOM;
        b.setOnClickListener(v -> action.run());
        root.addView(b, p);
    }

    private void showPage() {
        if (page == 0) showRoutineChoice();
        else if (page == 1) showStruggles();
        else if (page == 2) showWakeTime();
        else showPreview();
    }

    // Onboarding order follows the user's screenshots: routine choice, struggle, wake time, preview.
    private void showRoutineChoice() {
        base("Which routine should we build first?", "We'll build just one for now.", 1);
        LinearLayout choices = new LinearLayout(this);
        choices.setOrientation(LinearLayout.HORIZONTAL);
        Button morning = button("☀\n\nMorning routine", routineType.equals("Morning routine"), () -> {
            routineType = "Morning routine"; showPage();
        });
        Button evening = button("🌙\n\nEvening routine", routineType.equals("Evening routine"), () -> {
            routineType = "Evening routine"; showPage();
        });
        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(0, dp(180), 1);
        cp.setMargins(0, dp(18), dp(8), 0);
        choices.addView(morning, cp);
        LinearLayout.LayoutParams ep = new LinearLayout.LayoutParams(0, dp(180), 1);
        ep.setMargins(dp(8), dp(18), 0, 0);
        choices.addView(evening, ep);
        root.addView(choices);
        View spacer = new View(this);
        root.addView(spacer, new LinearLayout.LayoutParams(1, 0, 1));
        nextButton("Next", () -> { page = 1; showPage(); });
    }

    private void showStruggles() {
        base("What do you struggle with?", "We'll suggest tasks that fit.", 2);
        String[] options = {
            "I keep planning without taking action 🥺",
            "I struggle to manage my schedule ⏰",
            "I struggle to stay focused 🌀",
            "I keep forgetting my tasks 📋",
            "I'm constantly busy without a break 🙄",
            "I feel drained and unmotivated 😮‍💨"
        };
        for (String option : options) {
            addChoice(option, struggle.equals(option), () -> { struggle = option; showPage(); });
        }
        addChoice("I'm not sure", struggle.equals("I'm not sure"), () -> { struggle = "I'm not sure"; showPage(); });
        View spacer = new View(this);
        root.addView(spacer, new LinearLayout.LayoutParams(1, 0, 1));
        nextButton("Next", () -> { page = 2; showPage(); });
    }

    private void showWakeTime() {
        base("What time do you wake up?", "Your routine will start at this time.", 3);
        TextView time = text(String.format(Locale.getDefault(), "%d:%02d %s",
            (wakeHour % 12 == 0 ? 12 : wakeHour % 12), wakeMinute, wakeHour < 12 ? "AM" : "PM"), 36, INK, true);
        LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(-1, dp(180));
        root.addView(time, tp);
        Button choose = button("Choose time", false, () -> {
            TimePickerDialog dialog = new TimePickerDialog(this, (view, hour, minute) -> {
                wakeHour = hour; wakeMinute = minute; showPage();
            }, wakeHour, wakeMinute, false);
            dialog.show();
        });
        root.addView(choose, new LinearLayout.LayoutParams(-1, dp(56)));
        View spacer = new View(this);
        root.addView(spacer, new LinearLayout.LayoutParams(1, 0, 1));
        nextButton("Next", () -> { page = 3; showPage(); });
    }

    private void showPreview() {
        base("This is how your day starts", "You can edit it anytime", 4);
        LinearLayout card = column();
        card.setBackgroundColor(PALE);
        card.addView(text(String.format(Locale.getDefault(), "☀  Every day %d:%02d%s · 6 min",
            (wakeHour % 12 == 0 ? 12 : wakeHour % 12), wakeMinute, wakeHour < 12 ? "am" : "pm"), 14, MUTED, false));
        TextView name = text(routineType, 23, INK, true);
        name.setGravity(Gravity.START);
        LinearLayout.LayoutParams np = new LinearLayout.LayoutParams(-1, -2);
        np.setMargins(0, dp(16), 0, dp(12));
        card.addView(name, np);
        for (int i = 0; i < tasks.length; i++) {
            TextView task = text((i + 1) + ".   " + tasks[i], 16, INK, false);
            task.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
            p.setMargins(0, dp(10), 0, dp(10));
            card.addView(task, p);
        }
        root.addView(card, new LinearLayout.LayoutParams(-1, -2));
        View spacer = new View(this);
        root.addView(spacer, new LinearLayout.LayoutParams(1, 0, 1));
        nextButton("Looks good", () -> {
            prefs.edit().putBoolean("setup_complete", true)
                .putString("routine_type", routineType)
                .putString("struggle", struggle)
                .putInt("wake_hour", wakeHour)
                .putInt("wake_minute", wakeMinute)
                .putBoolean("routine_created", true).apply();
            showHome();
        });
    }

    private void showHome() {
        root = column();
        root.setBackgroundColor(Color.WHITE);
        setContentView(root);
        root.addView(text("GoodMorning", 30, INK, true));
        root.addView(text("Your routine, one step at a time.", 16, MUTED, false));
        TextView routine = text(prefs.getString("routine_type", routineType), 24, INK, true);
        routine.setGravity(Gravity.START);
        LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(-1, -2);
        rp.setMargins(0, dp(32), 0, dp(12));
        root.addView(routine, rp);
        for (int i = 0; i < tasks.length; i++) {
            addHomeTask(i, tasks[i]);
        }
        nextButton("Start routine", () -> {
            prefs.edit().putInt("last_started_at", (int)(System.currentTimeMillis() / 1000L)).apply();
            Toast.makeText(this, "Routine started. Timer screen is the next implementation step.", Toast.LENGTH_LONG).show();
        });
        Button reset = button("Restart onboarding", false, () -> {
            prefs.edit().clear().apply(); page = 0; struggle = "I'm not sure"; showPage();
        });
        root.addView(reset);
    }

    private void addHomeTask(int index, String label) {
        Button b = button((index + 1) + "   " + label, false, () -> {
            bToast("Task selected: " + label);
        });
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
        p.setMargins(0, dp(5), 0, dp(5));
        root.addView(b, p);
    }

    private void bToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
