package com.stillfocus.app;

import android.app.*;
import android.content.*;
import android.net.*;
import android.os.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

/** Persistent split-route DNS VPN. Timers never control the tunnel lifetime. */
public final class FocusVpnService extends VpnService {
    public static final String ACTION_RECONNECT="com.stillfocus.app.RECONNECT";
    public static volatile boolean running,alwaysOn;
    public static volatile String status="Protection needs VPN permission";
    private static volatile long connectedAt,lastQuery,lastAdSuccess,lastAdFailure;
    private static final AtomicLong queries=new AtomicLong();
    private volatile boolean closing,revoked;
    private volatile Tunnel current;
    private ExecutorService reader;
    private ThreadPoolExecutor workers;
    private ScheduledExecutorService maintenance;
    private final Object retrySignal=new Object();
    private ConnectivityManager.NetworkCallback networkCallback;
    private FocusStore store;
    private NormalAppBlocker appBlocker;
    private String lastNotice="";
    private long lastHeartbeat;
    private String previousExit="";
    private static final class Tunnel {
        final ParcelFileDescriptor descriptor;
        final FileOutputStream output;
        boolean closed;
        Tunnel(ParcelFileDescriptor descriptor) { this.descriptor=descriptor; output=new FileOutputStream(descriptor.getFileDescriptor()); }
    }
    @Override public void onCreate() {
        super.onCreate(); CallSafety.refresh(); store=new FocusStore(this);
        getSystemService(NotificationManager.class).createNotificationChannel(new NotificationChannel("protection","Website protection",NotificationManager.IMPORTANCE_LOW));
        startForeground(1,notification("Starting protection"));
        previousExit=ProtectionRecovery.lastExit(this);
        try { ProtectionRecovery.schedule(this); } catch(RuntimeException e) { ProtectionRecovery.record(this,"Recovery check unavailable"); }
        appBlocker=new NormalAppBlocker(this); appBlocker.start();
        networkCallback=new ConnectivityManager.NetworkCallback() {
            @Override public void onAvailable(Network n) { wakeRecovery(); }
            @Override public void onLost(Network n) { wakeRecovery(); }
        };
        try { getSystemService(ConnectivityManager.class).registerNetworkCallback(new NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET).addCapability(NetworkCapabilities.NET_CAPABILITY_NOT_VPN).build(),networkCallback); }
        catch(RuntimeException ignored) { networkCallback=null; }
    }
    private Notification notification(String text) {
        return new Notification.Builder(this,"protection").setSmallIcon(R.drawable.ic_shield)
            .setContentTitle("Still Focus").setContentText(text).setOngoing(true)
            .setContentIntent(PendingIntent.getActivity(this,0,new Intent(this,MainActivity.class),PendingIntent.FLAG_IMMUTABLE)).build();
    }
    @Override public synchronized int onStartCommand(Intent intent,int flags,int id) {
        if(reader!=null) {
            if(intent!=null && ACTION_RECONNECT.equals(intent.getAction())) { closeTunnel(current); wakeRecovery(); }
            return START_STICKY;
        }
        reader=Executors.newSingleThreadExecutor();
        workers=new ThreadPoolExecutor(4,8,30,TimeUnit.SECONDS,new ArrayBlockingQueue<>(128),new ThreadPoolExecutor.AbortPolicy());
        maintenance=Executors.newSingleThreadScheduledExecutor();
        try { ScheduleReceiver.schedule(this); } catch(RuntimeException e) { ProtectionRecovery.record(this,"Schedule alarm unavailable"); }
        try { SessionReceiver.schedule(this); } catch(RuntimeException e) { ProtectionRecovery.record(this,"Timer alarm unavailable"); }
        maintenance.scheduleWithFixedDelay(()-> {
            try { ManagedProtection.reconcile(this); } catch(RuntimeException ignored) { }
            if(SystemClock.elapsedRealtime()-lastHeartbeat>=60000 || lastHeartbeat==0) {
                lastHeartbeat=SystemClock.elapsedRealtime();
                getSharedPreferences("protectionHealth",0).edit().putLong("heartbeat",System.currentTimeMillis()).putString("processExit",previousExit).apply();
            }
            try {
                if(Build.VERSION.SDK_INT>=29) alwaysOn=isAlwaysOn();
                String notice=PaymentMode.enabled(this)?(running?"Payment mode - DNS on; app and Shorts blocking off":"Payment mode - DNS needs attention"):!running?status:AdBlocking.enabled(this) && lastAdFailure>lastAdSuccess && !CallSafety.callAudio(this)?"AdGuard unavailable - filtered requests are blocked":"Protection connected - no time limit";
                if(!notice.equals(lastNotice)) { lastNotice=notice; getSystemService(NotificationManager.class).notify(1,notification(notice)); }
            } catch(RuntimeException ignored) { }
        },0,1,TimeUnit.SECONDS);
        reader.execute(this::runTunnel); return START_STICKY;
    }
    private void wakeRecovery() { synchronized(retrySignal) { retrySignal.notifyAll(); } }
    private void reportIssue(String reason) {
        status=reason;
        getSharedPreferences("protectionHealth",0).edit().putString("lastIssue",reason).putLong("issueAt",System.currentTimeMillis()).apply();
    }
    private void runTunnel() {
        RecoveryBackoff retry=new RecoveryBackoff();
        while(!closing && !revoked) {
            Tunnel connection=null; long started=0;
            try {
                if(VpnService.prepare(this)!=null) { revoked=true; reportIssue("VPN permission needed. Open Settings to restore it."); stopSelf(); return; }
                status="Connecting protection";
                WebsiteFilter.load(this);
                if(closing) return;
                Builder builder=new Builder().setSession("Still Focus - local DNS")
                    .setMtu(1500).addAddress("10.111.0.1",32).addDnsServer("10.111.0.2")
                    .addRoute("10.111.0.2",32).allowFamily(android.system.OsConstants.AF_INET6).setBlocking(true);
                for(String pkg:DnsPolicy.vpnExemptPackages(CallSafety.packages(this))) {
                    try { builder.addDisallowedApplication(pkg); } catch(android.content.pm.PackageManager.NameNotFoundException ignored) { }
                }
                ParcelFileDescriptor established=builder.establish();
                if(established==null) throw new IOException("VPN unavailable");
                connection=new Tunnel(established);
                synchronized(this) {
                    if(closing || revoked) { established.close(); return; }
                    current=connection; connectedAt=started=SystemClock.elapsedRealtime(); running=true;
                }
                status="Protection connected";
                getSystemService(NotificationManager.class).notify(1,notification("Protection connected - no time limit"));
                FileInputStream input=new FileInputStream(established.getFileDescriptor()); byte[] buffer=new byte[32767];
                while(!closing && current==connection) {
                    int n=input.read(buffer); if(n<0) throw new IOException("Tunnel ended");
                    DnsPacket packet=DnsPacket.parse(buffer,n); if(packet==null) continue;
                    queries.incrementAndGet(); lastQuery=SystemClock.elapsedRealtime();
                    if(WebsiteFilter.blocked(this,packet.host)) write(connection,packet,packet.error(3));
                    else {
                        final Tunnel target=connection;
                        try { workers.execute(()->resolve(target,packet)); }
                        catch(RejectedExecutionException busy) { write(target,packet,packet.error(2)); }
                    }
                }
            } catch(Exception e) {
                if(!closing && !revoked) reportIssue("Connection interrupted; reconnecting automatically");
            } finally { closeTunnel(connection); }
            if(closing || revoked) return;
            status="Reconnecting protection";
            getSystemService(NotificationManager.class).notify(1,notification(status));
            long delay=retry.next(started==0?0:SystemClock.elapsedRealtime()-started);
            synchronized(retrySignal) {
                try { if(!closing && !revoked) retrySignal.wait(delay); }
                catch(InterruptedException interrupted) { Thread.currentThread().interrupt(); return; }
            }
        }
    }
    private List<Network> networks() {
        ConnectivityManager cm=getSystemService(ConnectivityManager.class); List<Network> result=new ArrayList<>();
        for(Network n:cm.getAllNetworks()) {
            NetworkCapabilities caps=cm.getNetworkCapabilities(n);
            if(caps!=null && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) && !caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) result.add(n);
        }
        result.sort(Comparator.comparingInt(n->{ NetworkCapabilities caps=cm.getNetworkCapabilities(n);
            return caps==null?9:(caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)?0:2)+(caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)?0:1); }));
        return result;
    }
    private void resolve(Tunnel target,DnsPacket packet) {
        if(closing || current!=target) return;
        byte[] answer=null;
        ConnectivityManager cm=getSystemService(ConnectivityManager.class);
        boolean useAds=!DnsPolicy.mayUseNetworkDns(AdBlocking.enabled(this),CallSafety.callAudio(this),packet.host);
        try {
            List<Network> available=networks();
            // Required filtered requests never fall through to an unfiltered resolver.
            if(useAds) {
                for(Network network:available) {
                    if(closing || current!=target) return;
                    answer=AdBlocking.resolve(network,packet); if(answer!=null) { lastAdSuccess=SystemClock.elapsedRealtime(); break; }
                }
                if(answer==null) lastAdFailure=SystemClock.elapsedRealtime();
            }
            if(answer==null && !useAds) outer: for(Network network:available) {
                LinkProperties link=cm.getLinkProperties(network); if(link==null) continue;
                for(InetAddress server:link.getDnsServers()) {
                    if(closing || current!=target) return;
                    try(DatagramSocket socket=new DatagramSocket()) {
                        if(!protect(socket)) throw new IOException("Cannot protect socket");
                        network.bindSocket(socket); socket.connect(server,53); socket.setSoTimeout(2000);
                        socket.send(new DatagramPacket(packet.query,packet.query.length));
                        byte[] buffer=new byte[4096]; DatagramPacket response=new DatagramPacket(buffer,buffer.length);
                        socket.receive(response); byte[] candidate=Arrays.copyOf(buffer,response.getLength());
                        if(!packet.validResponse(candidate)) continue;
                        answer=candidate; break outer;
                    } catch(IOException ignored) { }
                }
            }
        } catch(RuntimeException failure) { /* Failed queries receive SERVFAIL, not a dead worker. */ }
        if(!CallDomains.carrier(packet.host) && store.blocks(packet.host)) answer=packet.error(3);
        write(target,packet,answer==null?packet.error(2):answer);
    }
    private void write(Tunnel target,DnsPacket packet,byte[] dns) {
        boolean failed=false;
        synchronized(target) {
            if(closing || current!=target || target.closed) return;
            try { target.output.write(packet.response(dns)); } catch(IOException e) { failed=true; }
        }
        if(failed && current==target) { reportIssue("Connection write failed; reconnecting automatically"); closeTunnel(target); wakeRecovery(); }
    }
    private synchronized void closeTunnel(Tunnel target) {
        if(target==null) return;
        synchronized(target) {
            if(target.closed) return;
            target.closed=true;
            if(current==target) { current=null; running=false; }
            try { target.descriptor.close(); } catch(IOException ignored) { }
        }
    }
    public static String diagnostics(Context c) {
        long now=SystemClock.elapsedRealtime();
        String connection=running?"Connected for "+Math.max(0,(now-connectedAt)/60000)+" minutes":status;
        String traffic=lastQuery==0?"No DNS requests received yet. Check browser Secure DNS and Android Private DNS.":queries.get()+" DNS requests received; last request "+Math.max(0,(now-lastQuery)/1000)+" seconds ago";
        String ads=!AdBlocking.enabled(c)?"Ad blocking: off":CallSafety.callAudio(c)?"Ad blocking: call bypass active":lastAdFailure>lastAdSuccess?"Ad blocking: AdGuard unavailable; filtered DNS requests are waiting. Turn off ad blocking to restore ordinary DNS.":lastAdSuccess>0?"Ad blocking: AdGuard responding":"Ad blocking: enabled; waiting for a DNS request";
        android.content.SharedPreferences health=c.getSharedPreferences("protectionHealth",0);
        long issueAt=health.getLong("issueAt",0);
        long boundary=health.getLong("nextSchedule",0);
        String schedule=boundary==0?"No enabled schedule boundary":"Next schedule boundary: "+android.text.format.DateFormat.format("MMM d, HH:mm",boundary);
        String recovery=health.getString("recovery","No recovery attempt recorded");
        long recoveryAt=health.getLong("recoveryAt",0),alarmAt=health.getLong("lastScheduleAlarm",0),beat=health.getLong("heartbeat",0);
        if(recoveryAt>0) recovery+=" ("+android.text.format.DateFormat.format("MMM d, HH:mm",recoveryAt)+")";
        schedule+="\nPrecise schedule alarms: "+(ScheduleReceiver.exactAllowed(c)?"allowed":"not allowed; sleep can delay boundaries");
        if(alarmAt>0) schedule+="\nLast schedule alarm: "+android.text.format.DateFormat.format("MMM d, HH:mm",alarmAt);
        if(beat>0) schedule+="\nLast service heartbeat: "+android.text.format.DateFormat.format("MMM d, HH:mm",beat);
        schedule+="\nAccessibility: "+(FocusAccessibilityService.active()?"connected":"not connected; usage polling requires running service");
        return connection+"\nScreen-aware blocking: "+FocusScreenService.status+"\n"+schedule+"\nRecovery: "+recovery+health.getString("processExit","")+"\n"+traffic+"\n"+ads+"\nTruecaller: included in DNS filtering; protected from app timers."+"\n"+dnsConfiguration(c)+(issueAt==0?"":"\nLast interruption: "+android.text.format.DateFormat.format("MMM d, HH:mm",issueAt)+" - "+health.getString("lastIssue",""));
    }
    public static String dnsConfiguration(Context c) {
        ConnectivityManager cm=c.getSystemService(ConnectivityManager.class);
        for(Network network:cm.getAllNetworks()) {
            NetworkCapabilities caps=cm.getNetworkCapabilities(network); LinkProperties link=cm.getLinkProperties(network);
            if(caps!=null && !caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN) && link!=null && Build.VERSION.SDK_INT>=28 && link.isPrivateDnsActive()) return "Android Private DNS is active: set it to Off for this filter. Browser Secure DNS must also be Off.";
        }
        return "Browser Secure DNS must be Off. Encrypted DNS and cached connections can bypass website rules.";
    }
    @Override public void onRevoke() { revoked=true; reportIssue("VPN access was revoked; protection is off"); closeTunnel(current); wakeRecovery(); stopSelf(); }
    @Override public synchronized void onDestroy() {
        closing=true; running=false; alwaysOn=false;
        if(!revoked) status="Protection stopped. Open Still Focus to reconnect.";
        if(appBlocker!=null) appBlocker.stop();
        closeTunnel(current); wakeRecovery();
        if(networkCallback!=null) try { getSystemService(ConnectivityManager.class).unregisterNetworkCallback(networkCallback); } catch(RuntimeException ignored) { }
        if(reader!=null) reader.shutdownNow(); if(workers!=null) workers.shutdownNow();
        if(maintenance!=null) maintenance.shutdownNow();
        super.onDestroy();
    }
}
