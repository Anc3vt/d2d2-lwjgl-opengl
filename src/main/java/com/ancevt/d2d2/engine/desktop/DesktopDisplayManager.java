/**
 * Copyright (C) 2025 the original author or authors.
 * See the notice.md file distributed with this work for additional
 * information regarding copyright ownership.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ancevt.d2d2.engine.desktop;

import com.ancevt.d2d2.engine.DisplayManager;
import com.ancevt.d2d2.engine.Monitor;
import com.ancevt.d2d2.engine.WindowState;
import com.ancevt.d2d2.exception.MonitorException;
import org.lwjgl.PointerBuffer;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.system.MemoryUtil.NULL;

public final class DesktopDisplayManager implements DisplayManager {
    private WindowState savedWindowState;

    private boolean mouseVisible;
    private boolean borderless;
    private boolean visible;
    private String windowTitle;

    private static long getWindowId() {
        return CanvasControl.getWindowId();
    }

    @Override
    public List<Monitor> getMonitors() {
        List<Monitor> result = new ArrayList<>();
        PointerBuffer glfwMonitors = glfwGetMonitors();
        for (int i = 0; i < Objects.requireNonNull(glfwMonitors).limit(); i++) {
            long id = glfwMonitors.get(i);
            result.add(new DesktopMonitor(id, this));
        }
        return result;
    }

    @Override
    public Monitor getPrimaryMonitor() {
        return getMonitors().stream()
                .filter(Monitor::isPrimary)
                .findAny()
                .orElseThrow(() -> new MonitorException("No primary monitor detected"));
    }

    @Override
    public WindowState getWindowState() {
        int[] width = new int[1];
        int[] height = new int[1];
        glfwGetWindowSize(getWindowId(), width, height);

        int[] xPos = new int[1];
        int[] yPos = new int[1];
        glfwGetWindowPos(getWindowId(), xPos, yPos);

        return new WindowState(getWindowId(), xPos[0], yPos[0], width[0], height[0]);
    }

    @Override
    public void setTitle(String windowTitle) {
        this.windowTitle = windowTitle;
        glfwSetWindowTitle(getWindowId(), windowTitle);
    }

    @Override
    public String getTitle() {
        return windowTitle;
    }


    void memorizeWindowState() {
        if (savedWindowState == null) {
            savedWindowState = getWindowState();
        }
    }

    @Override
    public void restoreWindowedMode() {
        if (savedWindowState != null) {
            glfwSetWindowMonitor(
                    getWindowId(),
                    NULL,
                    savedWindowState.getX(),
                    savedWindowState.getY(),
                    savedWindowState.getWidth(),
                    savedWindowState.getHeight(),
                    GLFW_DONT_CARE
            );
            savedWindowState = null;
        }
    }

    @Override
    public void setBorderless(boolean borderless) {
        this.borderless = borderless;
        glfwWindowHint(GLFW_DECORATED, borderless ? GLFW_FALSE : GLFW_TRUE);
    }

    @Override
    public boolean isBorderless() {
        return borderless;
    }

    @Override
    public void focusWindow() {
        glfwFocusWindow(getWindowId());
    }

    @Override
    public void setWindowXY(int x, int y) {
        glfwSetWindowPos(getWindowId(), x, y);
    }

    @Override
    public void setWindowSize(int width, int height) {
        glfwSetWindowSize(getWindowId(), width, height);
    }

    @Override
    public void setMouseVisible(boolean mouseVisible) {
        if (this.mouseVisible == mouseVisible) return;
        this.mouseVisible = mouseVisible;
        //glfwSetInputMode(getWindowId(), GLFW_CURSOR, mouseVisible ? GLFW_CURSOR_NORMAL : GLFW_CURSOR_HIDDEN);
    }

    @Override
    public boolean isMouseVisible() {
        return mouseVisible;
    }

    @Override
    public void setVisible(boolean visible) {
        if (this.visible == visible) return;

        this.visible = visible;
        if (visible) {
            glfwShowWindow(getWindowId());
        } else {
            glfwHideWindow(getWindowId());
        }
    }

    @Override
    public boolean isVisible() {
        return visible;
    }


}
