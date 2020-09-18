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
package org.spongepowered.asm.service.modlauncher;

import java.io.InputStream;
import java.util.Collection;

import org.spongepowered.asm.launch.IClassProcessor;
import org.spongepowered.asm.launch.platform.container.ContainerHandleModLauncher;
import org.spongepowered.asm.mixim.MiximEnvironment.CompatibilityLevel;
import org.spongepowered.asm.mixim.MiximEnvironment.Phase;
import org.spongepowered.asm.mixim.transformer.MiximTransformationHandler;
import org.spongepowered.asm.service.IClassBytecodeProvider;
import org.spongepowered.asm.service.IClassProvider;
import org.spongepowered.asm.service.IClassTracker;
import org.spongepowered.asm.service.IMiximAuditTrail;
import org.spongepowered.asm.service.ITransformerProvider;
import org.spongepowered.asm.service.MiximServiceAbstract;
import org.spongepowered.asm.util.IConsumer;

import com.google.common.collect.ImmutableList;

import cpw.mods.modlauncher.Launcher;
import cpw.mods.modlauncher.api.ITransformationService;

/**
 * Mixim service for ModLauncher
 */
public class MiximServiceModLauncher extends MiximServiceAbstract {
    
    /**
     * Specification version to check for at startup
     */
    private static final String MODLAUNCHER_SPECIFICATION_VERSION = "4.0";

    /**
     * Class provider, either uses hacky internals or provided service
     */
    private IClassProvider classProvider;
    
    /**
     * Bytecode provider, either uses hacky internals or provided service
     */
    private IClassBytecodeProvider bytecodeProvider;
    
    /**
     * Container for the mixim pipeline which is called by the launch plugin
     */
    private MiximTransformationHandler transformationHandler;
    
    /**
     * Class tracker, tracks class loads and registered invalid classes
     */
    private ModLauncherClassTracker classTracker;
    
    /**
     * Audit trail adapter 
     */
    private ModLauncherAuditTrail auditTrail;

    /**
     * Environment phase consumer, TEMP
     */
    private IConsumer<Phase> phaseConsumer;
    
    /**
     * Only allow onInit to be called once
     */
    private volatile boolean initialised;

    /**
     * Root container
     */
    private ContainerHandleModLauncher rootContainer = new ContainerHandleModLauncher(this.getName());
    
    /**
     * Begin init
     * 
     * @param bytecodeProvider bytecode provider
     */
    public void onInit(IClassBytecodeProvider bytecodeProvider) {
        if (this.initialised) {
            throw new IllegalStateException("Already initialised");
        }
        this.initialised = true;
        this.bytecodeProvider = bytecodeProvider;
    }
    
    /**
     * Lifecycle event
     */
    public void onStartup() {
        this.phaseConsumer.accept(Phase.DEFAULT);
    }

    // TEMP
    @SuppressWarnings("deprecation")
    @Override
    public void wire(Phase phase, IConsumer<Phase> phaseConsumer) {
        super.wire(phase, phaseConsumer);
        this.phaseConsumer = phaseConsumer;
    }

    /* (non-Javadoc)
     * @see org.spongepowered.asm.service.IMiximService#getName()
     */
    @Override
    public String getName() {
        return "ModLauncher";
    }
    
    /* (non-Javadoc)
     * @see org.spongepowered.asm.service.IMiximService
     *      #getMinCompatibilityLevel()
     */
    @Override
    public CompatibilityLevel getMinCompatibilityLevel() {
        return CompatibilityLevel.JAVA_8;
    }

    /* (non-Javadoc)
     * @see org.spongepowered.asm.service.IMiximService#isValid()
     */
    @Override
    public boolean isValid() {
        try {
            Launcher.INSTANCE.hashCode();
            final Package pkg = ITransformationService.class.getPackage();
            if (!pkg.isCompatibleWith(MiximServiceModLauncher.MODLAUNCHER_SPECIFICATION_VERSION)) {
                return false;
            }
        } catch (Throwable th) {
            return false;
        }
        return true;
    }

    /* (non-Javadoc)
     * @see org.spongepowered.asm.service.IMiximService#getClassProvider()
     */
    @Override
    public IClassProvider getClassProvider() {
        if (this.classProvider == null) {
            this.classProvider = new ModLauncherClassProvider();
        }
        return this.classProvider;
    }

    /* (non-Javadoc)
     * @see org.spongepowered.asm.service.IMiximService#getBytecodeProvider()
     */
    @Override
    public IClassBytecodeProvider getBytecodeProvider() {
        if (this.bytecodeProvider == null) {
            throw new IllegalStateException("Service initialisation incomplete");
        }
        return this.bytecodeProvider;
    }
    
    /* (non-Javadoc)
     * @see org.spongepowered.asm.service.IMiximService#getTransformerProvider()
     */
    @Override
    public ITransformerProvider getTransformerProvider() {
        return null;
    }
    
    /* (non-Javadoc)
     * @see org.spongepowered.asm.service.IMiximService#getClassTracker()
     */
    @Override
    public IClassTracker getClassTracker() {
        if (this.classTracker == null) {
            this.classTracker = new ModLauncherClassTracker();
        }
        return this.classTracker;
    }
    
    /* (non-Javadoc)
     * @see org.spongepowered.asm.service.IMiximService#getAuditTrail()
     */
    @Override
    public IMiximAuditTrail getAuditTrail() {
        if (this.auditTrail == null) {
            this.auditTrail = new ModLauncherAuditTrail();
        }
        return this.auditTrail;
    }

    /**
     * Get (or create) the transformation handler
     */
    private IClassProcessor getTransformationHandler() {
        if (this.transformationHandler == null) {
            this.transformationHandler = new MiximTransformationHandler();
        }
        return this.transformationHandler;
    }
    
    /* (non-Javadoc)
     * @see org.spongepowered.asm.service.IMiximService#getPlatformAgents()
     */
    @Override
    public Collection<String> getPlatformAgents() {
        return ImmutableList.<String>of(
            "org.spongepowered.asm.launch.platform.MiximPlatformAgentMinecraftForge"
        );
    }
    
    /* (non-Javadoc)
     * @see org.spongepowered.asm.service.IMiximService#getPrimaryContainer()
     */
    @Override
    public ContainerHandleModLauncher getPrimaryContainer() {
        return this.rootContainer;
    }

    /* (non-Javadoc)
     * @see org.spongepowered.asm.service.IMiximService#getResourceAsStream(
     *      java.lang.String)
     */
    @Override
    public InputStream getResourceAsStream(String name) {
        return Thread.currentThread().getContextClassLoader().getResourceAsStream(name);
    }

    /**
     * Internal method to retrieve the class processors which will be called by
     * the launch plugin
     */
    public Collection<IClassProcessor> getProcessors() {
        return ImmutableList.<IClassProcessor>of(
            this.getTransformationHandler(),
            (IClassProcessor)this.getClassTracker()
        );
    }

}
