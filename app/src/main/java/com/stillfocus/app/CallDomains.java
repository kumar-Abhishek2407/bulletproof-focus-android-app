package com.stillfocus.app;

/** Conservative carrier/communication DNS exceptions, matched at domain boundaries. */
public final class CallDomains {
    private CallDomains() { }
    public static boolean carrier(String host) {
        for(String domain:new String[]{"3gppnetwork.org","3gppnetwork.net","epdg.epc.att.net","vzwwo.com","spcsdns.net"})
            if(DomainRules.covers(domain,host)) return true;
        return false;
    }
    public static boolean communication(String host) {
        if(carrier(host)) return true;
        for(String domain:new String[]{"whatsapp.net","whatsapp.com","signal.org","telegram.org","t.me","zoom.us","webex.com","skype.com","lync.com","teams.microsoft.com","meet.google.com","duo.google.com"})
            if(DomainRules.covers(domain,host)) return true;
        return false;
    }
}
