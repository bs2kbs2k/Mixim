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
package org.spongepowered.tools.obfuscation.validation;

import java.util.Collection;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;

import org.spongepowered.asm.mixim.gen.Accessor;
import org.spongepowered.asm.mixim.gen.Invoker;
import org.spongepowered.tools.obfuscation.MiximValidator;
import org.spongepowered.tools.obfuscation.SupportedOptions;
import org.spongepowered.tools.obfuscation.interfaces.IMiximAnnotationProcessor;
import org.spongepowered.tools.obfuscation.mirror.AnnotationHandle;
import org.spongepowered.tools.obfuscation.mirror.TypeUtils;
import org.spongepowered.tools.obfuscation.mirror.TypeHandle;

/**
 * Validator which checks that the mixim targets are sane
 */
public class TargetValidator extends MiximValidator {

    /**
     * ctor
     * 
     * @param ap Processing environment
     */
    public TargetValidator(IMiximAnnotationProcessor ap) {
        super(ap, ValidationPass.LATE);
    }

    /* (non-Javadoc)
     * @see org.spongepowered.tools.obfuscation.MiximValidator
     *      #validate(javax.lang.model.element.TypeElement,
     *      org.spongepowered.tools.obfuscation.AnnotationHandle,
     *      java.util.Collection)
     */
    @Override
    public boolean validate(TypeElement mixim, AnnotationHandle annotation, Collection<TypeHandle> targets) {
        if ("true".equalsIgnoreCase(this.options.getOption(SupportedOptions.DISABLE_TARGET_VALIDATOR))) {
            return true;
        }
        
        if (mixim.getKind() == ElementKind.INTERFACE) {
            this.validateInterfaceMixim(mixim, targets);
        } else {
            this.validateClassMixim(mixim, targets);
        }
        
        return true;
    }

    private void validateInterfaceMixim(TypeElement mixim, Collection<TypeHandle> targets) {
        boolean containsNonAccessorMethod = false;
        for (Element element : mixim.getEnclosedElements()) {
            if (element.getKind() == ElementKind.METHOD) {
                boolean isAccessor = AnnotationHandle.of(element, Accessor.class).exists();
                boolean isInvoker = AnnotationHandle.of(element, Invoker.class).exists();
                containsNonAccessorMethod |= (!isAccessor && !isInvoker);
            }
        }
        
        if (!containsNonAccessorMethod) {
            return;
        }
        
        for (TypeHandle target : targets) {
            TypeElement targetType = target.getElement();
            if (targetType != null && !(targetType.getKind() == ElementKind.INTERFACE)) {
                this.error("Targetted type '" + target + " of " + mixim + " is not an interface", mixim);
            }
        }
    }

    private void validateClassMixim(TypeElement mixim, Collection<TypeHandle> targets) {
        TypeMirror superClass = mixim.getSuperclass();
        
        for (TypeHandle target : targets) {
            TypeMirror targetType = target.getType();
            if (targetType != null && !this.validateSuperClass(targetType, superClass)) {
                this.error("Superclass " + superClass + " of " + mixim + " was not found in the hierarchy of target class " + targetType, mixim);
            }
        }
    }

    private boolean validateSuperClass(TypeMirror targetType, TypeMirror superClass) {
        if (TypeUtils.isAssignable(this.processingEnv, targetType, superClass)) {
            return true;
        }
        
        return this.validateSuperClassRecursive(targetType, superClass);
    }

    private boolean validateSuperClassRecursive(TypeMirror targetType, TypeMirror superClass) {
        if (!(targetType instanceof DeclaredType)) {
            return false;
        }
        
        if (TypeUtils.isAssignable(this.processingEnv, targetType, superClass)) {
            return true;
        }
        
        TypeElement targetElement = (TypeElement)((DeclaredType)targetType).asElement();
        TypeMirror targetSuper = targetElement.getSuperclass();
        if (targetSuper.getKind() == TypeKind.NONE) {
            return false;
        }
        
        if (this.checkMiximsFor(targetSuper, superClass)) {
            return true;
        }
        
        return this.validateSuperClassRecursive(targetSuper, superClass);
    }

    private boolean checkMiximsFor(TypeMirror targetType, TypeMirror superClass) {
        for (TypeMirror miximType : this.getMiximsTargeting(targetType)) {
            if (TypeUtils.isAssignable(this.processingEnv, miximType, superClass)) {
                return true;
            }
        }
        
        return false;
    }
}
