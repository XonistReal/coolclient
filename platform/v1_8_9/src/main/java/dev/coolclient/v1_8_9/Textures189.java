package dev.coolclient.v1_8_9;

import dev.coolclient.core.api.Textures;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;
import org.lwjgl.opengl.GL11;

import java.util.HashMap;
import java.util.Map;

/** Dynamic textures registered with the TextureManager so vanilla code (capes) can bind them. */
public final class Textures189 implements Textures {
    private static final class Tex {
        NativeImageBackedTexture texture;
        Identifier id;
        boolean linear;
        int size;
    }

    private final Map<Integer, Tex> textures = new HashMap<Integer, Tex>();
    private int next = 1;
    private int white = -1;

    @Override
    public int white() {
        if (white < 0) white = create("white", 1, 1, new int[]{0xFFFFFFFF}, false);
        return white;
    }

    @Override
    public int create(String name, int width, int height, int[] argb, boolean linear) {
        NativeImageBackedTexture t = new NativeImageBackedTexture(width, height);
        System.arraycopy(argb, 0, t.getPixels(), 0, Math.min(argb.length, width * height));
        t.upload();
        int handle = next++;
        Tex tex = new Tex();
        tex.texture = t;
        tex.linear = linear;
        tex.size = width * height;
        tex.id = new Identifier("coolclient", "dynamic/" + handle);
        MinecraftClient.getInstance().getTextureManager().loadTexture(tex.id, t);
        filter(t.getGlId(), linear);
        textures.put(handle, tex);
        return handle;
    }

    @Override
    public void update(int handle, int[] argb) {
        Tex t = textures.get(handle);
        if (t == null) return;
        System.arraycopy(argb, 0, t.texture.getPixels(), 0, Math.min(argb.length, t.size));
        t.texture.upload();
        filter(t.texture.getGlId(), t.linear);
    }

    @Override
    public void delete(int handle) {
        Tex t = textures.remove(handle);
        if (t != null) MinecraftClient.getInstance().getTextureManager().close(t.id);
    }

    public int glId(int handle) {
        Tex t = textures.get(handle);
        return t == null ? 0 : t.texture.getGlId();
    }

    public Identifier identifier(int handle) {
        Tex t = textures.get(handle);
        return t == null ? null : t.id;
    }

    static void filter(int glId, boolean linear) {
        com.mojang.blaze3d.platform.GlStateManager.bindTexture(glId);
        int f = linear ? GL11.GL_LINEAR : GL11.GL_NEAREST;
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, f);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, f);
    }
}
