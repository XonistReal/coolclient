package dev.coolclient.core;

import dev.coolclient.core.api.GuiRenderer;
import dev.coolclient.core.api.Platform;
import dev.coolclient.core.config.ConfigManager;
import dev.coolclient.core.cosmetic.CosmeticsManager;
import dev.coolclient.core.event.EventBus;
import dev.coolclient.core.event.Events;
import dev.coolclient.core.gui.Notifications;
import dev.coolclient.core.gui.UIScreen;
import dev.coolclient.core.gui.screen.HudEditorScreen;
import dev.coolclient.core.hud.HudManager;
import dev.coolclient.core.module.ModuleManager;
import dev.coolclient.core.modules.Modules;
import dev.coolclient.core.modules.client.ClientSettings;
import dev.coolclient.core.net.ApiClient;
import dev.coolclient.core.net.ServerRules;
import dev.coolclient.core.render.Scissor;
import dev.coolclient.core.util.CpsTracker;
import dev.coolclient.core.util.Keys;
import dev.coolclient.core.util.Log;
import dev.coolclient.core.util.Scheduler;
import dev.coolclient.core.util.Time;
import dev.coolclient.core.waypoint.WaypointManager;

/**
 * The client. Platforms call {@link #init} once and then forward game hooks to the
 * {@code on*} methods below; everything else is version-independent.
 */
public final class CoolClient {
    public static final String NAME = "Cool Client";
    public static final String VERSION = "0.1.5-beta";
    // Set these two before a public release.
    /** Public source repository (shown on the welcome screen). */
    public static final String SOURCE_URL = "https://github.com/XonistReal/coolclient";
    /** The official API server (cosmetics, entitlements). */
    public static final String DEFAULT_API_URL = "https://coolclient-api.coolclient-api.workers.dev";
    /** The placeholder 0.1.0 and 0.1.1 saved as the API server; such configs move to {@link #DEFAULT_API_URL}. */
    public static final String LEGACY_API_URL = "http://localhost:8787";

    private static CoolClient instance;

    private final Platform platform;
    private final EventBus events = new EventBus();
    private final ModuleManager modules = new ModuleManager();
    private final HudManager hud = new HudManager();
    private final Notifications notifications = new Notifications();
    private final ServerRules serverRules = new ServerRules();
    private final CpsTracker leftCps = new CpsTracker();
    private final CpsTracker rightCps = new CpsTracker();
    private ConfigManager config;
    private CosmeticsManager cosmetics;
    private ApiClient api;
    private WaypointManager waypoints;
    private ClientSettings client;
    private GuiRenderer lastRenderer;
    private String server;
    private boolean inWorld;
    private long worldJoinTime;

    private CoolClient(Platform platform) {
        this.platform = platform;
    }

    public static CoolClient get() {
        return instance;
    }

    public static void init(Platform platform) {
        if (instance != null) return;
        long start = System.currentTimeMillis();
        instance = new CoolClient(platform);
        instance.start();
        Log.info(NAME + " " + VERSION + " ready on Minecraft " + platform.minecraftVersion() + " ("
                + instance.modules.all().size() + " modules, " + (System.currentTimeMillis() - start) + " ms)");
    }

    private void start() {
        config = new ConfigManager(platform.gameDir());
        Modules.registerAll(modules);
        client = modules.get(ClientSettings.class);
        hud.collect();
        config.load();
        api = new ApiClient(config);
        cosmetics = new CosmeticsManager(config.dir(), api);
        cosmetics.load();
        waypoints = new WaypointManager(config.dir());
        waypoints.load();
        client.apply();
    }

    // ================================================================== accessors

    public Platform platform() { return platform; }

    public EventBus events() { return events; }

    public ModuleManager modules() { return modules; }

    public HudManager hud() { return hud; }

    public ConfigManager config() { return config; }

    public Notifications notifications() { return notifications; }

    public CosmeticsManager cosmetics() { return cosmetics; }

    public ApiClient api() { return api; }

    public WaypointManager waypoints() { return waypoints; }

    public ClientSettings client() { return client; }

    public ServerRules serverRules() { return serverRules; }

