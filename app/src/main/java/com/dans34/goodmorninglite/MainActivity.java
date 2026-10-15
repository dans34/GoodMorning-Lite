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
        String id, name, notes, period; int seconds;
        Task(String n, int s, String note) { this(UUID.randomUUID().toString(),n,s,note,"Morning"); }
        Task(String n, int s, String note, String p) { this(UUID.randomUUID().toString(),n,s,note,p); }
        Task(String taskId,String n,int s,String note,String p) { id=taskId;name=n;seconds=s;notes=note;period=p; }
        JSONObject json() { JSONObject o=new JSONObject(); try { o.put("id",id);o.put("name",name); o.put("seconds",seconds); o.put("notes",notes); o.put("period",period); } catch(Exception ignored) {} return o; }
    }
    private final ArrayList<Task> tasks = new ArrayList<>();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private SharedPreferences prefs;
    private LinearLayout root;
    private String routineType="Morning routine", activePeriod="Morning", struggle="I'm not sure";
    private int wakeHour=6, wakeMinute=0, page=0, currentTask=0;
    private long remainingMs=DEFAULT_SECONDS*1000L, deadlineMs=0L;
    private boolean timerRunning=false, routineFinished=false;
    private String currentTaskId="";
    private String libraryCategory="Everyday";
    private Runnable ticker;
    private TextView countdownView;
    private Button pauseButton;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        prefs=getSharedPreferences("goodmorning_local",MODE_PRIVATE);
        routineType=prefs.getString("routine_type",routineType); activePeriod=prefs.getString("active_period",routineType.replace(" routine","")); struggle=prefs.getString("struggle",struggle);
        wakeHour=prefs.getInt("wake_hour",6); wakeMinute=prefs.getInt("wake_minute",0);
        loadTasks();
        currentTask=prefs.getInt("timer_task_index",0); currentTaskId=prefs.getString("timer_task_id",""); remainingMs=prefs.getLong("timer_remaining_ms",DEFAULT_SECONDS*1000L);
        routineFinished=prefs.getBoolean("routine_finished",false);
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
    private void startScreen(){root=column();root.setBackgroundColor(Color.WHITE);setContentView(root);}
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
            Task t=new Task(id,o.optString("name","Task"),Math.max(1,o.optInt("seconds",DEFAULT_SECONDS)),o.optString("notes",""),o.optString("period",routineType.replace(" routine","")));
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
        LinearLayout header=new LinearLayout(this);header.setOrientation(0);header.setGravity(Gravity.TOP);
        LinearLayout titleBlock=new LinearLayout(this);titleBlock.setOrientation(1);
        TextView greeting=text(greetingForCurrentTime(),25,INK,true);greeting.setGravity(Gravity.START);
        TextView subtitle=text("Your routine, one step at a time.",15,MUTED,false);subtitle.setGravity(Gravity.START);
        titleBlock.addView(greeting);LinearLayout.LayoutParams subParams=new LinearLayout.LayoutParams(-1,-2);subParams.topMargin=dp(7);titleBlock.addView(subtitle,subParams);
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
        Button history=new Button(this);history.setText("📅");history.setTextSize(20);history.setAllCaps(false);history.setTextColor(INK);history.setContentDescription("History and streaks");history.setPadding(0,0,0,dp(2));history.setMinWidth(0);history.setMinimumWidth(0);history.setMinHeight(0);history.setMinimumHeight(0);history.setBackgroundTintList(android.content.res.ColorStateList.valueOf(PALE));
        LinearLayout.LayoutParams historyParams=new LinearLayout.LayoutParams(dp(38),dp(38));historyParams.leftMargin=dp(4);actions.addView(history,historyParams);history.setOnClickListener(v->showHistory());
        LinearLayout.LayoutParams actionBlockParams=new LinearLayout.LayoutParams(-2,dp(42));actionBlockParams.topMargin=dp(1);header.addView(actions,actionBlockParams);
        root.addView(header);
        LinearLayout.LayoutParams headerGap=new LinearLayout.LayoutParams(-1,-2);headerGap.bottomMargin=dp(12);header.setLayoutParams(headerGap);
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
            Button taskRow=button((completed?"✓  ":"○  ")+(i+1)+".  "+t.name+"  ·  "+duration(t.seconds),()->Toast.makeText(this,"Use Manage Routines to edit tasks.",Toast.LENGTH_SHORT).show());
            taskRow.setTextSize(14);taskRow.setPadding(dp(8),0,dp(8),0);
            LinearLayout.LayoutParams taskParams=new LinearLayout.LayoutParams(-1,dp(42));taskParams.setMargins(0,dp(2),0,dp(2));root.addView(taskRow,taskParams);
        }
        root.addView(new View(this),new LinearLayout.LayoutParams(1,dp(12)));
        primary("Start routine",()->{
            currentTask=firstIncomplete();ArrayList<Task> list=routineTasks();
            if(list.isEmpty()){Toast.makeText(this,"Add a task to this routine first.",Toast.LENGTH_SHORT).show();return;}
            if(currentTask>=list.size()){Toast.makeText(this,"All tasks are complete.",Toast.LENGTH_SHORT).show();return;}
            currentTaskId=list.get(currentTask).id;remainingMs=list.get(currentTask).seconds*1000L;timerRunning=true;deadlineMs=System.currentTimeMillis()+remainingMs;
            prefs.edit().putBoolean("timer_screen_active",true).putLong("routine_start_time",System.currentTimeMillis()).apply();persistTimer();showTimer();
        });
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
        if(visible.isEmpty())addButton("No tasks yet — browse the library",this::showTaskLibrary);
        for(int i=0;i<visible.size();i++){final int ix=globalTaskIndex(i);Task t=visible.get(i);Button taskRow=button("✏️  "+t.name+"  ·  "+duration(t.seconds)+"  ›",()->taskActions(ix));taskRow.setTextSize(14);taskRow.setPadding(dp(8),0,dp(8),0);LinearLayout.LayoutParams taskParams=new LinearLayout.LayoutParams(-1,dp(42));taskParams.setMargins(0,dp(2),0,dp(2));root.addView(taskRow,taskParams);}
        primary("+ Add task",this::showTaskLibrary);
        addButton("Restart routine progress",()->startNewRoutine());
        Button credits=button("♥  Credits",this::showCredits);credits.setTextSize(16);credits.setTextColor(Color.rgb(190,55,85));credits.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.rgb(255,241,245)));
        LinearLayout.LayoutParams creditParams=new LinearLayout.LayoutParams(-1,dp(48));creditParams.setMargins(0,dp(12),0,dp(4));root.addView(credits,creditParams);
        addButton("‹ Back to home",this::showHome);
    }
    private void showCredits(){
        startScreen();root.setGravity(Gravity.CENTER);
        TextView heart=text("♥",64,Color.rgb(220,65,100),true);
        LinearLayout.LayoutParams heartParams=new LinearLayout.LayoutParams(-1,dp(94));heartParams.bottomMargin=dp(18);root.addView(heart,heartParams);
        TextView message=text("To my darling Ronni,\n\nThis is what happens when a nerd falls in love with a cute, kind and amazing farmer's daughter.\n\nWith oodies of love,\n\nYour Dan",21,INK,false);
        message.setGravity(Gravity.CENTER);message.setLineSpacing(dp(5),1.0f);root.addView(message,new LinearLayout.LayoutParams(-1,-2));
        root.addView(new View(this),new LinearLayout.LayoutParams(1,dp(28)));
        primary("♥  Back to Manage Routines",this::showManageRoutines);
    }
    private void returnToRoutinePage(){if(managePageOpen)showManageRoutines();else showHome();}
    private int firstIncomplete(){ArrayList<Task> list=routineTasks();for(int i=0;i<list.size();i++)if(!prefs.getBoolean(doneKey(list.get(i)),false))return i;return list.size();}
    private void taskActions(int index){String[] options={"Edit task","Move up","Move down","Delete task"};new AlertDialog.Builder(this).setTitle(tasks.get(index).name).setItems(options,(d,which)->{if(which==0)editTask(index);else if(which==1&&index>0&&tasks.get(index-1).period.equals(tasks.get(index).period)){Task t=tasks.remove(index);tasks.add(index-1,t);saveTasks();returnToRoutinePage();}else if(which==2&&index<tasks.size()-1&&tasks.get(index+1).period.equals(tasks.get(index).period)){Task t=tasks.remove(index);tasks.add(index+1,t);saveTasks();returnToRoutinePage();}else if(which==3)new AlertDialog.Builder(this).setMessage("Delete this task?").setNegativeButton("Cancel",null).setPositiveButton("Delete",(dd,w)->{String removedId=tasks.get(index).id;tasks.remove(index);prefs.edit().remove("done_"+removedId).apply();saveTasks();returnToRoutinePage();}).show();}).show();}
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
    private void addLibraryTask(LibraryTask item){
        tasks.add(new Task(item.name,item.minutes*60,item.description,activePeriod));saveTasks();
        Toast.makeText(this,"Added to "+activePeriod+" routine",Toast.LENGTH_SHORT).show();
        showTaskLibrary();
    }
    private void showCustomTaskForm(String period){
        startScreen();heading("Create a task","Make it your own for your "+period.toLowerCase(Locale.ROOT)+" routine.");
        EditText name=new EditText(this);name.setHint("What do you want to do?");name.setSingleLine(true);root.addView(name,new LinearLayout.LayoutParams(-1,dp(56)));
        TextView durationLabel=text("DURATION (MINUTES)",12,MUTED,true);durationLabel.setGravity(Gravity.START);LinearLayout.LayoutParams dl=new LinearLayout.LayoutParams(-1,-2);dl.topMargin=dp(14);root.addView(durationLabel,dl);
        EditText minutes=new EditText(this);minutes.setInputType(InputType.TYPE_CLASS_NUMBER);minutes.setText("5");minutes.setSingleLine(true);root.addView(minutes,new LinearLayout.LayoutParams(-1,dp(56)));
        TextView notesLabel=text("NOTES (OPTIONAL)",12,MUTED,true);notesLabel.setGravity(Gravity.START);LinearLayout.LayoutParams nl=new LinearLayout.LayoutParams(-1,-2);nl.topMargin=dp(14);root.addView(notesLabel,nl);
        EditText notes=new EditText(this);notes.setHint("Add a helpful reminder");notes.setMinLines(2);notes.setGravity(Gravity.TOP|Gravity.START);root.addView(notes,new LinearLayout.LayoutParams(-1,dp(92)));
        root.addView(new View(this),new LinearLayout.LayoutParams(1,0,1));
        primary("Save task",()->{
            String n=name.getText().toString().trim();if(n.isEmpty()){name.setError("Enter a task name");return;}
            int mins;try{mins=Integer.parseInt(minutes.getText().toString());}catch(Exception e){mins=5;}
            mins=Math.max(1,Math.min(240,mins));tasks.add(new Task(n,mins*60,notes.getText().toString().trim(),period));saveTasks();
            Toast.makeText(this,"Added to "+period+" routine",Toast.LENGTH_SHORT).show();
            if(managePageOpen)showManageRoutines();else showHome();
        });
        addButton("Cancel",this::showTaskLibrary);
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
    private void editTask(int index){boolean add=index<0;Task old=add?new Task("",DEFAULT_SECONDS,""):tasks.get(index);LinearLayout form=new LinearLayout(this);form.setPadding(dp(20),dp(8),dp(20),0);form.setOrientation(1);EditText name=new EditText(this);name.setHint("Task name");name.setText(old.name);form.addView(name);EditText minutes=new EditText(this);minutes.setHint("Duration in minutes");minutes.setInputType(InputType.TYPE_CLASS_NUMBER);minutes.setText(String.valueOf(Math.max(1,(old.seconds+59)/60)));form.addView(minutes);EditText notes=new EditText(this);notes.setHint("Notes (optional)");notes.setText(old.notes);form.addView(notes);new AlertDialog.Builder(this).setTitle(add?"Add task":"Edit task").setView(form).setNegativeButton("Cancel",null).setPositiveButton("Save",(d,w)->{String n=name.getText().toString().trim();if(n.isEmpty()){Toast.makeText(this,"Enter a task name.",Toast.LENGTH_SHORT).show();return;}int mins;try{mins=Integer.parseInt(minutes.getText().toString());}catch(Exception e){mins=2;}mins=Math.max(1,Math.min(240,mins));Task t=add?new Task(n,mins*60,notes.getText().toString().trim(),activePeriod):new Task(old.id,n,mins*60,notes.getText().toString().trim(),old.period);if(add)tasks.add(t);else tasks.set(index,t);saveTasks();returnToRoutinePage();}).show();}
    private void showTimer(){
        if(ticker!=null)handler.removeCallbacks(ticker);
        ArrayList<Task> list=routineTasks();if(list.isEmpty()){showHome();return;}
        if(!currentTaskId.isEmpty()){for(int i=0;i<list.size();i++)if(list.get(i).id.equals(currentTaskId)){currentTask=i;break;}}
        currentTask=Math.max(0,Math.min(currentTask,list.size()-1));currentTaskId=list.get(currentTask).id;startScreen();
        int completed=0;for(Task task:list)if(prefs.getBoolean(doneKey(task),false))completed++;
        heading(activePeriod+" routine","TASK "+(currentTask+1)+" OF "+list.size()+" · "+completed+" COMPLETED");
        android.widget.ProgressBar progress=new android.widget.ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);
        progress.setMax(list.size());progress.setProgress(completed);progress.setProgressTintList(android.content.res.ColorStateList.valueOf(YELLOW));progress.setProgressBackgroundTintList(android.content.res.ColorStateList.valueOf(PALE));
        LinearLayout.LayoutParams pp=new LinearLayout.LayoutParams(-1,dp(8));pp.bottomMargin=dp(20);root.addView(progress,pp);
        root.addView(text(list.get(currentTask).name,27,INK,true));
        if(!list.get(currentTask).notes.isEmpty())root.addView(text(list.get(currentTask).notes,16,MUTED,false));
        countdownView=text(formatTime(remainingMs),54,INK,true);root.addView(countdownView,new LinearLayout.LayoutParams(-1,dp(150)));
        root.addView(text("About "+duration(list.get(currentTask).seconds)+" planned",14,MUTED,false));
        root.addView(new View(this),new LinearLayout.LayoutParams(1,0,1));
        pauseButton=button(timerRunning?"Pause":"Resume",()->toggleTimer());root.addView(pauseButton);
        primary("Complete task",()->{prefs.edit().putBoolean(doneKey(list.get(currentTask)),true).apply();nextTask();});
        addButton("Skip task",this::nextTask);
        addButton("End routine",()->{stopTimer();prefs.edit().putBoolean("timer_screen_active",false).apply();showHome();});
        if(timerRunning)runTicker();
    }
    private void toggleTimer(){ArrayList<Task> list=routineTasks();if(list.isEmpty())return;if(timerRunning){remainingMs=Math.max(0,deadlineMs-System.currentTimeMillis());timerRunning=false;}else{if(remainingMs<=0)remainingMs=list.get(currentTask).seconds*1000L;deadlineMs=System.currentTimeMillis()+remainingMs;timerRunning=true;}persistTimer();showTimer();}
    private void runTicker(){ticker=new Runnable(){public void run(){if(!timerRunning||countdownView==null)return;remainingMs=Math.max(0,deadlineMs-System.currentTimeMillis());countdownView.setText(formatTime(remainingMs));if(remainingMs==0){timerRunning=false;persistTimer();Toast.makeText(MainActivity.this,"Time is up — complete the task or continue.",Toast.LENGTH_LONG).show();if(pauseButton!=null)pauseButton.setText("Resume");}else handler.postDelayed(this,250);}};handler.post(ticker);}
    private void nextTask(){stopTimer();ArrayList<Task> list=routineTasks();if(currentTask+1>=list.size()){routineFinished=true;prefs.edit().putBoolean("routine_finished",true).putBoolean("timer_screen_active",false).putLong("last_completion_time",System.currentTimeMillis()).apply();showCompletion();return;}currentTask++;currentTaskId=list.get(currentTask).id;remainingMs=list.get(currentTask).seconds*1000L;timerRunning=true;deadlineMs=System.currentTimeMillis()+remainingMs;persistTimer();showTimer();}
    private void showCompletion(){
        stopTimer();startScreen();ArrayList<Task> list=routineTasks();heading("You're all done!","You made time for your "+activePeriod.toLowerCase(Locale.ROOT)+" routine.");
        int done=0,totalSeconds=0;for(Task task:list){if(prefs.getBoolean(doneKey(task),false)){done++;totalSeconds+=task.seconds;}}
        long started=prefs.getLong("routine_start_time",System.currentTimeMillis());long elapsed=Math.max(0,(System.currentTimeMillis()-started)/1000L);
        saveRoutineSession(activePeriod,done,list.size(),elapsed);
        root.addView(text(done+" of "+list.size()+" tasks completed",21,INK,true));
        TextView stats=text("Planned time completed: "+duration(totalSeconds)+"\nTime in this session: "+duration((int)elapsed),15,MUTED,false);
        stats.setGravity(Gravity.CENTER);LinearLayout.LayoutParams statp=new LinearLayout.LayoutParams(-1,-2);statp.setMargins(0,dp(16),0,dp(22));root.addView(stats,statp);
        android.widget.ProgressBar progress=new android.widget.ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);progress.setMax(Math.max(1,list.size()));progress.setProgress(done);progress.setProgressTintList(android.content.res.ColorStateList.valueOf(YELLOW));root.addView(progress,new LinearLayout.LayoutParams(-1,dp(8)));
        primary("Back to Today",this::showHome);addButton("Run again",this::startNewRoutine);
    }
    private String todayKey(long millis){return new java.text.SimpleDateFormat("yyyy-MM-dd",Locale.US).format(new java.util.Date(millis));}
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
        startScreen();heading("History & streaks","Celebrate the routines you complete.");
        JSONArray history=historyArray();java.util.HashSet<String> activeDates=new java.util.HashSet<>();int sessions=0,fullSessions=0;
        long now=System.currentTimeMillis();String today=todayKey(now);
        for(int i=0;i<history.length();i++){JSONObject o=history.optJSONObject(i);if(o==null)continue;sessions++;if(o.optBoolean("full",false)){fullSessions++;activeDates.add(o.optString("date",""));}}
        int streak=0;java.util.Calendar day=java.util.Calendar.getInstance();day.setTimeInMillis(now);
        if(!activeDates.contains(today))day.add(java.util.Calendar.DAY_OF_YEAR,-1);
        while(activeDates.contains(todayKey(day.getTimeInMillis()))&&streak<3660){streak++;day.add(java.util.Calendar.DAY_OF_YEAR,-1);}
        root.addView(text(String.valueOf(streak),42,INK,true));
        root.addView(text("day streak",16,MUTED,false));
        LinearLayout.LayoutParams gap=new LinearLayout.LayoutParams(-1,-2);gap.topMargin=dp(12);gap.bottomMargin=dp(12);
        root.addView(text(fullSessions+" completed routines  ·  "+sessions+" sessions recorded",15,INK,false),gap);
        TextView weekly=text("THIS WEEK",13,MUTED,true);weekly.setGravity(Gravity.START);root.addView(weekly);
        java.util.Calendar cal=java.util.Calendar.getInstance();cal.set(java.util.Calendar.DAY_OF_WEEK,cal.getFirstDayOfWeek());
        StringBuilder week=new StringBuilder();
        for(int d=0;d<7;d++){String date=todayKey(cal.getTimeInMillis());if(d>0)week.append("   ");week.append(new java.text.SimpleDateFormat("EEE",Locale.US).format(cal.getTime())).append(" ").append(activeDates.contains(date)?"✓":"·");cal.add(java.util.Calendar.DAY_OF_YEAR,1);}
        root.addView(text(week.toString(),15,INK,false),gap);
        TextView recent=text("RECENT SESSIONS",13,MUTED,true);recent.setGravity(Gravity.START);root.addView(recent);
        int shown=0;for(int i=history.length()-1;i>=0&&shown<20;i--){JSONObject o=history.optJSONObject(i);if(o==null)continue;String label=o.optString("period","Routine")+" · "+o.optString("date","")+"\n"+o.optInt("done",0)+"/"+o.optInt("total",0)+" tasks · "+duration((int)o.optLong("elapsed",0))+(o.optBoolean("full",false)?" · Completed":" · Partial");TextView sessionRow=text(label,14,INK,false);sessionRow.setGravity(Gravity.START);sessionRow.setPadding(dp(10),dp(10),dp(10),dp(10));root.addView(sessionRow,new LinearLayout.LayoutParams(-1,-2));shown++;}
        if(shown==0)root.addView(text("Your completed routines will appear here.",15,MUTED,false));
        addButton("‹ Back to Today",this::showHome);
    }
    private void startNewRoutine(){stopTimer();ArrayList<Task> list=routineTasks();SharedPreferences.Editor editor=prefs.edit();for(Task task:list)editor.remove(doneKey(task));currentTask=0;currentTaskId=list.isEmpty()?"":list.get(0).id;remainingMs=list.isEmpty()?DEFAULT_SECONDS*1000L:list.get(0).seconds*1000L;routineFinished=false;editor.putBoolean("routine_finished",false).putBoolean("timer_screen_active",false).putInt("timer_task_index",0).putString("timer_task_id",currentTaskId).putLong("timer_remaining_ms",remainingMs).apply();showHome();}
    private void stopTimer(){timerRunning=false;if(ticker!=null)handler.removeCallbacks(ticker);ticker=null;}
    private String formatTime(long ms){long s=Math.max(0,(ms+999)/1000);return String.format(Locale.getDefault(),"%02d:%02d",s/60,s%60);}
    private void persistTimer(){if(timerRunning)remainingMs=Math.max(0,deadlineMs-System.currentTimeMillis());prefs.edit().putInt("timer_task_index",currentTask).putString("timer_task_id",currentTaskId).putLong("timer_remaining_ms",remainingMs).putBoolean("timer_running",timerRunning).putBoolean("routine_finished",routineFinished).apply();}
}
