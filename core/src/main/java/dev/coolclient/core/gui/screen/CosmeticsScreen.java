package dev.coolclient.core.gui.screen;

import dev.coolclient.core.CoolClient;
import dev.coolclient.core.api.GuiRenderer;
import dev.coolclient.core.cosmetic.Cosmetic;
import dev.coolclient.core.cosmetic.CosmeticCatalog;
import dev.coolclient.core.cosmetic.CosmeticType;
import dev.coolclient.core.cosmetic.CosmeticsManager;
import dev.coolclient.core.cosmetic.Loadout;
import dev.coolclient.core.cosmetic.cape.CapeCosmetic;
import dev.coolclient.core.font.FontRenderer;
import dev.coolclient.core.font.Fonts;
import dev.coolclient.core.gui.UIScreen;
import dev.coolclient.core.gui.widget.Button;
import dev.coolclient.core.gui.widget.ColorPicker;
import dev.coolclient.core.gui.widget.ScrollArea;
import dev.coolclient.core.gui.widget.Toggle;
import dev.coolclient.core.render.Animation;
import dev.coolclient.core.render.Colors;
import dev.coolclient.core.render.Draw;
import dev.coolclient.core.render.Icons;
import dev.coolclient.core.render.Scissor;
import dev.coolclient.core.render.Theme;
import dev.coolclient.core.setting.ColorSetting;
import dev.coolclient.core.util.MathUtil;
import dev.coolclient.core.util.Time;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** The cosmetics locker: browse, equip and recolour cosmetics with a live preview. */
public final class CosmeticsScreen extends UIScreen {
    private static final float SIDEBAR = 108;
    private static final float PREVIEW = 138;
    private static CosmeticType rememberedType = CosmeticType.CAPE;

    private CosmeticType type = rememberedType;
    private Cosmetic focused;
    private ColorSetting colorProxy;
    private ColorPicker picker;
    private final ScrollArea scroll = new ScrollArea();
    private final Map<Object, Animation> hovers = new HashMap<Object, Animation>();
    private final Toggle showOwn;
    private final Button unequip;
    private final Button unlock;
    private float px, py, pw, ph;
    private float previewYaw = 200f;
    private boolean draggingPreview;
    private float lastMx;

    public CosmeticsScreen(UIScreen parent) {
        super(parent);
        final CosmeticsManager cm = CoolClient.get().cosmetics();
        showOwn = new Toggle(cm::showOwn, cm::setShowOwn);
        unequip = new Button("Unequip", () -> {
            if (focused != null) cm.unequip(focused.type);
        }).icon(Icons.Icon.CLOSE);
        unlock = new Button("Unlock", () -> {
            String url = cm.entitlements().storeUrl();
            if (!url.isEmpty()) CoolClient.get().platform().openUrl(url);
        }).icon(Icons.Icon.STAR).primary();
        cm.entitlements().refresh(false);
        Cosmetic eq = cm.local().cosmetic(type);
        focus(eq != null ? eq : CosmeticCatalog.ofType(type).get(0));
    }

    private void focus(Cosmetic c) {
        focused = c;
        picker = null;
        colorProxy = null;
        if (c != null && c.tintable) {
            final Loadout l = CoolClient.get().cosmetics().local();
            final Cosmetic target = c;
            colorProxy = new ColorSetting("Color", l.baseColor(c), false);
            colorProxy.setChroma(l.chroma(c));
            colorProxy.onChange(() -> CoolClient.get().cosmetics().setColor(target, colorProxy.get()));
            picker = new ColorPicker(colorProxy);
        }
    }

    @Override
    protected void layout() {
        pw = Math.min(width - 24, 600);
        ph = Math.min(height - 24, 360);
        px = (width - pw) / 2f;
        py = (height - ph) / 2f;
    }

    private float stageHeight() {
        return Math.min(130, Math.max(70, (ph - 20) * 0.42f));
    }

    private Animation hover(Object key, boolean hot) {
        Animation a = hovers.get(key);
        if (a == null) {
            a = new Animation(140, 0f);
            hovers.put(key, a);
        }
        a.target(hot);
        return a;
    }

