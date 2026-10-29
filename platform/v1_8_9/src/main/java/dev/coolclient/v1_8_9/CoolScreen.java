package dev.coolclient.v1_8_9;

import dev.coolclient.core.CoolClient;
import dev.coolclient.core.gui.UIScreen;
import net.minecraft.client.gui.screen.Screen;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

/** Vanilla screen that hosts a core {@link UIScreen}. */
public final class CoolScreen extends Screen {
    public final UIScreen ui;

    public CoolScreen(UIScreen ui) {
        this.ui = ui;
    }

    private float mx() { return Mouse.getX() * width / (float) Math.max(1, client.width); }

    private float my() { return height - Mouse.getY() * height / (float) Math.max(1, client.height) - 1; }

    @Override
    public void init() {
        Keyboard.enableRepeatEvents(true);
        ui.init(width, height);
    }

    @Override
    public void render(int mouseX, int mouseY, float delta) {
        CoolClient.get().renderScreen(ui, Platform189.RENDERER.frame(), mx(), my(), delta);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int button) {
        ui.mouseClicked(mx(), my(), button);
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int button) {
        ui.mouseReleased(mx(), my(), button);
    }

    @Override
    public void handleMouse() {
        super.handleMouse();
        int wheel = Mouse.getEventDWheel();
        if (wheel != 0) ui.mouseScrolled(mx(), my(), wheel > 0 ? 1 : -1);
    }

    @Override
    protected void keyPressed(char c, int key) {
        int mods = (hasShiftDown() ? 1 : 0) | (hasControlDown() ? 2 : 0) | (hasAltDown() ? 4 : 0);
        int glfw = KeyMap.toGlfw(key);
        if (key != 0) ui.keyPressed(glfw, mods);
        if (c >= 32 && c != 127 && (mods & 2) == 0) ui.charTyped(c);
    }

    @Override
    public void tick() { ui.tick(); }

    @Override
    public void removed() {
        Keyboard.enableRepeatEvents(false);
        ui.onClose();
    }

    @Override
    public boolean shouldPauseGame() { return ui.pausesGame(); }
}
