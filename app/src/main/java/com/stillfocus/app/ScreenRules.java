package com.stillfocus.app;

/** Narrow package and UI-marker matching; no inference from arbitrary page/video text. */
public final class ScreenRules {
    public static final String WEB_SHORTS="youtube.com/shorts";
    public static boolean webShorts(String address) {
        String host=host(address);
        if(!DomainRules.covers("youtube.com",host)) return false;
        try {
            String value=address.trim();
            java.net.URI uri=new java.net.URI(value.contains("://")?value:"https://"+value).normalize();
            String path=uri.getPath();
            return "/shorts".equals(path) || path!=null && path.startsWith("/shorts/");
        } catch(java.net.URISyntaxException e) { return false; }
    }
    public static final String YOUTUBE="com.google.android.youtube";
    public static boolean browser(String pkg) {
        return "com.brave.browser".equals(pkg) || "com.brave.browser_beta".equals(pkg) || "com.brave.browser_nightly".equals(pkg);
    }
    public static String host(String address) {
        if(address==null || address.length()>8192) return "";
        try { return DomainRules.normalize(address); } catch(IllegalArgumentException e) { return ""; }
    }
    public static boolean pauseControl(String description) {
        if(description==null) return false;
        String value=description.trim().toLowerCase(java.util.Locale.ROOT);
        return value.equals("pause") || value.equals("pause video") || value.equals("pause (k)");
    }
    public static boolean playControl(String description) {
        if(description==null) return false;
        String value=description.trim().toLowerCase(java.util.Locale.ROOT);
        return value.equals("play") || value.equals("play video") || value.equals("play (k)");
    }
    public static boolean shortsMarker(String id,boolean selected,String description) {
        if(id!=null && id.startsWith(YOUTUBE+":id/")) {
            String local=id.substring(id.indexOf('/')+1);
            if(local.equals("reel_watch_fragment_root") || local.equals("reel_watch_player") || local.equals("shorts_player") || local.equals("reel_player_page_container")) return true;
        }
        return selected && ("Shorts".equalsIgnoreCase(description) || "\u0936\u0949\u0930\u094d\u091f\u094d\u0938".equals(description));
    }
    private ScreenRules() { }
}
