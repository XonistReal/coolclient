package dev.coolclient.core.util;

import java.util.ArrayDeque;

/** Counts clicks in a rolling one-second window. */
public final class CpsTracker {
    private final ArrayDeque<Long> clicks = new ArrayDeque<Long>();

    public synchronized void click() {
        clicks.addLast(Time.millis());
    }

    public synchronized int cps() {
        long cutoff = Time.millis() - 1000L;
        while (!clicks.isEmpty() && clicks.peekFirst() < cutoff) clicks.pollFirst();
        return clicks.size();
    }
}
