import com.stillfocus.app.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

public class CoreTests {
    private static int checks;
    private static void check(boolean value,String label) { checks++; if(!value) throw new AssertionError(label); }
    private static void invalid(String value) {
        try { DomainRules.normalize(value); throw new AssertionError(value); } catch(IllegalArgumentException expected) { checks++; }
    }
    private static byte[] query(String host) throws Exception {
        ByteArrayOutputStream b=new ByteArrayOutputStream();
        b.write(new byte[]{0x12,0x34,1,0,0,1,0,0,0,0,0,0});
        for(String s:host.split("\\.")) { b.write(s.length()); b.write(s.getBytes(StandardCharsets.US_ASCII)); }
        b.write(new byte[]{0,0,1,0,1}); return b.toByteArray();
    }
    private static byte[] udp(byte[] dns) {
        byte[] b=new byte[28+dns.length]; b[0]=0x45; b[2]=(byte)(b.length>>8); b[3]=(byte)b.length; b[8]=64; b[9]=17;
        b[12]=10; b[15]=1; b[16]=10; b[19]=2; b[20]=0x12; b[21]=0x34; b[23]=53;
        b[24]=(byte)((dns.length+8)>>8); b[25]=(byte)(dns.length+8); System.arraycopy(dns,0,b,28,dns.length); return b;
    }
    public static void main(String[] args) throws Exception {
        DailyUsageCounter usage=new DailyUsageCounter(1000,11000);
        usage.resume(500,"youtube","one"); usage.pause(3000,"youtube","one");
        usage.resume(4000,"youtube","two"); usage.pause(4500,"youtube","one");
        usage.screen(6000,false); usage.screen(8000,true); usage.stop(10000);
        check(usage.finish().get("youtube")==6000,"daily use clips midnight, ignores stale activity pause, and excludes screen-off/background");
        DailyUsageCounter lockedUsage=new DailyUsageCounter(0,10000);
        lockedUsage.resume(0,"one","a"); lockedUsage.lock(2000,true); lockedUsage.lock(5000,false);
        lockedUsage.resume(7000,"two","b");
        Map<String,Long> totals=lockedUsage.finish();
        check(totals.get("one")==4000 && totals.get("two")==3000,"screen lock excluded and app totals independent");
        check(!DailyUsageCounter.exhausted(119999,2,false),"daily limit editable while allowance remains");
        check(DailyUsageCounter.exhausted(120000,2,false),"daily limit locks exactly at allowance");
        check(DailyUsageCounter.exhausted(1000,120,true),"latched exhaustion cannot be undone by raising allowance or lower usage sample");
        check(ScreenRules.pauseControl("Pause video") && ScreenRules.pauseControl("Pause (k)"),"explicit web player pause labels");
        check(!ScreenRules.pauseControl("Play") && !ScreenRules.pauseControl("Pause your subscription"),"never click play or unrelated pause text");

        BrowserCoverState pageCover=new BrowserCoverState();
        pageCover.cover("com.brave.browser_nightly",7,60000,1000);
        check(pageCover.keep("com.brave.browser_nightly",7,60999),"editing blocked URL retains cover before expiry");
        check(!pageCover.keep("com.brave.browser_nightly",7,61000),"timer expiry removes cover even while editing URL");
        check(!pageCover.keep("com.brave.browser",7,2000),"cover cannot transfer to another browser");
        check(!pageCover.keep("com.brave.browser_nightly",8,2000),"cover cannot transfer to another window");
        pageCover.cover("com.brave.browser_nightly",7,Long.MAX_VALUE,1000);
        check(pageCover.keep("com.brave.browser_nightly",7,999999999),"persistent site rule does not expire like a timer");
        pageCover.clear();
        check(!pageCover.keep("com.brave.browser_nightly",7,2000),"leaving browser clears retained page state");
        for(String url:new String[]{"youtube.com/shorts", "https://www.youtube.com/shorts/abc?feature=share", "https://m.youtube.com/shorts/abc", "https://youtube.com/%73horts/abc", "https://youtube.com/a/../shorts/abc", "https://youtube.com/shorts#feed"})
            check(ScreenRules.webShorts(url),"web Shorts path detected: "+url);
        for(String url:new String[]{"youtube.com", "https://youtube.com/watch?v=abc", "https://youtube.com/watch?next=/shorts/abc", "https://youtube.com/#/shorts", "https://youtube.com/shortstuff", "https://youtube.com/shorts/../watch", "https://youtube.com.evil.test/shorts/abc", "https://evil.test/youtube.com/shorts", "https://youtube.com@evil.test/shorts", "about:blank", "", "youtube.com/SHORTS"})
            check(!ScreenRules.webShorts(url),"non-Shorts page stays allowed: "+url);
        check(!ScreenRules.webShorts(null),"missing address never triggers web Shorts");
        TimedBlock webShorts=new TimedBlock("web-shorts",ScreenRules.WEB_SHORTS,false,false,120000,120000,3,true);
        check(webShorts.shorts && !webShorts.app,"browser Shorts is separate from native app Shorts");
        check(webShorts.remaining(3,60000,60000)==60000 && webShorts.remaining(3,120000,120000)==0,"web Shorts expires at its own deadline");
        Set<String> callingApps=new HashSet<>(Arrays.asList("com.truecaller","com.android.phone","com.android.server.telecom","com.android.dialer","com.whatsapp"));
        Set<String> bypass=DnsPolicy.vpnExemptPackages(callingApps);
        check(!bypass.contains("com.truecaller"),"Truecaller DNS is filtered even when it is a calling app");
        check(callingApps.contains("com.truecaller"),"Truecaller remains protected against app suspension");
        check(bypass.contains("com.android.phone") && bypass.contains("com.android.server.telecom") && bypass.contains("com.android.dialer"),"phone infrastructure retains VPN exemptions");
        check(!DnsPolicy.mayUseNetworkDns(true,false,"ads.example.com"),"ad filtering never silently falls back to network DNS");
        check(DnsPolicy.mayUseNetworkDns(false,false,"ads.example.com"),"turning ad filtering off restores network DNS");
        check(DnsPolicy.mayUseNetworkDns(true,true,"calls.example.com"),"active calls preserve network DNS safety bypass");
        check(DnsPolicy.mayUseNetworkDns(true,false,"epdg.epc.mnc001.mcc404.pub.3gppnetwork.org"),"carrier calling domains bypass provider outages");
        check(!DnsPolicy.mayUseNetworkDns(true,false,"ads.truecaller.com"),"Truecaller ad hosts have no broad domain exemption");
        RecoveryBackoff retry=new RecoveryBackoff();
        check(retry.next(0)==1000 && retry.next(0)==2000,"recovery starts quickly and backs off");
        for(int i=0;i<400;i++) check(retry.next(0)<=30000,"recovery never grows past 30 seconds or expires");
        check(retry.next(60000)==1000,"stable connection resets retry delay");
        check(SiteRules.prefixBlocked("xxexample.com"),"xx root prefix");
        check(SiteRules.prefixBlocked("www.XHexample.com"),"xh prefix under www, case insensitive");
        check(SiteRules.prefixBlocked("cdn.xhexample.com"),"prefix applies under other subdomains");
        check(!SiteRules.prefixBlocked("example.com") && !SiteRules.prefixBlocked("myxxexample.com"),"ordinary and interior matches allowed");
        check(!SiteRules.prefixBlocked("example.xx"),"do not match top-level label alone");
        check(SiteRules.prefixBlocked("safe-sex.example") && SiteRules.prefixBlocked("www.FSIBlog.com"),"requested keyword and site rules");
        check(SiteRules.prefixBlocked("cdn.fsiblog.example") && SiteRules.prefixBlocked("sex.example.com"),"keyword rules cover subdomains and alternate suffixes");
        check(SiteRules.prefixBlocked("sussex.example"),"broad substring rule also matches unrelated names");
        check(!SiteRules.prefixBlocked("example.com") && !SiteRules.prefixBlocked("blog.example"),"ordinary host remains allowed");
        check(ScreenRules.browser("com.brave.browser_nightly"),"Nightly is a supported browser");
        check(!ScreenRules.browser("com.brave.browser_nightly.fake"),"browser package boundaries");
        check(ScreenRules.host("https://example.com/path?q=word").equals("example.com"),"only hostname used for browser checks");
        check(ScreenRules.host("search words").isEmpty(),"ignore search/address editing text");
        check(ScreenRules.host("brave://settings").isEmpty(),"ignore browser settings");
        check(ScreenRules.host("https://example.com@evil.test").isEmpty(),"reject ambiguous credential-style URL");
        check(ScreenRules.shortsMarker(ScreenRules.YOUTUBE+":id/reel_watch_player",false,""),"Shorts player marker");
        check(!ScreenRules.shortsMarker(ScreenRules.YOUTUBE+":id/reel_shelf",false,"Shorts"),"Shorts preview shelf does not block ordinary Home");
        check(!ScreenRules.shortsMarker("",false,"Shorts"),"unselected Shorts tab is not a Shorts screen");
        check(ScreenRules.shortsMarker("",true,"Shorts"),"selected Shorts tab detected");
        check(!ScreenRules.shortsMarker("other:id/reel_watch_player",false,""),"foreign resource is not a YouTube marker");
        check(!DnsPolicy.vpnExemptPackages(Set.of("com.brave.browser_nightly","com.android.phone")).contains("com.brave.browser_nightly"),"Nightly cannot inherit calling DNS exemption");
        TimedBlock shortsTimer=new TimedBlock("shorts",ScreenRules.YOUTUBE,true,false,60000,60000,1,true);
        check(shortsTimer.shorts && !shortsTimer.strict && shortsTimer.remaining(1,30000,30000)==30000,"Shorts timer retains independent deadline");
        check(!new TimedBlock("legacy",ScreenRules.YOUTUBE,true,false,60000,60000,1).shorts,"old app timers remain whole-app timers");
        BlockedAppExit exit=new BlockedAppExit();
        check(exit.shouldExit("youtube",10,100),"redirect immediately on detection");
        check(!exit.shouldExit("youtube",11,200),"activity changes do not spam Home");
        check(!exit.shouldExit("youtube",11,1599),"retry cooldown respected");
        check(exit.shouldExit("youtube",11,1600),"retry ignored Home request");
        check(exit.shouldExit("music",12,1700),"another blocked app redirects immediately");
        check(!exit.shouldExit("",13,1800),"leaving clears exit tracking");
        check(exit.shouldExit("music",14,1900),"Recents reopen redirects immediately");
        exit.clear(); check(exit.shouldExit("music",14,2000),"new detection after clear redirects");
        check(exit.shouldExit("music",14,100),"clock rollback resets cooldown");
        TimedBlock shortApp=new TimedBlock("a","youtube",true,false,121000,120000,4);
        TimedBlock longApp=new TimedBlock("b","other",true,true,601000,600000,4);
        TimedBlock website=new TimedBlock("c","example.com",false,false,301000,300000,4);
        check(shortApp.remaining(4,121000,120000)==0,"short app expires independently");
        check(longApp.remaining(4,121000,120000)==480000,"long app remains after short app expiry");
        check(website.remaining(4,121000,120000)==180000,"website has separate deadline");
        ForegroundApp tracker=new ForegroundApp();
        tracker.event(1,"youtube","old",true); tracker.event(2,"youtube","new",true); tracker.event(3,"youtube","old",false);
        check(tracker.current().equals("youtube"),"old activity pause does not clear resumed activity");
        tracker.event(4,"launcher","recents",true); tracker.event(5,"youtube","new",true);
        check(tracker.current().equals("youtube") && tracker.resumedAt()==5,"Recents return produces fresh resume");
        check(CallDomains.carrier("epdg.epc.mnc001.mcc404.pub.3gppnetwork.org"),"carrier calling DNS bypass");
        check(!CallDomains.carrier("3gppnetwork.org.example.com"),"carrier bypass is suffix bounded");
        check(CallDomains.communication("call.whatsapp.net") && !CallDomains.communication("google.com"),"calling domain exclusions remain narrow");
        java.time.ZoneId india=java.time.ZoneId.of("Asia/Kolkata");
        DailyWindow overnight=new DailyWindow(1260,540);
        check(overnight.activeMinute(1260),"schedule includes 9pm start");
        check(overnight.activeMinute(0) && overnight.activeMinute(539),"overnight continues after midnight");
        check(!overnight.activeMinute(540) && !overnight.activeMinute(1259),"schedule excludes 9am end and daytime");
        java.time.ZonedDateTime evening=java.time.ZonedDateTime.of(2026,11,2,21,0,0,0,india);
        check(overnight.remaining(evening)==12*3600000L,"9pm to 9am lasts twelve hours");
        check(java.time.Instant.ofEpochMilli(overnight.nextBoundary(evening)).atZone(india).getHour()==9,"next overnight boundary is morning");
        check(overnight.remaining(evening.plusHours(12))==0,"ends exactly at 9am");
        DailyWindow day=new DailyWindow(540,1020);
        check(day.activeMinute(540) && !day.activeMinute(1020),"same-day boundaries");
        try { new DailyWindow(540,540); throw new AssertionError("equal schedule times"); } catch(IllegalArgumentException expected) { checks++; }
        try { new DailyWindow(-1,540); throw new AssertionError("invalid start"); } catch(IllegalArgumentException expected) { checks++; }
        check(DailyWindow.coversWholeDay(List.of(overnight,new DailyWindow(540,1260))),"detect continuous strict coverage");
        check(!DailyWindow.coversWholeDay(List.of(overnight,new DailyWindow(600,1200))),"strict schedules can leave daily release gap");
        java.time.ZoneId ny=java.time.ZoneId.of("America/New_York");
        check(overnight.remaining(java.time.ZonedDateTime.of(2026,3,7,21,0,0,0,ny))==11*3600000L,"spring clock change follows local times");
        check(overnight.remaining(java.time.ZonedDateTime.of(2026,10,31,21,0,0,0,ny))==13*3600000L,"fall clock change follows local times");
        DailyWindow repeated=new DailyWindow(75,105);
        java.time.ZonedDateTime first=java.time.ZonedDateTime.of(2026,11,1,1,50,0,0,ny).withEarlierOffsetAtOverlap();
        java.time.ZonedDateTime next=java.time.Instant.ofEpochMilli(repeated.nextBoundary(first)).atZone(ny);
        check(next.getHour()==1 && next.getMinute()==0,"reschedule at repeated-hour clock transition");
        AppSchedule a=new AppSchedule("a","Night",Set.of("youtube"),1260,540,false,true);
        AppSchedule b=new AppSchedule("b","Late",Set.of("youtube","social"),1320,600,false,true);
        AppSchedule disabled=new AppSchedule("c","Disabled",Set.of("other"),1260,540,false,false);
        AppSchedule managed=new AppSchedule("d","Strict",Set.of("game"),1260,540,true,true);
        List<AppSchedule> schedules=List.of(a,b,disabled,managed);
        for(AppSchedule locked:List.of(a,managed)) {
            for(java.time.ZonedDateTime at:List.of(evening,evening.plusHours(2),evening.plusHours(11))) {
                try { locked.requireEditable(at); throw new AssertionError("active schedule edit accepted"); }
                catch(IllegalStateException expected) { checks++; }
            }
            locked.requireEditable(evening.minusNanos(1)); checks++;
            locked.requireEditable(evening.plusHours(12)); checks++;
        }
        disabled.requireEditable(evening.plusHours(2)); checks++;

        check(AppSchedule.activeApps(schedules,false,evening.plusHours(2)).equals(Set.of("youtube","social")),"union overlapping normal schedules");
        check(AppSchedule.activeApps(schedules,true,evening.plusHours(2)).equals(Set.of("game")),"strict schedule union is independent");
        check(AppSchedule.activeApps(schedules,false,evening.plusHours(12)).contains("youtube"),"overlap keeps app paused when first schedule ends");
        check(AppSchedule.activeApps(schedules,false,evening.plusHours(13)).isEmpty(),"last schedule expiry restores app");
        try { SessionMode.validate(true,false,true,true); throw new AssertionError("Strict must refuse unprovisioned devices"); }
        catch(IllegalStateException expected) { check(expected.getMessage().contains("device-owner"),"strict setup error is actionable"); }
        try { SessionMode.validate(true,false,true,false); throw new AssertionError("Strict websites must also require an owner"); }
        catch(IllegalStateException expected) { checks++; }
        SessionMode.validate(true,true,false,true); check(true,"strict suspension needs no overlay permission");
        SessionMode.validate(false,false,true,true); check(true,"normal app timers restored without device owner");
        SessionMode.validate(false,false,false,false); check(true,"normal websites need no app permissions");
        try { SessionMode.validate(false,false,false,true); throw new AssertionError("normal app permissions required"); }
        catch(IllegalStateException expected) { check(expected.getMessage().contains("Usage access"),"permission recovery message"); }
        SetupState setup=new SetupState(false);
        check(setup.needsSetup(),"fresh install starts in setup");
        check(!setup.observe(true,false,false) && setup.needsSetup(),"VPN alone does not finish setup");
        check(!setup.observe(true,true,false) && setup.needsSetup(),"partial permission setup remains visible");
        check(setup.observe(true,true,true) && !setup.needsSetup(),"last grant completes setup once");
        check(!setup.observe(true,true,true),"completion is not reported repeatedly");
        check(!setup.observe(false,false,false) && !setup.needsSetup(),"revocation never reopens completed setup");
        check(!new SetupState(true).needsSetup(),"saved completion skips setup on restart");
        SetupState upgrade=new SetupState(false);
        check(upgrade.observe(true,true,true) && !upgrade.needsSetup(),"upgrade recognizes permissions already granted");
        ForegroundApp scheduledForeground=new ForegroundApp();
        scheduledForeground.event(1,"music",true);
        scheduledForeground.idle(true);
        check(scheduledForeground.current().equals("music"),"retain window package until schedule starts without another event");
        DailyWindow night=new DailyWindow(21*60,9*60);
        check(!night.activeMinute(20*60+59) && night.activeMinute(21*60),"9 PM activates while foreground stays unchanged");
        check(night.activeMinute(8*60+59) && !night.activeMinute(9*60),"overnight schedule releases at 9 AM");
        scheduledForeground.event(2,"settings",true); scheduledForeground.idle(true);
        check(scheduledForeground.current().equals("settings"),"inactive interval still tracks navigation away");
        scheduledForeground.idle(false);
        check(scheduledForeground.current().isEmpty(),"polling fallback resets idle event history");
        ForegroundApp foreground=new ForegroundApp();
        foreground.event(1,"youtube",true); check(foreground.current().equals("youtube"),"detect selected app");
        foreground.event(2,"settings",true); foreground.event(3,"youtube",false);
        check(foreground.current().equals("settings"),"late pause does not hide current settings app");
        foreground.event(1,"youtube",true); check(foreground.current().equals("settings"),"ignore old usage events");
        foreground.event(4,"settings",false); check(foreground.current().isEmpty(),"clear foreground on pause");
        foreground.event(5,"youtube",true); foreground.clear(); check(foreground.current().isEmpty(),"clear at expiry");
        check(SessionDeadline.remaining(10000,5000,1,1,999999,1000)==4000,"clock forward cannot expire current boot");
        check(SessionDeadline.remaining(10000,5000,1,1,1,1000)==4000,"clock backward cannot extend current boot");
        check(SessionDeadline.remaining(10000,5000,1,2,8000,100)==2000,"reboot uses saved deadline");
        check(SessionDeadline.remaining(10000,5000,1,2,11000,100)==0,"expiry over reboot");
        check(SessionDeadline.remaining(10000,5000,1,1,1,6000)==0,"monotonic expiry");
        check(SessionDeadline.remaining(10000,5000,-1,-1,8000,100)==2000,"unknown boot uses wall deadline");
        check(DomainRules.normalize("https://WWW.Example.com/a?q=b").equals("www.example.com"),"URL normalization");
        check(DomainRules.normalize("example.com.").equals("example.com"),"trailing dot");
        check(DomainRules.normalize("bücher.de").equals("xn--bcher-kva.de"),"IDN");
        for(String s:new String[]{"localhost","https://a@b.com","127.0.0.1","a..com","-a.com","ftp://example.com","https://","a.com:bad"}) invalid(s);
        check(DomainRules.covers("example.com","sub.example.com"),"subdomain");
        check(!DomainRules.covers("example.com","notexample.com"),"domain boundary");
        DomainIndex sample=new DomainIndex(new ByteArrayInputStream("a.example\nb.example\nexample.com\nz.example\n".getBytes()));
        check(sample.size()==4,"index count"); check(sample.covers("sub.example.com"),"suffix lookup");
        check(!sample.covers("example.com.evil"),"suffix boundary"); check(sample.contains("z.example"),"last entry");
        check(!sample.contains("missing.example"),"missing entry");
        byte[] packet=udp(query("ExAmPlE.com")); DnsPacket parsed=DnsPacket.parse(packet,packet.length);
        check(parsed!=null && parsed.host.equals("example.com"),"DNS parsing");
        byte[] question=parsed.error(0); int rr=question.length;
        byte[] longAnswer=Arrays.copyOf(question,rr+16); longAnswer[7]=1;
        byte[] record={(byte)0xc0,12,0,1,0,1,0,0,14,16,0,4,1,2,3,4};
        System.arraycopy(record,0,longAnswer,rr,16);
        byte[] limited=DnsTtl.limit(longAnswer,30);
        check(limited[rr+8]==0 && limited[rr+9]==30,"fallback answer TTL capped from one hour to 30 seconds");
        check(longAnswer[rr+8]==14 && longAnswer[rr+9]==16,"TTL limiting leaves original response intact");
        check(limited[rr+12]==1 && limited[rr+15]==4 && parsed.validResponse(limited),"address and matching DNS question preserved");
        longAnswer[rr+8]=0; longAnswer[rr+9]=5;
        check(DnsTtl.limit(longAnswer,30)[rr+9]==5,"short TTL is never extended");
        longAnswer[rr+3]=41; longAnswer[rr+8]=(byte)128; longAnswer[rr+9]=0;
        check(DnsTtl.limit(longAnswer,30)[rr+8]==(byte)128,"EDNS OPT flags are never treated as TTL");
        for(int n=0;n<longAnswer.length;n++) DnsTtl.limit(Arrays.copyOf(longAnswer,n),30);
        byte[] denied=parsed.error(3); check((denied[3]&15)==3 && (denied[2]&128)!=0,"NXDOMAIN");
        check(parsed.validResponse(denied),"matching response"); denied[0]++; check(!parsed.validResponse(denied),"transaction validation");
        byte[] response=parsed.response(parsed.error(3)); check(response[12]==10 && response[15]==2 && response[19]==1,"address swap");
        int sum=0; for(int i=0;i<20;i+=2) sum+=((response[i]&255)<<8)|(response[i+1]&255);
        while((sum>>16)!=0) sum=(sum&65535)+(sum>>16); check(sum==65535,"IP checksum");
        for(int i=0;i<packet.length;i++) check(DnsPacket.parse(packet,i)==null,"truncation "+i);
        byte[] fragmented=packet.clone(); fragmented[6]=0x20; check(DnsPacket.parse(fragmented,fragmented.length)==null,"fragment rejected");
        Random random=new Random(5);
        for(int i=0;i<10000;i++) { byte[] fuzz=new byte[random.nextInt(500)]; random.nextBytes(fuzz); DnsPacket.parse(fuzz,fuzz.length); DnsTtl.limit(fuzz,30); }
        try(InputStream in=Files.newInputStream(Path.of("app/src/main/assets/adult-domains.txt"))) {
            DomainIndex bundled=new DomainIndex(in); check(bundled.size()>900000,"full imported list");
            for(String domain:Files.readAllLines(Path.of("app/src/main/assets/adult-domains.txt"))) check(bundled.contains(domain),"bundled lookup");
        }
        System.out.println("Passed "+checks+" checks and 10,000 malformed-packet cases.");
    }
}
