package com.stillfocus.app;
import java.util.HashSet;
import java.util.Set;

/** App suspension exemptions and DNS routing exemptions serve different purposes. */
public final class DnsPolicy {
    private DnsPolicy() { }
    public static Set<String> vpnExemptPackages(Set<String> callingApps) {
        Set<String> result=new HashSet<>(callingApps);
        result.remove("com.truecaller");
        result.removeIf(ScreenRules::browser);
        return result;
    }
    // Narrow advertising endpoints from AdGuard's DNS filter. Never block all of
    // truecaller.com: caller lookup and calling services must remain available.
    // Source: https://adguardteam.github.io/AdGuardSDNSFilter/Filters/filter.txt
    public static boolean localAdBlocked(boolean enabled,String host) {
        if(!enabled || host==null) return false;
        String name=host.toLowerCase(java.util.Locale.ROOT);
        if(name.endsWith(".")) name=name.substring(0,name.length()-1);
        for(String ad:new String[]{"ads-segment-profile-noneu.truecaller.com",
                "ads-config-engine-noneu.truecaller.com","ads-router-noneu.truecaller.com"}) {
            if(name.equals(ad) || name.endsWith("."+ad)) return true;
        }
        return false;
    }
    public static boolean mayUseNetworkDns(boolean adBlocking,boolean callActive,String host) {
        return !adBlocking || callActive || CallDomains.communication(host);
    }
}
