package dev.coolclient.core.gui.screen;

import dev.coolclient.core.CoolClient;
import dev.coolclient.core.api.GuiRenderer;
import dev.coolclient.core.cosmetic.Cosmetic;
import dev.coolclient.core.cosmetic.CosmeticType;
import dev.coolclient.core.cosmetic.CosmeticsManager;
import dev.coolclient.core.cosmetic.Loadout;
import dev.coolclient.core.cosmetic.PlayerFigure;
import dev.coolclient.core.cosmetic.SkinLoader;
import dev.coolclient.core.cosmetic.Season;
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

    /** Opens on one slot's tab (the self-test uses it). */
    public static CosmeticsScreen on(UIScreen parent, CosmeticType t) {
        rememberedType = t;
        return new CosmeticsScreen(parent);
    }

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
        focus(eq != null ? eq : cm.available(type).get(0));
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
        List<Cosmetic> list = CoolClient.get().cosmetics().available(type);
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

    /**
     * Tier pill on cosmetics this account doesn't own (shown when the server enforces ownership, and
     * on seasonal cosmetics during their season until you keep one).
     */
    private static void lockChip(GuiRenderer r, Cosmetic c, float x, float y) {
        Season s = c.season();
        String label = s != null ? s.chip() : c.unlock.displayName.toUpperCase(java.util.Locale.ROOT);
        int color = s != null ? s.color : c.unlock.color;
        float w = Fonts.get(6f).width(label) + 16;
        Draw.rounded(r, x, y, w, 10, 5, Colors.withAlpha(0x000000, 150));
        Icons.draw(r, Icons.Icon.STAR, x + 3, y + 2, 6, color);
        Fonts.get(6f).draw(r, label, x + 11, y + 2.5f, color, false);
    }

    private void drawThumb(GuiRenderer r, Cosmetic c, float cx, float cy, Loadout l) {
        if (c instanceof CapeCosmetic) {
            int tex = CoolClient.get().cosmetics().capeTexture((CapeCosmetic) c, l.color(c));
            float w = 26, h = w * 1.6f;
            Draw.shadow(r, cx - w / 2f, cy - h / 2f, w, h, 2, 6, 0x60000000);
            Draw.texture(r, tex, cx - w / 2f, cy - h / 2f, w, h, 1 / 64f, 1 / 32f, 11 / 64f, 17 / 32f, 0xFFFFFFFF, true);
            return;
        }
        drawModelThumb(r, c, cx, cy, l);
    }

    private final PlayerFigure thumb = new PlayerFigure();
    private final PlayerFigure stage = new PlayerFigure();

    /** A mannequin (just the head for head cosmetics) wearing the cosmetic, turning slowly. */
    private void drawModelThumb(GuiRenderer r, Cosmetic c, float cx, float cy, Loadout l) {
        final PlayerFigure f = thumb;
        f.wearing.clear();
        f.wearing.put(c.type, c);
        f.colors = l;
        f.skin = 0;
        f.idle = false;
        f.headYaw = 0;
        f.headPitch = 0;
        float t = (float) Time.seconds();
        f.time = t;
        boolean head = c.type == CosmeticType.HAT || c.type == CosmeticType.HALO || c.type == CosmeticType.BANDANA;
        f.showBody = !head;
        boolean wings = c.type == CosmeticType.WINGS;
        float top = head ? -8 - Math.max(2.5f, c.headroom()) : wings ? -17 : -9, bottom = head ? 1 : wings ? 21 : 25;
        float span = head ? 15 : wings ? 50 : 30;
        float base = c.type == CosmeticType.WINGS ? 155 : c.type == CosmeticType.BANDANA ? -40 : -28;
        float yaw = base + (float) Math.sin(t * 0.7f + (c.id.hashCode() & 63)) * 18f;
        float s = Math.min(50f / (bottom - top), (wings ? 56f : 64f) / span);
        final float mid = (top + bottom) / 2f;
        r.drawModel(cx, cy, s, yaw, head ? 14 : 6, mr -> {
            mr.translate(0, -mid, 0);
            f.draw(mr);
        });
    }

    /** The figure on the preview stage outside a world: your skin, wearing your loadout plus whatever you're looking at. */
    private void drawStage(GuiRenderer r, float x, float y, float w, float h) {
        final PlayerFigure f = stage;
        f.wear(CoolClient.get().cosmetics().local());
        if (focused != null) f.wearing.put(focused.type, focused);
        f.skin = SkinLoader.texture();
        f.slim = SkinLoader.slim();
        f.legacySkin = SkinLoader.legacy();
        f.showBody = true;
        f.idle = true;
        f.time = (float) Time.seconds();
        f.headYaw = 0;
        f.headPitch = 0;
        Cosmetic hat = f.wearing.get(CosmeticType.HAT), halo = f.wearing.get(CosmeticType.HALO);
        float top = -9 - Math.max(hat != null ? hat.headroom() : 0, halo != null ? halo.headroom() : 0);
        float s = Math.min((h - 10) / (25 - top), (w - 10) / 30f);
        float feet = y + h - 4;
        Draw.shadow(r, x + w / 2f - 8 * s / 2f, feet - 3, 8 * s, 4, 4, 10, 0x50000000);
        r.drawModel(x + w / 2f, feet - 24 * s, s, previewYaw, 4, f::draw);
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
        } else {
            // No world (title screen): draw the figure ourselves.
            Draw.roundedGradient(r, x + 8, y + 8, w - 16, stageH, 6, Colors.withAlpha(Theme.accent(0f), 30), 0x00000000, false);
            drawStage(r, x + 8, y + 10, w - 16, stageH - 8);
            Fonts.small().drawCentered(r, "Drag to rotate", x + w / 2f, y + stageH + 2, Theme.TEXT_FAINT, false);
        }

        if (focused == null) return;
        float iy = y + stageH + 16;
        Fonts.body().draw(r, Fonts.body().trim(focused.name, w - 16), x + 8, iy, Theme.TEXT, false);
        Fonts.small().draw(r, focused.rarity.displayName + " " + focused.type.displayName.replaceAll("s$", ""), x + 8, iy + 11, focused.rarity.color, false);
        List<String> desc = ModMenuScreen.wrap(Fonts.small(), focused.description, w - 16);
        boolean eq = CoolClient.get().cosmetics().local().isEquipped(focused);
        boolean locked = !CoolClient.get().cosmetics().entitlements().owns(focused);
        Season season = focused.season();
        dev.coolclient.core.cosmetic.Unlock how = focused.unlock;
        String note = null;
        int noteColor = how.color;
        boolean buy = false;
        if (season != null) {
            noteColor = season.color;
            note = !locked ? season.displayName + " drop. It's yours to keep."
                    : "Free until " + season.lastDay() + ". " + (CoolClient.get().api().authenticated()
                    ? "Equip it once and it's yours to keep." : "Turn on Online Features and equip it to keep it.");
        } else if (locked) {
            note = how == dev.coolclient.core.cosmetic.Unlock.EXCLUSIVE ? "Given to the first players. Never sold."
                    : how == dev.coolclient.core.cosmetic.Unlock.MEDIA ? "For creators with the Media rank. Never sold."
                    : how == dev.coolclient.core.cosmetic.Unlock.OWNER ? "Only the owner of Cool Client wears this."
                    : eq ? "Preview: only you can see it" : "Others see it once unlocked";
            buy = how.sold();
        }
        List<String> noteLines = note == null ? java.util.Collections.<String>emptyList() : ModMenuScreen.wrap(Fonts.small(), note, w - 16);
        if (noteLines.size() > 2) noteLines = noteLines.subList(0, 2);
        float noteH = note == null ? 0 : noteLines.size() * 9 + (buy ? 20 : 0) + 2;
        float bottom = y + h - (eq ? 24 : 4) - noteH - (picker != null ? picker.preferredHeight() + 4 : 0);
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
        if (note != null) {
            float ly = y + h - (eq ? 24 : 4) - noteH + 1;
            for (int i = 0; i < noteLines.size(); i++) {
                Fonts.small().draw(r, Fonts.small().trim(noteLines.get(i), w - 16), x + 8, ly + i * 9, noteColor, false);
            }
            if (buy) {
                unlock.setLabel(how == dev.coolclient.core.cosmetic.Unlock.SUPPORTER ? "Get Supporter" : "Buy in store");
                unlock.setBounds(x + 8, ly + noteLines.size() * 9 + 2, w - 16, 16);
                unlock.render(r, mx, my);
            }
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
        if (focused != null && focused.unlock.sold() && focused.season() == null
                && !CoolClient.get().cosmetics().entitlements().owns(focused) && unlock.mouseClicked(mx, my, button)) return true;
        float y = py + 40;
        for (CosmeticType t : CosmeticType.values()) {
            if (MathUtil.inside(mx, my, px + 8, y, SIDEBAR - 16, 17)) {
                CoolClient.get().platform().playClickSound();
                type = t;
                rememberedType = t;
                scroll.reset();
                Cosmetic eq = CoolClient.get().cosmetics().local().cosmetic(t);
                focus(eq != null ? eq : CoolClient.get().cosmetics().available(t).get(0));
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
        List<Cosmetic> list = CoolClient.get().cosmetics().available(type);
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
