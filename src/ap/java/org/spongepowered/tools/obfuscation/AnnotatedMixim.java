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
package org.spongepowered.tools.obfuscation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic.Kind;

import org.spongepowered.asm.mixim.Mixim;
import org.spongepowered.asm.mixim.Pseudo;
import org.spongepowered.tools.obfuscation.AnnotatedMiximElementHandlerAccessor.AnnotatedElementAccessor;
import org.spongepowered.tools.obfuscation.AnnotatedMiximElementHandlerAccessor.AnnotatedElementInvoker;
import org.spongepowered.tools.obfuscation.AnnotatedMiximElementHandlerInjector.AnnotatedElementInjectionPoint;
import org.spongepowered.tools.obfuscation.AnnotatedMiximElementHandlerInjector.AnnotatedElementInjector;
import org.spongepowered.tools.obfuscation.AnnotatedMiximElementHandlerOverwrite.AnnotatedElementOverwrite;
import org.spongepowered.tools.obfuscation.interfaces.IMessagerSuppressible;
import org.spongepowered.tools.obfuscation.interfaces.IMiximAnnotationProcessor;
import org.spongepowered.tools.obfuscation.interfaces.IMiximValidator;
import org.spongepowered.tools.obfuscation.interfaces.IMiximValidator.ValidationPass;
import org.spongepowered.tools.obfuscation.interfaces.IObfuscationManager;
import org.spongepowered.tools.obfuscation.interfaces.ITypeHandleProvider;
import org.spongepowered.tools.obfuscation.mapping.IMappingConsumer;
import org.spongepowered.tools.obfuscation.mirror.AnnotationHandle;
import org.spongepowered.tools.obfuscation.mirror.TypeHandle;
import org.spongepowered.tools.obfuscation.mirror.TypeUtils;
import org.spongepowered.tools.obfuscation.struct.InjectorRemap;

/**
 * Information about a mixim stored during processing
 */
class AnnotatedMixim {

    /**
     * Mixim annotation
     */
    private final AnnotationHandle annotation;

    /**
     * Messager
     */
    private final IMessagerSuppressible messager;

    /**
     * Type handle provider
     */
    private final ITypeHandleProvider typeProvider;

    /**
     * Manager
     */
    private final IObfuscationManager obf;

    /**
     * Generated mappings
     */
    private final IMappingConsumer mappings;

    /**
     * Mixim class
     */
    private final TypeElement mixim;
    
    /**
     * Methods 
     */
    private final List<ExecutableElement> methods;

    /**
     * Mixim class
     */
    private final TypeHandle handle;

    /**
     * Specified targets
     */
    private final List<TypeHandle> targets = new ArrayList<TypeHandle>();

    /**
     * Target type (for single-target mixims)
     */
    private final TypeHandle primaryTarget;

    /**
     * Mixim class "reference" (bytecode name)
     */
    private final String classRef;

    /**
     * True if we will actually process remappings for this mixim
     */
    private final boolean remap;

    /**
     * True if the target class is allowed to not exist at compile time, we will
     * simulate the target in order to do as much validation as is feasible.
     *
     * <p>Implies <tt>remap=false</tt></p>
     */
    private final boolean virtual;

    /**
     * Overwrite handler
     */
    private final AnnotatedMiximElementHandlerOverwrite overwrites;

    /**
     * Shadow handler
     */
    private final AnnotatedMiximElementHandlerShadow shadows;

    /**
     * Injector handler
     */
    private final AnnotatedMiximElementHandlerInjector injectors;

    /**
     * Accessor handler
     */
    private final AnnotatedMiximElementHandlerAccessor accessors;

    /**
     * Soft implementation handler;
     */
    private final AnnotatedMiximElementHandlerSoftImplements softImplements;
    
    /**
     * True once the FINAL 
     */
    private boolean validated = false;

