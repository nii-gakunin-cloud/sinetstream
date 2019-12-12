package jp.ad.sinet.stream.example.perf;

import java.net.InetAddress;
import java.util.Calendar;
import java.text.SimpleDateFormat;

public class Util {
    public static String getHostName() {
        String hostname;
        try {
            hostname = InetAddress.getLocalHost().getHostName();
            hostname = hostname.split("[.]", 2)[0];
        }
        catch (Exception e) {
            e.printStackTrace();
            hostname = "UNKNOWN";
        }
        return hostname;
    }

    public static String getTime() {
        Calendar c = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd-HHmmss");
        return sdf.format(c.getTime());
    }
}
