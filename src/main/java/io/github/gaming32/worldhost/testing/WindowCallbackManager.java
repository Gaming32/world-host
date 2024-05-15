package io.github.gaming32.worldhost.testing;

import org.lwjgl.glfw.GLFWCharModsCallbackI;
import org.lwjgl.glfw.GLFWCursorPosCallbackI;
import org.lwjgl.glfw.GLFWDropCallbackI;
import org.lwjgl.glfw.GLFWKeyCallbackI;
import org.lwjgl.glfw.GLFWMouseButtonCallbackI;
import org.lwjgl.glfw.GLFWScrollCallbackI;

public class WindowCallbackManager {
    public static GLFWKeyCallbackI keyCallback;
    public static GLFWCharModsCallbackI charModsCallback;
    public static GLFWCursorPosCallbackI cursorPosCallback;
    public static GLFWMouseButtonCallbackI mouseButtonCallback;
    public static GLFWScrollCallbackI scrollCallback;
    public static GLFWDropCallbackI dropCallback;
}
