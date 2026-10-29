package dev.coolclient.v1_8_9;

import dev.coolclient.core.api.Input;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

final class Input189 implements Input {
    @Override
    public boolean isKeyDown(int glfwKey) {
        int k = KeyMap.toLwjgl(glfwKey);
        return k > 0 && Keyboard.isCreated() && Keyboard.isKeyDown(k);
    }

    @Override
    public boolean isMouseDown(int button) {
        return Mouse.isCreated() && button >= 0 && button < Mouse.getButtonCount() && Mouse.isButtonDown(button);
    }
}
