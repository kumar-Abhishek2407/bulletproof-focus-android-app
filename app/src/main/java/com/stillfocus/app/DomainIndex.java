package com.stillfocus.app;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/** Sorted ASCII domains stored as bytes, avoiding a million Java String objects. */
public final class DomainIndex {
    private final byte[] data;
    private final int[] offsets;
    public DomainIndex(InputStream stream) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        byte[] chunk = new byte[32768]; int n;
        while ((n = stream.read(chunk)) != -1) bytes.write(chunk, 0, n);
        data = bytes.toByteArray();
        int count = 0;
        for (byte b : data) if (b == '\n') count++;
        if (data.length > 0 && data[data.length - 1] != '\n') count++;
        offsets = new int[count + 1]; int j = 1;
        for (int i = 0; i < data.length; i++) if (data[i] == '\n' && j < count) offsets[j++] = i + 1;
        offsets[count] = data.length;
    }
    public int size() { return offsets.length - 1; }
    public boolean contains(String host) {
        byte[] key = host.getBytes(StandardCharsets.US_ASCII);
        int low = 0, high = size() - 1;
        while (low <= high) {
            int mid = (low + high) >>> 1, start = offsets[mid], end = offsets[mid+1];
            if (end > start && data[end-1] == '\n') end--;
            int len = end - start, cmp = 0;
            for (int i = 0; i < Math.min(key.length, len); i++) {
                cmp = (data[start+i] & 255) - (key[i] & 255); if (cmp != 0) break;
            }
            if (cmp == 0) cmp = len - key.length;
            if (cmp == 0) return true;
            if (cmp < 0) low = mid+1; else high = mid-1;
        }
        return false;
    }
    public boolean covers(String host) {
        for (;;) {
            if (contains(host)) return true;
            int dot = host.indexOf('.'); if (dot < 0) return false;
            host = host.substring(dot+1);
        }
    }
}
