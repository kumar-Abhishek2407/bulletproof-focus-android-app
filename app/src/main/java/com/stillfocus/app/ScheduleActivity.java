package com.stillfocus.app;

import android.app.*;
import android.content.*;
import android.content.pm.*;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.VpnService;
import android.os.*;
import android.provider.Settings;
import android.view.*;
import android.widget.*;
import java.util.*;
import java.util.concurrent.*;

/** Daily app schedules, with a small list > editor > app picker hierarchy. */
public final class ScheduleActivity extends Activity {
    private static final int GREEN=FocusUi.ACCENT,INK=FocusUi.INK,MUTED=FocusUi.MUTED,PAPER=FocusUi.PAPER;
    private int transitionDirection=0;
    private int renderGeneration;
    private final ExecutorService worker=Executors.newSingleThreadExecutor();
    private final FocusNavigation.Swipe swipe=new FocusNavigation.Swipe();
    @Override public boolean dispatchTouchEvent(MotionEvent event) {
        int tab=swipe.target(this,event,2);
        if(tab>=0) { MotionEvent cancel=MotionEvent.obtain(event); cancel.setAction(MotionEvent.ACTION_CANCEL); super.dispatchTouchEvent(cancel); cancel.recycle(); FocusNavigation.open(this,tab); return true; }
        return super.dispatchTouchEvent(event);
    }
    private int page=0,start=21*60,end=9*60;
    private String id="",name="Evening pause";
    private boolean strict=false,enabled=true;
    private final Set<String> apps=new HashSet<>();
    private LinearLayout root,footer;
    private EditText nameInput;
    private TextView selectedCount;
    private ScheduleStore store;
    private final Handler handler=new Handler(Looper.getMainLooper());
    private String displayedState="";
    private String stateKey() {
        StringBuilder key=new StringBuilder().append(FocusVpnService.running).append(FocusAccessibilityService.active()).append(NormalAppBlocker.ready(this)).append(ScheduleReceiver.exactAllowed(this)); for(AppSchedule s:store.all()) key.append(s.id).append(s.enabled).append(s.active(ScheduleStore.now())); return key.toString();
    }
    private final Runnable refresh=new Runnable() {
        public void run() { if(page==0 && !displayedState.equals(stateKey())) render();
            else if(page!=0 && !id.isEmpty()) { AppSchedule saved=store.find(id); if(saved!=null && saved.active(ScheduleStore.now())) { page=0; render(); message("Schedule active","Editing unlocks when this schedule ends."); } } handler.postDelayed(this,1000); }
    };
    @Override public void onCreate(Bundle state) {
        super.onCreate(state); store=new ScheduleStore(this);
        if(state!=null) {
            page=state.getInt("page"); start=state.getInt("start",1260); end=state.getInt("end",540);
            id=state.getString("id",""); name=state.getString("name","Evening pause"); strict=state.getBoolean("strict"); enabled=state.getBoolean("enabled",true);
            ArrayList<String> saved=state.getStringArrayList("apps"); if(saved!=null) apps.addAll(saved);
        }
        if(Build.VERSION.SDK_INT>=33) getOnBackInvokedDispatcher().registerOnBackInvokedCallback(android.window.OnBackInvokedDispatcher.PRIORITY_DEFAULT,this::back);
        render();
    }
    @Override protected void onSaveInstanceState(Bundle out) {
        capture(); super.onSaveInstanceState(out); out.putInt("page",page); out.putInt("start",start); out.putInt("end",end);
        out.putString("id",id); out.putString("name",name); out.putBoolean("strict",strict); out.putBoolean("enabled",enabled); out.putStringArrayList("apps",new ArrayList<>(apps));
    }
    @Override protected void onNewIntent(Intent intent) { super.onNewIntent(intent); setIntent(intent); }
    private void capture() { if(nameInput!=null) name=nameInput.getText().toString(); }
    private void navigate(int next) { capture(); transitionDirection=next<page?-1:1; page=next; render(); }
    private void back() { if(page==0) FocusNavigation.open(this,0); else navigate(page==2?1:0); }
    @Override public void onBackPressed() { back(); }
    private int dp(int n) { return (int)(n*getResources().getDisplayMetrics().density+.5f); }
    private LinearLayout column() { LinearLayout box=new LinearLayout(this); box.setOrientation(LinearLayout.VERTICAL); return box; }
    private GradientDrawable bg(int color) { return FocusUi.surface(this,color,24); }
    private TextView text(String s,int size,int color) { TextView t=new TextView(this); t.setText(s); t.setTextSize(size); t.setTextColor(color); t.setPadding(0,dp(5),0,dp(5)); return t; }
    private Button button(String title,boolean primary,Runnable click) {
        Button b=new Button(this); b.setText(title); b.setAllCaps(false); b.setTextColor(primary?Color.WHITE:GREEN); FocusUi.button(b,primary); b.setMinHeight(dp(48));
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2); lp.topMargin=dp(10); b.setLayoutParams(lp); b.setOnClickListener(v->click.run()); return b;
    }
    private LinearLayout card() {
        LinearLayout box=column(); box.setPadding(dp(18),dp(14),dp(18),dp(16)); box.setBackground(bg(Color.WHITE)); box.setElevation(dp(1));
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2); lp.topMargin=dp(12); root.addView(box,lp); return box;
    }
    private void message(String title,String body) { new AlertDialog.Builder(this).setTitle(title).setMessage(body).setPositiveButton("OK",null).show(); }
    private String time(int minute) {
        Calendar c=Calendar.getInstance(); c.set(Calendar.HOUR_OF_DAY,minute/60); c.set(Calendar.MINUTE,minute%60);
        return android.text.format.DateFormat.getTimeFormat(this).format(c.getTime());
    }
    private String label(String pkg) {
        try { return getPackageManager().getApplicationLabel(getPackageManager().getApplicationInfo(pkg,0)).toString(); }
        catch(PackageManager.NameNotFoundException e) { return "Removed app"; }
    }
    private ImageView icon(String pkg) {
        ImageView v=new ImageView(this); v.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        AppCatalog.icon(v,pkg);
        return v;
    }
    private void render() {
        renderGeneration++;
        nameInput=null; selectedCount=null;
        LinearLayout shell=column(); shell.setBackgroundColor(PAPER);
        ScrollView scroll=new ScrollView(this); root=column(); root.setPadding(dp(20),dp(14),dp(20),dp(24)); scroll.addView(root); shell.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        footer=column(); footer.setPadding(dp(20),0,dp(20),dp(12)); shell.addView(footer); shell.addView(FocusNavigation.bar(this,2,tab->{ if(tab==2) navigate(0); else FocusNavigation.open(this,tab); },page!=0)); setContentView(shell);
        if(Build.VERSION.SDK_INT>=30) shell.setOnApplyWindowInsetsListener((v,i)-> { android.graphics.Insets b=i.getInsets(WindowInsets.Type.systemBars()); v.setPadding(b.left,b.top,b.right,b.bottom); return i; });
        else shell.setOnApplyWindowInsetsListener((v,i)-> { v.setPadding(i.getSystemWindowInsetLeft(),i.getSystemWindowInsetTop(),i.getSystemWindowInsetRight(),i.getSystemWindowInsetBottom()); return i; });
        if(page!=0) root.addView(button(page==2?"< Schedule":"< Schedules",false,this::back));
        root.addView(text(page==0?"Schedules":page==2?"Choose apps":id.isEmpty()?"New schedule":"Edit schedule",28,INK));
        if(page==0) list(); else if(page==1) editor(); else picker();
        FocusUi.enter(root,transitionDirection); transitionDirection=0;
    }
    private void list() {
        displayedState=stateKey();
        root.addView(text("Pause apps every day. Active schedules cannot be edited, disabled or deleted until their end time.",15,MUTED));
        List<AppSchedule> entries=store.all();
        boolean normalEnabled=false; for(AppSchedule s:entries) normalEnabled|=s.enabled && !s.strict;
        if(normalEnabled && !NormalAppBlocker.ready(this)) {
            root.addView(text("App-blocking permission is missing.",14,MUTED));
            if(!NormalAppBlocker.usageAllowed(this)) root.addView(button("Restore Usage access",false,()->openSetting(Settings.ACTION_USAGE_ACCESS_SETTINGS)));
            if(!Settings.canDrawOverlays(this)) root.addView(button("Restore Display over apps",false,()->openSetting(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)));
        }
        if(entries.isEmpty()) { LinearLayout empty=card(); empty.setBackground(bg(FocusUi.PEACH)); empty.addView(text("Make evenings quieter",22,INK)); empty.addView(text("For example, pause YouTube from 9 PM to 9 AM.",16,MUTED)); }
        for(AppSchedule s:entries) {
            LinearLayout row=card(); row.setBackground(bg(s.active(ScheduleStore.now())?FocusUi.MINT:FocusUi.LAVENDER)); row.addView(text(s.name,20,INK)); row.addView(text(time(s.window.start)+" - "+time(s.window.end),23,INK));
            row.addView(text("Every day  /  "+s.apps.size()+" apps  /  "+(s.strict?"Strict":"Normal"),14,MUTED));
            boolean active=s.active(ScheduleStore.now());
            boolean ready=s.strict?ManagedProtection.enforced(this):NormalAppBlocker.ready(this) && (FocusAccessibilityService.active() || FocusVpnService.running);
            row.addView(text(!s.enabled?"Off":active?(ready?"Window active":"Window active - protection needs attention"):"Starts at "+time(s.window.start),14,GREEN));
            LinearLayout icons=new LinearLayout(this); int shown=0;
            for(String pkg:new TreeSet<>(s.apps)) { if(shown++>=5) break; ImageView v=icon(pkg); LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(dp(32),dp(32)); lp.rightMargin=dp(8); icons.addView(v,lp); } row.addView(icons);
            Switch toggle=new Switch(this); toggle.setText("Enabled"); toggle.setTextColor(INK); toggle.setMinHeight(dp(48)); toggle.setChecked(s.enabled); toggle.setEnabled(!active); row.addView(toggle);
            toggle.setOnCheckedChangeListener((v,on)-> {
                try { store.save(new AppSchedule(s.id,s.name,s.apps,s.window.start,s.window.end,s.strict,on)); render(); }
                catch(RuntimeException e) { render(); message("Could not change schedule",e.getMessage()); }
            });
            Button edit=button(active?"Locked until "+time(s.window.end):"Edit",false,()-> { load(s); navigate(1); }); edit.setEnabled(!active); row.addView(edit);
        }
        footer.addView(button("Add schedule",true,()-> { id=""; name="Evening pause"; start=1260; end=540; strict=false; enabled=true; apps.clear(); navigate(1); }));
        if(!ScheduleReceiver.exactAllowed(this)) {
            root.addView(text("Phone sleep can delay changes. Allow Alarms & reminders for more accurate timing.",13,MUTED));
            root.addView(button("Allow precise scheduling",false,this::requestAlarms));
        }
        root.addView(text("Times follow your phone's local time. Force-stopping Still Focus or revoking permissions can interrupt normal schedules.",13,MUTED));
    }
    private void load(AppSchedule s) { id=s.id; name=s.name; start=s.window.start; end=s.window.end; strict=s.strict; enabled=s.enabled; apps.clear(); apps.addAll(s.apps); }
    private void editor() {
        LinearLayout details=card(); nameInput=new EditText(this); nameInput.setText(name); nameInput.setHint("Schedule name"); nameInput.setSingleLine(true); details.addView(nameInput);
        details.addView(text("Every day",14,MUTED));
        details.addView(button("From  "+time(start),false,()->pickTime(true))); details.addView(button("Until  "+time(end),false,()->pickTime(false)));
        details.addView(text(start>end?"Ends the next day.":"Starts and ends on the same day.",13,MUTED));
        LinearLayout selection=card(); selection.addView(text(apps.size()+" apps selected",19,INK));
        List<String> names=new ArrayList<>(); for(String pkg:apps) names.add(label(pkg)); Collections.sort(names);
        selection.addView(text(names.isEmpty()?"Choose which apps to pause.":String.join(", ",names),15,MUTED)); selection.addView(button("Choose apps",false,()->navigate(2)));
        LinearLayout mode=card(); mode.addView(text("Protection",18,INK));
        RadioGroup modes=new RadioGroup(this); RadioButton normal=new RadioButton(this),managed=new RadioButton(this);
        normal.setId(View.generateViewId()); normal.setText("Normal"); managed.setId(View.generateViewId()); managed.setText("Strict (device owner required)"); modes.addView(normal); modes.addView(managed); modes.check(strict?managed.getId():normal.getId());
        modes.setOnCheckedChangeListener((g,value)-> { capture(); strict=value==managed.getId(); render(); }); mode.addView(modes);
        mode.addView(text("Normal covers detected apps and returns Home. Strict suspends apps. All active schedules lock until their end time.",13,MUTED));
        if(strict && !ManagedProtection.ready(this)) mode.addView(text("Set up device-owner management in Still Focus Settings first.",14,MUTED));
        footer.addView(button("Save schedule",true,this::save));
        if(!id.isEmpty()) root.addView(button("Delete schedule",false,()->new AlertDialog.Builder(this).setTitle("Delete this schedule?").setNegativeButton("Keep",null).setPositiveButton("Delete",(d,w)-> {
            try { store.delete(id); navigate(0); } catch(RuntimeException e) { message("Could not delete",e.getMessage()); }
        }).show()));
    }
    private void pickTime(boolean from) {
        capture(); int minute=from?start:end;
        new TimePickerDialog(this,(v,h,m)-> { if(from) start=h*60+m; else end=h*60+m; render(); },minute/60,minute%60,android.text.format.DateFormat.is24HourFormat(this)).show();
    }
    private void save() {
        capture();
        try {
            if(VpnService.prepare(this)!=null) throw new IllegalStateException("Enable protection from Still Focus Settings first.");
            AppSchedule next=new AppSchedule(id.isEmpty()?UUID.randomUUID().toString():id,name,apps,start,end,strict,enabled);
            store.save(next); new FocusStore(this).enable();
            try { startForegroundService(new Intent(this,FocusVpnService.class)); } catch(RuntimeException e) { message("Schedule saved","Open Still Focus to keep protection running."); }
            navigate(0);
        } catch(RuntimeException e) { message("Check schedule",e.getMessage()); }
    }
    private void picker() {
        selectedCount=text(apps.size()+" apps selected",15,MUTED); root.addView(selectedCount);
        EditText search=new EditText(this); search.setHint("Search apps"); search.setSingleLine(true); root.addView(search);
        LinearLayout list=column(); root.addView(list);
        final int generation=renderGeneration;
        TextView loading=text("Loading apps...",14,MUTED); list.addView(loading);
        AppCatalog.load(this,Collections.emptySet(),entries-> {
            if(isDestroyed() || generation!=renderGeneration) return;
            list.removeView(loading); appendAppChoices(entries,0,list,search,generation);
        });
        search.addTextChangedListener(new android.text.TextWatcher() {
            public void beforeTextChanged(CharSequence s,int a,int n,int after) { }
            public void onTextChanged(CharSequence s,int a,int before,int count) { for(int i=0;i<list.getChildCount();i++) { View v=list.getChildAt(i); if(v.getTag()!=null) v.setVisibility(v.getTag().toString().contains(s.toString().toLowerCase(Locale.ROOT))?View.VISIBLE:View.GONE); } }
            public void afterTextChanged(android.text.Editable e) { }
        });
        footer.addView(button("Done",true,()->navigate(1)));
    }
    private void appendAppChoices(List<AppCatalog.Entry> entries,int start,LinearLayout list,EditText search,int generation) {
        if(isDestroyed() || generation!=renderGeneration) return;
        int end=Math.min(start+8,entries.size());
        for(int index=start;index<end;index++) {
            AppCatalog.Entry entry=entries.get(index); String pkg=entry.packageName;
            LinearLayout row=new LinearLayout(this); row.setGravity(Gravity.CENTER_VERTICAL); row.setPadding(dp(4),dp(8),0,dp(8)); row.setMinimumHeight(dp(64)); row.setTag(entry.label.toLowerCase(Locale.ROOT));
            row.addView(icon(pkg),new LinearLayout.LayoutParams(dp(40),dp(40)));
            TextView name=text(entry.label,17,INK); name.setPadding(dp(12),0,dp(4),0); row.addView(name,new LinearLayout.LayoutParams(0,-2,1));
            CheckBox check=new CheckBox(this); check.setButtonTintList(FocusUi.selection()); check.setContentDescription("Select "+entry.label); check.setChecked(apps.contains(pkg)); row.setBackground(bg(check.isChecked()?FocusUi.LAVENDER:Color.WHITE)); row.addView(check,new LinearLayout.LayoutParams(dp(48),dp(48)));
            check.setOnCheckedChangeListener((v,on)-> { if(on) apps.add(pkg); else apps.remove(pkg); row.setBackground(bg(on?FocusUi.LAVENDER:Color.WHITE)); selectedCount.setText(apps.size()+" apps selected"); }); row.setOnClickListener(v->check.toggle()); list.addView(row);
        }
        String query=search.getText().toString().toLowerCase(Locale.ROOT);
        for(int i=start;i<end;i++) { View row=list.getChildAt(i); row.setVisibility(row.getTag().toString().contains(query)?View.VISIBLE:View.GONE); }
        if(end<entries.size()) list.postOnAnimation(()->appendAppChoices(entries,end,list,search,generation));
        else if(entries.isEmpty()) list.addView(text("No launchable apps found on this profile.",14,MUTED));
    }
    private void requestAlarms() {
        if(Build.VERSION.SDK_INT<31) return;
        try { startActivity(new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,android.net.Uri.parse("package:"+getPackageName()))); }
        catch(ActivityNotFoundException e) { message("Open Android Settings","Find Still Focus under Alarms & reminders."); }
    }
    private void openSetting(String action) {
        try { startActivity(new Intent(action,android.net.Uri.parse("package:"+getPackageName()))); }
        catch(ActivityNotFoundException e) { message("Open Android Settings","Find Still Focus under Special app access."); }
    }
    @Override protected void onResume() {
        super.onResume(); worker.execute(()-> { try { ScheduleReceiver.schedule(getApplicationContext()); } catch(RuntimeException ignored) { } });
        handler.removeCallbacks(refresh); handler.postDelayed(refresh,220);
    }
    @Override protected void onPause() { capture(); handler.removeCallbacks(refresh); swipe.reset(); super.onPause(); }
    @Override protected void onDestroy() { renderGeneration++; worker.shutdown(); super.onDestroy(); }
}
