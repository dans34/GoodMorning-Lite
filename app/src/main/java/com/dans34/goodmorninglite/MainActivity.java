package com.dans34.goodmorninglite;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.TimePickerDialog;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.animation.ValueAnimator;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.GridLayout;
import android.widget.HorizontalScrollView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.Locale;
import java.util.UUID;

public class MainActivity extends Activity {
    private static final int INK = Color.rgb(57,58,70), MUTED = Color.rgb(140,141,150);
    private static final int PALE = Color.rgb(249,249,250), YELLOW = Color.rgb(255,224,0);
    private static final int DEFAULT_SECONDS = 120;
    private static class Task {
        String id, name, notes, period, emoji; int seconds;
        Task(String n, int s, String note) { this(UUID.randomUUID().toString(),n,s,note,"Morning"); }
        Task(String n, int s, String note, String p) { this(UUID.randomUUID().toString(),n,s,note,p); }
        Task(String taskId,String n,int s,String note,String p) { id=taskId;name=n;seconds=s;notes=note;period=p;emoji=emojiForTaskName(n); }
        JSONObject json() { JSONObject o=new JSONObject(); try { o.put("id",id);o.put("name",name); o.put("seconds",seconds); o.put("notes",notes); o.put("period",period);o.put("emoji",emoji); } catch(Exception ignored) {} return o; }
    }
    private final ArrayList<Task> tasks = new ArrayList<>();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private SharedPreferences prefs;
    private LinearLayout root;
    private ScrollView screenScroll;
    private String routineType="Morning routine", activePeriod="Morning", struggle="I'm not sure";
    private int wakeHour=6, wakeMinute=0, page=0, currentTask=0;
    private long remainingMs=DEFAULT_SECONDS*1000L, deadlineMs=0L;
    private boolean timerRunning=false, routineFinished=false, singleTaskMode=false;
    private String currentTaskId="";
    private String libraryCategory="Everyday";
    private Runnable ticker;
    private TextView countdownView;
    private Button pauseButton;
    private boolean completionAnimationRunning=false;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        prefs=getSharedPreferences("goodmorning_local",MODE_PRIVATE);
        routineType=prefs.getString("routine_type",routineType); activePeriod=prefs.getString("active_period",routineType.replace(" routine","")); struggle=prefs.getString("struggle",struggle);
        wakeHour=prefs.getInt("wake_hour",6); wakeMinute=prefs.getInt("wake_minute",0);
        loadTasks();
        currentTask=prefs.getInt("timer_task_index",0); currentTaskId=prefs.getString("timer_task_id",""); remainingMs=prefs.getLong("timer_remaining_ms",DEFAULT_SECONDS*1000L);
        routineFinished=prefs.getBoolean("routine_finished",false); singleTaskMode=prefs.getBoolean("single_task_mode",false);
        // Always open on the home screen after launch; saved routine progress remains intact.
        if (prefs.getBoolean("setup_complete",false)) {
            showHome();
        } else {
            showPage();
        }
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
    private void startScreen(){screenScroll=new ScrollView(this);screenScroll.setFillViewport(true);screenScroll.setClipToPadding(false);root=column();root.setBackgroundColor(Color.WHITE);screenScroll.addView(root,new ScrollView.LayoutParams(-1,-2));setContentView(screenScroll);}
    private void heading(String title,String sub){TextView h=text(title,25,INK,true);h.setGravity(Gravity.START);root.addView(h);TextView s=text(sub,15,MUTED,false);s.setGravity(Gravity.START);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.setMargins(0,dp(7),0,dp(18));root.addView(s,p);}
    private Button button(String label,Runnable action){Button b=new Button(this);b.setText(label);b.setTextSize(15);b.setAllCaps(false);b.setTextColor(INK);b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(PALE));b.setMinHeight(dp(48));b.setOnClickListener(v->action.run());return b;}
    private void addButton(String label,Runnable action){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.setMargins(0,dp(4),0,dp(4));root.addView(button(label,action),p);}
    private void primary(String label,Runnable action){Button b=button(label,action);b.setTextColor(Color.WHITE);b.setTextSize(17);b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(INK));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,dp(58));p.setMargins(0,dp(14),0,dp(8));root.addView(b,p);}
    private void showPage(){if(page==0){startScreen();heading("Which routine should we build first?","We'll build just one for now.");addButton("☀  Morning routine",()->{routineType="Morning routine";activePeriod="Morning";page=1;showPage();});addButton("☾  Evening routine",()->{routineType="Evening routine";activePeriod="Evening";page=1;showPage();});}else if(page==1){startScreen();heading("What do you struggle with?","We'll use this to shape your routine.");String[] a={"Planning without taking action","Managing my schedule","Staying focused","Remembering tasks","Being busy without breaks","Feeling drained and unmotivated","I'm not sure"};for(String x:a)addButton((struggle.equals(x)?"✓  ":"")+" "+x,()->{struggle=x;page=2;showPage();});}else if(page==2){startScreen();heading("What time do you wake up?","You can change this later.");root.addView(text(String.format(Locale.getDefault(),"%d:%02d %s",wakeHour%12==0?12:wakeHour%12,wakeMinute,wakeHour<12?"AM":"PM"),38,INK,true));addButton("Choose time",()->new TimePickerDialog(this,(v,h,m)->{wakeHour=h;wakeMinute=m;showPage();},wakeHour,wakeMinute,false).show());primary("Next",()->{page=3;showPage();});}else{startScreen();heading("Your first routine","Start with these simple tasks. You can edit them any time.");for(Task t:tasks)root.addView(text("•  "+t.name+" · "+duration(t.seconds),16,INK,false));primary("Looks good",()->{prefs.edit().putBoolean("setup_complete",true).putString("routine_type",routineType).putString("active_period",activePeriod).putString("struggle",struggle).putInt("wake_hour",wakeHour).putInt("wake_minute",wakeMinute).apply();saveTasks();showHome();});}}
    private void loadTasks(){
        tasks.clear();boolean migrated=false;
        try{JSONArray a=new JSONArray(prefs.getString("routine_tasks",""));for(int i=0;i<a.length();i++){
            JSONObject o=a.getJSONObject(i);String id=o.optString("id","");
            if(id.isEmpty()){id=UUID.randomUUID().toString();migrated=true;}
            Task t=new Task(id,o.optString("name","Task"),Math.max(1,o.optInt("seconds",DEFAULT_SECONDS)),o.optString("notes",""),o.optString("period",routineType.replace(" routine","")));t.emoji=o.optString("emoji",emojiForTaskName(t.name));
            tasks.add(t);
            // Preserve existing completion state while moving from index-based to task-based keys.
            String legacy="done_"+t.period.toLowerCase(Locale.ROOT)+"_"+countPriorTasksForPeriod(i,t.period);
            if(prefs.getBoolean(legacy,false))prefs.edit().putBoolean("done_"+t.id,true).apply();
        }}catch(Exception ignored){}
        if(tasks.isEmpty()){tasks.add(new Task("Stretch and drink a glass of water",120,"","Morning"));tasks.add(new Task("Tidy up the bed",120,"","Morning"));tasks.add(new Task("Check the weather",120,"","Morning"));migrated=true;}
        if(migrated)saveTasks();
    }
    private int countPriorTasksForPeriod(int index,String period){int count=0;try{JSONArray a=new JSONArray(prefs.getString("routine_tasks",""));for(int i=0;i<index;i++)if(a.getJSONObject(i).optString("period",routineType.replace(" routine","")).equals(period))count++;}catch(Exception ignored){}return count;}
    private void saveTasks(){JSONArray a=new JSONArray();for(Task t:tasks)a.put(t.json());prefs.edit().putString("routine_tasks",a.toString()).putString("active_period",activePeriod).apply();}
    private ArrayList<Task> routineTasks(){ArrayList<Task> out=new ArrayList<>();for(Task t:tasks)if(t.period.equals(activePeriod))out.add(t);return out;}
    private int globalTaskIndex(int visibleIndex){int n=0;for(int i=0;i<tasks.size();i++)if(tasks.get(i).period.equals(activePeriod)){if(n==visibleIndex)return i;n++;}return -1;}
    private String doneKey(Task task){return "done_"+task.id;}
    private String periodEmoji(String p){return p.equals("Morning")?"☀️":p.equals("Evening")?"🌤️":"🌙";}
    private static String emojiForTaskName(String name){
        String n=name.toLowerCase(Locale.ROOT);
        if(n.contains("water")||n.contains("drink"))return "💧";
        if(n.contains("bed"))return "🛏️";
        if(n.contains("weather"))return "🌤️";
        if(n.contains("shopping")||n.contains("shop"))return "🛒";
        if(n.contains("parcel")||n.contains("pack"))return "📦";
        if(n.contains("account")||n.contains("payment")||n.contains("bill"))return "💳";
        if(n.contains("dish")||n.contains("wash"))return "🍽️";
        if(n.contains("washing")||n.contains("laundry"))return "🧺";
        if(n.contains("recycl"))return "♻️";
        if(n.contains("surface")||n.contains("tidy")||n.contains("clean"))return "🧹";
        if(n.contains("plan")||n.contains("schedule")||n.contains("arrange"))return "🗓️";
        if(n.contains("call")||n.contains("check in"))return "📞";
        if(n.contains("note")||n.contains("write"))return "📝";
        if(n.contains("stretch")||n.contains("breath"))return "🧘";
        if(n.contains("walk"))return "🚶";
        if(n.contains("move"))return "🏃";
        if(n.contains("workspace")||n.contains("focus")||n.contains("work"))return "📚";
        if(n.contains("tomorrow")||n.contains("evening"))return "🌙";
        return "✨";
    }
    private String duration(int seconds){return seconds/60+" min"+(seconds%60==0?"":" "+seconds%60+" sec");}
    private String greetingForCurrentTime(){
        int hour=java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY);
        if(hour>=5 && hour<12)return "Good Morning";
        if(hour>=12 && hour<17)return "Good Afternoon";
        return "Good Evening";
    }
    private void showHome(){
        managePageOpen=false;
        if(ticker!=null)handler.removeCallbacks(ticker);
        startScreen();
        root.setPadding(dp(22),dp(8),dp(22),dp(16));
        LinearLayout header=new LinearLayout(this);header.setOrientation(0);header.setGravity(Gravity.TOP);
        LinearLayout titleBlock=new LinearLayout(this);titleBlock.setOrientation(1);
        TextView greeting=text(greetingForCurrentTime(),25,INK,true);greeting.setGravity(Gravity.START);
        TextView subtitle=text("Your routine, one step at a time.",15,MUTED,false);subtitle.setGravity(Gravity.START);
        titleBlock.addView(greeting);LinearLayout.LayoutParams subParams=new LinearLayout.LayoutParams(-1,-2);subParams.topMargin=dp(3);titleBlock.addView(subtitle,subParams);
        header.addView(titleBlock,new LinearLayout.LayoutParams(0,-2,1));
        LinearLayout actions=new LinearLayout(this);actions.setOrientation(0);actions.setGravity(Gravity.TOP);
        Button add=new Button(this);add.setText("+");add.setTextSize(28);add.setAllCaps(false);add.setTextColor(INK);
        add.setPadding(0,0,0,dp(3));add.setMinWidth(0);add.setMinimumWidth(0);add.setMinHeight(0);add.setMinimumHeight(0);
        add.setBackgroundTintList(android.content.res.ColorStateList.valueOf(YELLOW));
        LinearLayout.LayoutParams addParams=new LinearLayout.LayoutParams(dp(42),dp(42));addParams.leftMargin=dp(4);
        actions.addView(add,addParams);add.setOnClickListener(v->showAddTaskType());
        Button manage=new Button(this);manage.setText("⚙");manage.setTextSize(22);manage.setAllCaps(false);manage.setTextColor(INK);
        manage.setContentDescription("Manage routines");manage.setPadding(0,0,0,dp(2));manage.setMinWidth(0);manage.setMinimumWidth(0);manage.setMinHeight(0);manage.setMinimumHeight(0);
        manage.setBackgroundTintList(android.content.res.ColorStateList.valueOf(PALE));
        LinearLayout.LayoutParams manageParams=new LinearLayout.LayoutParams(dp(38),dp(38));manageParams.leftMargin=dp(4);
        actions.addView(manage,manageParams);manage.setOnClickListener(v->showManageRoutines());
        Button history=new Button(this);history.setText("🏆");history.setTextSize(20);history.setAllCaps(false);history.setTextColor(INK);history.setContentDescription("History and streaks");history.setPadding(0,0,0,dp(2));history.setMinWidth(0);history.setMinimumWidth(0);history.setMinHeight(0);history.setMinimumHeight(0);history.setBackgroundTintList(android.content.res.ColorStateList.valueOf(PALE));
        LinearLayout.LayoutParams historyParams=new LinearLayout.LayoutParams(dp(38),dp(38));historyParams.leftMargin=dp(4);actions.addView(history,historyParams);history.setOnClickListener(v->showHistory());
        LinearLayout.LayoutParams actionBlockParams=new LinearLayout.LayoutParams(-2,dp(42));actionBlockParams.topMargin=dp(1);header.addView(actions,actionBlockParams);
        root.addView(header);
        LinearLayout.LayoutParams headerGap=new LinearLayout.LayoutParams(-1,-2);headerGap.bottomMargin=dp(8);header.setLayoutParams(headerGap);
        TextView today=text("TODAY'S ROUTINE",13,MUTED,true);today.setGravity(Gravity.START);root.addView(today);
        LinearLayout periodRow=new LinearLayout(this);periodRow.setOrientation(0);periodRow.setGravity(Gravity.CENTER_VERTICAL);
        for(String p:new String[]{"Morning","Evening","Night"}){
            final String chosen=p;
            Button periodButton=button(periodEmoji(p)+"  "+p,()->{
                activePeriod=chosen; routineType=chosen+" routine";
                prefs.edit().putString("active_period",activePeriod).putString("routine_type",routineType).apply();
                showHome();
            });
            periodButton.setTextSize(13);periodButton.setPadding(dp(2),0,dp(2),0);
            boolean selected=activePeriod.equals(p);
            periodButton.setBackgroundTintList(android.content.res.ColorStateList.valueOf(selected?INK:PALE));
            periodButton.setTextColor(selected?Color.WHITE:INK);
            LinearLayout.LayoutParams periodParams=new LinearLayout.LayoutParams(0,dp(46),1);
            periodParams.setMargins(0,0,dp(6),0);
            periodRow.addView(periodButton,periodParams);
        }
        if(periodRow.getChildCount()>0){periodRow.getChildAt(periodRow.getChildCount()-1).setLayoutParams(new LinearLayout.LayoutParams(0,dp(46),1));}
        LinearLayout.LayoutParams periodRowParams=new LinearLayout.LayoutParams(-1,-2);periodRowParams.bottomMargin=dp(10);root.addView(periodRow,periodRowParams);
        TextView rt=text(activePeriod+" routine",23,INK,true);rt.setGravity(Gravity.START);root.addView(rt);
        ArrayList<Task> visible=routineTasks();
        for(int i=0;i<visible.size();i++){
            Task t=visible.get(i);boolean completed=prefs.getBoolean(doneKey(t),false);
            LinearLayout taskCard=new LinearLayout(this);taskCard.setOrientation(0);taskCard.setGravity(Gravity.CENTER_VERTICAL);
            taskCard.setPadding(dp(12),0,dp(12),0);
            android.graphics.drawable.GradientDrawable cardBackground=new android.graphics.drawable.GradientDrawable();
            cardBackground.setColor(Color.rgb(245,245,248));cardBackground.setCornerRadius(dp(12));
            taskCard.setBackground(cardBackground);taskCard.setClickable(true);taskCard.setFocusable(true);
            if(android.os.Build.VERSION.SDK_INT>=21){
                taskCard.setBackground(new android.graphics.drawable.RippleDrawable(
                    android.content.res.ColorStateList.valueOf(Color.rgb(225,225,232)),cardBackground,null));
            }
            taskCard.setOnClickListener(v->Toast.makeText(this,"Use Manage Routines to edit tasks.",Toast.LENGTH_SHORT).show());
            TextView number=text(String.valueOf(i+1),12,MUTED,true);
            taskCard.addView(number,new LinearLayout.LayoutParams(dp(20),-2));
            TextView taskIcon=text(t.emoji,20,INK,false);
            LinearLayout.LayoutParams taskIconParams=new LinearLayout.LayoutParams(dp(32),dp(34));taskIconParams.leftMargin=dp(2);taskCard.addView(taskIcon,taskIconParams);
            TextView taskName=text(t.name,14,INK,true);taskName.setGravity(Gravity.CENTER_VERTICAL|Gravity.START);taskName.setMaxLines(2);
            LinearLayout.LayoutParams nameParams=new LinearLayout.LayoutParams(0,-2,1);nameParams.leftMargin=dp(4);taskCard.addView(taskName,nameParams);
            TextView taskDuration=text(duration(t.seconds),12,MUTED,false);
            LinearLayout.LayoutParams durationParams=new LinearLayout.LayoutParams(-2,-2);durationParams.leftMargin=dp(6);taskCard.addView(taskDuration,durationParams);
            TextView status=text(completed?"✓":"○",18,completed?Color.rgb(72,145,104):MUTED,true);
            LinearLayout.LayoutParams statusParams=new LinearLayout.LayoutParams(dp(28),dp(34));statusParams.leftMargin=dp(4);taskCard.addView(status,statusParams);
            LinearLayout.LayoutParams taskParams=new LinearLayout.LayoutParams(-1,dp(48));taskParams.setMargins(0,dp(3),0,dp(3));root.addView(taskCard,taskParams);
        }
        root.addView(new View(this),new LinearLayout.LayoutParams(1,dp(12)));
        primary("Start routine",()->startRoutine(false));
        addButton("✨  Just get started",()->startRoutine(true));
    }
    private boolean managePageOpen=false;
    private void showManageRoutines(){
        managePageOpen=true;startScreen();heading("Manage routines","Organise tasks for each part of your day.");
        LinearLayout periodRow=new LinearLayout(this);periodRow.setOrientation(0);periodRow.setGravity(Gravity.CENTER_VERTICAL);
        for(String p:new String[]{"Morning","Evening","Night"}){
            final String chosen=p;
            Button periodButton=button(periodEmoji(p)+"  "+p,()->{activePeriod=chosen;routineType=chosen+" routine";prefs.edit().putString("active_period",activePeriod).putString("routine_type",routineType).apply();showManageRoutines();});
            periodButton.setTextSize(13);periodButton.setPadding(dp(2),0,dp(2),0);
            boolean selected=activePeriod.equals(p);periodButton.setBackgroundTintList(android.content.res.ColorStateList.valueOf(selected?INK:PALE));periodButton.setTextColor(selected?Color.WHITE:INK);
            LinearLayout.LayoutParams pp=new LinearLayout.LayoutParams(0,dp(42),1);pp.setMargins(0,0,dp(6),0);periodRow.addView(periodButton,pp);
        }
        if(periodRow.getChildCount()>0)periodRow.getChildAt(periodRow.getChildCount()-1).setLayoutParams(new LinearLayout.LayoutParams(0,dp(42),1));
        LinearLayout.LayoutParams periodGap=new LinearLayout.LayoutParams(-1,-2);periodGap.bottomMargin=dp(4);root.addView(periodRow,periodGap);
        TextView section=text(activePeriod+" tasks",21,INK,true);section.setGravity(Gravity.START);LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(-1,-2);sp.setMargins(0,dp(8),0,dp(4));root.addView(section,sp);
        ArrayList<Task> visible=routineTasks();
        int totalSeconds=0;
        for(Task task:visible)totalSeconds+=task.seconds;
        TextView routineSummary=text(visible.size()+" "+(visible.size()==1?"task":"tasks")+"  ·  "+duration(totalSeconds)+" estimated total",14,INK,true);
        routineSummary.setGravity(Gravity.CENTER_VERTICAL|Gravity.START);
        routineSummary.setPadding(dp(12),dp(8),dp(12),dp(8));
        android.graphics.drawable.GradientDrawable summaryBackground=new android.graphics.drawable.GradientDrawable();
        summaryBackground.setColor(Color.rgb(255,248,210));summaryBackground.setCornerRadius(dp(12));
        routineSummary.setBackground(summaryBackground);
        LinearLayout.LayoutParams summaryParams=new LinearLayout.LayoutParams(-1,-2);
        summaryParams.setMargins(0,dp(2),0,dp(10));
        root.addView(routineSummary,summaryParams);
        if(visible.isEmpty())addButton("No tasks yet — browse the library",this::showTaskLibrary);
        for(int i=0;i<visible.size();i++){
            final int ix=globalTaskIndex(i);Task t=visible.get(i);
            LinearLayout taskLine=new LinearLayout(this);taskLine.setOrientation(0);taskLine.setGravity(Gravity.CENTER_VERTICAL);taskLine.setTag(t.id);
            Button taskRow=button("✏️  "+t.name+"  ·  "+duration(t.seconds)+"  ›",()->taskActions(globalTaskIndexForId(t.id)));taskRow.setTextSize(14);taskRow.setGravity(Gravity.CENTER_VERTICAL|Gravity.START);taskRow.setPadding(dp(8),0,dp(4),0);
            taskRow.setOnLongClickListener(v->{android.content.ClipData data=android.content.ClipData.newPlainText("task_id",t.id);android.view.View.DragShadowBuilder shadow=new View.DragShadowBuilder(taskLine);taskLine.startDragAndDrop(data,shadow,t.id,0);return true;});
            taskLine.setOnDragListener((view,event)->{
                switch(event.getAction()){
                    case android.view.DragEvent.ACTION_DRAG_STARTED:return event.getLocalState() instanceof String;
                    case android.view.DragEvent.ACTION_DRAG_ENTERED:view.setBackgroundColor(Color.rgb(255,248,190));return true;
                    case android.view.DragEvent.ACTION_DRAG_LOCATION: {
                        if(screenScroll!=null){
                            int[] scrollLocation=new int[2];int[] rowLocation=new int[2];
                            screenScroll.getLocationOnScreen(scrollLocation);view.getLocationOnScreen(rowLocation);
                            int pointerY=rowLocation[1]+(int)event.getY();
                            int topEdge=scrollLocation[1]+dp(72);
                            int bottomEdge=scrollLocation[1]+screenScroll.getHeight()-dp(72);
                            if(pointerY<topEdge)screenScroll.smoothScrollBy(0,-dp(14));
                            else if(pointerY>bottomEdge)screenScroll.smoothScrollBy(0,dp(14));
                        }
                        return true;
                    }
                    case android.view.DragEvent.ACTION_DRAG_EXITED:view.setBackgroundColor(Color.TRANSPARENT);return true;
                    case android.view.DragEvent.ACTION_DRAG_ENDED:view.setBackgroundColor(Color.TRANSPARENT);return true;
                    case android.view.DragEvent.ACTION_DROP:
                        String sourceId=(String)event.getLocalState();String targetId=(String)view.getTag();
                        if(sourceId.equals(targetId))return true;
                        int sourceIndex=globalTaskIndexForId(sourceId);int targetIndex=globalTaskIndexForId(targetId);
                        if(sourceIndex<0||targetIndex<0)return false;
                        Task moving=tasks.get(sourceIndex);if(!moving.period.equals(activePeriod)||!tasks.get(targetIndex).period.equals(activePeriod))return false;
                        tasks.remove(sourceIndex);targetIndex=globalTaskIndexForId(targetId);
                        if(targetIndex<0)targetIndex=tasks.size();
                        tasks.add(targetIndex,moving);saveTasks();showManageRoutines();return true;
                }
                return true;
            });
            taskLine.addView(taskRow,new LinearLayout.LayoutParams(0,dp(42),1));
            Button delete=button("🗑",()->new AlertDialog.Builder(this).setTitle("Delete task").setMessage("Delete “"+t.name+"” from your "+activePeriod.toLowerCase(Locale.ROOT)+" routine?").setNegativeButton("Cancel",null).setPositiveButton("Delete",(d,w)->{String removedId=tasks.get(ix).id;tasks.remove(ix);prefs.edit().remove("done_"+removedId).apply();saveTasks();showManageRoutines();}).show());
            delete.setTextSize(17);delete.setContentDescription("Delete "+t.name);delete.setPadding(0,0,0,0);delete.setMinWidth(0);delete.setMinimumWidth(0);delete.setMinHeight(0);delete.setMinimumHeight(0);delete.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.rgb(255,241,245)));delete.setTextColor(Color.rgb(190,55,85));
            LinearLayout.LayoutParams deleteParams=new LinearLayout.LayoutParams(dp(42),dp(42));deleteParams.leftMargin=dp(6);taskLine.addView(delete,deleteParams);
            LinearLayout.LayoutParams taskLineParams=new LinearLayout.LayoutParams(-1,-2);taskLineParams.setMargins(0,0,0,0);root.addView(taskLine,taskLineParams);
        }
        primary("+ Add task",this::showTaskLibrary);
        addButton("Restart routine progress",()->startNewRoutine());
        Button credits=button("♥  Credits",this::showCredits);credits.setTextSize(16);credits.setTextColor(Color.rgb(190,55,85));credits.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.rgb(255,241,245)));
        LinearLayout.LayoutParams creditParams=new LinearLayout.LayoutParams(-1,dp(48));creditParams.setMargins(0,dp(12),0,dp(4));root.addView(credits,creditParams);
        addButton("‹ Back to home",this::showHome);
        TextView buildLabel=text("Build  ·  "+BuildConfig.VERSION_NAME+"  ·  "+shortCommit(BuildConfig.GIT_COMMIT),12,MUTED,false);
        buildLabel.setPadding(0,dp(12),0,dp(4));
        buildLabel.setPaintFlags(buildLabel.getPaintFlags()|android.graphics.Paint.UNDERLINE_TEXT_FLAG);
        buildLabel.setContentDescription("Show build and commit information");
        buildLabel.setOnClickListener(v->showBuildInformation());
        root.addView(buildLabel,new LinearLayout.LayoutParams(-1,dp(42)));
    }
    private String shortCommit(String hash){
        if(hash==null||hash.isEmpty()||hash.equals("unknown"))return "unknown";
        return hash.length()>7?hash.substring(0,7):hash;
    }
    private void showBuildInformation(){
        String hash=BuildConfig.GIT_COMMIT==null?"unknown":BuildConfig.GIT_COMMIT;
        String date=BuildConfig.BUILD_DATE==null?"unknown":BuildConfig.BUILD_DATE;
        String dirty=!BuildConfig.GIT_METADATA_KNOWN?"Unknown — Git metadata was unavailable during the build.":(BuildConfig.GIT_DIRTY?"Yes — this build included uncommitted local changes.":"No — the working tree was clean when built.");
        String message="Version: "+BuildConfig.VERSION_NAME+
            "\nBuild date: "+date+
            "\nFull commit: "+hash+
            "\nWorking tree: "+dirty;
        AlertDialog dialog=new AlertDialog.Builder(this)
            .setTitle("Build information")
            .setMessage(message)
            .setNegativeButton("Close",null)
            .setNeutralButton("Copy commit",null)
            .setPositiveButton("View on GitHub",null)
            .create();
        dialog.setOnShowListener(ignored->{
            dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(v->{
                android.content.ClipboardManager clipboard=(android.content.ClipboardManager)getSystemService(CLIPBOARD_SERVICE);
                if(clipboard!=null){clipboard.setPrimaryClip(android.content.ClipData.newPlainText("Git commit",hash));Toast.makeText(this,"Commit hash copied",Toast.LENGTH_SHORT).show();}
            });
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{
                if(hash.equals("unknown")){Toast.makeText(this,"Commit information is unavailable for this build.",Toast.LENGTH_SHORT).show();return;}
                try{startActivity(new android.content.Intent(android.content.Intent.ACTION_VIEW,android.net.Uri.parse("https://github.com/dans34/GoodMorning-Lite/commit/"+hash)));}
                catch(Exception e){Toast.makeText(this,"Could not open GitHub.",Toast.LENGTH_SHORT).show();}
            });
        });
        dialog.show();
    }
    private int globalTaskIndexForId(String id){for(int i=0;i<tasks.size();i++)if(tasks.get(i).id.equals(id))return i;return -1;}
    private void showCredits(){
        startScreen();root.setGravity(Gravity.CENTER);
        TextView heart=text("♥",64,Color.rgb(220,65,100),true);
        LinearLayout.LayoutParams heartParams=new LinearLayout.LayoutParams(-1,dp(94));heartParams.bottomMargin=dp(18);root.addView(heart,heartParams);
        TextView message=text("My dearest Ronni,\nYou are my partner, my world & my future.\n\nThis is what happens when a nerd falls in love with a cute, kind and amazing farmer's daughter.\n\nWith oodles of love,\n\nYour Dan",21,INK,false);
        message.setGravity(Gravity.CENTER);message.setLineSpacing(dp(5),1.0f);root.addView(message,new LinearLayout.LayoutParams(-1,-2));
        root.addView(new View(this),new LinearLayout.LayoutParams(1,dp(28)));
        primary("♥  Back to Manage Routines",this::showManageRoutines);
    }
    private void returnToRoutinePage(){if(managePageOpen)showManageRoutines();else showHome();}
    private int firstIncomplete(){ArrayList<Task> list=routineTasks();for(int i=0;i<list.size();i++)if(!prefs.getBoolean(doneKey(list.get(i)),false))return i;return list.size();}
    private void taskActions(int index){if(index>=0&&index<tasks.size())editTask(index);}
    private void showAddTaskType(){showTaskLibrary();}

    private static class LibraryTask {
        final String category, emoji, name, description;
        final int minutes;
        LibraryTask(String c,String e,String n,int m,String d){category=c;emoji=e;name=n;minutes=m;description=d;}
    }
    private final LibraryTask[] libraryTasks = {
        new LibraryTask("Everyday","💧","Have a glass of water",1,"A small, easy start to the day."),
        new LibraryTask("Everyday","🛏️","Straighten your bed",3,"Make your space feel a little calmer."),
        new LibraryTask("Everyday","🗓️","Look over today's plans",5,"Pick out anything you don't want to forget."),
        new LibraryTask("Home","🧹","Clear one small surface",5,"Choose a table, shelf or worktop."),
        new LibraryTask("Home","🍽️","Wash a few dishes",10,"Tackle what's in the sink, one item at a time."),
        new LibraryTask("Home","🧺","Start a load of washing",5,"Gather clothes and get a wash started."),
        new LibraryTask("Home","♻️","Sort the recycling",5,"Collect recyclable items from one room."),
        new LibraryTask("Errands","🛒","Write a shopping list",5,"Note down the essentials before heading out."),
        new LibraryTask("Errands","📦","Prepare a parcel",10,"Pack and label something that needs sending."),
        new LibraryTask("Errands","💳","Check an important account",5,"Review a statement or upcoming payment."),
        new LibraryTask("People","📞","Check in with someone",10,"Send a message or make a quick call."),
        new LibraryTask("People","💌","Send a kind note",5,"Let someone know you're thinking of them."),
        new LibraryTask("People","📅","Arrange a catch-up",5,"Suggest a time to meet or have a chat."),
        new LibraryTask("Movement","🧘","Stretch for a few minutes",5,"Loosen up at a comfortable pace."),
        new LibraryTask("Movement","🚶","Take a short walk",15,"Get outside or walk around indoors."),
        new LibraryTask("Movement","🏃","Do a quick movement break",10,"Choose an activity that suits your energy."),
        new LibraryTask("Focus","📚","Set up your workspace",5,"Gather what you need and reduce distractions."),
        new LibraryTask("Focus","📝","Choose the next small step",5,"Write down one action you can begin now."),
        new LibraryTask("Focus","⏱️","Work on one thing",15,"Give one task your attention for a short block."),
        new LibraryTask("Unwind","🌿","Take a quiet breathing break",3,"Pause and take a few comfortable breaths."),
        new LibraryTask("Unwind","📓","Write a few thoughts down",5,"Capture what's on your mind without editing."),
        new LibraryTask("Unwind","🌙","Prepare for tomorrow",5,"Set out one thing that will help in the morning.")
    };
    private void showTaskLibrary(){
        startScreen();
        LinearLayout top=new LinearLayout(this);top.setOrientation(0);top.setGravity(Gravity.CENTER_VERTICAL);
        TextView title=text("Add a task",25,INK,true);title.setGravity(Gravity.START);
        top.addView(title,new LinearLayout.LayoutParams(0,-2,1));
        Button close=button("✕",this::showHome);close.setMinWidth(dp(44));top.addView(close);root.addView(top);
        TextView sub=text("Choose an idea for your "+activePeriod.toLowerCase(Locale.ROOT)+" routine.",15,MUTED,false);sub.setGravity(Gravity.START);
        LinearLayout.LayoutParams subp=new LinearLayout.LayoutParams(-1,-2);subp.topMargin=dp(6);subp.bottomMargin=dp(12);root.addView(sub,subp);
        Button templatesButton=button("📋  Browse routine templates  ›",this::showRoutineTemplates);
        templatesButton.setGravity(Gravity.CENTER_VERTICAL|Gravity.START);
        templatesButton.setPadding(dp(14),0,dp(10),0);
        templatesButton.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.rgb(255,248,210)));
        LinearLayout.LayoutParams templatesParams=new LinearLayout.LayoutParams(-1,dp(54));
        templatesParams.bottomMargin=dp(10);root.addView(templatesButton,templatesParams);
        HorizontalScrollView tabsScroll=new HorizontalScrollView(this);tabsScroll.setHorizontalScrollBarEnabled(false);
        tabsScroll.setFillViewport(false);
        LinearLayout tabs=new LinearLayout(this);tabs.setOrientation(0);
        for(String cat:new String[]{"Everyday","Home","Errands","People","Movement","Focus","Unwind"}){
            final String chosen=cat;Button tab=button((libraryCategory.equals(cat)?"●  ":"")+cat,()->{libraryCategory=chosen;showTaskLibrary();});
            tab.setTextSize(13);tab.setPadding(dp(12),0,dp(12),0);
            tab.setBackgroundTintList(android.content.res.ColorStateList.valueOf(libraryCategory.equals(cat)?INK:PALE));
            tab.setTextColor(libraryCategory.equals(cat)?Color.WHITE:INK);
            LinearLayout.LayoutParams tp=new LinearLayout.LayoutParams(-2,dp(42));tp.setMargins(0,0,dp(7),0);tabs.addView(tab,tp);
        }
        tabsScroll.addView(tabs);root.addView(tabsScroll);
        tabsScroll.post(()->{
            int selectedIndex=0;
            String[] categories={"Everyday","Home","Errands","People","Movement","Focus","Unwind"};
            for(int i=0;i<categories.length;i++)if(categories[i].equals(libraryCategory)){selectedIndex=i;break;}
            View selectedTab=tabs.getChildAt(selectedIndex);
            if(selectedTab!=null){
                int target=selectedTab.getLeft()-Math.max(0,(tabsScroll.getWidth()-selectedTab.getWidth())/2);
                tabsScroll.scrollTo(Math.max(0,target),0);
            }
        });
        ScrollView listScroll=new ScrollView(this);LinearLayout list=new LinearLayout(this);list.setOrientation(1);
        for(LibraryTask item:libraryTasks)if(item.category.equals(libraryCategory)){
            LinearLayout row=new LinearLayout(this);row.setOrientation(0);row.setGravity(Gravity.CENTER_VERTICAL);row.setPadding(0,dp(10),0,dp(10));
            TextView emoji=text(item.emoji,23,INK,false);row.addView(emoji,new LinearLayout.LayoutParams(dp(42),-2));
            LinearLayout details=new LinearLayout(this);details.setOrientation(1);
            TextView name=text(item.name,16,INK,true);name.setGravity(Gravity.START);
            TextView meta=text(item.minutes+" min",13,MUTED,true);meta.setGravity(Gravity.START);
            TextView desc=text(item.description,13,MUTED,false);desc.setGravity(Gravity.START);
            details.addView(name);LinearLayout.LayoutParams metap=new LinearLayout.LayoutParams(-1,-2);metap.topMargin=dp(2);details.addView(meta,metap);
            LinearLayout.LayoutParams descp=new LinearLayout.LayoutParams(-1,-2);descp.topMargin=dp(3);details.addView(desc,descp);
            row.addView(details,new LinearLayout.LayoutParams(0,-2,1));
            Button plus=button("+",()->addLibraryTask(item));plus.setTextSize(23);plus.setMinWidth(0);plus.setMinimumWidth(0);
            plus.setBackgroundTintList(android.content.res.ColorStateList.valueOf(PALE));row.addView(plus,new LinearLayout.LayoutParams(dp(44),dp(44)));
            list.addView(row,new LinearLayout.LayoutParams(-1,-2));
            View line=new View(this);line.setBackgroundColor(Color.rgb(239,239,242));list.addView(line,new LinearLayout.LayoutParams(-1,dp(1)));
        }
        listScroll.addView(list);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,0,1);lp.topMargin=dp(8);root.addView(listScroll,lp);
        primary("Create a custom task",()->showCustomTaskForm(activePeriod));
    }
    private static class RoutineTemplate {
        final String emoji,name,description;
        final LibraryTask[] items;
        RoutineTemplate(String e,String n,String d,LibraryTask... tasks){emoji=e;name=n;description=d;items=tasks;}
        int totalMinutes(){int total=0;for(LibraryTask item:items)total+=item.minutes;return total;}
    }
    private final RoutineTemplate[] routineTemplates = {
        new RoutineTemplate("☀️","Gentle Morning","A low-pressure start to the day.",
            new LibraryTask("Everyday","💧","Have a drink of water",1,"Rehydrate before getting caught up in the day."),
            new LibraryTask("Everyday","🪟","Let the daylight in",1,"Open the curtains and take a moment to wake up."),
            new LibraryTask("Everyday","🧼","Freshen up",3,"Wash your face and feel a little more refreshed."),
            new LibraryTask("Movement","🧘","Stretch gently",3,"Loosen up with a few comfortable stretches."),
            new LibraryTask("Focus","🎯","Choose today's priority",2,"Pick one thing you'd like to get done today.")),
        new RoutineTemplate("🎯","Ready to Focus","Get organised and make a start.",
            new LibraryTask("Home","🧹","Clear your workspace",3,"Move distractions aside and make room to work."),
            new LibraryTask("Everyday","🥤","Prepare a drink",2,"Get water, tea or coffee ready before you begin."),
            new LibraryTask("Focus","📝","Write down your next step",3,"Identify the first concrete action you need to take."),
            new LibraryTask("Focus","🔕","Silence distractions",2,"Mute unnecessary notifications and close spare tabs."),
            new LibraryTask("Focus","▶️","Start the first task",2,"Spend a couple of minutes making a start.")),
        new RoutineTemplate("🌙","Wind Down","Ease out of the day at a comfortable pace.",
            new LibraryTask("Unwind","📱","Put distractions away",1,"Set your phone aside or enable a quiet mode."),
            new LibraryTask("Home","🧹","Reset one small area",4,"Tidy a surface or put a few things back in place."),
            new LibraryTask("Everyday","🧼","Get comfortable",4,"Change into comfortable clothes or freshen up."),
            new LibraryTask("Unwind","📖","Enjoy a quiet moment",5,"Read, listen to something relaxing or sit quietly.")),
        new RoutineTemplate("🚪","Ready to Leave","Prepare for going out without the last-minute rush.",
            new LibraryTask("Everyday","👕","Get dressed",3,"Put on the clothes you've chosen for the day."),
            new LibraryTask("Errands","🎒","Check your bag",2,"Make sure your essentials are packed."),
            new LibraryTask("Errands","🔑","Gather essentials",1,"Check keys, phone, wallet and anything needed today."),
            new LibraryTask("Errands","🌦️","Check the weather",1,"Choose a coat, umbrella or other useful extras."),
            new LibraryTask("Everyday","🚶","Final check",3,"Make sure everything is ready before heading out."))
    };
    private void showRoutineTemplates(){
        startScreen();heading("Routine templates","Start with a complete example, then make it your own.");
        TextView note=text("Templates add editable copies of their tasks to your "+activePeriod.toLowerCase(Locale.ROOT)+" routine.",14,MUTED,false);
        note.setGravity(Gravity.START);
        LinearLayout.LayoutParams noteParams=new LinearLayout.LayoutParams(-1,-2);noteParams.bottomMargin=dp(10);root.addView(note,noteParams);
        for(RoutineTemplate template:routineTemplates){
            LinearLayout card=new LinearLayout(this);card.setOrientation(1);card.setPadding(dp(14),dp(12),dp(14),dp(12));
            android.graphics.drawable.GradientDrawable bg=new android.graphics.drawable.GradientDrawable();bg.setColor(PALE);bg.setCornerRadius(dp(14));card.setBackground(bg);
            TextView title=text(template.emoji+"  "+template.name,18,INK,true);title.setGravity(Gravity.START);card.addView(title);
            TextView desc=text(template.description,14,MUTED,false);desc.setGravity(Gravity.START);
            LinearLayout.LayoutParams descParams=new LinearLayout.LayoutParams(-1,-2);descParams.topMargin=dp(4);card.addView(desc,descParams);
            TextView meta=text(template.totalMinutes()+" min  ·  "+template.items.length+" tasks",13,INK,true);meta.setGravity(Gravity.START);
            LinearLayout.LayoutParams metaParams=new LinearLayout.LayoutParams(-1,-2);metaParams.topMargin=dp(6);card.addView(meta,metaParams);
            Button preview=button("Preview tasks  ›",()->previewRoutineTemplate(template));preview.setGravity(Gravity.CENTER_VERTICAL|Gravity.START);
            LinearLayout.LayoutParams previewParams=new LinearLayout.LayoutParams(-1,dp(42));previewParams.topMargin=dp(8);card.addView(preview,previewParams);
            LinearLayout.LayoutParams cardParams=new LinearLayout.LayoutParams(-1,-2);cardParams.setMargins(0,0,0,dp(10));root.addView(card,cardParams);
        }
        addButton("‹ Back to task library",this::showTaskLibrary);
    }
    private void previewRoutineTemplate(RoutineTemplate template){
        StringBuilder details=new StringBuilder(template.description).append("\n\n");
        for(LibraryTask item:template.items)details.append(item.emoji).append("  ").append(item.name).append(" — ").append(item.minutes).append(" min\n").append("    ").append(item.description).append("\n");
        details.append("\nTotal: ").append(template.totalMinutes()).append(" minutes");
        new AlertDialog.Builder(this).setTitle(template.emoji+"  "+template.name).setMessage(details.toString())
            .setNegativeButton("Back",null)
            .setPositiveButton("Add to "+activePeriod,(dialog,which)->addRoutineTemplate(template))
            .show();
    }
    private void addRoutineTemplate(RoutineTemplate template){
        for(LibraryTask item:template.items){
            Task added=new Task(item.name,item.minutes*60,item.description,activePeriod);
            added.emoji=item.emoji;tasks.add(added);
        }
        saveTasks();
        Toast.makeText(this,template.name+" added to "+activePeriod+" routine",Toast.LENGTH_SHORT).show();
        showManageRoutines();
    }
    private void addLibraryTask(LibraryTask item){
        Task added=new Task(item.name,item.minutes*60,item.description,activePeriod);added.emoji=item.emoji;tasks.add(added);saveTasks();
        Toast.makeText(this,"Added to "+activePeriod+" routine",Toast.LENGTH_SHORT).show();
        showTaskLibrary();
    }
    private void showCustomTaskForm(String period){
        activePeriod=period;
        routineType=period+" routine";
        prefs.edit().putString("active_period",activePeriod).putString("routine_type",routineType).apply();
        editTask(-1);
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
        new AlertDialog.Builder(this).setTitle(period+" task").setMessage("Add a task to your "+period.toLowerCase(Locale.ROOT)+" routine.").setView(form).setNegativeButton("Cancel",(d,w)->returnToRoutinePage()).setPositiveButton("Create task",(d,w)->{
            String n=name.getText().toString().trim();
            if(n.isEmpty()){Toast.makeText(this,"Enter a task name.",Toast.LENGTH_SHORT).show();return;}
            int mins;
            try{mins=Integer.parseInt(minutes.getText().toString());}catch(Exception e){mins=2;}
            mins=Math.max(1,Math.min(240,mins));
            tasks.add(new Task(n,mins*60,notes.getText().toString().trim(),period));
            saveTasks();
            returnToRoutinePage();
        }).show();
    }
    private void editTask(int index){
        boolean add=index<0;
        Task old=add?new Task("",DEFAULT_SECONDS,"",activePeriod):tasks.get(index);
        LinearLayout form=new LinearLayout(this);form.setPadding(dp(20),dp(8),dp(20),0);form.setOrientation(1);
        TextView nameLabel=text("TASK NAME",12,MUTED,true);nameLabel.setGravity(Gravity.START);
        LinearLayout.LayoutParams nameLabelParams=new LinearLayout.LayoutParams(-1,-2);nameLabelParams.topMargin=dp(6);form.addView(nameLabel,nameLabelParams);
        EditText name=new EditText(this);name.setHint("What do you want to do?");name.setSingleLine(true);name.setText(old.name);form.addView(name,new LinearLayout.LayoutParams(-1,dp(52)));
        TextView durationLabel=text("DURATION (MINUTES)",12,MUTED,true);durationLabel.setGravity(Gravity.START);
        LinearLayout.LayoutParams durationLabelParams=new LinearLayout.LayoutParams(-1,-2);durationLabelParams.topMargin=dp(10);form.addView(durationLabel,durationLabelParams);
        android.widget.NumberPicker minutes=new android.widget.NumberPicker(this);
        minutes.setMinValue(1);minutes.setMaxValue(240);minutes.setValue(Math.max(1,Math.min(240,(old.seconds+59)/60)));
        minutes.setWrapSelectorWheel(false);
        LinearLayout durationPickerRow=new LinearLayout(this);durationPickerRow.setOrientation(0);durationPickerRow.setGravity(Gravity.CENTER_VERTICAL);
        durationPickerRow.addView(minutes,new LinearLayout.LayoutParams(dp(100),dp(112)));
        TextView minutesUnit=text("minutes",15,MUTED,false);minutesUnit.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams minutesUnitParams=new LinearLayout.LayoutParams(-2,-1);minutesUnitParams.leftMargin=dp(8);durationPickerRow.addView(minutesUnit,minutesUnitParams);
        form.addView(durationPickerRow);
        final String[] chosenPeriod={old.period==null||old.period.isEmpty()?activePeriod:old.period};
        TextView routineLabel=text("ROUTINE",12,MUTED,true);routineLabel.setGravity(Gravity.START);
        LinearLayout.LayoutParams routineLabelParams=new LinearLayout.LayoutParams(-1,-2);routineLabelParams.topMargin=dp(10);form.addView(routineLabel,routineLabelParams);
        final Button[] routineButtonLabel={null};
        routineButtonLabel[0]=button(periodEmoji(chosenPeriod[0])+"  "+chosenPeriod[0],()->{
            String[] periods={"Morning","Evening","Night"};
            new AlertDialog.Builder(this).setTitle("Move task to routine").setItems(periods,(dialog,which)->{
                chosenPeriod[0]=periods[which];routineButtonLabel[0].setText(periodEmoji(chosenPeriod[0])+"  "+chosenPeriod[0]);
            }).show();
        });
        form.addView(routineButtonLabel[0]);
        TextView notesLabel=text("NOTES (OPTIONAL)",12,MUTED,true);notesLabel.setGravity(Gravity.START);
        LinearLayout.LayoutParams notesLabelParams=new LinearLayout.LayoutParams(-1,-2);notesLabelParams.topMargin=dp(10);form.addView(notesLabel,notesLabelParams);
        EditText notes=new EditText(this);notes.setHint("Add a helpful reminder");notes.setMinLines(2);notes.setGravity(Gravity.TOP|Gravity.START);notes.setText(old.notes);form.addView(notes,new LinearLayout.LayoutParams(-1,dp(82)));
        final String[] chosenEmoji={old.emoji==null||old.emoji.isEmpty()?emojiForTaskName(old.name):old.emoji};
        LinearLayout emojiSection=new LinearLayout(this);emojiSection.setOrientation(1);
        TextView emojiLabel=text("TASK ICON",12,MUTED,true);emojiLabel.setGravity(Gravity.START);
        LinearLayout.LayoutParams emojiLabelParams=new LinearLayout.LayoutParams(-1,-2);emojiLabelParams.topMargin=dp(12);emojiSection.addView(emojiLabel,emojiLabelParams);
        TextView emojiPreview=text(chosenEmoji[0],28,INK,false);
        android.graphics.drawable.GradientDrawable previewBg=new android.graphics.drawable.GradientDrawable();previewBg.setColor(PALE);previewBg.setCornerRadius(dp(12));emojiPreview.setBackground(previewBg);
        emojiPreview.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams previewParams=new LinearLayout.LayoutParams(dp(56),dp(48));previewParams.topMargin=dp(6);emojiSection.addView(emojiPreview,previewParams);
        String[] emojis={"✨","💧","🛏️","🌤️","🛒","📦","💳","🍽️","🧺","♻️","🧹","🗓️","📞","📝","🧘","🚶","🏃","📚","🌙","❤️","⭐","☕","🍎","🎯"};
        GridLayout emojiGrid=new GridLayout(this);emojiGrid.setColumnCount(6);emojiGrid.setUseDefaultMargins(false);
        for(String emoji:emojis){
            Button emojiChoice=button(emoji,()->{chosenEmoji[0]=emoji;emojiPreview.setText(emoji);});
            emojiChoice.setTextSize(22);emojiChoice.setPadding(0,0,0,0);emojiChoice.setMinWidth(0);emojiChoice.setMinimumWidth(0);emojiChoice.setMinHeight(0);emojiChoice.setMinimumHeight(0);
            emojiChoice.setBackgroundTintList(android.content.res.ColorStateList.valueOf(emoji.equals(chosenEmoji[0])?Color.rgb(255,248,190):PALE));
            GridLayout.LayoutParams ep=new GridLayout.LayoutParams();ep.width=0;ep.height=dp(44);ep.columnSpec=GridLayout.spec(GridLayout.UNDEFINED,1f);ep.setMargins(dp(2),dp(2),dp(2),dp(2));emojiGrid.addView(emojiChoice,ep);
        }
        LinearLayout.LayoutParams gridParams=new LinearLayout.LayoutParams(-1,-2);gridParams.topMargin=dp(6);emojiSection.addView(emojiGrid,gridParams);
        form.addView(emojiSection);
        new AlertDialog.Builder(this).setTitle(add?"Add task":"Edit task").setView(form).setNegativeButton("Cancel",null).setPositiveButton("Save",(d,w)->{
            String n=name.getText().toString().trim();
            if(n.isEmpty()){Toast.makeText(this,"Enter a task name.",Toast.LENGTH_SHORT).show();return;}
            int mins=Math.max(1,Math.min(240,minutes.getValue()));
            Task t=add?new Task(n,mins*60,notes.getText().toString().trim(),chosenPeriod[0]):new Task(old.id,n,mins*60,notes.getText().toString().trim(),chosenPeriod[0]);
            t.emoji=chosenEmoji[0];
            if(add)tasks.add(t);else tasks.set(index,t);
            saveTasks();returnToRoutinePage();
        }).show();
    }
    private void showTimer(){
        if(ticker!=null)handler.removeCallbacks(ticker);
        ArrayList<Task> list=routineTasks();if(list.isEmpty()){showHome();return;}
        if(!currentTaskId.isEmpty()){for(int i=0;i<list.size();i++)if(list.get(i).id.equals(currentTaskId)){currentTask=i;break;}}
        currentTask=Math.max(0,Math.min(currentTask,list.size()-1));currentTaskId=list.get(currentTask).id;
        startScreen();root.setPadding(dp(22),dp(10),dp(22),dp(16));
        int completed=0;for(Task task:list)if(prefs.getBoolean(doneKey(task),false))completed++;
        TextView taskTitle=text(list.get(currentTask).name,27,INK,true);
        taskTitle.setGravity(Gravity.CENTER);taskTitle.setMaxLines(2);
        LinearLayout.LayoutParams titleParams=new LinearLayout.LayoutParams(-1,-2);titleParams.setMargins(0,dp(8),0,dp(12));root.addView(taskTitle,titleParams);
        TextView progressLabel=text("TASK "+(currentTask+1)+" OF "+list.size()+"  ·  "+completed+" COMPLETED",12,MUTED,true);
        root.addView(progressLabel,new LinearLayout.LayoutParams(-1,dp(24)));
        android.widget.ProgressBar progress=new android.widget.ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);
        progress.setMax(list.size());progress.setProgress(completed);progress.setProgressTintList(android.content.res.ColorStateList.valueOf(YELLOW));progress.setProgressBackgroundTintList(android.content.res.ColorStateList.valueOf(PALE));
        LinearLayout.LayoutParams pp=new LinearLayout.LayoutParams(-1,dp(5));pp.setMargins(0,dp(4),0,dp(12));root.addView(progress,pp);

        LinearLayout timerFace=new LinearLayout(this);timerFace.setOrientation(1);timerFace.setGravity(Gravity.CENTER);
        android.graphics.drawable.GradientDrawable faceBackground=new android.graphics.drawable.GradientDrawable();
        faceBackground.setShape(android.graphics.drawable.GradientDrawable.OVAL);faceBackground.setColor(Color.rgb(249,249,250));timerFace.setBackground(faceBackground);
        LinearLayout.LayoutParams faceParams=new LinearLayout.LayoutParams(dp(300),dp(300));faceParams.gravity=Gravity.CENTER_HORIZONTAL;faceParams.setMargins(0,dp(4),0,dp(8));root.addView(timerFace,faceParams);
        TextView taskIcon=text(list.get(currentTask).emoji,44,INK,false);
        LinearLayout.LayoutParams iconParams=new LinearLayout.LayoutParams(-1,dp(76));iconParams.topMargin=dp(18);timerFace.addView(taskIcon,iconParams);
        countdownView=text(formatTime(remainingMs),54,INK,true);
        LinearLayout.LayoutParams countdownParams=new LinearLayout.LayoutParams(-1,dp(78));countdownParams.topMargin=dp(4);timerFace.addView(countdownView,countdownParams);
        TextView planned=text(duration(list.get(currentTask).seconds)+" planned",14,MUTED,false);
        timerFace.addView(planned,new LinearLayout.LayoutParams(-1,dp(30)));

        if(!list.get(currentTask).notes.isEmpty()){
            TextView note=text(list.get(currentTask).notes,15,MUTED,false);note.setMaxLines(3);note.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams noteParams=new LinearLayout.LayoutParams(-1,-2);noteParams.setMargins(dp(4),dp(4),dp(4),dp(14));root.addView(note,noteParams);
        }else{
            root.addView(new View(this),new LinearLayout.LayoutParams(1,dp(14)));
        }

        LinearLayout controls=new LinearLayout(this);controls.setOrientation(0);controls.setGravity(Gravity.CENTER);LinearLayout.LayoutParams controlsParams=new LinearLayout.LayoutParams(-1,dp(72));controlsParams.setMargins(0,dp(4),0,dp(8));root.addView(controls,controlsParams);
        int controlSize=dp(60);
        Button pause=button(timerRunning?"Ⅱ":"▶",()->toggleTimer());pause.setTextSize(24);pause.setTextColor(MUTED);pause.setBackgroundTintList(android.content.res.ColorStateList.valueOf(PALE));pause.setMinWidth(0);pause.setMinimumWidth(0);pause.setContentDescription(timerRunning?"Pause timer":"Resume timer");
        LinearLayout.LayoutParams pauseParams=new LinearLayout.LayoutParams(controlSize,controlSize);pauseParams.setMargins(dp(4),0,dp(4),0);controls.addView(pause,pauseParams);pauseButton=pause;
        Button complete=button("✓",()->{if(completionAnimationRunning)return;completionAnimationRunning=true;prefs.edit().putBoolean(doneKey(list.get(currentTask)),true).apply();completionAnimationRunning=false;if(singleTaskMode)showJustStartedChoice();else nextTask();});complete.setTextSize(30);complete.setTextColor(Color.WHITE);complete.setBackgroundTintList(android.content.res.ColorStateList.valueOf(INK));complete.setMinWidth(0);complete.setMinimumWidth(0);complete.setContentDescription("Complete task");
        LinearLayout.LayoutParams completeParams=new LinearLayout.LayoutParams(controlSize,controlSize);completeParams.setMargins(dp(4),0,dp(4),0);controls.addView(complete,completeParams);
        Button skip=button("⏭",()->{Toast.makeText(this,"Task skipped",Toast.LENGTH_SHORT).show();nextTask();});skip.setTextSize(23);skip.setTextColor(MUTED);skip.setBackgroundTintList(android.content.res.ColorStateList.valueOf(PALE));skip.setMinWidth(0);skip.setMinimumWidth(0);skip.setContentDescription("Skip task");
        LinearLayout.LayoutParams skipParams=new LinearLayout.LayoutParams(controlSize,controlSize);skipParams.setMargins(dp(4),0,dp(4),0);controls.addView(skip,skipParams);
        Button end=button("■",()->new AlertDialog.Builder(this).setTitle("End routine?").setMessage("Are you sure you want to end this routine? Your completed tasks will be kept.").setNegativeButton("Keep going",null).setPositiveButton("End routine",(dialog,which)->{stopTimer();singleTaskMode=false;prefs.edit().putBoolean("single_task_mode",false).putBoolean("timer_screen_active",false).apply();showHome();}).show());end.setTextSize(20);end.setTextColor(Color.rgb(190,55,85));end.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.rgb(255,241,245)));end.setMinWidth(0);end.setMinimumWidth(0);end.setContentDescription("End routine");
        LinearLayout.LayoutParams endParams=new LinearLayout.LayoutParams(controlSize,controlSize);endParams.setMargins(dp(4),0,dp(4),0);controls.addView(end,endParams);
        if(timerRunning)runTicker();
    }

    private void startRoutine(boolean justGetStarted){
        ArrayList<Task> list=routineTasks();
        if(list.isEmpty()){Toast.makeText(this,"Add a task to this routine first.",Toast.LENGTH_SHORT).show();return;}
        currentTask=firstIncomplete();
        if(currentTask>=list.size()){Toast.makeText(this,"All tasks are complete.",Toast.LENGTH_SHORT).show();return;}
        singleTaskMode=justGetStarted;
        currentTaskId=list.get(currentTask).id;
        remainingMs=list.get(currentTask).seconds*1000L;
        timerRunning=true;
        deadlineMs=System.currentTimeMillis()+remainingMs;
        long started=System.currentTimeMillis();
        prefs.edit().putBoolean("single_task_mode",singleTaskMode).putBoolean("timer_screen_active",true).putLong("routine_start_time",started).putString("routine_session_saved_id","").apply();
        persistTimer();
        showTimer();
    }

    private void showJustStartedChoice(){
        stopTimer();
        persistTimer();
        new AlertDialog.Builder(this)
            .setTitle("Nice work!")
            .setMessage("You've completed one task. Would you like to continue your routine, or finish for now?")
            .setNegativeButton("Finish for now",(dialog,which)->{
                singleTaskMode=false;
                routineFinished=true;
                prefs.edit().putBoolean("single_task_mode",false).putBoolean("routine_finished",true).putBoolean("timer_screen_active",false).putLong("last_completion_time",System.currentTimeMillis()).apply();
                showCompletion();
            })
            .setPositiveButton("Continue routine",(dialog,which)->continueRoutineAfterFirstTask())
            .show();
    }

    private void continueRoutineAfterFirstTask(){
        singleTaskMode=false;
        prefs.edit().putBoolean("single_task_mode",false).apply();
        ArrayList<Task> list=routineTasks();
        int next=currentTask+1;
        while(next<list.size() && prefs.getBoolean(doneKey(list.get(next)),false))next++;
        if(next>=list.size()){
            routineFinished=true;
            prefs.edit().putBoolean("routine_finished",true).putBoolean("timer_screen_active",false).putLong("last_completion_time",System.currentTimeMillis()).apply();
            showCompletion();
            return;
        }
        currentTask=next;
        currentTaskId=list.get(currentTask).id;
        remainingMs=list.get(currentTask).seconds*1000L;
        timerRunning=true;
        deadlineMs=System.currentTimeMillis()+remainingMs;
        persistTimer();
        showTimer();
    }

    private void toggleTimer(){ArrayList<Task> list=routineTasks();if(list.isEmpty())return;if(timerRunning){remainingMs=Math.max(0,deadlineMs-System.currentTimeMillis());timerRunning=false;}else{if(remainingMs<=0)remainingMs=list.get(currentTask).seconds*1000L;deadlineMs=System.currentTimeMillis()+remainingMs;timerRunning=true;}persistTimer();showTimer();}
    private void runTicker(){ticker=new Runnable(){public void run(){if(!timerRunning||countdownView==null)return;remainingMs=Math.max(0,deadlineMs-System.currentTimeMillis());countdownView.setText(formatTime(remainingMs));if(remainingMs==0){timerRunning=false;persistTimer();Toast.makeText(MainActivity.this,"Time is up — complete the task or continue.",Toast.LENGTH_LONG).show();if(pauseButton!=null)pauseButton.setText("Resume");}else handler.postDelayed(this,250);}};handler.post(ticker);}
    private void nextTask(){stopTimer();ArrayList<Task> list=routineTasks();if(currentTask+1>=list.size()){routineFinished=true;prefs.edit().putBoolean("routine_finished",true).putBoolean("timer_screen_active",false).putLong("last_completion_time",System.currentTimeMillis()).apply();showCompletion();return;}currentTask++;currentTaskId=list.get(currentTask).id;remainingMs=list.get(currentTask).seconds*1000L;timerRunning=true;deadlineMs=System.currentTimeMillis()+remainingMs;persistTimer();showTimer();}
    private void showCompletion(){
        stopTimer();startScreen();ArrayList<Task> list=routineTasks();
        int done=0,totalSeconds=0;for(Task task:list){if(prefs.getBoolean(doneKey(task),false)){done++;totalSeconds+=task.seconds;}}
        boolean allCompleted=!list.isEmpty()&&done==list.size();
        heading(allCompleted?"🎉 Routine complete!":"Routine finished",allCompleted?"Every task completed — brilliant work!":"You made time for your "+activePeriod.toLowerCase(Locale.ROOT)+" routine.");
        long started=prefs.getLong("routine_start_time",System.currentTimeMillis());long elapsed=Math.max(0,(System.currentTimeMillis()-started)/1000L);
        saveRoutineSession(activePeriod,done,list.size(),elapsed);
        root.addView(text(done+" of "+list.size()+" tasks completed",21,INK,true));
        TextView stats=text("Planned time completed: "+duration(totalSeconds)+"\nTime in this session: "+duration((int)elapsed),15,MUTED,false);
        stats.setGravity(Gravity.CENTER);LinearLayout.LayoutParams statp=new LinearLayout.LayoutParams(-1,-2);statp.setMargins(0,dp(16),0,dp(22));root.addView(stats,statp);
        android.widget.ProgressBar progress=new android.widget.ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);progress.setMax(Math.max(1,list.size()));progress.setProgress(done);progress.setProgressTintList(android.content.res.ColorStateList.valueOf(YELLOW));root.addView(progress,new LinearLayout.LayoutParams(-1,dp(8)));
        primary("Back to Today",this::showHome);addButton("Run again",this::startNewRoutine);
    }
    private String todayKey(long millis){return new java.text.SimpleDateFormat("dd/MM/yyyy",Locale.UK).format(new java.util.Date(millis));}
    private String displayHistoryDate(String storedDate){
        if(storedDate==null||storedDate.isEmpty())return "";
        if(storedDate.matches("\\d{2}/\\d{2}/\\d{4}"))return storedDate;
        try{
            java.text.SimpleDateFormat oldFormat=new java.text.SimpleDateFormat("yyyy-MM-dd",Locale.US);
            oldFormat.setLenient(false);
            java.util.Date parsed=oldFormat.parse(storedDate);
            if(parsed!=null)return new java.text.SimpleDateFormat("dd/MM/yyyy",Locale.UK).format(parsed);
        }catch(Exception ignored){}
        return storedDate;
    }
    private JSONArray historyArray(){try{return new JSONArray(prefs.getString("routine_history","[]"));}catch(Exception e){return new JSONArray();}}
    private void saveRoutineSession(String period,int done,int total,long elapsed){
        String sessionId=prefs.getString("routine_session_saved_id","");
        String currentId=period+"_"+prefs.getLong("routine_start_time",0L);
        if(currentId.equals(sessionId))return;
        JSONObject item=new JSONObject();try{item.put("date",todayKey(System.currentTimeMillis()));item.put("period",period);item.put("done",done);item.put("total",total);item.put("elapsed",elapsed);item.put("full",total>0&&done==total);item.put("timestamp",System.currentTimeMillis());}catch(Exception ignored){}
        JSONArray history=historyArray();history.put(item);
        while(history.length()>500){JSONArray trimmed=new JSONArray();for(int i=history.length()-500;i<history.length();i++)trimmed.put(history.opt(i));history=trimmed;}
        prefs.edit().putString("routine_history",history.toString()).putString("routine_session_saved_id",currentId).apply();
    }
    private void showHistory(){
        startScreen();
        heading("History & streaks","See your progress, one routine at a time.");

        JSONArray history=historyArray();
        java.util.HashSet<String> activeDates=new java.util.HashSet<>();
        java.util.HashSet<String> progressDates=new java.util.HashSet<>();
        int sessions=0,fullSessions=0;
        long now=System.currentTimeMillis();
        java.util.Calendar consistencyStart=java.util.Calendar.getInstance();
        consistencyStart.setTimeInMillis(now);
        consistencyStart.set(java.util.Calendar.HOUR_OF_DAY,0);
        consistencyStart.set(java.util.Calendar.MINUTE,0);
        consistencyStart.set(java.util.Calendar.SECOND,0);
        consistencyStart.set(java.util.Calendar.MILLISECOND,0);
        consistencyStart.add(java.util.Calendar.DAY_OF_YEAR,-6);
        long consistencyStartMs=consistencyStart.getTimeInMillis();
        java.util.Calendar weekStart=java.util.Calendar.getInstance();
        weekStart.setTimeInMillis(now);
        weekStart.set(java.util.Calendar.HOUR_OF_DAY,0);
        weekStart.set(java.util.Calendar.MINUTE,0);
        weekStart.set(java.util.Calendar.SECOND,0);
        weekStart.set(java.util.Calendar.MILLISECOND,0);
        weekStart.set(java.util.Calendar.DAY_OF_WEEK,weekStart.getFirstDayOfWeek());
        long weekStartMs=weekStart.getTimeInMillis();
        int weekSessions=0,weekFull=0,weekTasksDone=0,weekTasksTotal=0;
        java.util.HashSet<String> weekActiveDates=new java.util.HashSet<>();

        for(int i=0;i<history.length();i++){
            JSONObject o=history.optJSONObject(i);
            if(o==null)continue;
            sessions++;
            boolean full=o.optBoolean("full",false);
            String date=displayHistoryDate(o.optString("date",""));
            if(full){fullSessions++;activeDates.add(date);}
            long timestamp=o.optLong("timestamp",0L);
            if(timestamp>=consistencyStartMs && timestamp<=now && o.optInt("done",0)>0){
                progressDates.add(date);
            }
            if(timestamp>=weekStartMs && timestamp<=now){
                weekSessions++;
                if(full){weekFull++;weekActiveDates.add(date);}
                weekTasksDone+=Math.max(0,o.optInt("done",0));
                weekTasksTotal+=Math.max(0,o.optInt("total",0));
            }
        }

        int streak=0;
        java.util.Calendar day=java.util.Calendar.getInstance();
        day.setTimeInMillis(now);
        if(!activeDates.contains(todayKey(now)))day.add(java.util.Calendar.DAY_OF_YEAR,-1);
        while(activeDates.contains(todayKey(day.getTimeInMillis()))&&streak<3660){
            streak++;
            day.add(java.util.Calendar.DAY_OF_YEAR,-1);
        }

        LinearLayout streakCard=new LinearLayout(this);
        streakCard.setOrientation(1);
        streakCard.setGravity(Gravity.CENTER);
        streakCard.setPadding(dp(16),dp(14),dp(16),dp(14));
        android.graphics.drawable.GradientDrawable streakBg=new android.graphics.drawable.GradientDrawable();
        streakBg.setColor(Color.rgb(255,248,190));
        streakBg.setCornerRadius(dp(16));
        streakCard.setBackground(streakBg);
        streakCard.addView(text("🔥  "+streak,36,INK,true));
        TextView streakCaption=text("DAY STREAK",12,INK,true);
        LinearLayout.LayoutParams streakCaptionParams=new LinearLayout.LayoutParams(-1,-2);
        streakCaptionParams.topMargin=dp(2);
        streakCard.addView(streakCaption,streakCaptionParams);
        TextView streakInfo=text(" "+fullSessions+" fully completed routines overall",13,INK,false);
        LinearLayout.LayoutParams streakInfoParams=new LinearLayout.LayoutParams(-1,-2);
        streakInfoParams.topMargin=dp(6);
        streakCard.addView(streakInfo,streakInfoParams);
        TextView consistencyInfo=text("Progress on "+progressDates.size()+" of the last 7 days",16,INK,true);
        LinearLayout.LayoutParams consistencyParams=new LinearLayout.LayoutParams(-1,-2);
        consistencyParams.topMargin=dp(12);
        streakCard.addView(consistencyInfo,consistencyParams);
        TextView encouragement=text("Every fresh start counts.",13,INK,false);
        LinearLayout.LayoutParams encouragementParams=new LinearLayout.LayoutParams(-1,-2);
        encouragementParams.topMargin=dp(3);
        streakCard.addView(encouragement,encouragementParams);
        LinearLayout.LayoutParams streakCardParams=new LinearLayout.LayoutParams(-1,-2);
        streakCardParams.bottomMargin=dp(14);
        root.addView(streakCard,streakCardParams);

        TextView weekTitle=text("THIS WEEK",13,MUTED,true);
        weekTitle.setGravity(Gravity.START);
        root.addView(weekTitle);
        int completionRate=weekTasksTotal==0?0:Math.round(100f*weekTasksDone/weekTasksTotal);

        LinearLayout weekCard=new LinearLayout(this);
        weekCard.setOrientation(1);
        weekCard.setPadding(dp(14),dp(12),dp(14),dp(12));
        android.graphics.drawable.GradientDrawable weekBg=new android.graphics.drawable.GradientDrawable();
        weekBg.setColor(Color.rgb(249,249,250));
        weekBg.setCornerRadius(dp(14));
        weekCard.setBackground(weekBg);

        LinearLayout metrics=new LinearLayout(this);
        metrics.setOrientation(0);
        String[][] metricData={{String.valueOf(weekFull),"Completed"},{String.valueOf(weekSessions),"Sessions"},{completionRate+"%","Task rate"}};
        for(String[] metric:metricData){
            LinearLayout metricBox=new LinearLayout(this);
            metricBox.setOrientation(1);
            metricBox.setGravity(Gravity.CENTER);
            metricBox.addView(text(metric[0],22,INK,true));
            TextView metricLabel=text(metric[1],11,MUTED,false);
            LinearLayout.LayoutParams metricLabelParams=new LinearLayout.LayoutParams(-1,-2);
            metricLabelParams.topMargin=dp(2);
            metricBox.addView(metricLabel,metricLabelParams);
            metrics.addView(metricBox,new LinearLayout.LayoutParams(0,-2,1));
        }
        weekCard.addView(metrics);
        View metricDivider=new View(this);
        metricDivider.setBackgroundColor(Color.rgb(230,230,234));
        LinearLayout.LayoutParams metricDividerParams=new LinearLayout.LayoutParams(-1,dp(1));
        metricDividerParams.topMargin=dp(12);
        metricDividerParams.bottomMargin=dp(10);
        weekCard.addView(metricDivider,metricDividerParams);

        java.util.Calendar cal=(java.util.Calendar)weekStart.clone();
        LinearLayout daysRow=new LinearLayout(this);
        daysRow.setOrientation(0);
        for(int d=0;d<7;d++){
            String date=todayKey(cal.getTimeInMillis());
            boolean completed=weekActiveDates.contains(date);
            boolean isToday=date.equals(todayKey(now));
            LinearLayout dayBox=new LinearLayout(this);
            dayBox.setOrientation(1);
            dayBox.setGravity(Gravity.CENTER);
            TextView dayMark=text(completed?"✓":(isToday?"•":"—"),18,completed?Color.rgb(55,130,85):MUTED,true);
            dayBox.addView(dayMark,new LinearLayout.LayoutParams(-1,dp(28)));
            TextView dayName=text(new java.text.SimpleDateFormat("EEE",Locale.US).format(cal.getTime()),11,INK,isToday);
            dayBox.addView(dayName,new LinearLayout.LayoutParams(-1,dp(22)));
            daysRow.addView(dayBox,new LinearLayout.LayoutParams(0,-2,1));
            cal.add(java.util.Calendar.DAY_OF_YEAR,1);
        }
        weekCard.addView(daysRow);
        LinearLayout.LayoutParams weekCardParams=new LinearLayout.LayoutParams(-1,-2);
        weekCardParams.topMargin=dp(8);
        weekCardParams.bottomMargin=dp(18);
        root.addView(weekCard,weekCardParams);

        TextView recent=text("RECENT SESSIONS",13,MUTED,true);
        recent.setGravity(Gravity.START);
        root.addView(recent);
        int shown=0;
        for(int i=history.length()-1;i>=0&&shown<20;i--){
            JSONObject o=history.optJSONObject(i);
            if(o==null)continue;
            boolean full=o.optBoolean("full",false);
            String period=o.optString("period","Routine");
            String date=displayHistoryDate(o.optString("date",""));
            String label=period+" routine  ·  "+date+"\n"+
                o.optInt("done",0)+" of "+o.optInt("total",0)+" tasks  ·  "+
                duration((int)o.optLong("elapsed",0))+" elapsed";
            LinearLayout sessionCard=new LinearLayout(this);
            sessionCard.setOrientation(0);
            sessionCard.setGravity(Gravity.CENTER_VERTICAL);
            sessionCard.setPadding(dp(12),dp(10),dp(12),dp(10));
            android.graphics.drawable.GradientDrawable sessionBg=new android.graphics.drawable.GradientDrawable();
            sessionBg.setColor(Color.rgb(249,249,250));
            sessionBg.setCornerRadius(dp(12));
            sessionCard.setBackground(sessionBg);
            TextView sessionDetails=text(label,13,INK,false);
            sessionDetails.setGravity(Gravity.START|Gravity.CENTER_VERTICAL);
            sessionCard.addView(sessionDetails,new LinearLayout.LayoutParams(0,-2,1));
            TextView sessionStatus=text(full?"✓":"◷",21,full?Color.rgb(55,130,85):MUTED,true);
            sessionCard.addView(sessionStatus,new LinearLayout.LayoutParams(dp(30),dp(34)));
            LinearLayout.LayoutParams sessionParams=new LinearLayout.LayoutParams(-1,-2);
            sessionParams.topMargin=dp(6);
            root.addView(sessionCard,sessionParams);
            shown++;
        }
        if(shown==0){
            TextView empty=text("Your routine sessions will appear here once you finish a routine.",14,MUTED,false);
            empty.setGravity(Gravity.START);
            LinearLayout.LayoutParams emptyParams=new LinearLayout.LayoutParams(-1,-2);
            emptyParams.topMargin=dp(10);
            root.addView(empty,emptyParams);
        }
        addButton("‹ Back to Today",this::showHome);
    }
    private void startNewRoutine(){stopTimer();ArrayList<Task> list=routineTasks();SharedPreferences.Editor editor=prefs.edit();for(Task task:list)editor.remove(doneKey(task));currentTask=0;currentTaskId=list.isEmpty()?"":list.get(0).id;remainingMs=list.isEmpty()?DEFAULT_SECONDS*1000L:list.get(0).seconds*1000L;routineFinished=false;editor.putBoolean("routine_finished",false).putBoolean("timer_screen_active",false).putInt("timer_task_index",0).putString("timer_task_id",currentTaskId).putLong("timer_remaining_ms",remainingMs).apply();showHome();}
    private void stopTimer(){timerRunning=false;if(ticker!=null)handler.removeCallbacks(ticker);ticker=null;}
    private String formatTime(long ms){long s=Math.max(0,(ms+999)/1000);return String.format(Locale.getDefault(),"%02d:%02d",s/60,s%60);}
    private void persistTimer(){if(timerRunning)remainingMs=Math.max(0,deadlineMs-System.currentTimeMillis());prefs.edit().putInt("timer_task_index",currentTask).putString("timer_task_id",currentTaskId).putLong("timer_remaining_ms",remainingMs).putBoolean("timer_running",timerRunning).putBoolean("routine_finished",routineFinished).apply();}
}
