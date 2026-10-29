package dev.coolclient.v1_8_9;

import com.mojang.blaze3d.platform.GLX;
import com.mojang.blaze3d.platform.GlStateManager;
import dev.coolclient.core.CoolClient;
import dev.coolclient.core.modules.visual.HandChams;
import org.lwjgl.opengl.GL11;

/**
 * GL state for Hand Chams. Only ever applied around the LOCAL player's first-person arm and
 * held item, never to other entities.
 */
public final class Chams189 {
    /** ARGB colour the item renderer should use while drawing the first-person item, or 0. */
    public static int itemColor;
    /** ARGB colour for the local first-person arm (vanilla resets GL colour inside renderRightArm), or 0. */
    public static int armColor;
    public static boolean wireframePass;

    private Chams189() {}

    public static HandChams module() {
        return CoolClient.get().modules().get(HandChams.class);
    }

    public static void begin(int argb, boolean forItem) {
        HandChams m = module();
        float a = ((argb >>> 24) & 0xFF) / 255f, r = ((argb >> 16) & 0xFF) / 255f;
        float g = ((argb >> 8) & 0xFF) / 255f, b = (argb & 0xFF) / 255f;
        GlStateManager.enableBlend();
        if (m.glow()) {
            GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
            GlStateManager.disableLighting();
            GLX.gl13MultiTexCoord2f(GLX.lightmapTextureUnit, 240f, 240f);
        } else {
            GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        }
        if (!m.textured()) GlStateManager.disableTexture();
        GlStateManager.color(r, g, b, a);
        if (forItem) itemColor = argb == 0 ? 1 : argb;
        else armColor = argb == 0 ? 1 : argb;
    }

    public static void end() {
        itemColor = 0;
        armColor = 0;
        GlStateManager.color(1f, 1f, 1f, 1f);
        GlStateManager.enableTexture();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GlStateManager.disableBlend();
        GlStateManager.enableLighting();
    }

    public static void beginWireframe(int argb, float width, boolean forItem) {
        wireframePass = true;
        GlStateManager.disableTexture();
        GlStateManager.enableBlend();
        GL11.glPolygonMode(GL11.GL_FRONT_AND_BACK, GL11.GL_LINE);
        GL11.glLineWidth(width);
        int opaque = argb | 0xFF000000;
        GlStateManager.color(((opaque >> 16) & 0xFF) / 255f, ((opaque >> 8) & 0xFF) / 255f, (opaque & 0xFF) / 255f, 1f);
        if (forItem) itemColor = opaque;
        else armColor = opaque;
    }

    public static void endWireframe() {
        GL11.glPolygonMode(GL11.GL_FRONT_AND_BACK, GL11.GL_FILL);
        GL11.glLineWidth(1f);
        wireframePass = false;
        end();
    }
}
