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
package org.spongepowered.asm.mixim.injection.code;

import java.util.HashMap;
import java.util.Map;

import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodNode;
import org.spongepowered.asm.mixim.extensibility.IMiximConfig;
import org.spongepowered.asm.mixim.injection.InjectionPoint;
import org.spongepowered.asm.mixim.injection.struct.Target;
import org.spongepowered.asm.mixim.transformer.meta.MiximMerged;
import org.spongepowered.asm.util.Annotations;

/**
 * Couples {@link MethodSlice method slices} to a {@link Target} for injection
 * purposes.
 */
public class InjectorTarget {

    /**
     * Owner injector
     */
    private final ISliceContext context;

    /**
     * Cache of slices
     */
    private final Map<String, ReadOnlyInsnList> cache = new HashMap<String, ReadOnlyInsnList>();

    /**
     * Target method data
     */
    private final Target target;
    
    private final String mergedBy;
    
    private final int mergedPriority;

    /**
     * ctor
     * 
     * @param context owner
     * @param target target
     */
    public InjectorTarget(ISliceContext context, Target target) {
        this.context = context;
        this.target = target;
        
        AnnotationNode merged = Annotations.getVisible(target.method, MiximMerged.class);
        this.mergedBy = Annotations.<String>getValue(merged, "mixim");
        this.mergedPriority = Annotations.<Integer>getValue(merged, "priority", IMiximConfig.DEFAULT_PRIORITY);
    }
    
    @Override
    public String toString() {
        return this.target.toString();
    }
    
    /**
     * Get the target reference
     */
    public Target getTarget() {
        return this.target;
    }
    
    /**
     * Get the target method
     */
    public MethodNode getMethod() {
        return this.target.method;
    }
    
    /**
     * Get whether this target method was merged by another mixim
     */
    public boolean isMerged() {
        return this.mergedBy != null;
    }
    
    /**
     * Get the name of the mixim which merged this method, returns null for non-
     * mixim methods
     */
    public String getMergedBy() {
        return this.mergedBy;
    }
    
    /**
     * Get the priority of the mixim which merged this method, or default
     * priority for non-mixim methods
     */
    public int getMergedPriority() {
        return this.mergedPriority;
    }
    
    /**
     * Get the slice instructions for the specified slice id
     * 
     * @param id slice id
     * @return insn slice
     */
    public InsnList getSlice(String id) {
        ReadOnlyInsnList slice = this.cache.get(id);
        if (slice == null) {
            MethodSlice sliceInfo = this.context.getSlice(id);
            if (sliceInfo != null) {
                slice = sliceInfo.getSlice(this.target.method);
            } else {
                // No slice exists so just wrap the method insns
                slice = new ReadOnlyInsnList(this.target.method.instructions);
            }
            this.cache.put(id, slice);
        }
        
        return slice;
    }

    /**
     * Get the slice instructions for the specified injection point
     * 
     * @param injectionPoint injection point to fetch slice for
     * @return slice
     */
    public InsnList getSlice(InjectionPoint injectionPoint) {
        return this.getSlice(injectionPoint.getSlice());
    }
    
    /**
     * Dispose all cached instruction lists
     */
    public void dispose() {
        for (ReadOnlyInsnList insns : this.cache.values()) {
            insns.dispose();
        }
        
        this.cache.clear();
    }
}

