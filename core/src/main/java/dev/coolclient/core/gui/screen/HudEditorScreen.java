package dev.coolclient.core.gui.screen;

import dev.coolclient.core.CoolClient;
import dev.coolclient.core.api.GuiRenderer;
import dev.coolclient.core.font.FontRenderer;
import dev.coolclient.core.font.Fonts;
import dev.coolclient.core.gui.UIScreen;
import dev.coolclient.core.hud.HudElement;
import dev.coolclient.core.render.Animation;
import dev.coolclient.core.render.Colors;
import dev.coolclient.core.render.Draw;
import dev.coolclient.core.render.Easing;
import dev.coolclient.core.render.Icons;
import dev.coolclient.core.render.Theme;
import dev.coolclient.core.util.MathUtil;

import java.util.List;

/** Drag-and-drop HUD layout editor with the main "dock" of CoolClient menus in the centre. */
public final class HudEditorScreen extends UIScreen {
    private static final float SNAP = 4f;

    private HudElement dragging;
    private float dragOffX, dragOffY;
    private HudElement hovered;
    private float guideX = -1, guideY = -1;
    private final Animation dock = new Animation(300, Easing.OUT_BACK, 0f);
    private final Animation[] dockHover = {new Animation(150, 0f), new Animation(150, 0f), new Animation(150, 0f), new Animation(150, 0f)};

    public HudEditorScreen(UIScreen parent) {
        super(parent);
        dock.target(1f);
    }

    private static final String[] DOCK_LABELS = {"Mods", "Cosmetics", "Settings", "Reset HUD"};
    private static final Icons.Icon[] DOCK_ICONS = {Icons.Icon.GRID, Icons.Icon.HANGER, Icons.Icon.GEAR, Icons.Icon.RESET};

    @Override
    public void render(GuiRenderer r, float mx, float my, float delta) {
        float open = openProgress();
        Draw.rect(r, 0, 0, width, height, Colors.fade(0x66040710, open));
        List<HudElement> elements = CoolClient.get().hud().elements();

        if (dragging != null) {
            if (!CoolClient.get().platform().input().isMouseDown(0)) {
                dragging = null;
                CoolClient.get().config().markDirty();
            } else {
                drag(mx, my, elements);
            }
        }
        if (dragging != null) drawGrid(r);

        hovered = null;
        for (int i = elements.size() - 1; i >= 0; i--) {
            HudElement e = elements.get(i);
            if (!e.isEnabled()) continue;
            if (MathUtil.inside(mx, my, e.screenX(width), e.screenY(height), e.scaledWidth(), e.scaledHeight())) {
                hovered = e;
                break;
            }
        }

        for (HudElement e : elements) {
            if (!e.isEnabled()) continue;
            try {
                e.render(r, true);
            } catch (Throwable t) {
                continue;
            }
            float x = e.screenX(width), y = e.screenY(height), w = e.scaledWidth(), h = e.scaledHeight();
            boolean hot = e == hovered || e == dragging;
            Draw.rounded(r, x - 1.5f, y - 1.5f, w + 3, h + 3, 2.5f, Colors.withAlpha(0xFFFFFF, hot ? 28 : 10));
            Draw.roundedOutline(r, x - 1.5f, y - 1.5f, w + 3, h + 3, 2.5f, 0.75f,
                    hot ? Theme.accent(0.2f) : Colors.withAlpha(0xFFFFFF, 50));
        }

        if (guideX >= 0) Draw.rect(r, guideX - 0.25f, 0, 0.5f, height, Colors.withAlpha(Theme.accent(0f), 200));
        if (guideY >= 0) Draw.rect(r, 0, guideY - 0.25f, width, 0.5f, Colors.withAlpha(Theme.accent(1f), 200));

        if (hovered != null && dragging == null) {
            FontRenderer f = Fonts.small();
            String tip = hovered.name() + "  ·  " + Math.round(hovered.scale() * 100) + "%  ·  scroll to resize, right-click for settings";
            float tw = f.width(tip) + 10;
            float tx = MathUtil.clamp(mx + 8, 2, width - tw - 2), ty = MathUtil.clamp(my + 12, 2, height - 14);
            Draw.rounded(r, tx, ty, tw, 12, 3, Theme.panel());
            f.draw(r, tip, tx + 5, ty + 2.5f, Theme.TEXT, false);
        }

        drawDock(r, mx, my);
    }

    private void drawGrid(GuiRenderer r) {
        int c = Colors.withAlpha(0xFFFFFF, 8);
        for (float x = 0; x < width; x += 20) Draw.rect(r, x, 0, 0.5f, height, c);
        for (float y = 0; y < height; y += 20) Draw.rect(r, 0, y, width, 0.5f, c);
        Draw.rect(r, width / 2f - 0.25f, 0, 0.5f, height, Colors.withAlpha(0xFFFFFF, 25));
        Draw.rect(r, 0, height / 2f - 0.25f, width, 0.5f, Colors.withAlpha(0xFFFFFF, 25));
    }

    private float[] dockRect() {
        float w = 260, h = 66;
        return new float[]{(width - w) / 2f, (height - h) / 2f, w, h};
    }

