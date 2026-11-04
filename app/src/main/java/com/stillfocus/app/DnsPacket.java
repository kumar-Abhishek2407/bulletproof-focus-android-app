package com.stillfocus.app;

import java.util.Arrays;
import java.util.Locale;

/** IPv4 UDP DNS codec. Bounds checks fail closed; no parsing of browsing payloads. */
public final class DnsPacket {
    public final byte[] packet, query;
    public final String host;
    private final int ipLength;
    private DnsPacket(byte[] p, byte[] q, String h, int len) { packet=p; query=q; host=h; ipLength=len; }
    private static int u16(byte[] b, int i) { return ((b[i]&255)<<8) | (b[i+1]&255); }
    private static void put16(byte[] b, int i, int v) { b[i]=(byte)(v>>8); b[i+1]=(byte)v; }
    public static DnsPacket parse(byte[] raw, int length) {
        if (length < 40 || (raw[0] & 240) != 64) return null;
        int ihl=(raw[0]&15)*4, total=u16(raw,2);
        if (ihl < 20 || total > length || total < ihl+20 || raw[9]!=17 || (u16(raw,6)&0x3fff)!=0) return null;
        int udp=u16(raw,ihl+4);
        if (u16(raw,ihl+2)!=53 || udp < 20 || ihl+udp!=total) return null;
        byte[] q=Arrays.copyOfRange(raw,ihl+8,total);
        if ((q[2]&0xf8)!=0 || u16(q,4)!=1 || u16(q,6)!=0 || u16(q,8)!=0) return null;
        StringBuilder host=new StringBuilder(); int pos=12;
        while (pos<q.length) {
            int size=q[pos++]&255;
            if (size==0) break;
            if (size>63 || pos+size>=q.length) return null;
            if (host.length()>0) host.append('.');
            for (int i=0;i<size;i++) {
                int ch=q[pos++]&255;
                if (!(ch>='a'&&ch<='z'||ch>='A'&&ch<='Z'||ch>='0'&&ch<='9'||ch=='-'||ch=='_')) return null;
                host.append((char)ch);
            }
            if (host.length()>253) return null;
        }
        if (host.length()==0 || pos+4>q.length) return null;
        return new DnsPacket(Arrays.copyOf(raw,total),q,host.toString().toLowerCase(Locale.ROOT),ihl);
    }
    public byte[] error(int code) {
        // Copy only the question, removing EDNS options and all advertised records.
        int pos=12; while (pos<query.length && query[pos]!=0) pos+=(query[pos]&255)+1;
        byte[] result=Arrays.copyOf(query,pos+5);
        result[2]=(byte)(0x80 | (query[2]&1)); result[3]=(byte)(0x80 | code);
        Arrays.fill(result,6,12,(byte)0); return result;
    }
    public boolean validResponse(byte[] answer) {
        if (answer.length<12 || answer[0]!=query[0] || answer[1]!=query[1] || (answer[2]&128)==0 || u16(answer,4)!=1) return false;
        int end=12; while (end<query.length && query[end]!=0) end+=(query[end]&255)+1;
        end+=5;
        if (answer.length<end) return false;
        for (int i=12;i<end;i++) if (answer[i]!=query[i]) return false;
        return true;
    }
    public byte[] response(byte[] dns) {
        byte[] result=new byte[28+dns.length]; result[0]=0x45;
        put16(result,2,result.length); result[8]=64; result[9]=17;
        System.arraycopy(packet,16,result,12,4); System.arraycopy(packet,12,result,16,4);
        put16(result,20,53); result[22]=packet[ipLength]; result[23]=packet[ipLength+1];
        put16(result,24,8+dns.length); System.arraycopy(dns,0,result,28,dns.length);
        int sum=0; for(int i=0;i<20;i+=2) sum+=u16(result,i);
        while ((sum>>16)!=0) sum=(sum&65535)+(sum>>16);
        put16(result,10,~sum); // IPv4 permits a zero UDP checksum.
        return result;
    }
}
