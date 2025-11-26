package com.coffrefort.client.model;

public class Quota {
    private final long used;
    private final long max;

    public Quota(long used, long max) {
        this.used = used;
        this.max = max;
    }

    public long getUsed() { return used; }
    public long getMax() { return max; }

    public double getUsageRatio() {
        if (max <= 0) return 0.0;
        return (double) used / (double) max;
    }
}
