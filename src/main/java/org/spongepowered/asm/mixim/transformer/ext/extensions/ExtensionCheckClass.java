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
package org.spongepowered.asm.mixim.transformer.ext.extensions;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.util.CheckClassAdapter;
import org.spongepowered.asm.mixim.MiximEnvironment;
import org.spongepowered.asm.mixim.MiximEnvironment.Option;
import org.spongepowered.asm.mixim.throwables.MiximException;
import org.spongepowered.asm.mixim.transformer.ext.IExtension;
import org.spongepowered.asm.mixim.transformer.ext.ITargetClassContext;
import org.spongepowered.asm.transformers.MiximClassWriter;

/**
 * Mixim transformer module which runs CheckClassAdapter on the post-mixim
 * bytecode
 */
public class ExtensionCheckClass implements IExtension {
    
    /**
     * Exception thrown when checkclass fails
     */
    public static class ValidationFailedException extends MiximException {

        private static final long serialVersionUID = 1L;

        public ValidationFailedException(String message, Throwable cause) {
            super(message, cause);
        }

        public ValidationFailedException(String message) {
            super(message);
        }

        public ValidationFailedException(Throwable cause) {
            super(cause);
        }
        
    }
    
    /* (non-Javadoc)
     * @see org.spongepowered.asm.mixim.transformer.ext.IExtension#checkActive(
     *      org.spongepowered.asm.mixim.MiximEnvironment)
     */
    @Override
    public boolean checkActive(MiximEnvironment environment) {
        return environment.getOption(Option.DEBUG_VERIFY);
    }

    /* (non-Javadoc)
     * @see org.spongepowered.asm.mixim.transformer.IMiximTransformerModule
     *     #preApply(org.spongepowered.asm.mixim.transformer.TargetClassContext)
     */
    @Override
    public void preApply(ITargetClassContext context) {
    }
    
    /* (non-Javadoc)
     * @see org.spongepowered.asm.mixim.transformer.IMiximTransformerModule
     *    #postApply(org.spongepowered.asm.mixim.transformer.TargetClassContext)
     */
    @Override
    public void postApply(ITargetClassContext context) {
        try {
            context.getClassNode().accept(new CheckClassAdapter(new MiximClassWriter(ClassWriter.COMPUTE_FRAMES)));
        } catch (RuntimeException ex) {
            throw new ValidationFailedException(ex.getMessage(), ex);
        }
    }
    
    /* (non-Javadoc)
     * @see org.spongepowered.asm.mixim.transformer.ext.IExtension
     *      #export(org.spongepowered.asm.mixim.MiximEnvironment,
     *      java.lang.String, boolean, org.objectweb.asm.tree.ClassNode)
     */
    @Override
    public void export(MiximEnvironment env, String name, boolean force, ClassNode classNode) {
    }

}
