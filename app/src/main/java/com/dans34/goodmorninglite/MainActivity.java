package com.dans34.goodmorninglite;

import android.app.Activity;
import android.app.TimePickerDialog;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import java.util.Locale;

public class MainActivity extends Activity {
    private static final int INK = Color.rgb(57, 58, 70);
    private static final int MUTED = Color.rgb(150, 151, 158);
    private static final int PALE = Color.rgb(249, 249, 250);
    private static final int YELLOW = Color.rgb(255, 224, 0);
    private static final int TASK_SECONDS = 120;

    private LinearLayout root;
    private SharedPreferences prefs;
    private int page = 0;
    private String routineType = "Morning routine";
    private String struggle = "I'm not sure";
    private int wakeHour = 6;
    private int wakeMinute = 0;
    private final String[] tasks = {
        "Stretch and drink a glass of water",
        "Tidy up the bed",
        "Check the weather"
    };
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable ticker;
    private int currentTask = 0;
    private long remainingMs = TASK_SECONDS * 1000L;
    private long deadlineMs = 0L;
    private boolean timerRunning = false;
    private boolean routineFinished = false;
    private TextView countdownView;
    private TextView taskPositionView;
    private TextView taskTitleView;
    private Button pauseButton;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        prefs = getSharedPreferences("goodmorning_local", MODE_PRIVATE);
        routineType = prefs.getString("routine_type", routineType);
        struggle = prefs.getString("struggle", struggle);
        wakeHour = prefs.getInt("wake_hour", wakeHour);
        wakeMinute = prefs.getInt("wake_minute", wakeMinute);
        currentTask = prefs.getInt("timer_task_index", 0);
        remainingMs = prefs.getLong("timer_remaining_ms", TASK_SECONDS * 1000L);
        routineFinished = prefs.getBoolean("routine_finished", false);
        if (prefs.getBoolean("setup_complete", false)) showHome();
        else showPage();
    }

    @Override protected void onPause() {
        super.onPause();
        persistTimer();
        if (ticker != null) handler.removeCallbacks(ticker);
    }

    @Override protected void onDestroy() {
        if (ticker != null) handler.removeCallbacks(ticker);
        super.onDestroy();
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
        b.setTextColor(INK);
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
        root.addView(new View(this), new LinearLayout.LayoutParams(1, 0, 1));
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
        for (String option : options) addChoice(option, struggle.equals(option), () -> { struggle = option; showPage(); });
        addChoice("I'm not sure", struggle.equals("I'm not sure"), () -> { struggle = "I'm not sure"; showPage(); });
        root.addView(new View(this), new LinearLayout.LayoutParams(1, 0, 1));
        nextButton("Next", () -> { page = 2; showPage(); });
    }

    private void showWakeTime() {
        base("What time do you wake up?", "Your routine will start at this time.", 3);
        TextView time = text(String.format(Locale.getDefault(), "%d:%02d %s",
            (wakeHour % 12 == 0 ? 12 : wakeHour % 12), wakeMinute, wakeHour < 12 ? "AM" : "PM"), 36, INK, true);
        root.addView(time, new LinearLayout.LayoutParams(-1, dp(180)));
        root.addView(button("Choose time", false, () -> {
            TimePickerDialog dialog = new TimePickerDialog(this, (view, hour, minute) -> {
                wakeHour = hour; wakeMinute = minute; showPage();
            }, wakeHour, wakeMinute, false);
            dialog.show();
        }), new LinearLayout.LayoutParams(-1, dp(56)));
        root.addView(new View(this), new LinearLayout.LayoutParams(1, 0, 1));
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
        root.addView(new View(this), new LinearLayout.LayoutParams(1, 0, 1));
        nextButton("Looks good", () -> {
            prefs.edit().putBoolean("setup_complete", true).putString("routine_type", routineType)
                .putString("struggle", struggle).putInt("wake_hour", wakeHour)
                .putInt("wake_minute", wakeMinute).putBoolean("routine_created", true).apply();
            showHome();
        });
    }

    private void showHome() {
        if (ticker != null) handler.removeCallbacks(ticker);
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
            final int index = i;
            addChoice((prefs.getBoolean("task_done_" + i, false) ? "✓  " : (i + 1) + "   ") + tasks[i],
                prefs.getBoolean("task_done_" + i, false), () -> {
                    if (prefs.getBoolean("task_done_" + index, false))
                        prefs.edit().putBoolean("task_done_" + index, false).apply();
                    else prefs.edit().putBoolean("task_done_" + index, true).apply();
                    showHome();
                });
        }
        if (routineFinished) {
            TextView finished = text("Routine complete — well done!", 18, INK, true);
            root.addView(finished);
            nextButton("Start again", () -> startNewRoutine());
        } else {
            nextButton("Start routine", () -> {
                currentTask = firstIncompleteTask();
                if (currentTask >= tasks.length) {
                    finishRoutine();
                } else {
                    remainingMs = TASK_SECONDS * 1000L;
                    timerRunning = false;
                    persistTimer();
                    showTimer();
                }
            });
        }
        root.addView(button("Restart onboarding", false, () -> {
            if (ticker != null) handler.removeCallbacks(ticker);
            prefs.edit().clear().apply(); page = 0; struggle = "I'm not sure";
            routineFinished = false; currentTask = 0; remainingMs = TASK_SECONDS * 1000L; showPage();
        }));
    }

    private int firstIncompleteTask() {
        for (int i = 0; i < tasks.length; i++) if (!prefs.getBoolean("task_done_" + i, false)) return i;
        return tasks.length;
    }

    private void showTimer() {
        if (ticker != null) handler.removeCallbacks(ticker);
        root = column();
        root.setBackgroundColor(Color.WHITE);
        setContentView(root);
        root.addView(text(prefs.getString("routine_type", routineType), 20, MUTED, true));
        taskPositionView = text("TASK " + (currentTask + 1) + " OF " + tasks.length, 14, MUTED, true);
        LinearLayout.LayoutParams pos = new LinearLayout.LayoutParams(-1, -2);
        pos.setMargins(0, dp(36), 0, dp(16));
        root.addView(taskPositionView, pos);
        taskTitleView = text(tasks[currentTask], 27, INK, true);
        LinearLayout.LayoutParams title = new LinearLayout.LayoutParams(-1, dp(150));
        root.addView(taskTitleView, title);
        countdownView = text(formatTime(remainingMs), 56, INK, true);
        LinearLayout.LayoutParams time = new LinearLayout.LayoutParams(-1, dp(130));
        time.setMargins(0, dp(20), 0, dp(20));
        root.addView(countdownView, time);
        root.addView(text("Take it one step at a time.", 16, MUTED, false));
        root.addView(new View(this), new LinearLayout.LayoutParams(1, 0, 1));
        pauseButton = button(timerRunning ? "Pause" : "Resume", false, () -> toggleTimer());
        root.addView(pauseButton, new LinearLayout.LayoutParams(-1, dp(60)));
        nextButton("Complete task  →", () -> completeCurrentTask());
        root.addView(button("Skip task", false, () -> advanceTask(false)), new LinearLayout.LayoutParams(-1, dp(54)));
        root.addView(button("End routine", false, () -> {
            stopTimer(); persistTimer(); showHome();
        }), new LinearLayout.LayoutParams(-1, dp(54)));
        if (timerRunning) runTicker();
    }

    private void toggleTimer() {
        if (timerRunning) {
            remainingMs = Math.max(0, deadlineMs - System.currentTimeMillis());
            timerRunning = false;
            if (ticker != null) handler.removeCallbacks(ticker);
            persistTimer();
        } else {
            if (remainingMs <= 0) remainingMs = TASK_SECONDS * 1000L;
            deadlineMs = System.currentTimeMillis() + remainingMs;
            timerRunning = true;
            persistTimer();
        }
        showTimer();
    }

    private void runTicker() {
        if (ticker != null) handler.removeCallbacks(ticker);
        ticker = new Runnable() {
            @Override public void run() {
                if (!timerRunning || countdownView == null) return;
                remainingMs = Math.max(0, deadlineMs - System.currentTimeMillis());
                countdownView.setText(formatTime(remainingMs));
                if (remainingMs <= 0) {
                    timerRunning = false;
                    persistTimer();
                    Toast.makeText(MainActivity.this, "Time is up — complete the task or continue.", Toast.LENGTH_LONG).show();
                    if (pauseButton != null) pauseButton.setText("Resume");
                } else {
                    handler.postDelayed(this, 250);
                }
            }
        };
        handler.post(ticker);
    }

    private String formatTime(long millis) {
        long seconds = Math.max(0, (millis + 999) / 1000);
        return String.format(Locale.getDefault(), "%02d:%02d", seconds / 60, seconds % 60);
    }

    private void completeCurrentTask() {
        stopTimer();
        prefs.edit().putBoolean("task_done_" + currentTask, true).apply();
        advanceTask(true);
    }

    private void advanceTask(boolean completed) {
        stopTimer();
        if (currentTask + 1 >= tasks.length) {
            finishRoutine();
            return;
        }
        currentTask++;
        remainingMs = TASK_SECONDS * 1000L;
        persistTimer();
        showTimer();
    }

    private void finishRoutine() {
        stopTimer();
        routineFinished = true;
        prefs.edit().putBoolean("routine_finished", true)
            .putLong("last_completion_time", System.currentTimeMillis()).apply();
        showCompletion();
    }

    private void showCompletion() {
        root = column();
        root.setBackgroundColor(Color.WHITE);
        setContentView(root);
        root.addView(text("You're all done!", 30, INK, true));
        root.addView(text("You completed your routine.", 17, MUTED, false));
        int completed = 0;
        for (int i = 0; i < tasks.length; i++) if (prefs.getBoolean("task_done_" + i, false)) completed++;
        root.addView(text(completed + " of " + tasks.length + " tasks completed", 20, INK, true));
        root.addView(new View(this), new LinearLayout.LayoutParams(1, 0, 1));
        nextButton("Back to Today", () -> showHome());
        root.addView(button("Run again", false, () -> startNewRoutine()));
    }

    private void startNewRoutine() {
        stopTimer();
        currentTask = 0;
        remainingMs = TASK_SECONDS * 1000L;
        routineFinished = false;
        SharedPreferences.Editor edit = prefs.edit().putBoolean("routine_finished", false)
            .putInt("timer_task_index", 0).putLong("timer_remaining_ms", remainingMs);
        for (int i = 0; i < tasks.length; i++) edit.putBoolean("task_done_" + i, false);
        edit.apply();
        showHome();
    }

    private void stopTimer() {
        timerRunning = false;
        if (ticker != null) handler.removeCallbacks(ticker);
        ticker = null;
    }

    private void persistTimer() {
        if (timerRunning) remainingMs = Math.max(0, deadlineMs - System.currentTimeMillis());
        prefs.edit().putInt("timer_task_index", currentTask)
            .putLong("timer_remaining_ms", remainingMs)
            .putBoolean("timer_running", timerRunning)
            .putBoolean("routine_finished", routineFinished).apply();
    }
}
