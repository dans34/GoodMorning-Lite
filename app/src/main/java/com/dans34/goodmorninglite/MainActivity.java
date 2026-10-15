package com.dans34.goodmorninglite;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.TimePickerDialog;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.Locale;

public class MainActivity extends Activity {
    private static final int INK = Color.rgb(57,58,70), MUTED = Color.rgb(140,141,150);
    private static final int PALE = Color.rgb(249,249,250), YELLOW = Color.rgb(255,224,0);
    private static final int DEFAULT_SECONDS = 120;
    private static class Task {
        String name, notes; int seconds;
        Task(String n, int s, String note) { name=n; seconds=s; notes=note; }
        JSONObject json() { JSONObject o=new JSONObject(); try { o.put("name",name); o.put("seconds",seconds); o.put("notes",notes); } catch(Exception ignored) {} return o; }
    }
    private final ArrayList<Task> tasks = new ArrayList<>();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private SharedPreferences prefs;
    private LinearLayout root;
    private String routineType="Morning routine", struggle="I'm not sure";
    private int wakeHour=6, wakeMinute=0, page=0, currentTask=0;
    private long remainingMs=DEFAULT_SECONDS*1000L, deadlineMs=0L;
    private boolean timerRunning=false, routineFinished=false;
    private Runnable ticker;
    private TextView countdownView;
    private Button pauseButton;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        prefs=getSharedPreferences("goodmorning_local",MODE_PRIVATE);
        routineType=prefs.getString("routine_type",routineType); struggle=prefs.getString("struggle",struggle);
        wakeHour=prefs.getInt("wake_hour",6); wakeMinute=prefs.getInt("wake_minute",0);
        loadTasks();
        currentTask=prefs.getInt("timer_task_index",0); remainingMs=prefs.getLong("timer_remaining_ms",DEFAULT_SECONDS*1000L);
        routineFinished=prefs.getBoolean("routine_finished",false);
        if (prefs.getBoolean("setup_complete",false)) {
            if (prefs.getBoolean("timer_screen_active",false) && !routineFinished && currentTask>=0 && currentTask<tasks.size()) showTimer();
            else if (routineFinished) showCompletion(); else showHome();
        } else showPage();
    }
    @Override protected void onPause() {
        super.onPause();
        if(timerRunning){remainingMs=Math.max(0,deadlineMs-System.currentTimeMillis());timerRunning=false;}
        if(ticker!=null)handler.removeCallbacks(ticker);
        persistTimer();
        if(countdownView!=null)countdownView.setText(formatTime(remainingMs));
        if(pauseButton!=null)pauseButton.setText("Resume");
    }
    @Override protected void onDestroy(){if(ticker!=null)handler.removeCallbacks(ticker);super.onDestroy();}
    private int dp(float v){return (int)(v*getResources().getDisplayMetrics().density+0.5f);}
    private TextView text(String s,int size,int color,boolean bold){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color);t.setGravity(Gravity.CENTER);if(bold)t.setTypeface(Typeface.create("sans-serif",Typeface.BOLD));return t;}
    private LinearLayout column(){LinearLayout l=new LinearLayout(this);l.setOrientation(1);l.setPadding(dp(22),dp(16),dp(22),dp(16));return l;}
    private void startScreen(){root=column();root.setBackgroundColor(Color.WHITE);setContentView(root);}
    private void heading(String title,String sub){TextView h=text(title,25,INK,true);h.setGravity(Gravity.START);root.addView(h);TextView s=text(sub,15,MUTED,false);s.setGravity(Gravity.START);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.setMargins(0,dp(7),0,dp(18));root.addView(s,p);}
    private Button button(String label,Runnable action){Button b=new Button(this);b.setText(label);b.setTextSize(15);b.setAllCaps(false);b.setTextColor(INK);b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(PALE));b.setMinHeight(dp(48));b.setOnClickListener(v->action.run());return b;}
    private void addButton(String label,Runnable action){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.setMargins(0,dp(4),0,dp(4));root.addView(button(label,action),p);}
    private void primary(String label,Runnable action){Button b=button(label,action);b.setTextColor(Color.WHITE);b.setTextSize(17);b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(INK));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,dp(58));p.setMargins(0,dp(14),0,dp(8));root.addView(b,p);}
    private void showPage(){if(page==0){startScreen();heading("Which routine should we build first?","We'll build just one for now.");addButton("☀  Morning routine",()->{routineType="Morning routine";page=1;showPage();});addButton("☾  Evening routine",()->{routineType="Evening routine";page=1;showPage();});}else if(page==1){startScreen();heading("What do you struggle with?","We'll use this to shape your routine.");String[] a={"Planning without taking action","Managing my schedule","Staying focused","Remembering tasks","Being busy without breaks","Feeling drained and unmotivated","I'm not sure"};for(String x:a)addButton((struggle.equals(x)?"✓  ":"")+" "+x,()->{struggle=x;page=2;showPage();});}else if(page==2){startScreen();heading("What time do you wake up?","You can change this later.");root.addView(text(String.format(Locale.getDefault(),"%d:%02d %s",wakeHour%12==0?12:wakeHour%12,wakeMinute,wakeHour<12?"AM":"PM"),38,INK,true));addButton("Choose time",()->new TimePickerDialog(this,(v,h,m)->{wakeHour=h;wakeMinute=m;showPage();},wakeHour,wakeMinute,false).show());primary("Next",()->{page=3;showPage();});}else{startScreen();heading("Your first routine","Start with these simple tasks. You can edit them any time.");for(Task t:tasks)root.addView(text("•  "+t.name+" · "+duration(t.seconds),16,INK,false));primary("Looks good",()->{prefs.edit().putBoolean("setup_complete",true).putString("routine_type",routineType).putString("struggle",struggle).putInt("wake_hour",wakeHour).putInt("wake_minute",wakeMinute).apply();saveTasks();showHome();});}}
    private void loadTasks(){tasks.clear();try{JSONArray a=new JSONArray(prefs.getString("routine_tasks",""));for(int i=0;i<a.length();i++){JSONObject o=a.getJSONObject(i);tasks.add(new Task(o.optString("name","Task"),Math.max(1,o.optInt("seconds",DEFAULT_SECONDS)),o.optString("notes","")));}}catch(Exception ignored){}if(tasks.isEmpty()){tasks.add(new Task("Stretch and drink a glass of water",120,""));tasks.add(new Task("Tidy up the bed",120,""));tasks.add(new Task("Check the weather",120,""));}}
    private void saveTasks(){JSONArray a=new JSONArray();for(Task t:tasks)a.put(t.json());prefs.edit().putString("routine_tasks",a.toString()).apply();}
    private String duration(int seconds){return seconds/60+" min"+(seconds%60==0?"":" "+seconds%60+" sec");}
    private String greetingForCurrentTime(){\n        int hour=java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY);\n        if(hour>=5 && hour<12)return "Good Morning";\n        if(hour>=12 && hour<17)return "Good Afternoon";\n        return "Good Evening";\n    }\n    private void showHome(){if(ticker!=null)handler.removeCallbacks(ticker);startScreen();heading(greetingForCurrentTime(),"Your routine, one step at a time.");TextView today=text("TODAY'S ROUTINE",13,MUTED,true);today.setGravity(Gravity.START);root.addView(today);TextView rt=text(routineType,23,INK,true);rt.setGravity(Gravity.START);root.addView(rt);for(int i=0;i<tasks.size();i++){final int ix=i;addButton((prefs.getBoolean("done_"+i,false)?"✓  ": (i+1)+".  ")+tasks.get(i).name+"  ·  "+duration(tasks.get(i).seconds),()->editTask(ix));}primary("Start routine",()->{currentTask=firstIncomplete();if(currentTask>=tasks.size()){Toast.makeText(this,"All tasks are complete.",Toast.LENGTH_SHORT).show();return;}remainingMs=tasks.get(currentTask).seconds*1000L;timerRunning=true;deadlineMs=System.currentTimeMillis()+remainingMs;prefs.edit().putBoolean("timer_screen_active",true).apply();persistTimer();showTimer();});addButton("+ Add task",()->showAddTaskType());TextView manage=text("MANAGE ROUTINE",13,MUTED,true);manage.setGravity(Gravity.START);LinearLayout.LayoutParams mp=new LinearLayout.LayoutParams(-1,-2);mp.setMargins(0,dp(22),0,dp(6));root.addView(manage,mp);for(int i=0;i<tasks.size();i++){final int ix=i;addButton("✏️  "+tasks.get(i).name+"  ·  ⚙️",()->taskActions(ix));}addButton("Restart routine progress",()->startNewRoutine());}
    private int firstIncomplete(){for(int i=0;i<tasks.size();i++)if(!prefs.getBoolean("done_"+i,false))return i;return tasks.size();}
    private void taskActions(int index){String[] options={"Edit task","Move up","Move down","Delete task"};new AlertDialog.Builder(this).setTitle(tasks.get(index).name).setItems(options,(d,which)->{if(which==0)editTask(index);else if(which==1&&index>0){Task t=tasks.remove(index);tasks.add(index-1,t);saveTasks();showHome();}else if(which==2&&index<tasks.size()-1){Task t=tasks.remove(index);tasks.add(index+1,t);saveTasks();showHome();}else if(which==3)new AlertDialog.Builder(this).setMessage("Delete this task?").setNegativeButton("Cancel",null).setPositiveButton("Delete",(dd,w)->{tasks.remove(index);saveTasks();showHome();}).show();}).show();}
    private void showAddTaskType(){
        startScreen();
        heading("When does this task belong?","Choose a time of day for your new task.");
        addButton("☀️  Morning",()->editTaskForPeriod("Morning"));
        addButton("🌤️  Evening",()->editTaskForPeriod("Evening"));
        addButton("🌙  Night",()->editTaskForPeriod("Night"));
        addButton("Cancel",this::showHome);
    }
    private void editTaskForPeriod(String period){
        final EditText name=new EditText(this);
        name.setHint("Task name");
        LinearLayout form=new LinearLayout(this);
        form.setPadding(dp(20),dp(8),dp(20),0);
        form.setOrientation(1);
        form.addView(name);
        EditText minutes=new EditText(this);
        minutes.setHint("Duration in minutes");
        minutes.setInputType(InputType.TYPE_CLASS_NUMBER);
        minutes.setText("2");
        form.addView(minutes);
        EditText notes=new EditText(this);
        notes.setHint("Notes (optional)");
        form.addView(notes);
        new AlertDialog.Builder(this).setTitle(period+" task").setMessage("Add a task to your "+period.toLowerCase(Locale.ROOT)+" routine.").setView(form).setNegativeButton("Cancel",(d,w)->showHome()).setPositiveButton("Create task",(d,w)->{
            String n=name.getText().toString().trim();
            if(n.isEmpty()){Toast.makeText(this,"Enter a task name.",Toast.LENGTH_SHORT).show();return;}
            int mins;
            try{mins=Integer.parseInt(minutes.getText().toString());}catch(Exception e){mins=2;}
            mins=Math.max(1,Math.min(240,mins));
            tasks.add(new Task(n,mins*60,notes.getText().toString().trim()));
            saveTasks();
            showHome();
        }).show();
    }
    private void editTask(int index){boolean add=index<0;Task old=add?new Task("",DEFAULT_SECONDS,""):tasks.get(index);LinearLayout form=new LinearLayout(this);form.setPadding(dp(20),dp(8),dp(20),0);form.setOrientation(1);EditText name=new EditText(this);name.setHint("Task name");name.setText(old.name);form.addView(name);EditText minutes=new EditText(this);minutes.setHint("Duration in minutes");minutes.setInputType(InputType.TYPE_CLASS_NUMBER);minutes.setText(String.valueOf(Math.max(1,(old.seconds+59)/60)));form.addView(minutes);EditText notes=new EditText(this);notes.setHint("Notes (optional)");notes.setText(old.notes);form.addView(notes);new AlertDialog.Builder(this).setTitle(add?"Add task":"Edit task").setView(form).setNegativeButton("Cancel",null).setPositiveButton("Save",(d,w)->{String n=name.getText().toString().trim();if(n.isEmpty()){Toast.makeText(this,"Enter a task name.",Toast.LENGTH_SHORT).show();return;}int mins;try{mins=Integer.parseInt(minutes.getText().toString());}catch(Exception e){mins=2;}mins=Math.max(1,Math.min(240,mins));Task t=new Task(n,mins*60,notes.getText().toString().trim());if(add)tasks.add(t);else tasks.set(index,t);saveTasks();showHome();}).show();}
    private void showTimer(){if(ticker!=null)handler.removeCallbacks(ticker);if(tasks.isEmpty()){showHome();return;}currentTask=Math.max(0,Math.min(currentTask,tasks.size()-1));startScreen();heading(routineType,"TASK "+(currentTask+1)+" OF "+tasks.size());root.addView(text(tasks.get(currentTask).name,27,INK,true));if(!tasks.get(currentTask).notes.isEmpty())root.addView(text(tasks.get(currentTask).notes,16,MUTED,false));countdownView=text(formatTime(remainingMs),54,INK,true);root.addView(countdownView,new LinearLayout.LayoutParams(-1,dp(150)));root.addView(new View(this),new LinearLayout.LayoutParams(1,0,1));pauseButton=button(timerRunning?"Pause":"Resume",()->toggleTimer());root.addView(pauseButton);primary("Complete task →",()->{prefs.edit().putBoolean("done_"+currentTask,true).apply();nextTask();});addButton("Skip task",this::nextTask);addButton("End routine",()->{stopTimer();prefs.edit().putBoolean("timer_screen_active",false).apply();showHome();});if(timerRunning)runTicker();}
    private void toggleTimer(){if(timerRunning){remainingMs=Math.max(0,deadlineMs-System.currentTimeMillis());timerRunning=false;}else{if(remainingMs<=0)remainingMs=tasks.get(currentTask).seconds*1000L;deadlineMs=System.currentTimeMillis()+remainingMs;timerRunning=true;}persistTimer();showTimer();}
    private void runTicker(){ticker=new Runnable(){public void run(){if(!timerRunning||countdownView==null)return;remainingMs=Math.max(0,deadlineMs-System.currentTimeMillis());countdownView.setText(formatTime(remainingMs));if(remainingMs==0){timerRunning=false;persistTimer();Toast.makeText(MainActivity.this,"Time is up — complete the task or continue.",Toast.LENGTH_LONG).show();if(pauseButton!=null)pauseButton.setText("Resume");}else handler.postDelayed(this,250);}};handler.post(ticker);}
    private void nextTask(){stopTimer();if(currentTask+1>=tasks.size()){routineFinished=true;prefs.edit().putBoolean("routine_finished",true).putBoolean("timer_screen_active",false).putLong("last_completion_time",System.currentTimeMillis()).apply();showCompletion();return;}currentTask++;remainingMs=tasks.get(currentTask).seconds*1000L;timerRunning=true;deadlineMs=System.currentTimeMillis()+remainingMs;persistTimer();showTimer();}
    private void showCompletion(){stopTimer();startScreen();heading("You're all done!","You completed your routine.");int done=0;for(int i=0;i<tasks.size();i++)if(prefs.getBoolean("done_"+i,false))done++;root.addView(text(done+" of "+tasks.size()+" tasks completed",20,INK,true));primary("Back to Today",this::showHome);addButton("Run again",this::startNewRoutine);}
    private void startNewRoutine(){stopTimer();for(int i=0;i<tasks.size();i++)prefs.edit().remove("done_"+i).apply();currentTask=0;remainingMs=tasks.isEmpty()?DEFAULT_SECONDS*1000L:tasks.get(0).seconds*1000L;routineFinished=false;prefs.edit().putBoolean("routine_finished",false).putBoolean("timer_screen_active",false).putInt("timer_task_index",0).putLong("timer_remaining_ms",remainingMs).apply();showHome();}
    private void stopTimer(){timerRunning=false;if(ticker!=null)handler.removeCallbacks(ticker);ticker=null;}
    private String formatTime(long ms){long s=Math.max(0,(ms+999)/1000);return String.format(Locale.getDefault(),"%02d:%02d",s/60,s%60);}
    private void persistTimer(){if(timerRunning)remainingMs=Math.max(0,deadlineMs-System.currentTimeMillis());prefs.edit().putInt("timer_task_index",currentTask).putLong("timer_remaining_ms",remainingMs).putBoolean("timer_running",timerRunning).putBoolean("routine_finished",routineFinished).apply();}
}
