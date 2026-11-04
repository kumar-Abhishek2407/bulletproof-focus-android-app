package com.stillfocus.app;
import java.util.Locale;
/** User-requested domain rules, applied to domain labels including subdomains. */
public final class SiteRules {
    private SiteRules() { }
    public static boolean prefixBlocked(String host) {
        if(host==null) return false;
        String normalized=host.toLowerCase(Locale.ROOT);
        if(normalized.contains("sex") || normalized.contains("fsiblog")) return true;
        String[] labels=normalized.split("\\.");
        for(int i=0;i<labels.length-1;i++) if(labels[i].startsWith("xx") || labels[i].startsWith("xh")) return true;
        return false;
    }
}
