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

import com.ancevt.d2d2.engine.SoundManager;
import com.ancevt.d2d2.sound.Sound;
import org.lwjgl.openal.AL;
import org.lwjgl.openal.ALC;
import org.lwjgl.openal.ALCCapabilities;
import org.lwjgl.openal.ALCapabilities;
import org.lwjgl.system.MemoryStack;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.openal.ALC10.*;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.NULL;

public class DesktopSoundManager implements SoundManager {

    private static class Holder {
        private static final DesktopSoundManager INSTANCE = new DesktopSoundManager();
    }

    public static DesktopSoundManager getInstance() {
        return DesktopSoundManager.Holder.INSTANCE;
    }


    private long device;
    private long context;

    public DesktopSoundManager() {
        device = alcOpenDevice((ByteBuffer) null);
        if (device == NULL) {
            throw new IllegalStateException("Failed to open the default OpenAL device.");
        }

        // Create context
        try (MemoryStack stack = stackPush()) {
            IntBuffer contextAttribList = stack.mallocInt(1);
            contextAttribList.put(0, 0);
            context = alcCreateContext(device, contextAttribList);
        }

        if (context == NULL) {
            throw new IllegalStateException("Failed to create OpenAL context.");
        }

        // Make the context current
        alcMakeContextCurrent(context);
        ALCCapabilities alcCapabilities = ALC.createCapabilities(device);
        ALCapabilities alCapabilities = AL.createCapabilities(alcCapabilities);

        if (!alCapabilities.OpenAL10) {
            throw new IllegalStateException("OpenAL 1.0 not supported.");
        }
    }

    @Override
    public void cleanup() {
        alcDestroyContext(context);
        alcCloseDevice(device);
    }

    @Override
    public Sound loadSound(InputStream inputStream) {
        return new DesktopSound(inputStream);
    }

    @Override
    public Sound loadSound(String assetFileName) {
        return new DesktopSound(assetFileName);
    }
}
