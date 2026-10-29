package dev.coolclient.v1_21_4;

import com.mojang.blaze3d.platform.InputConstants;
import dev.coolclient.core.api.Input;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

final class Input21 implements Input {
    private static long window() { return Minecraft.getInstance().getWindow().getWindow(); }

    @Override
    public boolean isKeyDown(int glfwKey) {
        return glfwKey > 0 && InputConstants.isKeyDown(window(), glfwKey);
    }

    @Override
    public boolean isMouseDown(int button) {
        return button >= 0 && GLFW.glfwGetMouseButton(window(), button) == GLFW.GLFW_PRESS;
    }
}