    private void drawDock(GuiRenderer r, float mx, float my) {
        float d = dock.get() * (dragging != null ? 0.25f : 1f);
        if (d <= 0.01f) return;
        float[] b = dockRect();
        float cx = b[0] + b[2] / 2f, cy = b[1] + b[3] / 2f;
        r.push();
        r.translate(cx, cy);
        float s = 0.9f + 0.1f * Math.min(1f, dock.get());
        r.scale(s, s);
        r.translate(-cx, -cy);
        int alpha = (int) (255 * Math.min(1f, d));
        Draw.shadow(r, b[0], b[1], b[2], b[3], 10, 16, Colors.withAlpha(0x000000, alpha / 2));
        Draw.panel(r, b[0], b[1], b[2], b[3], 10, d);
        Draw.roundedOutlineGradient(r, b[0], b[1], b[2], b[3], 10, 1f,
                Colors.fade(Theme.accent(0f), d * 0.7f), Colors.fade(Theme.accent(1f), d * 0.7f));
        float bw = (b[2] - 20) / 4f;
        for (int i = 0; i < 4; i++) {
            float bx = b[0] + 10 + i * bw, by = b[1] + 8;
            boolean hot = MathUtil.inside(mx, my, bx + 2, by, bw - 4, b[3] - 16) && dragging == null;
            dockHover[i].target(hot);
            float hv = dockHover[i].get();
            if (hv > 0.01f) Draw.rounded(r, bx + 2, by, bw - 4, b[3] - 16, 7, Theme.highlight((int) (18 * hv)));
            int ic = Colors.fade(Colors.lerp(Theme.TEXT_DIM, Theme.accent(i / 3f), hv), d);
            Icons.draw(r, DOCK_ICONS[i], bx + bw / 2f - 8, by + 7 - hv * 1.5f, 16, ic);
            Fonts.small().drawCentered(r, DOCK_LABELS[i], bx + bw / 2f, by + 31, Colors.fade(Theme.TEXT, d), false);
        }
        r.pop();
    }

    private void drag(float mx, float my, List<HudElement> elements) {
        float w = dragging.scaledWidth(), h = dragging.scaledHeight();
        float x = mx - dragOffX, y = my - dragOffY;
        guideX = -1;
        guideY = -1;
        boolean snap = CoolClient.get().client().hudSnapping.on() && !CoolClient.get().platform().input().shift();
        if (snap) {
            float[] xs = {x, x + w / 2f, x + w};
            float[] ys = {y, y + h / 2f, y + h};
            float bestDx = SNAP + 1, bestDy = SNAP + 1;
            float snapX = x, snapY = y;
            float[] targetsX = {SNAP, width / 2f, width - SNAP};
            float[] targetsY = {SNAP, height / 2f, height - SNAP};
            for (int i = 0; i < 3; i++) {
                for (float tx : targetsX) {
                    float dx = Math.abs(xs[i] - tx);
                    if (dx < bestDx) { bestDx = dx; snapX = tx - (xs[i] - x); guideX = tx; }
                }
                for (float ty : targetsY) {
                    float dy = Math.abs(ys[i] - ty);
                    if (dy < bestDy) { bestDy = dy; snapY = ty - (ys[i] - y); guideY = ty; }
                }
            }
            for (HudElement o : elements) {
                if (o == dragging || !o.isEnabled()) continue;
                float ox = o.screenX(width), oy = o.screenY(height), ow = o.scaledWidth(), oh = o.scaledHeight();
                float[] oxs = {ox, ox + ow / 2f, ox + ow};
                float[] oys = {oy, oy + oh / 2f, oy + oh};
                for (int i = 0; i < 3; i++) {
                    for (float tx : oxs) {
                        float dx = Math.abs(xs[i] - tx);
                        if (dx < bestDx) { bestDx = dx; snapX = tx - (xs[i] - x); guideX = tx; }
                    }
                    for (float ty : oys) {
                        float dy = Math.abs(ys[i] - ty);
                        if (dy < bestDy) { bestDy = dy; snapY = ty - (ys[i] - y); guideY = ty; }
                    }
                }
            }
            if (bestDx <= SNAP) x = snapX; else guideX = -1;
            if (bestDy <= SNAP) y = snapY; else guideY = -1;
        }
        dragging.setScreenPosition(x, y, width, height);
    }

    @Override
    public boolean mouseClicked(float mx, float my, int button) {
        float[] b = dockRect();
        if (MathUtil.inside(mx, my, b[0], b[1], b[2], b[3])) {
            float bw = (b[2] - 20) / 4f;
            int i = (int) ((mx - b[0] - 10) / bw);
            if (i >= 0 && i < 4 && button == 0) {
                CoolClient.get().platform().playClickSound();
                switch (i) {
                    case 0: CoolClient.get().platform().openScreen(new ModMenuScreen(this)); break;
                    case 1: CoolClient.get().platform().openScreen(new CosmeticsScreen(this)); break;
                    case 2: CoolClient.get().platform().openScreen(ModMenuScreen.clientSettings(this)); break;
                    default:
                        CoolClient.get().hud().resetAll();
                        CoolClient.get().notifications().info("HUD reset", "All elements moved back to default positions");
                        break;
                }
            }
            return true;
        }
        if (hovered != null) {
            if (button == 0) {
                dragging = hovered;
                dragOffX = mx - hovered.screenX(width);
                dragOffY = my - hovered.screenY(height);
                return true;
            }
            if (button == 1) {
                CoolClient.get().platform().openScreen(ModMenuScreen.forModule(this, hovered));
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseReleased(float mx, float my, int button) {
        if (dragging != null) {
            dragging = null;
            guideX = guideY = -1;
            CoolClient.get().config().markDirty();
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(float mx, float my, double amount) {
        if (hovered == null) return false;
        float cx = hovered.screenX(width) + hovered.scaledWidth() / 2f;
        float cy = hovered.screenY(height) + hovered.scaledHeight() / 2f;
        hovered.setScale(hovered.scale() + (amount > 0 ? 0.05f : -0.05f));
        hovered.setScreenPosition(cx - hovered.scaledWidth() / 2f, cy - hovered.scaledHeight() / 2f, width, height);
        CoolClient.get().config().markDirty();
        return true;
    }

    @Override
    public void onClose() {
        CoolClient.get().config().markDirty();
    }
}
