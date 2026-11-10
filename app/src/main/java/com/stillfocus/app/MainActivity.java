package com.stillfocus.app;

import android.app.*;
import android.content.*;
import android.content.pm.*;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.VpnService;
import android.os.*;
import android.provider.Settings;
import android.text.*;
import android.view.*;
import android.widget.*;
import java.util.*;

public final class MainActivity extends Activity {
    private static final int GREEN=FocusUi.ACCENT,INK=FocusUi.INK,MUTED=FocusUi.MUTED,PAPER=FocusUi.PAPER;
    private int transitionDirection=0;
    private int renderGeneration;
    private String renderedPermissions="";
    private enum Page { SETUP, HOME, BLOCKS, APPS, APP_PICKER, WEBSITES, SESSION, SETTINGS, STRICT_SETUP, PAYMENTS }
    private Page page;
    private final FocusNavigation.Swipe swipe=new FocusNavigation.Swipe();
    private int selectedTab() { return page==Page.SETTINGS || page==Page.STRICT_SETUP || page==Page.PAYMENTS?3:0; }
    private void selectTab(int tab) {
        captureDraft(); int direction=tab>selectedTab()?1:-1;
        if(tab==0 || tab==3) { page=tab==0?Page.HOME:Page.SETTINGS; transitionDirection=direction; render(); }
        else FocusNavigation.open(this,tab);
    }
    @Override public boolean dispatchTouchEvent(MotionEvent event) {
        int tab=page==Page.SETUP?-1:swipe.target(this,event,selectedTab());
        if(tab>=0) { MotionEvent cancel=MotionEvent.obtain(event); cancel.setAction(MotionEvent.ACTION_CANCEL); super.dispatchTouchEvent(cancel); cancel.recycle(); selectTab(tab); return true; }
        return super.dispatchTouchEvent(event);
    }
    private Page strictParent=Page.SETTINGS;
    private LinearLayout root,footer;
    private Button pickerDone;
    private TextView health,timer,sessionSummary,appCount,connectionDetails,paymentStatus;
    private EditText websiteInput;
    private NumberPicker durationHours,durationMinutes;
    private final Set<String> selectedApps=new HashSet<>();
    private String websiteDraft="",appDuration="25",websiteDuration="25";
    private FocusStore store;
    private SetupState setup;
    private android.content.SharedPreferences ui;
    private boolean serviceRequested;
    private boolean youtubeShorts,websiteShorts;
    private long lastServiceRequest;
    private boolean sessionWasActive;
    private final Handler handler=new Handler(Looper.getMainLooper());
    private final Runnable ticker=new Runnable() {
        @Override public void run() { refresh(); handler.postDelayed(this,1000); }
    };