    @Override
    public void render(GuiRenderer r, float mx, float my, float delta) {
        float open = openProgress();
        layout();
        py += (1f - open) * 14f;
        Draw.rect(r, 0, 0, width, height, Colors.fade(Theme.BACKDROP, open));
        Draw.beginPanels(r);
        Draw.shadow(r, px, py, pw, ph, 10, 24, Colors.fade(0x90000000, open));
        Draw.panel(r, px, py, pw, ph, 10, open);
        Draw.rounded(r, px, py, SIDEBAR, ph, 10, 0, 0, 10, Colors.fade(Theme.light ? 0x0C000000 : 0x40000000, open));
        Draw.roundedOutline(r, px, py, pw, ph, 10, 0.75f, Colors.fade(Theme.BORDER, open));

        // Sidebar
        boolean backHot = MathUtil.inside(mx, my, px + 10, py + 10, 18, 18);
        Draw.rounded(r, px + 10, py + 10, 18, 18, 5, backHot ? Theme.SURFACE_HOVER : Theme.SURFACE);
        Icons.draw(r, Icons.Icon.CHEVRON_LEFT, px + 14, py + 14, 10, Theme.TEXT);
        Fonts.title().draw(r, "Cosmetics", px + 34, py + 13, Theme.TEXT, false);
        float y = py + 40;
        for (CosmeticType t : CosmeticType.values()) {
            boolean active = t == type;
            boolean hot = MathUtil.inside(mx, my, px + 8, y, SIDEBAR - 16, 17);
            float hv = hover(t, hot || active).get();
            if (hv > 0.01f) Draw.rounded(r, px + 8, y, SIDEBAR - 16, 17, 5, Theme.highlight((int) ((active ? 22 : 12) * hv)));
            if (active) Draw.rounded(r, px + 8, y + 4, 2.5f, 9, 1.25f, Theme.accent(0.3f));
            Icons.draw(r, iconFor(t), px + 16, y + 4, 9, active ? Theme.accent(0.2f) : Theme.TEXT_DIM);
            Fonts.body().draw(r, t.displayName, px + 30, y + 4, active ? Theme.TEXT : Theme.TEXT_DIM, false);
            Cosmetic eq = CoolClient.get().cosmetics().local().cosmetic(t);
            if (eq != null) Draw.circle(r, px + SIDEBAR - 16, y + 8.5f, 2, Theme.accent(0.5f));
            y += 19;
        }
        Fonts.small().draw(r, "Show my cosmetics", px + 12, py + ph - 20, Theme.TEXT_DIM, false);
        showOwn.setBounds(px + SIDEBAR - 32, py + ph - 22, 22, 11);
        showOwn.render(r, mx, my);

        drawGrid(r, mx, my);
        drawPreview(r, mx, my);
    }

    private static Icons.Icon iconFor(CosmeticType t) {
        switch (t) {
            case CAPE: return Icons.Icon.HANGER;
            case WINGS: return Icons.Icon.SPARKLE;
            case HAT: return Icons.Icon.CROWN;
            case HALO: return Icons.Icon.STAR;
            case BANDANA: return Icons.Icon.USER;
            default: return Icons.Icon.MOON;
        }
    }

    private void drawGrid(GuiRenderer r, float mx, float my) {
        float gx = px + SIDEBAR + 12, gy = py + 12, gw = pw - SIDEBAR - PREVIEW - 30, gh = ph - 24;
        List<Cosmetic> list = CosmeticCatalog.ofType(type);
        int cols = Math.max(2, (int) (gw / 88));
        float gap = 7, cw = (gw - gap * (cols - 1)) / cols, ch = 96;
        int rowsN = (list.size() + cols - 1) / cols;
        scroll.setBounds(gx, gy, gw, gh);
        scroll.setContentHeight(rowsN * (ch + gap) - gap);
        Scissor.push(r, gx - 2, gy - 2, gw + 4, gh + 4);
        Loadout l = CoolClient.get().cosmetics().local();
        for (int i = 0; i < list.size(); i++) {
            Cosmetic c = list.get(i);
            float x = gx + (i % cols) * (cw + gap);
            float y = gy + (i / cols) * (ch + gap) - scroll.offset();
            if (y + ch < gy || y > gy + gh) continue;
            boolean hot = MathUtil.inside(mx, my, x, y, cw, ch) && scroll.contains(mx, my);
            float hv = hover(c, hot).get();
            boolean eq = l.isEquipped(c);
            boolean foc = c == focused;
            y -= hv * 1.5f;
            if (eq) Draw.glow(r, x, y, cw, ch, 7, 5, Colors.withAlpha(Theme.accent(0.3f), 70));
            Draw.rounded(r, x, y, cw, ch, 7, Colors.lerp(Theme.SURFACE, Theme.SURFACE_HOVER, hv));
            Draw.roundedOutline(r, x, y, cw, ch, 7, eq ? 1.2f : 0.75f,
                    eq ? Theme.accent(0.3f) : (foc ? Theme.BORDER_STRONG : Theme.BORDER));
            // Rarity gradient backdrop
            Draw.roundedGradient(r, x + 4, y + 4, cw - 8, 58, 5, Colors.withAlpha(c.rarity.color, 40), Colors.withAlpha(c.rarity.color, 8), false);
            drawThumb(r, c, x + cw / 2f, y + 33, l);
            FontRenderer f = Fonts.body();
            f.drawCentered(r, f.trim(c.name, cw - 8), x + cw / 2f, y + 67, Theme.TEXT, false);
            Fonts.small().drawCentered(r, c.rarity.displayName, x + cw / 2f, y + 79, c.rarity.color, false);
            if (eq) {
                Draw.circle(r, x + cw - 9, y + 9, 5, Theme.accent(0.3f));
                Icons.draw(r, Icons.Icon.CHECK, x + cw - 13, y + 5, 8, Theme.ON_ACCENT);
            }
            if (!CoolClient.get().cosmetics().entitlements().owns(c)) lockChip(r, c, x + 7, y + 7);
        }
        Scissor.pop(r);
        scroll.update(r, mx, my, CoolClient.get().platform().input().isMouseDown(0));
    }

