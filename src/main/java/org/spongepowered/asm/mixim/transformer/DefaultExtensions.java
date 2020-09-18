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
package org.spongepowered.asm.mixim.transformer;

import org.spongepowered.asm.mixim.MiximEnvironment;
import org.spongepowered.asm.mixim.injection.invoke.arg.ArgsClassGenerator;
import org.spongepowered.asm.mixim.transformer.ext.Extensions;
import org.spongepowered.asm.mixim.transformer.ext.extensions.ExtensionCheckClass;
import org.spongepowered.asm.mixim.transformer.ext.extensions.ExtensionCheckInterfaces;
import org.spongepowered.asm.mixim.transformer.ext.extensions.ExtensionClassExporter;
import org.spongepowered.asm.service.ISyntheticClassInfo;
import org.spongepowered.asm.util.IConsumer;

/**
 * Default extensions for mixim processor
 */
final class DefaultExtensions {

    private DefaultExtensions() {
    }

    static void create(final MiximEnvironment environment, final Extensions extensions, final SyntheticClassRegistry registry) {
        IConsumer<ISyntheticClassInfo> registryDelegate = new IConsumer<ISyntheticClassInfo>() {
            @Override
            public void accept(ISyntheticClassInfo item) {
                registry.registerSyntheticClass(item);
            }
        };
        
        extensions.add(new ArgsClassGenerator(registryDelegate));
        extensions.add(new InnerClassGenerator(registryDelegate));

        extensions.add(new ExtensionClassExporter(environment));
        extensions.add(new ExtensionCheckClass());
        extensions.add(new ExtensionCheckInterfaces());
    }

}