    public CpsTracker leftCps() { return leftCps; }

    public CpsTracker rightCps() { return rightCps; }

    /** The renderer of the most recent frame (for text measuring outside of draw calls). */
    public GuiRenderer lastRenderer() { return lastRenderer; }

    public String server() { return server; }

    public boolean inWorld() { return inWorld; }

    public long worldJoinTime() { return worldJoinTime; }

    // ================================================================== platform hooks

    /** Start of every rendered frame. */
    public void onFrame() {
        Time.frame();
        SelfTest.frame();
    }

    /** End of every client tick. */
    public void onTick() {
        Scheduler.runMainTasks();
        events.post(Events.Tick.INSTANCE);
        config.tick();
        cosmetics.tick();
        api.tick();
    }

    /** After the vanilla HUD has rendered. */
    public void onRenderHud(GuiRenderer r, float partialTicks) {
        lastRenderer = r;
        Scissor.reset(r);
        boolean editing = platform.currentScreen() instanceof HudEditorScreen;
        if (!platform.game().hudHidden() && !editing) {
            hud.render(r);
            events.post(new Events.RenderHud(r, partialTicks));
        }
        if (platform.currentScreen() == null) notifications.render(r);
    }

    /** Renders a core screen; called by the platform's screen wrapper. */
    public void renderScreen(UIScreen screen, GuiRenderer r, float mx, float my, float delta) {
        lastRenderer = r;
        Scissor.reset(r);
        try {
            screen.render(r, mx, my, delta);
        } catch (Throwable t) {
            Log.error("Screen " + screen.getClass().getSimpleName() + " crashed while rendering", t);
            Scissor.reset(r);
            platform.openScreen(null);
        }
        notifications.render(r);
    }

    /**
     * Key press/release while no screen is open. Returns true to swallow the key.
     */
    public boolean onKey(int key, boolean pressed) {
        Events.Key ev = events.post(new Events.Key(key, pressed));
        if (pressed) {
            if (client.menuKey.bound() && key == client.menuKey.key()) {
                openMenu();
                return true;
            }
            modules.onBindPressed(key);
        }
        return ev.isCancelled();
    }

    /** Mouse button in game (no screen). */
    public void onMouseButton(int button, boolean pressed) {
        if (pressed) {
            if (button == 0) leftCps.click();
            else if (button == 1) rightCps.click();
            modules.onBindPressed(Keys.mouse(button));
        }
        events.post(new Events.MouseButton(button, pressed));
    }

    /** Mouse wheel in game; returns true to cancel hotbar scrolling. */
    public boolean onScroll(double amount) {
        return events.post(new Events.Scroll(amount)).isCancelled();
    }

    public void onAttack(int targetId, String targetName, boolean isPlayer, double distance) {
        events.post(new Events.Attack(targetId, targetName, isPlayer, distance));
    }

    public void onSelfHurt() {
        events.post(Events.SelfHurt.INSTANCE);
    }

    public void onChat(String plain) {
        events.post(new Events.ChatReceived(plain));
    }

    public void onTotemPop(String name, boolean self) {
        events.post(new Events.TotemPop(name, self));
    }

    public void onPlayerDeath(String name) {
        events.post(new Events.PlayerDeath(name));
    }

    public void onWorldJoin(String serverAddress) {
        server = serverAddress;
        inWorld = true;
        worldJoinTime = Time.millis();
        serverRules.apply(serverAddress);
        waypoints.setContext(serverAddress, platform.game().dimension());
        events.post(new Events.WorldJoin(serverAddress));
        cosmetics.onWorldJoin();
    }

    public void onWorldLeave() {
        if (!inWorld) return;
        inWorld = false;
        server = null;
        serverRules.apply(null);
        events.post(Events.WorldLeave.INSTANCE);
    }

    public void onShutdown() {
        try {
            config.saveNow();
            cosmetics.saveNow();
            waypoints.saveNow();
        } catch (Throwable t) {
            Log.error("Failed to save on shutdown", t);
        }
    }

    public void openMenu() {
        if (platform.anyScreenOpen()) return;
        platform.openScreen(new HudEditorScreen(null));
    }
}
