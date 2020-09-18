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
package org.spongepowered.asm.mixim.injection.struct;

import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.MethodNode;
import org.spongepowered.asm.mixim.injection.ModifyVariable;
import org.spongepowered.asm.mixim.injection.code.Injector;
import org.spongepowered.asm.mixim.injection.modify.LocalVariableDiscriminator;
import org.spongepowered.asm.mixim.injection.modify.ModifyVariableInjector;
import org.spongepowered.asm.mixim.injection.struct.InjectionInfo.AnnotationType;
import org.spongepowered.asm.mixim.injection.struct.InjectionInfo.HandlerPrefix;
import org.spongepowered.asm.mixim.transformer.MiximTargetContext;

/**
 * Information about a {@link ModifyVariable} injector
 */
@AnnotationType(ModifyVariable.class)
@HandlerPrefix("localvar")
public class ModifyVariableInjectionInfo extends InjectionInfo {

    public ModifyVariableInjectionInfo(MiximTargetContext mixim, MethodNode method, AnnotationNode annotation) {
        super(mixim, method, annotation);
    }
    
    @Override
    protected Injector parseInjector(AnnotationNode injectAnnotation) {
        return new ModifyVariableInjector(this, LocalVariableDiscriminator.parse(injectAnnotation));
    }
    
    @Override
    protected String getDescription() {
        return "Variable modifier method";
    }
    
}