    /** Tier pill on cosmetics this account doesn't own (only shown when the server enforces ownership). */
    private static void lockChip(GuiRenderer r, Cosmetic c, float x, float y) {
        String label = c.unlock.displayName.toUpperCase(java.util.Locale.ROOT);
        float w = Fonts.get(6f).width(label) + 16;
        Draw.rounded(r, x, y, w, 10, 5, Colors.withAlpha(0x000000, 150));
        Icons.draw(r, Icons.Icon.STAR, x + 3, y + 2, 6, c.unlock.color);
        Fonts.get(6f).draw(r, label, x + 11, y + 2.5f, c.unlock.color, false);
    }

    private void drawThumb(GuiRenderer r, Cosmetic c, float cx, float cy, Loadout l) {
        if (c instanceof CapeCosmetic) {
            int tex = CoolClient.get().cosmetics().capeTexture((CapeCosmetic) c, l.color(c));
            float w = 26, h = w * 1.6f;
            Draw.shadow(r, cx - w / 2f, cy - h / 2f, w, h, 2, 6, 0x60000000);
            Draw.texture(r, tex, cx - w / 2f, cy - h / 2f, w, h, 1 / 64f, 1 / 32f, 11 / 64f, 17 / 32f, 0xFFFFFFFF, true);
            return;
        }
        int col = l.color(c);
        float bob = (float) Math.sin(Time.seconds() * 2 + c.id.hashCode()) * 1.5f;
        Draw.glow(r, cx - 10, cy - 10 + bob, 20, 20, 10, 10, Colors.withAlpha(col, 60));
        Icons.draw(r, iconFor(c.type), cx - 12, cy - 12 + bob, 24, Colors.withAlpha(col, 255));
    }

    private void drawPreview(GuiRenderer r, float mx, float my) {
        float x = px + pw - PREVIEW - 10, y = py + 10, w = PREVIEW, h = ph - 20;
        Draw.rounded(r, x, y, w, h, 8, 0x30000000);
        if (draggingPreview) {
            if (!CoolClient.get().platform().input().isMouseDown(0)) draggingPreview = false;
            else previewYaw += (mx - lastMx) * 2.2f;
        } else {
            previewYaw += Time.delta() * 20f;
        }
        lastMx = mx;
        float stageH = stageHeight();
        if (CoolClient.get().platform().game().inWorld()) {
            Draw.roundedGradient(r, x + 8, y + 8, w - 16, stageH, 6, Colors.withAlpha(Theme.accent(0f), 30), 0x00000000, false);
            r.playerPreview(x + w / 2f, y + stageH - 6, 52, previewYaw, 0);
            Fonts.small().drawCentered(r, "Drag to rotate", x + w / 2f, y + stageH + 2, Theme.TEXT_FAINT, false);
        } else if (focused instanceof CapeCosmetic) {
            Loadout l = CoolClient.get().cosmetics().local();
            int tex = CoolClient.get().cosmetics().capeTexture((CapeCosmetic) focused, l.color(focused));
            float chh = Math.min(96, stageH - 20), cw = chh / 1.6f;
            Draw.shadow(r, x + (w - cw) / 2f, y + 18, cw, chh, 3, 10, 0x70000000);
            Draw.texture(r, tex, x + (w - cw) / 2f, y + 18, cw, chh, 1 / 64f, 1 / 32f, 11 / 64f, 17 / 32f, 0xFFFFFFFF, true);
        } else {
            Fonts.small().drawCentered(r, "Join a world for a", x + w / 2f, y + 56, Theme.TEXT_FAINT, false);
            Fonts.small().drawCentered(r, "live 3D preview", x + w / 2f, y + 66, Theme.TEXT_FAINT, false);
        }

        if (focused == null) return;
        float iy = y + stageH + 16;
        Fonts.body().draw(r, Fonts.body().trim(focused.name, w - 16), x + 8, iy, Theme.TEXT, false);
        Fonts.small().draw(r, focused.rarity.displayName + " " + focused.type.displayName.replaceAll("s$", ""), x + 8, iy + 11, focused.rarity.color, false);
        List<String> desc = ModMenuScreen.wrap(Fonts.small(), focused.description, w - 16);
        boolean eq = CoolClient.get().cosmetics().local().isEquipped(focused);
        boolean locked = !CoolClient.get().cosmetics().entitlements().owns(focused);
        float bottom = y + h - (eq ? 24 : 4) - (locked ? 32 : 0) - (picker != null ? picker.preferredHeight() + 4 : 0);
        int lines = (int) Math.max(0, Math.min(3, Math.floor((bottom - (iy + 23)) / 9f)));
        for (int i = 0; i < Math.min(lines, desc.size()); i++) {
            Fonts.small().draw(r, desc.get(i), x + 8, iy + 23 + i * 9, Theme.TEXT_DIM, false);
        }
        float by = iy + 23 + Math.min(lines, desc.size()) * 9 + 4;
        if (picker != null) {
            Fonts.small().draw(r, "Color", x + 8, by + 3, Theme.TEXT_DIM, false);
            picker.setBounds(x + 8, by, w - 16, picker.h);
            picker.render(r, mx, my);
            Loadout l = CoolClient.get().cosmetics().local();
            if (colorProxy.chroma() != l.chroma(focused)) CoolClient.get().cosmetics().setChroma(focused, colorProxy.chroma());
            by += picker.preferredHeight() + 4;
        }
        if (eq) {
            unequip.setBounds(x + 8, y + h - 22, w - 16, 16);
            unequip.render(r, mx, my);
        }
        if (locked) {
            float ly = y + h - (eq ? 24 : 4) - 30;
            String note = eq ? "Preview: only you can see it" : "Others see it once unlocked";
            Fonts.small().draw(r, Fonts.small().trim(note, w - 16), x + 8, ly, focused.unlock.color, false);
            unlock.setLabel(focused.unlock == dev.coolclient.core.cosmetic.Unlock.SUPPORTER ? "Get Supporter" : "Buy in store");
            unlock.setBounds(x + 8, ly + 11, w - 16, 16);
            unlock.render(r, mx, my);
        }
    }

