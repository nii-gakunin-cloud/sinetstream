import java.util.concurrent.TimeUnit;

class TestSched {
    public static void show_stat(long xs[]) {
        int n = xs.length;
        long sum = 0;
        for (int i = 0; i < n; i++)
            sum += xs[i];
        double mean = (double)sum / (double)n;

        double sum2 = 0;
        for (int i = 0; i < n; i++) {
            double tmp = (double)xs[i] - mean;
            sum2 += tmp*tmp;
        }
        System.err.println("mean: " + mean);
        System.err.println("std: " + Math.sqrt(sum2 / (double)n));
    }
    public static void main(String[] args) {
        long interval = Long.parseLong(args[0]);
        int n = Integer.parseInt(args[1]);
        long ts[] = new long[n];
        long rs[] = new long[n];

        try {
            long next = System.nanoTime() + interval;
            for (int i = 0; i < n; i++) {
                long now = System.nanoTime();
                ts[i] = System.nanoTime();
                long rest = next - now;
                rs[i] = rest;
                if (rest > 0)
                    TimeUnit.NANOSECONDS.sleep(rest);
                next += interval;
            }
        }
        catch (InterruptedException e) {
            System.err.println("InterruptedException: " + e);
        }

        System.err.println("--jitter--");
        long ds[] = new long[n];
        for (int i = 0; i < n; i++) {
            long exp = ts[0] + i * interval;
            ds[i] = ts[i] - exp;
            System.out.println(ts[i] + "," + ds[i] + "," + rs[i]); // abstime,jitter,resttime
        }
        show_stat(ds);

        System.err.println("--rest--");
        show_stat(rs);

    }
}
