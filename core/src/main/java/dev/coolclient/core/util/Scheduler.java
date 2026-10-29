package dev.coolclient.core.util;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

/** Background worker pool plus a queue of tasks to run on the game thread. */
public final class Scheduler {
    private static final ConcurrentLinkedQueue<Runnable> MAIN = new ConcurrentLinkedQueue<Runnable>();
    private static final ExecutorService WORKERS = Executors.newFixedThreadPool(2, new ThreadFactory() {
        private int n;

        @Override
        public synchronized Thread newThread(Runnable r) {
            Thread t = new Thread(r, "Cool Client Worker " + (++n));
            t.setDaemon(true);
            return t;
        }
    });

    private Scheduler() {}

    public static void async(Runnable r) {
        WORKERS.execute(wrap(r));
    }

    public static void main(Runnable r) {
        MAIN.add(r);
    }

    /** Drains main-thread tasks; called from the client tick. */
    public static void runMainTasks() {
        Runnable r;
        int budget = 64;
        while (budget-- > 0 && (r = MAIN.poll()) != null) {
            wrap(r).run();
        }
    }

    private static Runnable wrap(final Runnable r) {
        return new Runnable() {
            @Override
            public void run() {
                try {
                    r.run();
                } catch (Throwable t) {
                    Log.error("Task failed", t);
                }
            }
        };
    }
}
