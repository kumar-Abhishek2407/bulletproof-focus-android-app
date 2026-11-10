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
    public static boolean mayUseNetworkDns(boolean adBlocking,boolean callActive,String host) {
        return !adBlocking || callActive || CallDomains.communication(host);
    }
}
