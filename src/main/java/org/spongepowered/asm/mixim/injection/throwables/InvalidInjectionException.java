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
package org.spongepowered.asm.mixim.injection.throwables;

import org.spongepowered.asm.mixim.injection.struct.InjectionInfo;
import org.spongepowered.asm.mixim.refmap.IMiximContext;
import org.spongepowered.asm.mixim.transformer.ActivityStack;
import org.spongepowered.asm.mixim.transformer.throwables.InvalidMiximException;

/**
 * Thrown when an injector fails a state check, for example if an injector
 * handler signature is invalid, an invalid opcode is targetted, etc.
 */
public class InvalidInjectionException extends InvalidMiximException {

    private static final long serialVersionUID = 2L;
    
    private final InjectionInfo info;

    public InvalidInjectionException(IMiximContext context, String message) {
        super(context, message);
        this.info = null;
    }

    public InvalidInjectionException(IMiximContext context, String message, ActivityStack activityContext) {
        super(context, message, activityContext);
        this.info = null;
    }

    public InvalidInjectionException(InjectionInfo info, String message) {
        super(info.getContext(), message);
        this.info = info;
    }

    public InvalidInjectionException(InjectionInfo info, String message, ActivityStack activityContext) {
        super(info.getContext(), message, activityContext);
        this.info = info;
    }

    public InvalidInjectionException(IMiximContext context, Throwable cause) {
        super(context, cause);
        this.info = null;
    }

    public InvalidInjectionException(IMiximContext context, Throwable cause, ActivityStack activityContext) {
        super(context, cause, activityContext);
        this.info = null;
    }

    public InvalidInjectionException(InjectionInfo info, Throwable cause) {
        super(info.getContext(), cause);
        this.info = info;
    }

    public InvalidInjectionException(InjectionInfo info, Throwable cause, ActivityStack activityContext) {
        super(info.getContext(), cause, activityContext);
        this.info = info;
    }

    public InvalidInjectionException(IMiximContext context, String message, Throwable cause) {
        super(context, message, cause);
        this.info = null;
    }

    public InvalidInjectionException(IMiximContext context, String message, Throwable cause, ActivityStack activityContext) {
        super(context, message, cause, activityContext);
        this.info = null;
    }

    public InvalidInjectionException(InjectionInfo info, String message, Throwable cause) {
        super(info.getContext(), message, cause);
        this.info = info;
    }
    
    public InvalidInjectionException(InjectionInfo info, String message, Throwable cause, ActivityStack activityContext) {
        super(info.getContext(), message, cause, activityContext);
        this.info = info;
    }
    
    public InjectionInfo getInjectionInfo() {
        return this.info;
    }
    
}
