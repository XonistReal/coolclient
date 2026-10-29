package dev.coolclient.v1_21_4;

import com.mojang.blaze3d.platform.NativeImage;
import dev.coolclient.core.api.Textures;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

/** Dynamic textures registered with the TextureManager (capes, fonts, cosmetics). */
public final class Textures21 implements Textures {
    private static final class Tex {
        DynamicTexture texture;
        ResourceLocation id;
        int width, height;
    }

    private final Map<Integer, Tex> textures = new HashMap<Integer, Tex>();
    private int next = 1;
    private int white = -1;

    @Override
    public int white() {
        if (white < 0) white = create("white", 1, 1, new int[]{0xFFFFFFFF}, false);
        return white;
    }

    private static void fill(NativeImage img, int w, int h, int[] argb) {
        int n = Math.min(argb.length, w * h);
        for (int i = 0; i < n; i++) img.setPixel(i % w, i / w, argb[i]);
    }

    @Override
    public int create(String name, int width, int height, int[] argb, boolean linear) {
        NativeImage img = new NativeImage(width, height, false);
        fill(img, width, height, argb);
        DynamicTexture t = new DynamicTexture(img);
        t.setFilter(linear, false);
        int handle = next++;
        Tex tex = new Tex();
        tex.texture = t;
        tex.width = width;
        tex.height = height;
        tex.id = ResourceLocation.fromNamespaceAndPath("coolclient", "dynamic/" + handle);
        Minecraft.getInstance().getTextureManager().register(tex.id, t);
        textures.put(handle, tex);
        return handle;
    }

    @Override
    public void update(int handle, int[] argb) {
        Tex t = textures.get(handle);
        if (t == null || t.texture.getPixels() == null) return;
        fill(t.texture.getPixels(), t.width, t.height, argb);
        t.texture.upload();
    }

    @Override
    public void delete(int handle) {
        Tex t = textures.remove(handle);
        if (t != null) Minecraft.getInstance().getTextureManager().release(t.id);
    }

    public int glId(int handle) {
        Tex t = textures.get(handle);
        return t == null ? 0 : t.texture.getId();
    }

    public ResourceLocation location(int handle) {
        Tex t = textures.get(handle);
        return t == null ? null : t.id;
    }
}