    public AnnotatedMixim(IMiximAnnotationProcessor ap, TypeElement type) {
        this.typeProvider = ap.getTypeProvider();
        this.obf = ap.getObfuscationManager();
        this.mappings = this.obf.createMappingConsumer();
        this.messager = ap;
        this.mixim = type;
        this.handle = new TypeHandle(type);
        this.methods = new ArrayList<ExecutableElement>(this.handle.<ExecutableElement>getEnclosedElements(ElementKind.METHOD));
        this.virtual = this.handle.getAnnotation(Pseudo.class).exists();
        this.annotation = this.handle.getAnnotation(Mixim.class);
        this.classRef = TypeUtils.getInternalName(type);
        this.primaryTarget = this.initTargets();
        this.remap = this.annotation.getBoolean("remap", true) && this.targets.size() > 0;

        this.overwrites = new AnnotatedMiximElementHandlerOverwrite(ap, this);
        this.shadows = new AnnotatedMiximElementHandlerShadow(ap, this);
        this.injectors = new AnnotatedMiximElementHandlerInjector(ap, this);
        this.accessors = new AnnotatedMiximElementHandlerAccessor(ap, this);
        this.softImplements = new AnnotatedMiximElementHandlerSoftImplements(ap, this);
    }

    AnnotatedMixim runValidators(ValidationPass pass, Collection<IMiximValidator> validators) {
        for (IMiximValidator validator : validators) {
            if (!validator.validate(pass, this.mixim, this.annotation, this.targets)) {
                break;
            }
        }
        
        if (pass == ValidationPass.FINAL && !this.validated) {
            this.validated = true;
            this.runFinalValidation();
        }

        return this;
    }

    private TypeHandle initTargets() {
        TypeHandle primaryTarget = null;

        // Public targets, referenced by class
        try {
            for (TypeMirror target : this.annotation.<TypeMirror>getList()) {
                TypeHandle type = new TypeHandle((DeclaredType)target);
                if (this.targets.contains(type)) {
                    continue;
                }
                this.addTarget(type);
                if (primaryTarget == null) {
                    primaryTarget = type;
                }
            }
        } catch (Exception ex) {
            this.printMessage(Kind.WARNING, "Error processing public targets: " + ex.getClass().getName() + ": " + ex.getMessage(), this);
        }

        // Private targets, referenced by name
        try {
            for (String softTarget : this.annotation.<String>getList("targets")) {
                TypeHandle type = this.typeProvider.getTypeHandle(softTarget);
                if (this.targets.contains(type)) {
                    continue;
                }
                if (this.virtual) {
                    type = this.typeProvider.getSimulatedHandle(softTarget, this.mixim.asType());
                } else if (type == null) {
                    this.printMessage(Kind.ERROR, "Mixim target " + softTarget + " could not be found", this);
                    return null;
                } else if (type.isPublic()) {
                    SuppressedBy suppressedBy = (type.getPackage().isUnnamed()) ? SuppressedBy.DEFAULT_PACKAGE : SuppressedBy.PUBLIC_TARGET;
                    this.printMessage(Kind.WARNING, "Mixim target " + softTarget + " is public and must be specified in value", this, suppressedBy);
                    return null;
                }
                this.addSoftTarget(type, softTarget);
                if (primaryTarget == null) {
                    primaryTarget = type;
                }
            }
        } catch (Exception ex) {
            this.printMessage(Kind.WARNING, "Error processing private targets: " + ex.getClass().getName() + ": " + ex.getMessage(), this);
        }

        if (primaryTarget == null) {
            this.printMessage(Kind.ERROR, "Mixim has no targets", this);
        }

        return primaryTarget;
    }

    /**
     * Print a message to the AP messager
     */
    private void printMessage(Kind kind, CharSequence msg, AnnotatedMixim mixim) {
        this.messager.printMessage(kind, msg, this.mixim, this.annotation.asMirror());
    }
    
    /**
     * Print a suppressible message to the AP messager
     */
    private void printMessage(Kind kind, CharSequence msg, AnnotatedMixim mixim, SuppressedBy suppressedBy) {
        this.messager.printMessage(kind, msg, this.mixim, this.annotation.asMirror(), suppressedBy);
    }

    private void addSoftTarget(TypeHandle type, String reference) {
        ObfuscationData<String> obfClassData = this.obf.getDataProvider().getObfClass(type);
        if (!obfClassData.isEmpty()) {
            this.obf.getReferenceManager().addClassMapping(this.classRef, reference, obfClassData);
        }

        this.addTarget(type);
    }

