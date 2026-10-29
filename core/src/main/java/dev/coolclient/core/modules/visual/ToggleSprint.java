package dev.coolclient.core.modules.visual;

import dev.coolclient.core.event.Events;
import dev.coolclient.core.hud.TextElement;
import dev.coolclient.core.module.Category;
import dev.coolclient.core.setting.BoolSetting;

/** Toggle sprint/sneak with a classic "[Sprinting (Toggled)]" HUD line. */
public final class ToggleSprint extends TextElement {
    public final BoolSetting sprint = add(new BoolSetting("Toggle Sprint", true));
    public final BoolSetting sneak = add(new BoolSetting("Toggle Sneak", false));
    public final BoolSetting showText = add(new BoolSetting("Show HUD Text", true));
    private boolean sprintToggled;
    private boolean sneakToggled;

    public ToggleSprint() {
        super("Toggle Sprint", "Press sprint once instead of holding it.");
        defaultPosition(Anchor.BOTTOM_LEFT, 4, -4);
        labelMode.set("None");
        brackets.set(true);
        style.background.set(false);
        enabledByDefault();
        on(Events.Key.class, e -> {
            if (!e.pressed) return;
            if (sprint.on() && e.key == game().bindingKey("sprint")) sprintToggled = !sprintToggled;
            if (sneak.on() && e.key == game().bindingKey("sneak")) sneakToggled = !sneakToggled;
        });
        on(Events.Tick.class, e -> {
            if (!game().inWorld()) return;
            if (sprint.on() && sprintToggled) game().setSprintHeld(true);
            if (sneak.on() && sneakToggled) game().setSneakHeld(true);
        });
        on(Events.WorldLeave.class, e -> sneakToggled = false);
    }

    @Override
    public dev.coolclient.core.module.Category category() { return Category.MECHANIC; }

    @Override
    protected void onDisable() {
        if (sprintToggled) game().setSprintHeld(false);
        if (sneakToggled) game().setSneakHeld(false);
        sprintToggled = false;
        sneakToggled = false;
    }

    @Override
    public boolean shouldRender() {
        return showText.on() && !value().isEmpty();
    }

    @Override protected String label() { return ""; }

    @Override
    protected String value() {
        if (sneak.on() && sneakToggled) return "Sneaking (Toggled)";
        if (game().inWorld() && game().sneaking()) return "Sneaking (Key Held)";
        if (sprint.on() && sprintToggled) return "Sprinting (Toggled)";
        if (game().inWorld() && game().sprinting()) return "Sprinting (Vanilla)";
        return "";
    }

    @Override protected String previewValue() { return "Sprinting (Toggled)"; }
}
