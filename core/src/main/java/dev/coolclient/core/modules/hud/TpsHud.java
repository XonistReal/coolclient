package dev.coolclient.core.modules.hud;

import dev.coolclient.core.event.Events;
import dev.coolclient.core.hud.TextElement;
import dev.coolclient.core.util.MathUtil;
import dev.coolclient.core.util.Time;

/**
 * Server ticks per second, estimated from the world-time packets vanilla servers send every 20
 * ticks: game ticks passed divided by real seconds between two packets.
 */
public final class TpsHud extends TextElement {
    private long lastGameTime = -1, lastAt;
    private double tps = -1;

    public TpsHud() {
        super("TPS", "Server ticks per second (20 is a healthy server).");
        defaultPosition(Anchor.TOP_LEFT, 4, 202);
        on(Events.ServerTime.class, e -> {
            long now = Time.millis();
            if (lastGameTime >= 0 && now > lastAt) {
                long ticks = e.gameTime - lastGameTime;
                double seconds = (now - lastAt) / 1000.0;
                if (ticks > 0 && ticks < 2000 && seconds > 0.2) {
                    double sample = MathUtil.clamp(ticks / seconds, 0, 20);
                    tps = tps < 0 ? sample : tps * 0.6 + sample * 0.4;
                }
            }
            lastGameTime = e.gameTime;
            lastAt = now;
        });
        on(Events.WorldJoin.class, e -> reset());
        on(Events.WorldLeave.class, e -> reset());
    }

    private void reset() {
        lastGameTime = -1;
        tps = -1;
    }

    @Override protected String label() { return "TPS"; }

    @Override
    protected String value() {
        // No time packet for 3 s means the server has stalled.
        if (lastGameTime >= 0 && Time.millis() - lastAt > 3000) return "0.0";
        return tps < 0 ? "..." : MathUtil.format(Math.min(20, tps + 0.04), 1);
    }

    @Override protected String previewValue() { return "20.0"; }
}
