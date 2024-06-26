/**
 * Copyright (C) 2024 the original author or authors.
 * See the notice.md file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.ancevt.d2d2.engine.lwjgl;

import com.ancevt.d2d2.D2D2;
import com.ancevt.d2d2.display.Renderer;
import com.ancevt.d2d2.display.Stage;
import com.ancevt.d2d2.display.interactive.InteractiveManager;
import com.ancevt.d2d2.display.text.Font;
import com.ancevt.d2d2.display.text.FractionalMetrics;
import com.ancevt.d2d2.display.text.TrueTypeFontBuilder;
import com.ancevt.d2d2.engine.DisplayManager;
import com.ancevt.d2d2.engine.Engine;
import com.ancevt.d2d2.event.BaseEventDispatcher;
import com.ancevt.d2d2.event.InteractiveEvent;
import com.ancevt.d2d2.event.LifecycleEvent;
import com.ancevt.d2d2.input.KeyCode;
import com.ancevt.d2d2.input.Mouse;
import com.ancevt.d2d2.lifecycle.SystemProperties;
import com.ancevt.d2d2.time.Timer;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWCursorPosCallback;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWMouseButtonCallback;
import org.lwjgl.glfw.GLFWScrollCallback;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.glfw.GLFWWindowSizeCallback;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static org.lwjgl.glfw.GLFW.GLFW_FALSE;
import static org.lwjgl.glfw.GLFW.GLFW_FLOATING;
import static org.lwjgl.glfw.GLFW.GLFW_MOD_ALT;
import static org.lwjgl.glfw.GLFW.GLFW_MOD_CONTROL;
import static org.lwjgl.glfw.GLFW.GLFW_MOD_SHIFT;
import static org.lwjgl.glfw.GLFW.GLFW_PRESS;
import static org.lwjgl.glfw.GLFW.GLFW_RELEASE;
import static org.lwjgl.glfw.GLFW.GLFW_REPEAT;
import static org.lwjgl.glfw.GLFW.GLFW_TRUE;
import static org.lwjgl.glfw.GLFW.glfwCreateWindow;
import static org.lwjgl.glfw.GLFW.glfwDefaultWindowHints;
import static org.lwjgl.glfw.GLFW.glfwGetPrimaryMonitor;
import static org.lwjgl.glfw.GLFW.glfwGetVideoMode;
import static org.lwjgl.glfw.GLFW.glfwInit;
import static org.lwjgl.glfw.GLFW.glfwMakeContextCurrent;
import static org.lwjgl.glfw.GLFW.glfwPollEvents;
import static org.lwjgl.glfw.GLFW.glfwSetCharCallback;
import static org.lwjgl.glfw.GLFW.glfwSetCursorPosCallback;
import static org.lwjgl.glfw.GLFW.glfwSetKeyCallback;
import static org.lwjgl.glfw.GLFW.glfwSetMouseButtonCallback;
import static org.lwjgl.glfw.GLFW.glfwSetScrollCallback;
import static org.lwjgl.glfw.GLFW.glfwSetWindowPos;
import static org.lwjgl.glfw.GLFW.glfwSetWindowSizeCallback;
import static org.lwjgl.glfw.GLFW.glfwSwapBuffers;
import static org.lwjgl.glfw.GLFW.glfwSwapInterval;
import static org.lwjgl.glfw.GLFW.glfwTerminate;
import static org.lwjgl.glfw.GLFW.glfwWindowHint;
import static org.lwjgl.glfw.GLFW.glfwWindowShouldClose;
import static org.lwjgl.system.MemoryUtil.NULL;

// TODO: rewrite with VBO and refactor
@Slf4j
public class LwjglEngine extends BaseEventDispatcher implements Engine {

    private static final String DEMO_TEXTURE_DATA_INF_FILE = "d2d2-core-demo-texture-data.inf";
    private LwjglRenderer renderer;
    private final int initialWidth;
    private final int initialHeight;
    private final String initialTitle;
    private int mouseX;
    private int mouseY;
    private boolean isDown;
    private Stage stage;
    private boolean running;
    private int frameRate = 60;
    private boolean alwaysOnTop;
    private boolean control;
    private boolean shift;
    private boolean alt;

    private long windowId;

    @Getter
    private int canvasWidth;

    @Getter
    private int canvasHeight;

    private final LwjglDisplayManager displayManager = new LwjglDisplayManager();

    @Getter
    @Setter
    private int timerCheckFrameFrequency = 1;

    public LwjglEngine(int initialWidth, int initialHeight, String initialTitle) {
        this.initialWidth = initialWidth;
        this.initialHeight = initialHeight;
        this.initialTitle = initialTitle;
        this.canvasWidth = initialWidth;
        this.canvasHeight = initialHeight;
        D2D2.textureManager().setTextureEngine(new LwjglTextureEngine());
    }

    @Override
    public void setCanvasSize(int width, int height) {
        canvasWidth = width;
        canvasHeight = height;
    }

    @Override
    public DisplayManager displayManager() {
        return displayManager;
    }

    @Override
    public void setAlwaysOnTop(boolean b) {
        this.alwaysOnTop = b;
        glfwWindowHint(GLFW_FLOATING, alwaysOnTop ? GLFW_TRUE : GLFW_FALSE);
    }

    @Override
    public boolean isAlwaysOnTop() {
        return alwaysOnTop;
    }


    @Override
    public void stop() {
        if (!running) return;
        running = false;
    }


    @Override
    public void create() {
        stage = new Stage();
        renderer = new LwjglRenderer(stage, this);
        renderer.setLWJGLTextureEngine((LwjglTextureEngine) D2D2.textureManager().getTextureEngine());
        displayManager.windowId = createWindow();
        displayManager.setVisible(true);
        stage.setSize(initialWidth, initialHeight);
        renderer.reshape();
    }

    @Override
    public void setSmoothMode(boolean value) {
        renderer.smoothMode = value;

        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_REPEAT);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL11.GL_REPEAT);

        if (value) {
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        } else {
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
        }
    }


    @Override
    public boolean isSmoothMode() {
        return ((LwjglRenderer) renderer).smoothMode;
    }

    @Override
    public void start() {
        running = true;
        stage.dispatchEvent(
            LifecycleEvent.builder()
                .type(LifecycleEvent.START_MAIN_LOOP)
                .build()
        );
        startRenderLoop();
        stage.dispatchEvent(
            LifecycleEvent.builder()
                .type(LifecycleEvent.EXIT_MAIN_LOOP)
                .build()
        );
    }

    @Override
    public Stage stage() {
        return stage;
    }

    @Override
    public Renderer getRenderer() {
        return renderer;
    }

    private long createWindow() {
        GLFWErrorCallback.createPrint(System.err).set();

        if (!glfwInit())
            throw new IllegalStateException("Unable to initialize GLFW");

        glfwDefaultWindowHints();
        //glfwWindowHint(GLFW.GLFW_SAMPLES, 4);

        if (Objects.equals(System.getProperty(SystemProperties.GLFW_HINT_ALWAYSONTOP), "true")) {
            glfwWindowHint(GLFW_FLOATING, 1);
        }

        windowId = glfwCreateWindow(initialWidth, initialHeight, initialTitle, NULL, NULL);

        if (windowId == NULL)
            throw new RuntimeException("Failed to create the GLFW window");

        WindowIconLoader.loadIcons(windowId);

        glfwSetWindowSizeCallback(windowId, new GLFWWindowSizeCallback() {
            @Override
            public void invoke(long l, int width, int height) {
                canvasWidth = width;
                canvasHeight = height;
                renderer.reshape();
            }
        });

        glfwSetScrollCallback(windowId, new GLFWScrollCallback() {
            @Override
            public void invoke(long win, double dx, double dy) {
                stage.dispatchEvent(InteractiveEvent.builder()
                    .type(InteractiveEvent.WHEEL)
                    .x(Mouse.getX())
                    .y(Mouse.getY())
                    .delta((int) dy)
                    .control(control)
                    .shift(shift)
                    .drag(isDown)
                    .build());
            }
        });

        glfwSetMouseButtonCallback(windowId, new GLFWMouseButtonCallback() {
            @Override
            public void invoke(long window, int mouseButton, int action, int mods) {
                isDown = action == 1;
                stage.dispatchEvent(InteractiveEvent.builder()
                    .type(action == 1 ? InteractiveEvent.DOWN : InteractiveEvent.UP)
                    .x(Mouse.getX())
                    .y(Mouse.getY())
                    .drag(isDown)
                    .mouseButton(mouseButton)
                    .shift((mods & GLFW_MOD_SHIFT) != 0)
                    .control((mods & GLFW_MOD_CONTROL) != 0)
                    .alt((mods & GLFW_MOD_ALT) != 0)
                    .build());

                InteractiveManager.getInstance().screenTouch(mouseX, mouseY, 0, mouseButton, isDown, shift, control, alt);
            }
        });

        glfwSetCursorPosCallback(windowId, new GLFWCursorPosCallback() {
            @Override
            public void invoke(long window, double x, double y) {
                mouseX = (int) (x * stage.getWidth() / canvasWidth);
                mouseY = (int) (y * stage.getHeight() / canvasHeight);

                Mouse.setXY(mouseX, mouseY);

                stage.dispatchEvent(InteractiveEvent.builder()
                    .type(InteractiveEvent.MOVE)
                    .x(Mouse.getX())
                    .y(Mouse.getY())
                    .drag(isDown)
                    .build());

                InteractiveManager.getInstance().screenMove(0, mouseX, mouseY, shift, control, alt);
            }
        });

        glfwSetCharCallback(windowId, (window, codepoint) -> {
            stage.dispatchEvent(InteractiveEvent.builder()
                .type(InteractiveEvent.KEY_TYPE)
                .x(Mouse.getX())
                .y(Mouse.getY())
                .alt(alt)
                .control(control)
                .shift(shift)
                .drag(isDown)
                .codepoint(codepoint)
                .keyType(String.valueOf(Character.toChars(codepoint)))
                .build());
        });

        glfwSetKeyCallback(windowId, (window, key, scancode, action, mods) -> {

            switch (action) {
                case GLFW_PRESS -> {
                    if (key == KeyCode.LEFT_SHIFT || key == KeyCode.RIGHT_SHIFT) shift = true;
                    if (key == KeyCode.LEFT_CONTROL || key == KeyCode.RIGHT_CONTROL) control = true;
                    if (key == KeyCode.LEFT_ALT || key == KeyCode.RIGHT_ALT) alt = true;

                    stage.dispatchEvent(InteractiveEvent.builder()
                        .type(InteractiveEvent.KEY_DOWN)
                        .x(Mouse.getX())
                        .y(Mouse.getY())
                        .character((char) key)
                        .keyCode(key)
                        .drag(isDown)
                        .shift((mods & GLFW_MOD_SHIFT) != 0)
                        .control((mods & GLFW_MOD_CONTROL) != 0)
                        .alt((mods & GLFW_MOD_ALT) != 0)
                        .build());
                }

                case GLFW_REPEAT -> stage.dispatchEvent(InteractiveEvent.builder()
                    .type(InteractiveEvent.KEY_REPEAT)
                    .x(Mouse.getX())
                    .y(Mouse.getY())
                    .keyCode(key)
                    .character((char) key)
                    .drag(isDown)
                    .shift((mods & GLFW_MOD_SHIFT) != 0)
                    .control((mods & GLFW_MOD_CONTROL) != 0)
                    .alt((mods & GLFW_MOD_ALT) != 0)
                    .build());

                case GLFW_RELEASE -> {
                    if (key == KeyCode.LEFT_SHIFT || key == KeyCode.RIGHT_SHIFT) shift = false;
                    if (key == KeyCode.LEFT_CONTROL || key == KeyCode.RIGHT_CONTROL) control = false;
                    if (key == KeyCode.LEFT_ALT || key == KeyCode.RIGHT_ALT) alt = false;

                    stage.dispatchEvent(InteractiveEvent.builder()
                        .type(InteractiveEvent.KEY_UP)
                        .x(Mouse.getX())
                        .y(Mouse.getY())
                        .keyCode(key)
                        .character((char) key)
                        .drag(isDown)
                        .shift((mods & GLFW_MOD_SHIFT) != 0)
                        .control((mods & GLFW_MOD_CONTROL) != 0)
                        .alt((mods & GLFW_MOD_ALT) != 0)
                        .build());
                }
            }
        });

        GLFWVidMode videoMode = glfwGetVideoMode(glfwGetPrimaryMonitor());

        glfwSetWindowPos(
            windowId,
            (videoMode.width() - initialWidth) / 2,
            (videoMode.height() - initialHeight) / 2
        );

        glfwMakeContextCurrent(windowId);
        GL.createCapabilities();

        glfwSwapInterval(1);

        // TODO: remove loading demo texture data info from here
        D2D2.textureManager().loadTextureDataInfo(DEMO_TEXTURE_DATA_INF_FILE);

        renderer.init(windowId);
        renderer.reshape();

        setSmoothMode(false);

        return windowId;
    }

    @Override
    public void setCursorXY(int x, int y) {
        GLFW.glfwSetCursorPos(windowId, x, y);
    }

    @Override
    public void putToClipboard(String string) {
        Toolkit.getDefaultToolkit()
            .getSystemClipboard()
            .setContents(
                new StringSelection(string),
                null
            );
    }

    @Override
    public String getStringFromClipboard() {
        try {
            return Toolkit.getDefaultToolkit()
                .getSystemClipboard()
                .getData(DataFlavor.stringFlavor).toString();
        } catch (UnsupportedFlavorException e) {
            //e.printStackTrace(); // ignore exception
            return "";
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public void setFrameRate(int frameRate) {
        this.frameRate = frameRate;
        renderer.setFrameRate(frameRate);
    }

    @Override
    public int getFrameRate() {
        return frameRate;
    }

    @Override
    public int getActualFps() {
        return renderer.getFps();
    }

    private void startRenderLoop() {

        long windowId = displayManager.getWindowId();

        while (!glfwWindowShouldClose(windowId) && running) {
            glfwPollEvents();
            renderer.renderFrame();
            glfwSwapBuffers(windowId);
            Timer.processTimers();
        }

        String prop = System.getProperty("d2d2.glfw.no-terminate");
        if (prop != null && prop.equals("true")) {
            log.warn("d2d2.glfw.no-terminate is set");
            return;
        }

        glfwTerminate();
    }


    private static void sleep(long t) {
        try {
            Thread.sleep(t);
        } catch (InterruptedException e) {
            log.error(e.getMessage(), e);
        }
    }

    @RequiredArgsConstructor
    @Getter
    @ToString
    private static class Size {
        private final int w;
        private final int h;
    }

    /*
    private static Size computeAtlasSize(Font font, String string, TrueTypeBitmapFontBuilder builder) {
        FontMetrics metrics = new Canvas().getFontMetrics(font);
        int width = 0;
        int currentHeight = 0;
        int maxWidth = 0;

        for (int i = 0; i < string.length(); i++) {
            char currentChar = string.charAt(i);
            int charWidth = metrics.charWidth(currentChar);
            width += charWidth;

            if (width > 4096) {
                maxWidth = Math.max(maxWidth, width - charWidth);
                width = charWidth;
                currentHeight += builder.getSpacingY();
            }
        }

        maxWidth = Math.max(maxWidth, width);
        currentHeight += metrics.getHeight();

        return new Size(maxWidth, currentHeight);
    }
     */

    private static Size computeSize(java.awt.Font font, String string, TrueTypeFontBuilder builder) {
        int x = 0;
        int y = 0;
        FontMetrics fontMetrics = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB).getGraphics().getFontMetrics(font);

        for (int i = 0; i < string.length(); i++) {
            char c = string.charAt(i);

            int w = fontMetrics.charWidth(c);
            int h = fontMetrics.getHeight();

            x += w + builder.getSpacingX();

            if (x >= 2048) {
                y += h + builder.getSpacingY();
                x = 0;
            }
        }

        return new Size(2048, y + font.getSize() * 2 + 128);
    }

    @SneakyThrows
    @Override
    public Font generateBitmapFont(TrueTypeFontBuilder builder) {

        final GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();

        InputStream inputStream = builder.getInputStream() != null ?
            builder.getInputStream() : new FileInputStream(builder.getFilePath().toFile());

        java.awt.Font font = java.awt.Font.createFont(java.awt.Font.TRUETYPE_FONT, inputStream);
        String fontName = font.getName();
        ge.registerFont(font);

        boolean bold = builder.isBold();
        boolean italic = builder.isItalic();
        int fontSize = builder.getFontSize();
        int fontStyle = java.awt.Font.PLAIN | (bold ? java.awt.Font.BOLD : java.awt.Font.PLAIN) | (italic ? java.awt.Font.ITALIC : java.awt.Font.PLAIN);

        font = new java.awt.Font(fontName, fontStyle, fontSize);

        String string = builder.getCharSourceString();

        Size size = computeSize(font, string, builder);

        int textureWidth = size.w;
        int textureHeight = size.h;
        BufferedImage bufferedImage = new BufferedImage(textureWidth, textureHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = bufferedImage.createGraphics();

        if (builder.fractionalMetrics() != null)
            g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, FractionalMetrics.nativeValue(builder.fractionalMetrics()));

        if (builder.isTextAntialiasOn())
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        if (builder.isTextAntialiasGasp())
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_GASP);

        if (builder.isTextAntialiasLcdHrgb())
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);

        if (builder.isTextAntialiasLcdHbgr())
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HBGR);

        if (builder.isTextAntialiasLcdVrgb())
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_VRGB);

        if (builder.isTextAntialiasLcdVbgr())
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_VBGR);

        g.setColor(Color.WHITE);

        List<CharInfo> charInfos = new ArrayList<>();

        int x = 0;
        int y = font.getSize();
        FontMetrics fontMetrics = g.getFontMetrics(font);

        for (int i = 0; i < string.length(); i++) {
            char c = string.charAt(i);

            int w = fontMetrics.charWidth(c);
            int h = fontMetrics.getHeight();
            int toY = fontMetrics.getDescent();

            g.setFont(font);
            g.drawString(String.valueOf(c), x, y);

            CharInfo charInfo = new CharInfo();
            charInfo.character = c;
            charInfo.x = x + builder.getOffsetX();
            charInfo.y = y - h + toY + builder.getOffsetY();

            charInfo.width = w + builder.getOffsetX();
            charInfo.height = h + builder.getOffsetY();

            charInfos.add(charInfo);

            x += w + builder.getSpacingX();

            if (x >= bufferedImage.getWidth() - font.getSize()) {
                y += h + builder.getSpacingY();
                x = 0;
            }
        }

        StringBuilder stringBuilder = new StringBuilder();

        // meta
        stringBuilder.append("#meta ");
        stringBuilder.append("spacingX ").append(builder.getSpacingX()).append(" ");
        stringBuilder.append("spacingY ").append(builder.getSpacingY()).append(" ");
        stringBuilder.append("\n");

        // char infos
        charInfos.forEach(charInfo ->
            stringBuilder
                .append(charInfo.character)
                .append(' ')
                .append(charInfo.x)
                .append(' ')
                .append(charInfo.y)
                .append(' ')
                .append(charInfo.width)
                .append(' ')
                .append(charInfo.height)
                .append('\n')
        );

        byte[] charsDataBytes = stringBuilder.toString().getBytes(StandardCharsets.UTF_8);

        ByteArrayOutputStream pngOutputStream = new ByteArrayOutputStream();
        ImageIO.write(bufferedImage, "png", pngOutputStream);
        byte[] pngDataBytes = pngOutputStream.toByteArray();

        if (System.getProperty(SystemProperties.D2D2_BITMAPFONT_SAVEBMF) != null) {
            String assetPath = builder.getAssetPath();
            Path ttfPath = builder.getFilePath();

            String fileName = assetPath != null ?
                Path.of(assetPath).getFileName().toString() : ttfPath.getFileName().toString();

            String saveToPathString = System.getProperty(SystemProperties.D2D2_BITMAPFONT_SAVEBMF);

            Path destinationPath = Files.createDirectories(Path.of(saveToPathString));

            fileName = fileName.substring(0, fileName.length() - 4) + "-" + fontSize;

            Files.write(destinationPath.resolve(fileName + ".png"), pngDataBytes);
            Files.writeString(destinationPath.resolve(fileName + ".bmf"), stringBuilder.toString());
            log.info("BMF written {}/{}", destinationPath, fileName);
        }

        return D2D2.bitmapFontManager().loadBitmapFont(
            new ByteArrayInputStream(charsDataBytes),
            new ByteArrayInputStream(pngDataBytes),
            builder.getName()
        );
    }

    private static class CharInfo {
        public char character;
        public int x;
        public int y;
        public int width;
        public int height;

        @Override
        public String toString() {
            return "CharInfo{" +
                "character=" + character +
                ", x=" + x +
                ", y=" + y +
                ", width=" + width +
                ", height=" + height +
                '}';
        }
    }
}
