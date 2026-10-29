package dev.coolclient.launcher.gl;

import dev.coolclient.core.api.Textures;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;

import static org.lwjgl.opengl.GL33C.*;

/** Core textures on plain OpenGL. Handles are GL texture names. Render thread only. */
public final class GlTextures implements Textures {
    private int white;

    @Override
    public int white() {
        if (white == 0) white = create("white", 1, 1, new int[]{0xFFFFFFFF}, false);
        return white;
    }

    @Override
    public int create(String name, int width, int height, int[] argb, boolean linear) {
        int id = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, id);
        int filter = linear ? GL_LINEAR : GL_NEAREST;
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, filter);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, filter);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        upload(width, height, argb, true);
        return id;
    }

    @Override
    public void update(int handle, int[] argb) {
        glBindTexture(GL_TEXTURE_2D, handle);
        int w = glGetTexLevelParameteri(GL_TEXTURE_2D, 0, GL_TEXTURE_WIDTH);
        int h = glGetTexLevelParameteri(GL_TEXTURE_2D, 0, GL_TEXTURE_HEIGHT);
        upload(w, h, argb, false);
    }

    @Override
    public void delete(int handle) {
        if (handle > 0 && handle != white) glDeleteTextures(handle);
    }

    private static void upload(int w, int h, int[] argb, boolean allocate) {
        ByteBuffer buf = MemoryUtil.memAlloc(w * h * 4);
        try {
            for (int i = 0; i < w * h; i++) {
                int c = argb[i];
                buf.put((byte) (c >> 16)).put((byte) (c >> 8)).put((byte) c).put((byte) (c >>> 24));
            }
            buf.flip();
            glPixelStorei(GL_UNPACK_ALIGNMENT, 1);
            if (allocate) glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, w, h, 0, GL_RGBA, GL_UNSIGNED_BYTE, buf);
            else glTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, w, h, GL_RGBA, GL_UNSIGNED_BYTE, buf);
        } finally {
            MemoryUtil.memFree(buf);
        }
    }
}
