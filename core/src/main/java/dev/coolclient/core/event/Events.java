package dev.coolclient.core.event;

import dev.coolclient.core.api.GuiRenderer;

/** All core events. Platforms post these through {@link dev.coolclient.core.CoolClient}. */
public final class Events {
    private Events() {}

    /** Fired at the end of every client tick (20/s). */
    public static final class Tick {
        public static final Tick INSTANCE = new Tick();
    }

    /** Fired after the vanilla HUD renders. */
    public static final class RenderHud {
        public final GuiRenderer renderer;
        public final float partialTicks;

        public RenderHud(GuiRenderer renderer, float partialTicks) {
            this.renderer = renderer;
            this.partialTicks = partialTicks;
        }
    }

    /** A key was pressed or released while no screen is open. Codes are GLFW. */
    public static final class Key extends Cancellable {
        public final int key;
        public final boolean pressed;

        public Key(int key, boolean pressed) {
            this.key = key;
            this.pressed = pressed;
        }
    }

    /** A mouse button changed state while in game (no screen). */
    public static final class MouseButton {
        public final int button;
        public final boolean pressed;

        public MouseButton(int button, boolean pressed) {
            this.button = button;
            this.pressed = pressed;
        }
    }

    /** Mouse wheel moved in game. Cancel to stop the hotbar from scrolling. */
    public static final class Scroll extends Cancellable {
        public final double amount;

        public Scroll(double amount) { this.amount = amount; }
    }

    /** The local player attacked an entity. */
    public static final class Attack {
        public final String targetName;
        public final boolean targetIsPlayer;
        public final double distance;
        public final int targetId;

        public Attack(int targetId, String targetName, boolean targetIsPlayer, double distance) {
            this.targetId = targetId;
            this.targetName = targetName;
            this.targetIsPlayer = targetIsPlayer;
            this.distance = distance;
        }
    }

    /** The local player took damage. */
    public static final class SelfHurt {
        public static final SelfHurt INSTANCE = new SelfHurt();
    }

    /** A chat message was received. {@code plain} has formatting stripped. */
    public static final class ChatReceived {
        public final String plain;

        public ChatReceived(String plain) { this.plain = plain; }
    }

    /** A player popped a totem (status 35). */
    public static final class TotemPop {
        public final String name;
        public final boolean self;

        public TotemPop(String name, boolean self) {
            this.name = name;
            this.self = self;
        }
    }

    /** A player died (health reached zero / death status). */
    public static final class PlayerDeath {
        public final String name;

        public PlayerDeath(String name) { this.name = name; }
    }

    public static final class WorldJoin {
        /** Server address, or null for singleplayer. */
        public final String server;

        public WorldJoin(String server) { this.server = server; }
    }

    public static final class WorldLeave {
        public static final WorldLeave INSTANCE = new WorldLeave();
    }
}
