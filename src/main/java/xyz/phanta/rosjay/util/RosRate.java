package xyz.phanta.rosjay.util;

public class RosRate {

    private final long periodNanos;

    private long lastCallTime;

    public RosRate(double freq) {
        this.periodNanos = (long)Math.ceil(1e9 / freq);
        this.lastCallTime = System.nanoTime();
    }

    public void sleep() {
        long now = System.nanoTime();
        long totalSleepTime = lastCallTime + periodNanos - now;
        lastCallTime = now;
        if (totalSleepTime > 0) {
            int sleepNanos = (int)(totalSleepTime % 1_000_000L);
            try {
                Thread.sleep((totalSleepTime - sleepNanos) / 1_000_000L, sleepNanos);
            } catch (InterruptedException e) {
                // NO-OP
            }
        }
    }

}