    @Override
    public boolean mouseClicked(float mx, float my, int button) {
        if (MathUtil.inside(mx, my, px + 10, py + 10, 18, 18)) {
            close();
            return true;
        }
        if (showOwn.mouseClicked(mx, my, button)) return true;
        if (picker != null && picker.mouseClicked(mx, my, button)) return true;
        if (focused != null && CoolClient.get().cosmetics().local().isEquipped(focused) && unequip.mouseClicked(mx, my, button)) return true;
        if (focused != null && !CoolClient.get().cosmetics().entitlements().owns(focused) && unlock.mouseClicked(mx, my, button)) return true;
        float y = py + 40;
        for (CosmeticType t : CosmeticType.values()) {
            if (MathUtil.inside(mx, my, px + 8, y, SIDEBAR - 16, 17)) {
                CoolClient.get().platform().playClickSound();
                type = t;
                rememberedType = t;
                scroll.reset();
                Cosmetic eq = CoolClient.get().cosmetics().local().cosmetic(t);
                focus(eq != null ? eq : CosmeticCatalog.ofType(t).get(0));
                return true;
            }
            y += 19;
        }
        float pxv = px + pw - PREVIEW - 10;
        if (MathUtil.inside(mx, my, pxv, py + 10, PREVIEW, stageHeight())) {
            draggingPreview = true;
            lastMx = mx;
            return true;
        }
        if (scroll.mouseClicked(mx, my)) return true;
        if (!scroll.contains(mx, my)) return false;
        float gx = px + SIDEBAR + 12, gy = py + 12, gw = pw - SIDEBAR - PREVIEW - 30;
        List<Cosmetic> list = CosmeticCatalog.ofType(type);
        int cols = Math.max(2, (int) (gw / 88));
        float gap = 7, cw = (gw - gap * (cols - 1)) / cols, ch = 96;
        for (int i = 0; i < list.size(); i++) {
            float x = gx + (i % cols) * (cw + gap);
            float cy = gy + (i / cols) * (ch + gap) - scroll.offset();
            if (MathUtil.inside(mx, my, x, cy, cw, ch)) {
                CoolClient.get().platform().playClickSound();
                Cosmetic c = list.get(i);
                if (focused == c || button == 0) CoolClient.get().cosmetics().toggle(c);
                focus(c);
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseReleased(float mx, float my, int button) {
        draggingPreview = false;
        return picker != null && picker.mouseReleased(mx, my, button);
    }

    @Override
    public boolean blursWorld() { return true; }

    @Override
    public boolean mouseScrolled(float mx, float my, double amount) {
        return scroll.scroll(mx, my, amount);
    }
}
