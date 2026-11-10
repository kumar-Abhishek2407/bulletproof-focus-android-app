package com.stillfocus.app;
import android.app.*;
import android.content.*;
import android.os.*;
import android.provider.Settings;
import android.view.*;
import android.widget.*;
import java.util.*;
import java.util.concurrent.*;

/** Per-app daily allowances, separate from pause timers and schedules. */
public final class DailyLimitActivity extends Activity {
    private DailyLimitStore store;
    private ActiveDotButton limitsButton;
    private LinearLayout list,navigation;
    private EditText search;
    private boolean listOpened=false;
    private final FocusNavigation.Swipe swipe=new FocusNavigation.Swipe();
    @Override public boolean dispatchTouchEvent(MotionEvent event) {
        int tab=swipe.target(this,event,1);
        if(tab>=0) { MotionEvent cancel=MotionEvent.obtain(event); cancel.setAction(MotionEvent.ACTION_CANCEL); super.dispatchTouchEvent(cancel); cancel.recycle(); FocusNavigation.open(this,tab); return true; }
        return super.dispatchTouchEvent(event);
    }
    private TextView health,empty;
    private boolean limitedOnly=true;
    private boolean appsLoaded,appsLoading;
    private int loadGeneration;
    private long appsLoadedAt;
    private String query="";
    private final Map<String,android.view.View> rows=new HashMap<>();
    private final Map<String,TextView> counters=new HashMap<>();
    private final Handler handler=new Handler(Looper.getMainLooper());
    private final ExecutorService worker=Executors.newSingleThreadExecutor();
    private final Runnable tick=new Runnable() { public void run() { update(); handler.postDelayed(this,2000); } };
    private int dp(int n) { return Math.round(n*getResources().getDisplayMetrics().density); }
    private TextView text(String value,int size) { TextView v=new TextView(this); v.setText(value); v.setTextSize(size); v.setTextColor(FocusUi.INK); v.setPadding(0,dp(8),0,dp(8)); return v; }
    private Button button(String label,Runnable action) { Button b=label.equals("Apps with limits")?new ActiveDotButton(this):new Button(this); b.setText(label); b.setAllCaps(false); FocusUi.button(b,false); LinearLayout.LayoutParams params=new LinearLayout.LayoutParams(-1,-2); params.topMargin=dp(10); b.setLayoutParams(params); b.setOnClickListener(v->action.run()); return b; }
    @Override public void onCreate(Bundle state) {
        super.onCreate(state); store=new DailyLimitStore(this);
        if(state!=null) { listOpened=state.getBoolean("listOpened",false); limitedOnly=state.getBoolean("limitedOnly",true); query=state.getString("query",""); }
        LinearLayout root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setPadding(dp(22),dp(18),dp(22),dp(18)); root.setBackgroundColor(FocusUi.PAPER);
        ScrollView scroll=new ScrollView(this); scroll.addView(root);
        LinearLayout shell=new LinearLayout(this); shell.setOrientation(LinearLayout.VERTICAL); shell.setBackgroundColor(FocusUi.PAPER);
        shell.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        navigation=new LinearLayout(this); navigation.setOrientation(LinearLayout.VERTICAL); shell.addView(navigation); refreshNavigation(); setContentView(shell);
        if(Build.VERSION.SDK_INT>=30) shell.setOnApplyWindowInsetsListener((v,i)-> { android.graphics.Insets bars=i.getInsets(WindowInsets.Type.systemBars()); v.setPadding(bars.left,bars.top,bars.right,bars.bottom); return i; });
        root.addView(text("Daily limit",28));
        root.addView(text("Set how long you can use each app today. Time already used counts. You can change a limit while time remains. Once used up, it stays locked until midnight.",15));
        health=text("",14); root.addView(health);
        if(!NormalAppBlocker.usageAllowed(this)) root.addView(button("Enable Usage access",()->startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))));
        Button add=button("Set daily limit",()->openList(false)); root.addView(add);
        limitsButton=(ActiveDotButton)button("Apps with limits",()->openList(true)); root.addView(limitsButton);
        limitsButton.setActive(!store.limits().isEmpty());
        empty=text("",15); root.addView(empty);
        search=new EditText(this); search.setHint("Search apps"); search.setSingleLine(true); root.addView(search);
        list=new LinearLayout(this); list.setOrientation(LinearLayout.VERTICAL); root.addView(list);
        search.addTextChangedListener(new android.text.TextWatcher() {
            public void beforeTextChanged(CharSequence s,int a,int c,int f) { }
            public void afterTextChanged(android.text.Editable e) { }
            public void onTextChanged(CharSequence s,int a,int before,int count) { query=s.toString().toLowerCase(Locale.ROOT); filter(); }
        });
        search.setText(query); filter();
        if(listOpened) loadApps();
        if(Build.VERSION.SDK_INT>=33) getOnBackInvokedDispatcher().registerOnBackInvokedCallback(android.window.OnBackInvokedDispatcher.PRIORITY_DEFAULT,this::back);
    }
    @Override protected void onSaveInstanceState(Bundle state) { super.onSaveInstanceState(state); state.putBoolean("listOpened",listOpened); state.putBoolean("limitedOnly",limitedOnly); state.putString("query",query); }
    private void refreshNavigation() {
        navigation.removeAllViews(); navigation.addView(FocusNavigation.bar(this,1,tab->{
            if(tab==1) { listOpened=false; query=""; search.setText(""); filter(); refreshNavigation(); }
            else FocusNavigation.open(this,tab);
        },listOpened));
    }
    private void openList(boolean configured) {
        listOpened=true; limitedOnly=configured; query=""; search.setText(""); filter(); refreshNavigation();
        loadApps();
        FocusUi.enter(list,1);
        if(configured) { search.clearFocus(); ((android.view.inputmethod.InputMethodManager)getSystemService(INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(search.getWindowToken(),0); }
    }
    private void filter() {
        list.setVisibility(listOpened?View.VISIBLE:View.GONE);
        empty.setVisibility(listOpened?View.VISIBLE:View.GONE);
        search.setVisibility(listOpened && !limitedOnly?View.VISIBLE:View.GONE);
        if(!listOpened) return;
        Map<String,Integer> limits=store.limits(); int visible=0;
        for(Map.Entry<String,View> entry:rows.entrySet()) {
            View row=entry.getValue(); boolean show=(!limitedOnly || limits.containsKey(entry.getKey())) && row.getTag().toString().contains(query);
            row.setVisibility(show?View.VISIBLE:View.GONE); if(show) visible++;
        }
        empty.setText(appsLoading?"Loading apps...":visible==0?(limitedOnly?"No matching daily limits. Tap Set daily limit to add one.":"No matching apps."):limitedOnly?"Today / remaining time":"Choose an app");
    }
    /** The home tab has no installed-app work. Pickers load only when explicitly opened. */
    private void loadApps() {
        if(appsLoading || appsLoaded && SystemClock.elapsedRealtime()-appsLoadedAt<60000) return;
        rows.clear(); counters.clear(); list.removeAllViews(); appsLoaded=false;
        appsLoading=true; filter(); final int generation=++loadGeneration;
        worker.execute(()-> {
            Set<String> configured=store.limits().keySet();
            AppCatalog.load(this,configured,entries-> {
                if(isDestroyed() || generation!=loadGeneration) return;
                appendApps(entries,0,generation);
            });
        });
    }
    private void appendApps(List<AppCatalog.Entry> entries,int start,int generation) {
        if(isDestroyed() || generation!=loadGeneration) return;
        int end=Math.min(start+8,entries.size());
        for(int index=start;index<end;index++) {
            AppCatalog.Entry entry=entries.get(index); String pkg=entry.packageName;
            LinearLayout row=new LinearLayout(this); row.setGravity(Gravity.CENTER_VERTICAL); row.setPadding(dp(12),dp(12),dp(12),dp(12)); row.setBackground(FocusUi.surface(this,FocusUi.LAVENDER,18));
            ImageView icon=new ImageView(this); AppCatalog.icon(icon,pkg); row.addView(icon,new LinearLayout.LayoutParams(dp(40),dp(40)));
            LinearLayout words=new LinearLayout(this); words.setPadding(dp(12),0,0,0); words.setOrientation(LinearLayout.VERTICAL); words.addView(text(entry.label,18));
            TextView counter=text("Tap to set daily limit",14); words.addView(counter); counters.put(pkg,counter); row.addView(words,new LinearLayout.LayoutParams(0,-2,1));
            rows.put(pkg,row); row.setTag(entry.label.toLowerCase(Locale.ROOT)); row.setOnClickListener(v->edit(pkg,entry.label)); row.setFocusable(true);
            LinearLayout.LayoutParams params=new LinearLayout.LayoutParams(-1,-2); params.bottomMargin=dp(8); list.addView(row,params);
        }
        if(end<entries.size()) list.postOnAnimation(()->appendApps(entries,end,generation));
        else { appsLoaded=true; appsLoadedAt=SystemClock.elapsedRealtime(); appsLoading=false; update(); }
        filter();
    }
    private String duration(long ms) { long minutes=Math.max(0,(ms+59999)/60000); return minutes/60+"h "+minutes%60+"m"; }
    private void update() {
        if(limitsButton!=null) limitsButton.setActive(!store.limits().isEmpty());
        store.requestRefresh();
        health.setText(PaymentMode.enabled(this)?"Payment mode: app enforcement paused; daily usage still counts.":!NormalAppBlocker.usageAllowed(this)?"Enable Usage access to count daily use.":!NormalAppBlocker.ready(this)?"Enable app blocking and display-over-other-apps access in Still Focus settings.":!FocusAccessibilityService.active() && !FocusVpnService.running?"Protection is stopped. Open Still Focus to restore it.":DailyLimitStore.status);
        if(!listOpened) return;
        Map<String,Integer> limits=store.limits();
        for(Map.Entry<String,TextView> e:counters.entrySet()) {
            String pkg=e.getKey(); Integer limit=limits.get(pkg);
            e.getValue().setText(limit==null?"Tap to set daily limit":duration(Math.max(0,limit*60000L-store.used(pkg)))+" remaining / "+duration(limit*60000L)+" daily"+(store.exhausted(pkg)?"\nReached - locked until midnight":"\nTap to adjust"));
        }
        filter();
    }
    private void edit(String pkg,String name) {
        if(store.exhausted(pkg)) { new AlertDialog.Builder(this).setTitle("Daily limit reached").setMessage("Available again at midnight. You cannot increase or remove today's exhausted limit.").setPositiveButton("Got it",null).show(); return; }
        LinearLayout box=new LinearLayout(this); box.setPadding(dp(20),dp(12),dp(20),dp(12)); box.setOrientation(LinearLayout.VERTICAL);
        box.addView(text("Daily allowance: hours and minutes",16));
        LinearLayout wheels=new LinearLayout(this); NumberPicker hours=new NumberPicker(this),minutes=new NumberPicker(this);
        hours.setMinValue(0); hours.setMaxValue(24); minutes.setMinValue(0); minutes.setMaxValue(59);
        int initial=store.limits().getOrDefault(pkg,120); hours.setValue(initial/60); minutes.setMaxValue(initial==1440?0:59); minutes.setValue(initial%60);
        hours.setContentDescription("Hours"); minutes.setContentDescription("Minutes");
        for(NumberPicker wheel:new NumberPicker[]{hours,minutes}) { wheel.setWrapSelectorWheel(false); wheel.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS); wheels.addView(wheel,new LinearLayout.LayoutParams(0,dp(160),1)); }
        hours.setOnValueChangedListener((p,o,n)->minutes.setMaxValue(n==24?0:59)); box.addView(wheels);
        TextView error=text("",14); box.addView(error);
        AlertDialog dialog=new AlertDialog.Builder(this).setTitle(name).setView(box).setNegativeButton("Cancel",null).setPositiveButton("Save",null).setNeutralButton("Remove limit",null).create();
        dialog.setOnShowListener(d-> {
            dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setEnabled(store.limits().containsKey(pkg));
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v-> { int value=hours.getValue()*60+minutes.getValue(); if(value==0) error.setText("Choose at least 1 minute."); else save(dialog,error,pkg,value); });
            dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(v->save(dialog,error,pkg,0));
        }); dialog.show();
    }
    private void save(AlertDialog dialog,TextView error,String pkg,int minutes) {
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false); dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setEnabled(false);
        worker.execute(()-> { String failure=null; try { store.save(pkg,minutes); } catch(RuntimeException e) { failure=e.getMessage(); } final String result=failure;
            runOnUiThread(()-> { if(isFinishing() || isDestroyed()) return; if(result==null) { dialog.dismiss(); openList(true); update(); } else { error.setText(result); dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true); dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setEnabled(store.limits().containsKey(pkg)); } });
        });
    }
    @Override protected void onNewIntent(Intent intent) { super.onNewIntent(intent); setIntent(intent); }
    private void back() {
        if(listOpened) { listOpened=false; filter(); refreshNavigation(); }
        else FocusNavigation.open(this,0);
    }
    @Override public void onBackPressed() { back(); }
    @Override protected void onResume() { super.onResume(); handler.removeCallbacks(tick); handler.postDelayed(tick,220); }
    @Override protected void onPause() { handler.removeCallbacks(tick); swipe.reset(); super.onPause(); }
    @Override protected void onDestroy() { loadGeneration++; worker.shutdown(); super.onDestroy(); }
}
