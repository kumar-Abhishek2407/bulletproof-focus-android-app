package com.stillfocus.app;
/** Keep temporary unfiltered fallback answers from being cached for hours. */
public final class DnsTtl {
    private DnsTtl() { }
    private static int u16(byte[] b,int p) { return (b[p]&255)*256+(b[p+1]&255); }
    private static int nameEnd(byte[] b,int p) {
        while(p<b.length) {
            int size=b[p++]&255;
            if(size==0) return p;
            if((size&192)==192) return p<b.length && ((size&63)*256+(b[p]&255))<b.length?p+1:-1;
            if(size>63 || p+size>b.length) return -1;
            p+=size;
        }
        return -1;
    }
    public static byte[] limit(byte[] original,int seconds) {
        if(original.length<12 || seconds<0) return original;
        byte[] b=original.clone(); int p=12;
        for(int q=0;q<u16(b,4);q++) { p=nameEnd(b,p); if(p<0 || p+4>b.length) return original; p+=4; }
        int records=u16(b,6)+u16(b,8)+u16(b,10);
        for(int i=0;i<records;i++) {
            p=nameEnd(b,p); if(p<0 || p+10>b.length) return original;
            int type=u16(b,p),length=u16(b,p+8); if(p+10+length>b.length) return original;
            long ttl=((long)(b[p+4]&255)<<24)|((long)(b[p+5]&255)<<16)|((b[p+6]&255)<<8)|(b[p+7]&255);
            // OPT uses this field for flags, not a cache lifetime.
            if(type!=41 && ttl>seconds) for(int k=0;k<4;k++) b[p+4+k]=(byte)(seconds >>> (24-k*8));
            p+=10+length;
        }
        return b;
    }
}