    @Override public void onCreate(Bundle saved) {
        super.onCreate(saved); store=new FocusStore(this); ui=getSharedPreferences("interface",0);
        setup=new SetupState(ui.getBoolean("setupComplete",false));
        if(saved!=null) {
            youtubeShorts=saved.getBoolean("youtubeShorts",false);
            websiteShorts=saved.getBoolean("websiteShorts",false);
            ArrayList<String> apps=saved.getStringArrayList("apps"); if(apps!=null) selectedApps.addAll(apps);
            websiteDraft=saved.getString("websites",""); appDuration=saved.getString("appDuration","25");
            websiteDuration=saved.getString("websiteDuration","25");
            try { strictParent=Page.valueOf(saved.getString("strictParent","SETTINGS")); } catch(IllegalArgumentException e) { strictParent=Page.SETTINGS; }
            try { page=Page.valueOf(saved.getString("page","HOME")); } catch(IllegalArgumentException e) { page=Page.HOME; }
        }
        observePermissions();
        if(page==null) page=setup.needsSetup()?Page.SETUP:Page.HOME;
        if(page==Page.SETUP && !setup.needsSetup()) page=Page.HOME;
        if(saved==null && getIntent().getIntExtra("navigationTab",0)==3 && page!=Page.SETUP) page=Page.SETTINGS;
        render();
        if(Build.VERSION.SDK_INT>=33) getOnBackInvokedDispatcher().registerOnBackInvokedCallback(android.window.OnBackInvokedDispatcher.PRIORITY_DEFAULT,this::handleBack);
    }
    @Override protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent); setIntent(intent);
        Page next=intent.getIntExtra("navigationTab",0)==3?Page.SETTINGS:Page.HOME;
        if(page==Page.SETUP && setup.needsSetup()) return;
        if(page!=next) { captureDraft(); page=next; transitionDirection=0; render(); }
    }
    @Override protected void onSaveInstanceState(Bundle out) {
        captureDraft(); super.onSaveInstanceState(out);
        out.putBoolean("youtubeShorts",youtubeShorts);
        out.putBoolean("websiteShorts",websiteShorts);
        out.putString("page",page.name()); out.putString("strictParent",strictParent.name()); out.putStringArrayList("apps",new ArrayList<>(selectedApps));
        out.putString("websites",websiteDraft); out.putString("appDuration",appDuration); out.putString("websiteDuration",websiteDuration);
    }
    private boolean vpnAllowed() { return VpnService.prepare(this)==null; }
    private boolean observePermissions() {
        boolean managed=strictPreferred() && ManagedProtection.ready(this);
        boolean changed=setup.observe(vpnAllowed(),managed || NormalAppBlocker.usageAllowed(this),managed || Settings.canDrawOverlays(this));
        if(changed) ui.edit().putBoolean("setupComplete",true).apply();
        return changed;
    }
    private void captureDraft() {
        if(websiteInput!=null) websiteDraft=websiteInput.getText().toString();
        if(durationHours!=null && durationMinutes!=null) {
            if(page==Page.APPS) appDuration=String.valueOf(durationHours.getValue()*60+durationMinutes.getValue());
            else if(page==Page.WEBSITES) websiteDuration=String.valueOf(durationHours.getValue()*60+durationMinutes.getValue());
        }
    }
    private void navigate(Page next) { captureDraft(); transitionDirection=(next==Page.HOME || next==Page.APPS && page==Page.APP_PICKER || page==Page.STRICT_SETUP)?-1:1; page=next==Page.SETUP && !setup.needsSetup()?Page.HOME:next; render(); }
    private int dp(float value) { return (int)(value*getResources().getDisplayMetrics().density+.5f); }
    private GradientDrawable background(int color,int radius) { return FocusUi.surface(this,color,radius); }
    private TextView text(String value,int size,int color,boolean bold) {
        TextView t=new TextView(this); t.setText(value); t.setTextSize(size); t.setTextColor(color);
        if(bold) t.setTypeface(Typeface.create("sans-serif",Typeface.BOLD)); t.setPadding(0,dp(5),0,dp(5)); return t;
    }
    private LinearLayout column() { LinearLayout l=new LinearLayout(this); l.setOrientation(LinearLayout.VERTICAL); return l; }
    private LinearLayout card(LinearLayout parent,int color) {
        LinearLayout l=column(); l.setPadding(dp(18),dp(16),dp(18),dp(16)); l.setBackground(background(color,24)); l.setElevation(dp(1));
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2); p.topMargin=dp(6); p.bottomMargin=dp(6); parent.addView(l,p); return l;
    }
    private ActiveDotButton activeBlockersButton;
    private Button button(String title,boolean primary,Runnable action) {
        Button b=title.equals("Active blockers")?new ActiveDotButton(this):new Button(this); b.setText(title); b.setAllCaps(false); b.setTextSize(15); b.setTextColor(primary?Color.WHITE:GREEN);
        FocusUi.button(b,primary); b.setPadding(dp(14),dp(10),dp(14),dp(10)); b.setMinHeight(dp(50));
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2); p.topMargin=dp(10); b.setLayoutParams(p); b.setOnClickListener(v->action.run()); return b;
    }
    private EditText input(String hint,int type) {
        EditText e=new EditText(this); e.setTextSize(16); e.setTextColor(INK); e.setHint(hint); e.setHintTextColor(MUTED); e.setBackgroundTintList(null); e.setInputType(type);
        e.setPadding(dp(14),dp(14),dp(14),dp(14)); e.setBackground(background(PAPER,12));
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2); p.topMargin=dp(8); p.bottomMargin=dp(8); e.setLayoutParams(p); return e;
    }
    private void heading(String eyebrow,String title,String description) {
        root.addView(text(title,28,INK,true)); if(!description.isEmpty()) root.addView(text(description,15,MUTED,false));
    }
    private void render() {
        View previous=root!=null && root.getParent() instanceof ScrollView?(View)root.getParent():null;
        renderGeneration++;
        renderedPermissions=permissionState();
        getIntent().putExtra("navigationTab",selectedTab());
        sessionWasActive=store.remaining()>0;
        paymentStatus=null; websiteInput=null; durationHours=null; durationMinutes=null; connectionDetails=null; health=null; timer=null; sessionSummary=null; appCount=null; pickerDone=null; footer=null;
        ScrollView scroll=new ScrollView(this); scroll.setFillViewport(true); scroll.setBackgroundColor(PAPER);
        root=column(); root.setPadding(dp(22),dp(18),dp(22),dp(28)); scroll.addView(root);
        FrameLayout stage=new FrameLayout(this); stage.addView(scroll,new FrameLayout.LayoutParams(-1,-1));
        LinearLayout shell=column(); shell.setBackgroundColor(PAPER); shell.addView(stage,new LinearLayout.LayoutParams(-1,0,1));
        if(page==Page.APP_PICKER) {
            footer=column(); footer.setPadding(dp(22),dp(4),dp(22),dp(12)); footer.setElevation(dp(6)); footer.setBackgroundColor(PAPER);
            shell.addView(footer,new LinearLayout.LayoutParams(-1,-2));
        }
        if(page!=Page.SETUP) shell.addView(FocusNavigation.bar(this,selectedTab(),this::selectTab,page!=Page.HOME && page!=Page.SETTINGS && page!=Page.BLOCKS));
        setContentView(shell);
        if(Build.VERSION.SDK_INT>=30) shell.setOnApplyWindowInsetsListener((v,i)-> {
            android.graphics.Insets bars=i.getInsets(WindowInsets.Type.systemBars()); v.setPadding(bars.left,bars.top,bars.right,bars.bottom); return i;
        });
        else shell.setOnApplyWindowInsetsListener((v,i)-> { v.setPadding(i.getSystemWindowInsetLeft(),i.getSystemWindowInsetTop(),i.getSystemWindowInsetRight(),i.getSystemWindowInsetBottom()); return i; });
        if(page!=Page.HOME && page!=Page.SETUP && page!=Page.BLOCKS && page!=Page.SETTINGS) root.addView(button(page==Page.APP_PICKER?"< Block apps":page==Page.STRICT_SETUP?"< Back":"< Home",false,()->navigate(page==Page.APP_PICKER?Page.APPS:page==Page.STRICT_SETUP?strictParent:Page.HOME)));
        root.addView(text("Still Focus",20,FocusUi.TEAL,true));
        switch(page) {
            case SETUP: setupPage(); break;
            case HOME: homePage(); break;
            case BLOCKS: blocksPage(); break;
            case APPS: appsPage(); break;
            case APP_PICKER: appPickerPage(); break;
            case WEBSITES: websitesPage(); break;
            case SESSION: sessionPage(); break;
            case SETTINGS: settingsPage(); break;
            case STRICT_SETUP: strictSetupPage(); break;
            case PAYMENTS: paymentPage(); break;
        }
        updateLiveViews();
        FocusUi.slidePages(stage,previous,scroll,transitionDirection); transitionDirection=0;
    }
    private void setupPage() {
        heading("WELCOME", "A little setup.\nA lot more focus.","Allow the permissions for your mode once. After setup, you'll go straight to your focus home whenever you open Still Focus.");
        permissionCard("01", "Website protection", "Filters website DNS locally for your focus rules. Allowed queries use network DNS by default. Optional AdGuard DNS in Settings sends DNS queries to AdGuard over HTTPS.",vpnAllowed(),this::requestVpn);
        if(!(strictPreferred() && ManagedProtection.ready(this))) {
            permissionCard("02", "Usage access", "Detects which app is open. Daily limits save only configured apps' daily usage totals on your phone; raw events are not saved or uploaded.",NormalAppBlocker.usageAllowed(this),()->openSpecialSetting(Settings.ACTION_USAGE_ACCESS_SETTINGS));
            permissionCard("03", "Display over other apps", "For normal app blocking: shows a labelled focus screen over your chosen apps. It does not read screen text or tap buttons.",Settings.canDrawOverlays(this),()->openSpecialSetting(Settings.ACTION_MANAGE_OVERLAY_PERMISSION));
            root.addView(button("Use strict device-owner setup instead",false,this::openStrictSetup));
            root.addView(text("Strict app suspension can replace Usage access and Display over apps after device-owner setup. VPN consent is still required.",13,MUTED,false));
        }
        LinearLayout note=card(root,0xffe8eee5);
        note.addView(text("Android says access is restricted?",17,INK,true));
        note.addView(text("For an APK you trust, Android provides: Settings > Apps > Still Focus > top-right menu > Allow restricted settings. Then return and grant Usage access. Keep Play Protect enabled.",14,MUTED,false));
        note.addView(button("Open Still Focus app info",false,()->openSpecialSetting(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)));
        root.addView(button("Check permissions and continue",true,()-> {
            observePermissions();
            if(!setup.needsSetup()) { ensureService(); navigate(Page.HOME); }
            else { render(); message("A little more setup","Grant the permissions marked Needed above, then return here. Android requires you to approve each permission yourself."); }
        }));
        if(Build.VERSION.SDK_INT>=33 && checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)
            root.addView(button("Allow status notifications (optional)",false,()->requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS},9)));
        root.addView(text("Normal mode needs no device-owner setup or reset. Strict mode needs computer-based provisioning, but no root.",13,MUTED,false));
    }
    private void permissionCard(String number,String title,String body,boolean granted,Runnable action) {
        LinearLayout box=card(root,Color.WHITE); box.addView(text(number+" / "+(granted?"ALLOWED":"NEEDED"),12,GREEN,true));
        box.addView(text(title,22,INK,true)); box.addView(text(body,14,MUTED,false));
        Button b=button(granted?"Permission granted":"Allow "+title.toLowerCase(Locale.ROOT),!granted,action);
        b.setEnabled(!granted); box.addView(b);
    }
    private void homePage() {
        heading("", "Focus", "A little space for what matters.");
        root.addView(button("Block applications",false,()->navigate(Page.APPS)));
        root.addView(button("Block websites",false,()->navigate(Page.WEBSITES)));
        activeBlockersButton=(ActiveDotButton)button("Active blockers",false,()->navigate(Page.SESSION));
        activeBlockersButton.setPaddingRelative(dp(36),dp(10),dp(36),dp(10));
        root.addView(activeBlockersButton);
        health=text("",13,MUTED,false); root.addView(health);
        if(PaymentMode.enabled(this)) root.addView(button("Restore paused protection",false,()->navigate(Page.PAYMENTS)));
    }
    private void blocksPage() { homePage(); }
    private void appsPage() {
        heading("HOME / APPLICATIONS", "Apps", "Choose apps and set a pause time.");
        modeCard();
        if(!strictPreferred() && !FocusAccessibilityService.active()) root.addView(button("App detection settings",false,()->navigate(Page.SETTINGS)));
        root.addView(text("Normal mode covers detected apps and returns Home immediately. Strict mode suspends apps through Android.",14,MUTED,false));
        if(!strictPreferred() && !NormalAppBlocker.ready(this)) {
            root.addView(button("Restore app permissions",false,()->navigate(Page.SETTINGS)));
        }
        LinearLayout selected=card(root,Color.WHITE);
        selected.addView(text(selectedApps.size()+" applications selected",21,INK,true));
        List<String> chosen=new ArrayList<>(selectedApps); chosen.sort(Comparator.comparing(this::appName,String.CASE_INSENSITIVE_ORDER));
        if(chosen.isEmpty()) selected.addView(text("Choose YouTube or any apps you want a break from.",15,MUTED,false));
        for(String pkg:chosen.subList(0,Math.min(5,chosen.size()))) {
            LinearLayout row=new LinearLayout(this); row.setGravity(Gravity.CENTER_VERTICAL); row.setPadding(0,dp(6),0,dp(6));
            row.addView(appIcon(pkg),new LinearLayout.LayoutParams(dp(36),dp(36)));
            TextView name=text(appName(pkg),16,INK,false); name.setPadding(dp(12),0,0,0); row.addView(name,new LinearLayout.LayoutParams(0,-2,1)); selected.addView(row);
        }
        if(chosen.size()>5) selected.addView(text("And "+(chosen.size()-5)+" more",14,MUTED,false));
        selected.addView(button("Choose applications",true,()->navigate(Page.APP_PICKER)));
        if(selectedApps.contains(ScreenRules.YOUTUBE) && youtubeShorts) {
            selected.addView(text("YouTube > Shorts only",15,GREEN,true));
            selected.addView(text("Regular videos stay available. Choose Whole application in the app picker to pause all of YouTube.",14,MUTED,false));
            screenProtectionCard();
        }
        durationCard(appDuration);
        addStartButton(true);
    }
    private void appPickerPage() {
        heading("HOME / APPLICATIONS / CHOOSE", "Choose apps", "Choose what you want to pause.");
        pickerDone=button("Done",true,()->navigate(Page.APPS)); footer.addView(pickerDone);
        LinearLayout selection=card(root,PAPER); selection.setElevation(0); selection.setPadding(0,0,0,0); appCount=text("",16,GREEN,true); selection.addView(appCount);
        EditText search=input("Search applications",android.text.InputType.TYPE_CLASS_TEXT); search.setSingleLine(true); selection.addView(search);
        LinearLayout list=column(); selection.addView(list);
        final int generation=renderGeneration;
        TextView loading=text("Loading apps...",14,MUTED,false); list.addView(loading);
        AppCatalog.load(this,Collections.emptySet(),entries-> {
            if(isDestroyed() || generation!=renderGeneration) return;
            list.removeView(loading);
            Set<String> paused=new HashSet<>(); for(TimedBlock block:store.active()) if(block.app) paused.add(block.target);
            appendAppChoices(entries,0,list,search,generation,paused);
        });
        search.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s,int start,int count,int after) { }
            public void onTextChanged(CharSequence s,int start,int before,int count) {
                String query=s.toString().toLowerCase(Locale.ROOT);
                for(int i=0;i<list.getChildCount();i++) { View v=list.getChildAt(i); if(v.getTag()!=null) v.setVisibility(v.getTag().toString().contains(query)?View.VISIBLE:View.GONE); }
            }
            public void afterTextChanged(Editable e) { }
        });
        updateAppCount();
    }
    private void appendAppChoices(List<AppCatalog.Entry> entries,int start,LinearLayout list,EditText search,int generation,Set<String> paused) {
        if(isDestroyed() || generation!=renderGeneration) return;
        int end=Math.min(start+8,entries.size());
        for(int index=start;index<end;index++) {
            AppCatalog.Entry entry=entries.get(index); String pkg=entry.packageName,label=entry.label;
            boolean youtube=ScreenRules.YOUTUBE.equals(pkg),already=paused.contains(pkg);
            if(already) selectedApps.remove(pkg);
            LinearLayout group=column(); group.setPadding(dp(12),dp(8),dp(12),dp(8)); group.setBackground(background(Color.WHITE,20));
            group.setTag((label+" "+pkg+(youtube?" shorts":"")).toLowerCase(Locale.ROOT));
            LinearLayout.LayoutParams groupParams=new LinearLayout.LayoutParams(-1,-2); groupParams.bottomMargin=dp(10); list.addView(group,groupParams);
            LinearLayout row=new LinearLayout(this); row.setGravity(Gravity.CENTER_VERTICAL); row.setMinimumHeight(dp(64)); group.addView(row);
            row.addView(appIcon(pkg),new LinearLayout.LayoutParams(dp(40),dp(40)));
            TextView name=text(label+(already?" (paused)":""),18,INK,true); name.setMaxLines(2); name.setPadding(dp(14),0,dp(6),0);
            row.addView(name,new LinearLayout.LayoutParams(0,-2,1));
            if(youtube) {
                TextView arrow=text("?",24,GREEN,true); arrow.setPadding(dp(10),0,dp(10),0); row.addView(arrow);
                LinearLayout children=column(); children.setPadding(dp(48),0,0,dp(4)); group.addView(children);
                RadioGroup choices=new RadioGroup(this); RadioButton whole=new RadioButton(this),shorts=new RadioButton(this);
                whole.setId(View.generateViewId()); shorts.setId(View.generateViewId()); whole.setText("Whole application"); shorts.setText("Shorts only");
                shorts.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_shorts,0,0,0); shorts.setCompoundDrawablePadding(dp(8));
                for(RadioButton choice:new RadioButton[]{whole,shorts}) {
                    choice.setButtonTintList(FocusUi.selection()); choice.setTextColor(INK); choice.setTextSize(16); choice.setMinHeight(dp(52)); choices.addView(choice); choice.setEnabled(!already);
                }
                children.addView(choices);
                children.addView(text("Keep regular videos available with Shorts only.",13,MUTED,false));
                if(selectedApps.contains(pkg)) choices.check(youtubeShorts?shorts.getId():whole.getId());
                if(already) choices.check(store.shortsRemaining()>0?shorts.getId():whole.getId());
                Button clear=button("Clear YouTube selection",false,()-> { choices.clearCheck(); selectedApps.remove(pkg); updateAppCount(); });
                clear.setVisibility(selectedApps.contains(pkg)?View.VISIBLE:View.GONE); children.addView(clear);
                Runnable style=()-> {
                    whole.setBackground(background(choices.getCheckedRadioButtonId()==whole.getId()?FocusUi.LAVENDER:Color.TRANSPARENT,14));
                    shorts.setBackground(background(choices.getCheckedRadioButtonId()==shorts.getId()?FocusUi.LAVENDER:Color.TRANSPARENT,14));
                    clear.setVisibility(selectedApps.contains(pkg) && !already?View.VISIBLE:View.GONE);
                };
                choices.setOnCheckedChangeListener((g,id)-> {
                    if(id==-1) selectedApps.remove(pkg);
                    else { selectedApps.add(pkg); youtubeShorts=id==shorts.getId(); }
                    style.run(); updateAppCount();
                }); style.run();
                row.setContentDescription("YouTube, expand or collapse blocking choices"); row.setFocusable(true);
                row.setOnClickListener(v-> { boolean visible=children.getVisibility()==View.VISIBLE; children.setVisibility(visible?View.GONE:View.VISIBLE); arrow.setText(visible?"?":"?"); });
            } else {
                CheckBox item=new CheckBox(this); item.setButtonTintList(FocusUi.selection()); item.setMinWidth(dp(48)); item.setMinHeight(dp(48)); item.setContentDescription("Select "+label);
                item.setEnabled(!already); item.setChecked(selectedApps.contains(pkg)); row.addView(item,new LinearLayout.LayoutParams(dp(48),-2));
                group.setBackground(background(item.isChecked()?FocusUi.LAVENDER:Color.WHITE,20));
                item.setOnCheckedChangeListener((v,on)-> { if(on) selectedApps.add(pkg); else selectedApps.remove(pkg); group.setBackground(background(on?FocusUi.LAVENDER:Color.WHITE,20)); updateAppCount(); });
                row.setOnClickListener(v->{ if(item.isEnabled()) item.toggle(); });
            }
        }
        String query=search.getText().toString().toLowerCase(Locale.ROOT);
        for(int i=start;i<end;i++) { View row=list.getChildAt(i); row.setVisibility(row.getTag().toString().contains(query)?View.VISIBLE:View.GONE); }
        if(end<entries.size()) list.postOnAnimation(()->appendAppChoices(entries,end,list,search,generation,paused));
        else if(entries.isEmpty()) list.addView(text("No launchable apps found on this profile.",14,MUTED,false));
        updateAppCount();
    }
    private void websitesPage() {
        heading("HOME / WEBSITES", "Block websites", "Choose your distracting domains. Their subdomains are included too.");
        modeCard();
        if(!vpnAllowed()) root.addView(button("Restore website protection permission",true,this::requestVpn));
        LinearLayout entry=card(root,Color.WHITE); entry.addView(text("Websites to pause",19,INK,true));
        websiteInput=input("youtube.com\ninstagram.com",android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE|android.text.InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        websiteInput.setId(R.id.website_input); websiteInput.setMinLines(4); websiteInput.setGravity(Gravity.TOP); websiteInput.setText(websiteDraft); entry.addView(websiteInput);
        websiteSuggestions(entry);
        entry.addView(text("One domain or URL per line. For the YouTube application, use Block apps instead.",13,MUTED,false));
        screenProtectionCard();
        durationCard(websiteDuration); root.addView(button("Browser setup instructions",false,this::browserHelp)); addStartButton(false);
    }
    private void websiteSuggestions(LinearLayout entry) {
        entry.addView(text("Quick picks",17,INK,true));
        String[] labels={"YouTube","Shorts only","Instagram","Reddit","Facebook","X / Twitter"};
        String[] sites={"youtube.com",ScreenRules.WEB_SHORTS,"instagram.com","reddit.com","facebook.com","x.com"};
        java.util.List<Button> picks=new ArrayList<>();
        Runnable refresh=()-> {
            Set<String> selected=new HashSet<>();
            for(String raw:websiteInput.getText().toString().split("[\\s,;]+")) {
                if(ScreenRules.webShorts(raw)) selected.add(ScreenRules.WEB_SHORTS);
                else { String host=ScreenRules.host(raw); if(!host.isEmpty()) selected.add(host); }
            }
            for(int i=0;i<picks.size();i++) {
                boolean chosen=selected.contains(sites[i]) || i==1 && websiteShorts;
                FocusUi.button(picks.get(i),chosen); picks.get(i).setSelected(chosen);
                picks.get(i).setContentDescription(labels[i]+(chosen?", selected":", tap to add"));
            }
        };
        for(int row=0;row<3;row++) {
            LinearLayout line=new LinearLayout(this);
            for(int col=0;col<2;col++) {
                final int index=row*2+col;
                Button pick=button(labels[index],false,()-> {
                    captureDraft();
                    boolean remove=picks.get(index).isSelected();
                    java.util.List<String> values=new ArrayList<>();
                    for(String raw:websiteDraft.split("[\\s,;]+")) {
                        if(raw.isEmpty()) continue;
                        boolean shorts=ScreenRules.webShorts(raw);
                        String host=ScreenRules.host(raw);
                        if(index==1 && (shorts || DomainRules.covers("youtube.com",host))) continue;
                        if(index==0 && shorts) continue;
                        if(!shorts && sites[index].equals(host)) continue;
                        values.add(raw);
                    }
                    if(index==1) websiteShorts=!remove;
                    else {
                        if(!remove) values.add(sites[index]);
                        if(index==0) websiteShorts=false;
                    }
                    websiteDraft=String.join("\n",values); websiteInput.setText(websiteDraft); refresh.run();
                });
                pick.setTextSize(14); picks.add(pick);
                LinearLayout.LayoutParams params=new LinearLayout.LayoutParams(0,-2,1);
                params.setMargins(dp(3),dp(4),dp(3),dp(4)); line.addView(pick,params);
            }
            entry.addView(line);
        }
        websiteInput.addTextChangedListener(new android.text.TextWatcher() {
            public void beforeTextChanged(CharSequence s,int start,int count,int after) { }
            public void onTextChanged(CharSequence s,int start,int before,int count) { refresh.run(); }
            public void afterTextChanged(android.text.Editable s) { }
        });
        refresh.run();
        entry.addView(text("Shorts only = YouTube Shorts in Brave, Beta and Nightly. Requires screen-aware blocking and an exposed page URL. Regular YouTube videos stay available unless you also block youtube.com. App Shorts has its own timer under Apps.",13,MUTED,false));
    }
    private void durationCard(String value) {
        LinearLayout box=card(root,Color.WHITE); box.addView(text("How long?",21,INK,true));
        int initial=25;
        try { initial=Math.max(0,Math.min(365*24*60,Integer.parseInt(value.trim()))); } catch(NumberFormatException ignored) { }
        durationHours=durationWheel("Hours",365*24);
        durationMinutes=durationWheel("Minutes",initial==365*24*60?0:59);
        durationHours.setValue(initial/60); durationMinutes.setValue(initial%60);
        LinearLayout wheels=new LinearLayout(this); wheels.setGravity(Gravity.CENTER);
        for(int i=0;i<2;i++) {
            LinearLayout column=new LinearLayout(this); column.setOrientation(LinearLayout.VERTICAL); column.setGravity(Gravity.CENTER);
            TextView label=text(i==0?"Hours":"Minutes",15,MUTED,true); label.setGravity(Gravity.CENTER); column.addView(label);
            column.addView(i==0?durationHours:durationMinutes,new LinearLayout.LayoutParams(-1,dp(156)));
            wheels.addView(column,new LinearLayout.LayoutParams(0,-2,1));
        }
        box.addView(wheels);
        TextView summary=text("",17,INK,true); summary.setGravity(Gravity.CENTER); box.addView(summary);
        LinearLayout presets=new LinearLayout(this);
        int[] values={2,25,60}; java.util.List<Button> buttons=new ArrayList<>();
        Runnable refreshDuration=()-> {
            durationMinutes.setMaxValue(durationHours.getValue()==365*24?0:59);
            int hours=durationHours.getValue(),minutes=durationMinutes.getValue(),total=hours*60+minutes;
            summary.setText(total==0?"Choose at least 1 minute":hours==0?minutes+" min":minutes==0?hours+" hr":hours+" hr "+minutes+" min");
            for(int i=0;i<buttons.size();i++) { boolean selected=total==values[i]; FocusUi.button(buttons.get(i),selected); buttons.get(i).setSelected(selected); }
            captureDraft();
        };
        for(int minutes:values) {
            Button button=button(minutes==60?"1 hr":minutes+" min",false,()-> {
                durationHours.setValue(minutes/60); durationMinutes.setMaxValue(59); durationMinutes.setValue(minutes%60); refreshDuration.run();
            });
            buttons.add(button); LinearLayout.LayoutParams params=new LinearLayout.LayoutParams(0,dp(48),1);
            params.setMargins(dp(3),dp(5),dp(3),dp(5)); presets.addView(button,params);
        }
        durationHours.setOnValueChangedListener((picker,oldValue,newValue)->refreshDuration.run());
        durationMinutes.setOnValueChangedListener((picker,oldValue,newValue)->refreshDuration.run());
        refreshDuration.run(); box.addView(presets);
        box.addView(text("Scroll hours and minutes. 1 minute to 365 days. No in-app cancel or shorten action.",13,MUTED,false));
    }
    private NumberPicker durationWheel(String label,int maximum) {
        NumberPicker picker=new NumberPicker(this); picker.setMinValue(0); picker.setMaxValue(maximum);
        picker.setWrapSelectorWheel(false); picker.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);
        picker.setContentDescription(label); picker.setFormatter(n->String.format(Locale.getDefault(),"%02d",n));
        if(Build.VERSION.SDK_INT>=29) { picker.setTextColor(INK); picker.setTextSize(24*getResources().getDisplayMetrics().scaledDensity); }
        // Keep the containing page from stealing vertical wheel gestures.
        picker.setOnTouchListener((view,event)-> {
            if(event.getActionMasked()==MotionEvent.ACTION_DOWN) view.getParent().requestDisallowInterceptTouchEvent(true);
            else if(event.getActionMasked()==MotionEvent.ACTION_UP || event.getActionMasked()==MotionEvent.ACTION_CANCEL) view.getParent().requestDisallowInterceptTouchEvent(false);
            return false;
        });
        return picker;
    }
    private void addStartButton(boolean apps) {
        root.addView(button(apps?"Start app block":"Start website block",true,()->startSession(apps)));
        if(store.remaining()>0) root.addView(button("View active blocks",false,()->navigate(Page.SESSION)));
    }
    private void startSession(boolean apps) {
        captureDraft();
        try {
            Set<String> domains=new HashSet<>(); Set<String> packages=apps?new HashSet<>(selectedApps):Collections.emptySet();
            boolean selectedWebShorts=!apps && websiteShorts;
            if(!apps) for(String raw:websiteDraft.split("[\\s,;]+")) if(!raw.isEmpty()) {
                if(ScreenRules.webShorts(raw)) selectedWebShorts=true;
                else domains.add(DomainRules.normalize(raw));
            }
            final boolean webShorts=selectedWebShorts;
            if(webShorts && !FocusScreenService.active()) throw new IllegalStateException("Enable Screen-aware blocking below before starting a web Shorts timer.");
            int minutes=Integer.parseInt(apps?appDuration.trim():websiteDuration.trim());
            if(minutes<1 || minutes>525600) throw new IllegalArgumentException("Enter 1 to 525600 minutes.");
            if(domains.isEmpty() && packages.isEmpty() && !webShorts) throw new IllegalArgumentException(apps?"Select at least one application.":"Enter at least one website.");
            final boolean strict=strictPreferred();
            final boolean shorts=apps && youtubeShorts && packages.contains(ScreenRules.YOUTUBE);
            if(shorts && strict) throw new IllegalArgumentException("Choose Normal mode for Shorts only. Strict mode suspends the whole application.");
            if(shorts && !FocusScreenService.active()) throw new IllegalStateException("Enable Screen-aware blocking first using the setup button below the YouTube choices.");
            SessionMode.validate(strict,ManagedProtection.ready(this),strict || NormalAppBlocker.ready(this),apps && !packages.isEmpty());
            if(!vpnAllowed()) { requestVpn(); return; }
            if(!FocusVpnService.running) { ensureService(); message("Protection is starting","Wait a moment for protection to start, then tap Start again."); return; }
            new AlertDialog.Builder(this).setTitle("Start a "+minutes+" minute "+(strict?"strict":"normal")+" block?")
                .setMessage((apps?packages.size()+" selected applications"+(shorts?" (YouTube Shorts only)":""):domains.size()+" whole websites"+(webShorts?" + YouTube Shorts (web)":""))+" will be blocked until the timer ends. There is no in-app cancellation. "+(strict?"Strict mode will block uninstall, force-stop, data clearing and clock changes for Still Focus, and suspend selected apps. The mode cannot be changed during the timer. Device-owner enrollment remains until you remove it after expiry. Root, debugging and resets remain outside this protection.":"Normal mode covers the detected app and immediately requests Home. It cannot stop background playback or hide Recents previews. Use Strict mode for Android app suspension. Android Settings can stop Still Focus. Existing device-owner enrollment, if present, must be removed before uninstall."))
                .setNegativeButton("Go back",null).setPositiveButton("Start session",(d,w)-> {
                    try { store.start(domains,packages,minutes,strict,shorts,webShorts); if(apps) selectedApps.clear(); else { websiteDraft=""; websiteShorts=false; if(websiteInput!=null) websiteInput.setText(""); } navigate(Page.SESSION); }
                    catch(RuntimeException e) { message("Could not start session",e.getMessage()); }
                }).show();
        } catch(RuntimeException e) { message("Check your selection",e instanceof NumberFormatException?"Enter a whole number of minutes.":e.getMessage()); }
    }
    private void sessionPage() {
        boolean active=store.remaining()>0;
        heading("HOME / ACTIVE BLOCKS",active?"Your focus is protected.":"You made space.",active?"Each app and website has its own countdown.":"Your session is complete. Choose your next focus space when you're ready.");
        LinearLayout panel=card(root,0xffdfe9d9); timer=text("",44,INK,true); panel.addView(timer);
        sessionSummary=text("",16,INK,false); panel.addView(sessionSummary);
        health=text("",14,MUTED,false); root.addView(health);
        root.addView(text(store.strict()?"Strict session: uninstall, force-stop and data clearing are locked until expiry. Afterwards, remove device-owner setup in Settings if you want to uninstall. Existing blocks keep their saved mode. New blocks can use either mode.":"Normal session: no in-app cancel button. Android Settings can stop Still Focus. Device-owner enrollment, if present, must be removed before uninstall.",13,MUTED,false));
        root.addView(button("Back to focus home",true,()->navigate(Page.HOME)));
    }
    private void settingsPage() {
        heading("HOME / SETTINGS", "Settings and help", "Permission controls stay here after your one-time setup. We won't reopen setup automatically.");
        if(PaymentMode.enabled(this)) root.addView(button("Restore paused protection",false,()->navigate(Page.PAYMENTS)));
        health=text("",14,MUTED,false); root.addView(health);
        modeCard();
        accessibilityCard();
        screenProtectionCard();
        root.addView(button("Ad blocking: "+(AdBlocking.enabled(this)?"On":"Off"),false,()-> {
            if(AdBlocking.enabled(this)) { AdBlocking.set(this,false); render(); return; }
            new AlertDialog.Builder(this).setTitle("Enable AdGuard DNS?")
                .setMessage("Allowed DNS queries will be sent over HTTPS to AdGuard's public ad-blocking resolver. This changes DNS inside Still Focus only. Phone services and known calling apps bypass it, except Truecaller, whose DNS is filtered. Calling domains and detected calls retain a safety bypass. If AdGuard is unavailable, other DNS requests fail until it returns or you turn ad blocking off. YouTube video ads cannot reliably be blocked by DNS. Call reliability depends on your carrier and phone: test mobile and Wi-Fi calls after enabling.")
                .setNegativeButton("Keep off",null).setPositiveButton("Enable",(d,w)-> { AdBlocking.set(this,true); render(); }).show();
        }));
        root.addView(text("Ad blocking includes Truecaller. If AdGuard is unavailable, ordinary DNS requests wait instead of using unfiltered DNS. Calling exceptions remain active.",13,MUTED,false));
        root.addView(button("Strict mode setup",false,this::openStrictSetup));
        LinearLayout permissions=card(root,Color.WHITE); permissions.addView(text("Permissions",22,INK,true));
        if(!vpnAllowed()) permissions.addView(button("Restore website protection",true,this::requestVpn));
        else permissions.addView(text("Website permission: allowed",15,GREEN,false));
        if(!NormalAppBlocker.usageAllowed(this)) permissions.addView(button("Restore Usage access",false,()->openSpecialSetting(Settings.ACTION_USAGE_ACCESS_SETTINGS)));
        else permissions.addView(text("Usage access: allowed",15,GREEN,false));
        if(!Settings.canDrawOverlays(this)) permissions.addView(button("Restore Display over apps",false,()->openSpecialSetting(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)));
        else permissions.addView(text("Display over apps: allowed",15,GREEN,false));
        permissions.addView(button("Restricted permission help",false,()->message("App was denied access?","Android may restrict special access for apps installed from an APK. For an app you trust: Settings > Apps > Still Focus > top-right menu > Allow restricted settings. Then return and enable Usage access or Display over other apps. Keep Play Protect enabled.")));
        permissions.addView(button("Open app info",false,()->openSpecialSetting(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)));
        root.addView(button("Browser setup and filtering limits",false,this::browserHelp));
        LinearLayout persistence=card(root,FocusUi.MINT);
        persistence.addView(text("Keep protection running",22,INK,true));
        persistence.addView(text("Website protection and your ad-blocking preference have no timer. Enable Always-on VPN and allow unrestricted battery use so Android can keep the service running.",15,MUTED,false));
        persistence.addView(text(FocusVpnService.alwaysOn?"Always-on VPN: enabled":"Always-on VPN: check Android settings",14,GREEN,true));
        boolean unrestricted=getSystemService(PowerManager.class).isIgnoringBatteryOptimizations(getPackageName());
        persistence.addView(text(unrestricted?"Battery optimization: exempt":"Battery optimization: active",14,MUTED,false));
        persistence.addView(button("Set up Always-on VPN",false,()->new AlertDialog.Builder(this).setTitle("Keep Still Focus connected")
            .setMessage("Open the gear beside Still Focus in Android VPN settings and enable Always-on VPN. Leave Block connections without VPN OFF: this DNS-only service does not carry normal internet or call traffic. No strict-mode setup is needed.")
            .setNegativeButton("Later",null).setPositiveButton("Open VPN settings",(d,w)->openSystemSetting(Settings.ACTION_VPN_SETTINGS)).show()));
        persistence.addView(button("Allow background battery use",false,()->new AlertDialog.Builder(this).setTitle("Battery settings")
            .setMessage("Find Still Focus, then choose Don't optimize if available. Also check App info > Battery > Unrestricted and allow background activity or Auto-start if your phone provides those options.")
            .setNegativeButton("Later",null).setPositiveButton("Open battery settings",(d,w)->openSystemSetting(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)).show()));
        persistence.addView(button("Open Still Focus app info",false,()->openSpecialSetting(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)));
        persistence.addView(button("Check browser and DNS settings",false,this::browserHelp));
        connectionDetails=text("",14,MUTED,false); connectionDetails.setTextIsSelectable(true); persistence.addView(connectionDetails);
        persistence.addView(button("Restart protection service",false,()-> {
            if(!vpnAllowed()) { requestVpn(); return; }
            try { store.enable(); startForegroundService(new Intent(this,FocusVpnService.class).setAction(FocusVpnService.ACTION_RECONNECT)); }
            catch(RuntimeException e) { message("Could not reconnect",e.getMessage()); }
        }));
        if(ManagedProtection.ready(this)) root.addView(button("Remove device-owner setup",false,()-> {
            if(store.remaining()>0 && store.strict()) { message("Strict session active","Wait for your existing strict session to finish."); return; }
            new AlertDialog.Builder(this).setTitle("Remove device-owner setup?").setMessage("This removes Still Focus as device owner without resetting your phone. Future strict sessions will require setup again. Normal mode remains available.")
                .setNegativeButton("Keep",null).setPositiveButton("Release",(d,w)-> { try { ManagedProtection.releaseOwnership(this); render(); } catch(RuntimeException e) { message("Could not release",e.getMessage()); } }).show();
        }));
        root.addView(text("Still Focus "+versionName()+" / Normal and strict modes\nNo account. No browsing history stored. Optional AdGuard DNS receives DNS queries.",13,MUTED,false));
    }
    private void paymentPage() {
        heading("", "Restore protection", "Finish a pause enabled in an earlier version.");
        LinearLayout panel=card(root,Color.WHITE);
        paymentStatus=text("",17,INK,true); panel.addView(paymentStatus);
        panel.addView(text("Normal app blocking, daily-limit enforcement and Shorts/page checks pause. Timers keep counting down and daily usage still counts. DNS filtering continues; existing Strict-mode suspensions remain active.",15,MUTED,false));
        panel.addView(text("After paying, finish this mode and manually enable your services in Android Accessibility settings. Still Focus cannot switch them back on automatically. Your payment app decides when its security alert clears.",15,MUTED,false));
        panel.addView(button("Check Android Accessibility settings",false,()->openSystemSetting(Settings.ACTION_ACCESSIBILITY_SETTINGS)));
        if(PaymentMode.enabled(this)) panel.addView(button("Finished paying - restore blocking",false,()-> {
            try { PaymentMode.finish(this); render(); openSystemSetting(Settings.ACTION_ACCESSIBILITY_SETTINGS); }
            catch(RuntimeException e) { message("Could not restore",e.getMessage()); }
        }));
        root.addView(text("If the payment app also requests display-over-apps access to be disabled, use the button below. Restore that permission afterwards for normal app blocking. This mode does not hide permissions from payment apps.",14,MUTED,false));
        root.addView(button("Display-over-apps settings",false,()->openSpecialSetting(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)));
    }
    private void screenProtectionCard() {
        if(PaymentMode.enabled(this)) { root.addView(button("Finish payment mode to restore screen checks",false,()->navigate(Page.PAYMENTS))); return; }
        LinearLayout panel=card(root,FocusUi.LAVENDER);
        panel.addView(text("Screen-aware blocking",19,INK,true));
        panel.addView(text(FocusScreenService.active()?"On. Checks Brave pages and YouTube Shorts.":"Checks Brave pages and YouTube Shorts when you use them.",14,MUTED,false));
        panel.addView(text(FocusScreenService.status,13,MUTED,false));
        panel.addView(button(FocusScreenService.active()?"Turn off screen-aware blocking":"Set up screen-aware blocking",false,()-> {
            if(FocusScreenService.active()) { FocusScreenService.turnOff(this); render(); return; }
            new AlertDialog.Builder(this).setTitle("Block Shorts and web pages?")
                .setMessage("Uses Accessibility to read YouTube controls and Brave addresses, including private tabs. It covers blocked pages, pauses web Shorts when possible, and exits Shorts in YouTube. It skips passwords and never saves or shares screen data.\n\nNext: turn on Still Focus screen-aware blocking. You can turn it off anytime.")
                .setNegativeButton("Not now",null).setPositiveButton("Agree and continue",(d,w)-> {
                    FocusScreenService.consent(this); openSystemSetting(Settings.ACTION_ACCESSIBILITY_SETTINGS);
                }).show();
        }));
    }
    private void accessibilityCard() {
        if(PaymentMode.enabled(this)) { root.addView(button("Finish payment mode to restore app detection",false,()->navigate(Page.PAYMENTS))); return; }
        LinearLayout card=card(root,FocusUi.LAVENDER);
        card.addView(text("Responsive app blocking",19,INK,true));
        card.addView(text(FocusAccessibilityService.active()?"On. Paused apps are detected as they open.":"Helps block paused apps faster, including from Recent apps.",14,MUTED,false));
        card.addView(button(FocusAccessibilityService.active()?"Turn off Accessibility blocking":"Set up Accessibility blocking",false,()-> {
            if(FocusAccessibilityService.active()) { FocusAccessibilityService.turnOff(this); render(); return; }
            new AlertDialog.Builder(this).setTitle("Block paused apps faster?")
                .setMessage("Uses Accessibility to detect which app opens. If that app is paused, Still Focus covers it and returns you Home. It does not read screen text or passwords, or save or share app activity.\n\nOn the next page, turn on Still Focus app blocking. You can turn it off anytime.")
                .setNegativeButton("Not now",null).setPositiveButton("Agree and continue",(d,w)-> {
                    FocusAccessibilityService.consent(this,true); openSystemSetting(Settings.ACTION_ACCESSIBILITY_SETTINGS);
                }).show();
        }));
    }
    private boolean strictPreferred() { return ui.getBoolean("strictPreferred",false); }
    private void modeCard() {
        boolean active=store.remaining()>0;
        boolean strict=strictPreferred();
        root.addView(button("Mode: "+(strict?"Strict":"Normal"),false,()-> {

            new AlertDialog.Builder(this).setTitle("Protection mode")
                .setSingleChoiceItems(new String[]{"Normal","Strict - device owner required"},strict?1:0,(dialog,which)-> {
                    captureDraft(); ui.edit().putBoolean("strictPreferred",which==1).apply(); dialog.dismiss(); render();
                }).setNegativeButton("Close",null).show();
        }));
        if(strict && !ManagedProtection.ready(this)) root.addView(button("Set up strict mode",false,this::openStrictSetup));
    }
    private void openStrictSetup() { strictParent=page; navigate(Page.STRICT_SETUP); }
    private void strictSetupPage() {
        heading("STRICT MODE / SETUP", "Protection that stays.", "No root required. Android device-owner management provides the uninstall protection.");
        LinearLayout state=card(root,0xffe8eee5);
        state.addView(text(ManagedProtection.ready(this)?"Device-owner setup complete":"Device-owner setup required",22,INK,true));
        state.addView(text("Strict sessions block uninstall, force-stop, clearing Still Focus data and clock changes during the timer. Selected apps are suspended. Settings cannot switch an active timer to normal mode.",15,MUTED,false));
        LinearLayout steps=card(root,Color.WHITE);
        steps.addView(text("Set up with a computer",23,INK,true));
        steps.addView(text("1. Back up your phone. Use a dedicated test phone or emulator when possible. Provisioning an everyday phone may require a factory reset; this app never resets it.\n\n2. Install Still Focus on an Android 11+ phone without accounts, an existing device owner or a work profile. Android decides whether provisioning is allowed.\n\n3. Install Android SDK Platform Tools on your computer. Enable USB debugging, connect the phone and approve your own computer.\n\n4. Run the command below. If Android refuses, read its error; do not bypass its checks.",15,MUTED,false));
        String command="adb shell dpm set-device-owner com.stillfocus.app/.FocusAdminReceiver";
        TextView code=text(command,15,INK,true); code.setTextIsSelectable(true); steps.addView(code);
        steps.addView(button("Copy setup command",false,()-> {
            getSystemService(android.content.ClipboardManager.class).setPrimaryClip(ClipData.newPlainText("Still Focus device-owner setup",command));
            Toast.makeText(this,"Setup command copied",Toast.LENGTH_SHORT).show();
        }));
        steps.addView(text("5. Reopen Still Focus. Confirm Device owner: ready, choose Strict mode, then start a short test session. Do not turn off Play Protect. Enterprise approval or developer review may be needed if Google blocks provisioning.",15,MUTED,false));
        root.addView(button("Check device-owner status",true,()-> {
            if(ManagedProtection.ready(this)) message("Strict mode is ready","You can now choose Strict when starting an app or website block.");
            else message("Setup not complete","Still Focus is not the device owner. Provision the phone from your computer first. Legacy device-admin activation is not enough.");
            render();
        }));
        root.addView(text("After expiry, timer policies are released. Device-owner enrollment remains: open Settings and help > Remove device-owner setup before uninstalling. That action is blocked during a strict timer. Battery sleep may delay release; reopening Still Focus checks expiry immediately.\n\nThis protects normal Android app controls, not rooted devices, privileged debugging, device resets, or every network bypass. This DNS-only VPN does not support full-traffic lockdown.",14,MUTED,false));
    }
    private void requestVpn() {
        Intent intent=VpnService.prepare(this);
        if(intent!=null) startActivityForResult(intent,7); else { ensureService(); permissionReturn(); }
    }
    private void ensureService() {
        if(!vpnAllowed() || FocusVpnService.running || serviceRequested && SystemClock.elapsedRealtime()-lastServiceRequest<5000) return;
        store.enable(); serviceRequested=true; lastServiceRequest=SystemClock.elapsedRealtime();
        try { startForegroundService(new Intent(this,FocusVpnService.class)); }
        catch(RuntimeException e) { serviceRequested=false; message("Could not start protection",e.getMessage()); }
    }
    private void openSystemSetting(String action) {
        try { startActivity(new Intent(action)); }
        catch(ActivityNotFoundException e) { message("Open Android Settings","Search phone Settings for VPN or Battery and select Still Focus."); }
    }
    private void openSpecialSetting(String action) {
        Intent intent=new Intent(action,android.net.Uri.parse("package:"+getPackageName()));
        try { startActivity(intent); }
        catch(ActivityNotFoundException missing) {
            try { startActivity(new Intent(action)); }
            catch(ActivityNotFoundException absent) { message("Open Android Settings","Find Still Focus under Special app access and enable the selected permission."); }
        }
    }
    private void permissionReturn() {
        boolean finished=observePermissions();
        if(finished && page==Page.SETUP) navigate(Page.HOME);
        else if(!renderedPermissions.equals(permissionState()) && (page==Page.SETUP || page==Page.SETTINGS || page==Page.APPS || page==Page.WEBSITES || page==Page.STRICT_SETUP)) { captureDraft(); render(); }
        if(!setup.needsSetup() || store.enabled()) ensureService();
    }
    private String permissionState() {
        return ""+vpnAllowed()+NormalAppBlocker.usageAllowed(this)+Settings.canDrawOverlays(this)+FocusAccessibilityService.active()+FocusScreenService.active()+PaymentMode.enabled(this)+strictPreferred()+ManagedProtection.ready(this)+ScheduleReceiver.exactAllowed(this)
            +getSystemService(PowerManager.class).isIgnoringBatteryOptimizations(getPackageName())+FocusVpnService.alwaysOn;
    }
    private void browserHelp() {
        message("Website filtering","Chrome and Brave (including Nightly): turn off Use secure DNS in browser privacy settings. Android: set Private DNS to Off for this DNS-only mode. Close and reopen browsers after starting a block; cached DNS and existing connections can remain accessible. For Brave variants, enable Screen-aware blocking as a second layer. Matching pages show a page cover, leaving the address bar and tab controls usable; the browser is not sent Home. It requires a visible, unfocused address bar; private/fullscreen pages may hide it.\n\nWebsite filtering stays active with protection. Encrypted DNS, proxies, direct IP access and mixed-content sites can bypass domain lists. This is not a full-traffic or encrypted VPN.\n\nAlways-on VPN can help recovery. Leave Block connections without VPN OFF: this DNS-only VPN cannot carry normal internet traffic.\n\nApp focus screens block interaction, not background audio or picture-in-picture. Normal mode cannot prevent Android Settings from stopping or uninstalling Still Focus.");
    }
    private void message(String title,String body) { new AlertDialog.Builder(this).setTitle(title).setMessage(body).setPositiveButton("Got it",null).show(); }
    private String appName(String pkg) {
        try { return getPackageManager().getApplicationLabel(getPackageManager().getApplicationInfo(pkg,0)).toString(); }
        catch(PackageManager.NameNotFoundException e) { return "App no longer installed"; }
    }
    private String versionName() {
        try { return getPackageManager().getPackageInfo(getPackageName(),0).versionName; }
        catch(PackageManager.NameNotFoundException e) { return ""; }
    }
    private ImageView appIcon(String pkg) {
        ImageView icon=new ImageView(this); icon.setScaleType(ImageView.ScaleType.FIT_CENTER);
        icon.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        AppCatalog.icon(icon,pkg);
        return icon;
    }
    private void updateAppCount() {
        if(appCount!=null) appCount.setText(selectedApps.size()+" apps selected");
        if(pickerDone!=null) pickerDone.setText(selectedApps.isEmpty()?"Done":"Done ("+selectedApps.size()+" selected)");
    }
    private void updateLiveViews() {
        if(activeBlockersButton!=null) activeBlockersButton.setActive(!store.active().isEmpty());
        if(paymentStatus!=null) {
            int enabled=PaymentMode.enabledServices(this);
            paymentStatus.setText(PaymentMode.enabled(this)?(enabled==0?"Both Still Focus Accessibility services are off.":enabled+" Still Focus service(s) still enabled. Turn both off in Android settings.")+(FocusVpnService.running?"\nDNS filtering is running.":"\nDNS filtering needs attention."):"Payment mode is off. Enable your chosen services in Android Accessibility settings to restore responsive app and Shorts blocking.");
        }
        if(connectionDetails!=null) connectionDetails.setText(FocusVpnService.diagnostics(this));
        long left=store.remaining(),seconds=(left+999)/1000;
        if(timer!=null) timer.setText(store.active().size()+" active blocks");
        if(sessionSummary!=null) {
            List<String> names=new ArrayList<>();
            for(TimedBlock t:store.active()) {
                long secondsLeft=(store.remaining(t)+999)/1000;
                names.add((t.app?appName(t.target)+(t.shorts?" Shorts"+(FocusScreenService.active()?"":" (screen-aware blocking is off)"):""):t.shorts?"YouTube Shorts (web)"+(FocusScreenService.active()?"":" (screen-aware blocking is off)"):t.target)+" - "+String.format(Locale.getDefault(),"%02d:%02d:%02d",secondsLeft/3600,secondsLeft/60%60,secondsLeft%60)+" ("+(t.strict?"Strict":"Normal")+")");
            }
            sessionSummary.setText(names.isEmpty()?"No active blockers. Start a pause above.":String.join("\n\n",names));
        }
        if(health!=null) {
            boolean strict=left>0?store.strict():strictPreferred();
            String mode;
            if(strict && left>0) mode=ManagedProtection.enforced(this)?"Strict mode: uninstall locked":"Strict mode: protection needs attention";
            else if(strict) mode=ManagedProtection.ready(this)?"Strict mode: ready":"Strict mode: device-owner setup required";
            else mode="Normal mode: "+(NormalAppBlocker.ready(this)?"app blocking ready":"app permission needed - see Settings");
            health.setText(PaymentMode.enabled(this)?"Payment mode: app and Shorts blocking paused; timers continue":page==Page.HOME?(FocusVpnService.running?"Protection on":"Protection off"):FocusVpnService.status+"\n"+mode);
        }
    }
    private void refresh() {
        if(store.enabled() && !FocusVpnService.running) ensureService();
        if(observePermissions() && page==Page.SETUP) { ensureService(); navigate(Page.HOME); return; }
        try { ManagedProtection.reconcile(this); } catch(RuntimeException ignored) { }
        if(sessionWasActive && store.remaining()==0) { captureDraft(); render(); return; }
        updateLiveViews();
    }
    @Override protected void onActivityResult(int request,int result,Intent data) {
        super.onActivityResult(request,result,data);
        if(request==7 && result==RESULT_OK) { serviceRequested=false; ensureService(); permissionReturn(); }
    }
    private void handleBack() {
        if(page==Page.HOME || page==Page.SETUP) moveTaskToBack(true);
        else navigate(page==Page.APP_PICKER?Page.APPS:page==Page.STRICT_SETUP?strictParent:Page.HOME);
    }
    @Override public void onBackPressed() { handleBack(); }
    @Override protected void onResume() { super.onResume(); permissionReturn(); handler.removeCallbacks(ticker); handler.postDelayed(ticker,220); }
    @Override protected void onPause() { captureDraft(); handler.removeCallbacks(ticker); swipe.reset(); super.onPause(); }
}
