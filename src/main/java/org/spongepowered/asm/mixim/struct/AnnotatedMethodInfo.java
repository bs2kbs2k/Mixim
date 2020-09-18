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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.MethodNode;
import org.spongepowered.asm.mixim.MiximEnvironment.Option;
import org.spongepowered.asm.mixim.injection.IInjectionPointContext;
import org.spongepowered.asm.mixim.refmap.IMiximContext;

/**
 * Data bundle for an annotated method in a mixim
 */
public class AnnotatedMethodInfo implements IInjectionPointContext {

    /**
     * Log more things
     */
    protected static final Logger logger = LogManager.getLogger("mixim");

    /**
     * Mixim context
     */
    private final IMiximContext context;
    
    /**
     * Annotated method
     */
    protected final MethodNode method;

    /**
     * Annotation on the method
     */
    protected final AnnotationNode annotation;
    
    public AnnotatedMethodInfo(IMiximContext mixim, MethodNode method, AnnotationNode annotation) {
        this.context = mixim;
        this.method = method;
        this.annotation = annotation;
    }

    /**
     * Get the mixim target context for this annotated method
     * 
     * @return the target context
     */
    @Override
    public final IMiximContext getContext() {
        return this.context;
    }

    /**
     * Get method being called
     * 
     * @return injector method
     */
    @Override
    public final MethodNode getMethod() {
        return this.method;
    }

    /**
     * Get the annotation which this InjectionInfo was created from
     *  
     * @return The annotation which this InjectionInfo was created from 
     */
    @Override
    public final AnnotationNode getAnnotation() {
        return this.annotation;
    }
    
    /* (non-Javadoc)
     * @see org.spongepowered.asm.mixim.injection.IInjectionPointContext
     *      #addMessage(java.lang.String, java.lang.Object[])
     */
    @Override
    public void addMessage(String format, Object... args) {
        if (this.context.getOption(Option.DEBUG_VERBOSE)) {
            AnnotatedMethodInfo.logger.warn(String.format(format, args));
        }
    }

}
