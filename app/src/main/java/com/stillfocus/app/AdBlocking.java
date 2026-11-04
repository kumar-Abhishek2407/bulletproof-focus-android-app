package com.stillfocus.app;

import android.content.Context;
import android.net.Network;
import java.io.*;
import java.net.URL;
import javax.net.ssl.HttpsURLConnection;

/** Optional public resolver. No traffic leaves for this provider until consent. */
public final class AdBlocking {
    public static boolean enabled(Context c) { return c.getSharedPreferences("network",0).getBoolean("adBlocking",false); }
    public static void set(Context c,boolean enabled) { c.getSharedPreferences("network",0).edit().putBoolean("adBlocking",enabled).apply(); }
    public static byte[] resolve(Network network,DnsPacket packet) {
        HttpsURLConnection connection=null;
        try {
            connection=(HttpsURLConnection)network.openConnection(new URL("https://dns.adguard-dns.com/dns-query"));
            connection.setConnectTimeout(3000); connection.setReadTimeout(3000);
            connection.setInstanceFollowRedirects(false); connection.setRequestMethod("POST"); connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type","application/dns-message");
            connection.setRequestProperty("Accept","application/dns-message");
            connection.setFixedLengthStreamingMode(packet.query.length);
            try(OutputStream out=connection.getOutputStream()) { out.write(packet.query); }
            if(connection.getResponseCode()!=200) return null;
            try(InputStream in=connection.getInputStream()) {
                ByteArrayOutputStream bytes=new ByteArrayOutputStream(); byte[] buffer=new byte[1024]; int count;
                while(bytes.size()<=4096 && (count=in.read(buffer))!=-1) bytes.write(buffer,0,count);
                byte[] answer=bytes.toByteArray();
                return answer.length<=4096 && packet.validResponse(answer) && ((answer[3]&15)==0 || (answer[3]&15)==3)?answer:null;
            }
        } catch(IOException | RuntimeException failure) { return null; }
        finally { if(connection!=null) connection.disconnect(); }
    }
}
