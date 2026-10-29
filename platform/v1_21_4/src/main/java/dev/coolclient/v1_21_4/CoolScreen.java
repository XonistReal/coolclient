package dev.coolclient.v1_21_4;

import com.mojang.blaze3d.platform.Window;
import dev.coolclient.core.CoolClient;
import dev.coolclient.core.gui.UIScreen;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Vanilla screen that hosts a core {@link UIScreen}. */
public final class CoolScreen extends Screen {
    public final UIScreen ui;

    public CoolScreen(UIScreen ui) {
        super(Component.literal("Cool Client"));
        this.ui = ui;
    }

    private float mx() {
        Window w = minecraft.getWindow();
        return (float) (minecraft.mouseHandler.xpos() * w.getGuiScaledWidth() / Math.max(1, w.getScreenWidth()));
    }

    private float my() {
        Window w = minecraft.getWindow();
        return (float) (minecraft.mouseHandler.ypos() * w.getGuiScaledHeight() / Math.max(1, w.getScreenHeight()));
    }

    @Override
    protected void init() {
        ui.init(width, height);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
        if (minecraft.level != null && ui.blursWorld()) renderBlurredBackground();
        CoolClient.get().renderScreen(ui, Platform21.RENDERER.frame(g), mx(), my(), delta);
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float delta) {
        // Core screens draw their own backgrounds.
    }

    @Override
    public boolean mouseClicked(double x, double y, int button) {
        return ui.mouseClicked((float) x, (float) y, button);
    }

    @Override
    public boolean mouseReleased(double x, double y, int button) {
        return ui.mouseReleased((float) x, (float) y, button);
    }

    @Override
    public boolean mouseScrolled(double x, double y, double horizontal, double vertical) {
        return ui.mouseScrolled((float) x, (float) y, vertical);
    }

    @Override
    public boolean keyPressed(int key, int scancode, int mods) {
        ui.keyPressed(key, mods);
        return true;
    }

    @Override
    public boolean charTyped(char c, int mods) {
        return ui.charTyped(c);
    }

    @Override
    public void tick() { ui.tick(); }

    @Override
    public void removed() { ui.onClose(); }

    @Override
    public boolean isPauseScreen() { return ui.pausesGame(); }

    @Override
    public boolean shouldCloseOnEsc() { return false; }
}
