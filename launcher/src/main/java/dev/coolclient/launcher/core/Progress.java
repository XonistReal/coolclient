package dev.coolclient.launcher.core;

/** Install/launch progress, shown on the Play button. */
public interface Progress {
    void update(String stage, float fraction);

    /** Progress within one stage. */
    interface Fraction {
        void update(float fraction);
    }

    default Fraction stage(String stage) {
        update(stage, 0f);
        return f -> update(stage, f);
    }
}
