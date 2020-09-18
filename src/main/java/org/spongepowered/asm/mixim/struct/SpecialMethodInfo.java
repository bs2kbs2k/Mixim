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
package org.spongepowered.asm.mixim.struct;

import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import org.spongepowered.asm.mixim.transformer.ClassInfo;
import org.spongepowered.asm.mixim.transformer.MiximTargetContext;
import org.spongepowered.asm.util.Bytecode;
import org.spongepowered.asm.util.asm.MethodNodeEx;

/**
 * Information about a special mixim method such as an injector or accessor
 */
public class SpecialMethodInfo extends AnnotatedMethodInfo {
    
    /**
     * Human-readable annotation type 
     */
    protected final String annotationType;
    
    /**
     * Class
     */
    protected final ClassNode classNode;
    
    /**
     * Original name of the method, if available 
     */
    protected final String methodName;

    /**
     * Mixim data
     */
    protected final MiximTargetContext mixim;
    
    public SpecialMethodInfo(MiximTargetContext mixim, MethodNode method, AnnotationNode annotation) {
        super(mixim, method, annotation);
        this.mixim = mixim;
        this.annotationType = this.annotation != null ? "@" + Bytecode.getSimpleName(this.annotation) : "Undecorated injector";
        this.classNode = mixim.getTargetClassNode();
        this.methodName = MethodNodeEx.getName(method);
    }

    /**
     * Get the class node for this injection
     * 
     * @return the class containing the injector and the target
     */
    public final ClassNode getClassNode() {
        return this.classNode;
    }
    
    /**
     * Get the class metadata for the mixim
     */
    public final ClassInfo getClassInfo() {
        return this.mixim.getClassInfo();
    }
    
    /**
     * Get the original name of the method, if available
     */
    public String getMethodName() {
        return this.methodName;
    }

}
