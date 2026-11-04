package com.stillfocus.app;

import java.net.IDN;
import java.net.URI;
import java.util.Locale;

public final class DomainRules {
    private DomainRules() {}
    public static String normalize(String input) {
        try {
            String text = input.trim();
            URI uri = new URI(text.contains("://") ? text : "https://" + text);
            if (!("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme())) || uri.getRawUserInfo() != null)
                throw new IllegalArgumentException();
            String host = uri.getHost();
            // URI.getHost rejects Unicode; allow a bare international domain via IDN.
            if (host == null && !text.contains("://") && !text.matches(".*[/:?#@].*")) host = text;
            if (host == null) throw new IllegalArgumentException();
            host = IDN.toASCII(host.toLowerCase(Locale.ROOT), IDN.USE_STD3_ASCII_RULES);
            if (host.endsWith(".")) host = host.substring(0, host.length() - 1);
            if (host.length() > 253 || !host.contains(".") || host.matches("[0-9.]+")) throw new IllegalArgumentException();
            for (String label : host.split("\\.", -1))
                if (label.isEmpty() || label.length() > 63 || label.startsWith("-") || label.endsWith("-") || !label.matches("[a-z0-9-]+")) throw new IllegalArgumentException();
            return host;
        } catch (Exception e) { throw new IllegalArgumentException("Enter a domain such as example.com or a website URL."); }
    }
    public static boolean covers(String domain, String host) { return host.equals(domain) || host.endsWith("." + domain); }
}
