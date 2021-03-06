/*
 * This file is part of Mixim, licensed under the MIT License (MIT).
 *
 * Copyright (c) SpongePowered <https://www.spongepowered.org>
 * Copyright (c) contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.spongepowered.asm.launch.platform;

import java.util.Collection;

import org.spongepowered.asm.launch.platform.container.IContainerHandle;

import net.minecraft.launchwrapper.Launch;

/**
 * Platform agent for LiteLoader in legacy (LaunchWrapper) environment
 */
public class MiximPlatformAgentLiteLoaderLegacy extends MiximPlatformAgentAbstract implements IMiximPlatformServiceAgent {

    private static final String GETSIDE_METHOD = "getEnvironmentType";
    private static final String LITELOADER_TWEAKER_NAME = "com.mumfrey.liteloader.launch.LiteLoaderTweaker";
    
    @Override
    public AcceptResult accept(MiximPlatformManager manager, IContainerHandle handle) {
        return AcceptResult.REJECTED;
    }

    /* (non-Javadoc)
     * @see org.spongepowered.asm.launch.platform.MiximPlatformAgentAbstract
     *      #getSideName()
     */
    @Override
    public String getSideName() {
        return MiximPlatformAgentAbstract.invokeStringMethod(Launch.classLoader, MiximPlatformAgentLiteLoaderLegacy.LITELOADER_TWEAKER_NAME,
                MiximPlatformAgentLiteLoaderLegacy.GETSIDE_METHOD);
    }
    
    /* (non-Javadoc)
     * @see org.spongepowered.asm.launch.platform.IMiximPlatformServiceAgent
     *      #init()
     */
    @Override
    public void init() {
        // Nothing to do here
    }

    /* (non-Javadoc)
     * @see org.spongepowered.asm.launch.platform.IMiximPlatformServiceAgent
     *      #getMiximContainers()
     */
    @Override
    public Collection<IContainerHandle> getMiximContainers() {
        return null;
    }
    
}