    private void addTarget(TypeHandle type) {
        this.targets.add(type);
    }

    @Override
    public String toString() {
        return this.mixim.getSimpleName().toString();
    }

    public AnnotationHandle getAnnotation() {
        return this.annotation;
    }

    /**
     * Get the mixim class
     */
    public TypeElement getMixim() {
        return this.mixim;
    }

    /**
     * Get the type handle for the mixim class
     */
    public TypeHandle getHandle() {
        return this.handle;
    }

    /**
     * Get the mixim class reference
     */
    public String getClassRef() {
        return this.classRef;
    }

    /**
     * Get whether this is an interface mixim
     */
    public boolean isInterface() {
        return this.mixim.getKind() == ElementKind.INTERFACE;
    }

    /**
     * Get the <em>primary</em> target
     */
    @Deprecated
    public TypeHandle getPrimaryTarget() {
        return this.primaryTarget;
    }

    /**
     * Get the mixim's targets
     */
    public List<TypeHandle> getTargets() {
        return this.targets;
    }

    /**
     * Get whether this is a multi-target mixim
     */
    public boolean isMultiTarget() {
        return this.targets.size() > 1;
    }

    /**
     * Get whether to remap annotations in this mixim
     */
    public boolean remap() {
        return this.remap;
    }

    public IMappingConsumer getMappings() {
        return this.mappings;
    }

    private void runFinalValidation() {
        for (ExecutableElement method : this.methods) {
            this.overwrites.registerMerge(method);
        }
    }

    public void registerOverwrite(ExecutableElement method, AnnotationHandle overwrite, boolean shouldRemap) {
        this.methods.remove(method);
        this.overwrites.registerOverwrite(new AnnotatedElementOverwrite(method, overwrite, shouldRemap));
    }

    public void registerShadow(VariableElement field, AnnotationHandle shadow, boolean shouldRemap) {
        this.shadows.registerShadow(this.shadows.new AnnotatedElementShadowField(field, shadow, shouldRemap));
    }

    public void registerShadow(ExecutableElement method, AnnotationHandle shadow, boolean shouldRemap) {
        this.methods.remove(method);
        this.shadows.registerShadow(this.shadows.new AnnotatedElementShadowMethod(method, shadow, shouldRemap));
    }

    public void registerInjector(ExecutableElement method, AnnotationHandle inject, InjectorRemap remap) {
        this.methods.remove(method);
        this.injectors.registerInjector(new AnnotatedElementInjector(method, inject, remap));

        List<AnnotationHandle> ats = inject.getAnnotationList("at");
        for (AnnotationHandle at : ats) {
            this.registerInjectionPoint(method, inject, at, remap, "@At(%s)");
        }

        List<AnnotationHandle> slices = inject.getAnnotationList("slice");
        for (AnnotationHandle slice : slices) {
            String id = slice.<String>getValue("id", "");

            AnnotationHandle from = slice.getAnnotation("from");
            if (from != null) {
                this.registerInjectionPoint(method, inject, from, remap, "@Slice[" + id + "](from=@At(%s))");
            }

            AnnotationHandle to = slice.getAnnotation("to");
            if (to != null) {
                this.registerInjectionPoint(method, inject, to, remap, "@Slice[" + id + "](to=@At(%s))");
            }
        }
    }

    public void registerInjectionPoint(ExecutableElement element, AnnotationHandle inject, AnnotationHandle at, InjectorRemap remap, String format) {
        this.injectors.registerInjectionPoint(new AnnotatedElementInjectionPoint(element, inject, at, remap), format);
    }

    public void registerAccessor(ExecutableElement element, AnnotationHandle accessor, boolean shouldRemap) {
        this.methods.remove(element);
        this.accessors.registerAccessor(new AnnotatedElementAccessor(element, accessor, shouldRemap));
    }

    public void registerInvoker(ExecutableElement element, AnnotationHandle invoker, boolean shouldRemap) {
        this.methods.remove(element);
        this.accessors.registerAccessor(new AnnotatedElementInvoker(element, invoker, shouldRemap));
    }

    public void registerSoftImplements(AnnotationHandle implementsAnnotation) {
        this.softImplements.process(implementsAnnotation);
    }

}
