package com.stillfocus.app;
import android.content.Context;
import java.io.*;
/** One shared immutable index for DNS and opt-in browser checking. Load off the UI thread. */
public final class WebsiteFilter {
    private static volatile DomainIndex[] indexes;
    public static synchronized void load(Context c) throws IOException {
        if(indexes!=null) return;
        DomainIndex suffix,exact;
        try(InputStream in=c.getAssets().open("adult-domains.txt")) { suffix=new DomainIndex(in); }
        try(InputStream in=c.getAssets().open("adult-exact.txt")) { exact=new DomainIndex(in); }
        indexes=new DomainIndex[]{suffix,exact};
    }
    public static boolean ready() { return indexes!=null; }
    public static boolean blocked(Context c,String host) {
        if(host.isEmpty() || CallDomains.carrier(host)) return false;
        return persistentBlock(host) || new FocusStore(c).blocks(host);
    }
    public static boolean persistentBlock(String host) {
        if(host.isEmpty() || CallDomains.carrier(host)) return false;
        DomainIndex[] current=indexes;
        return SiteRules.prefixBlocked(host) || current!=null && (current[0].covers(host) || current[1].contains(host));
    }
    private WebsiteFilter() { }
}
