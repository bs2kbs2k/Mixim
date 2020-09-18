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
package org.spongepowered.asm.launch.platform;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.launch.platform.container.IContainerHandle;
import org.spongepowered.asm.mixim.MiximEnvironment;
import org.spongepowered.asm.mixim.MiximEnvironment.CompatibilityLevel;
import org.spongepowered.asm.mixim.MiximEnvironment.Phase;
import org.spongepowered.asm.service.MiximService;
import org.spongepowered.asm.service.ServiceVersionError;
import org.spongepowered.asm.mixim.Mixims;
import org.spongepowered.asm.mixim.throwables.MiximError;

//import com.google.common.collect.ImmutableList;

/**
 * Handler for platform-specific behaviour required in different mixim
 * environments.
 */
public class MiximPlatformManager {

    private static final String DEFAULT_MAIN_CLASS = "net.minecraft.client.main.Main";
    
    /**
     * Make with the logging already
     */
    private static final Logger logger = LogManager.getLogger("mixim");
    
    /**
     * Bootstrap delegate 
     */
//    private final Delegate delegate;
    
    /**
     * Tweak containers
     */
    private final Map<IContainerHandle, MiximContainer> containers = new LinkedHashMap<IContainerHandle, MiximContainer>();
    
    /**
     * Connectors 
     */
    private final MiximConnectorManager connectors = new MiximConnectorManager();

    
    /**
     * Container for this tweaker 
     */
    private MiximContainer primaryContainer;
    
    /**
     * Tracks whether {@link #acceptOptions} was called yet, if true, causes new
     * agents to be <tt>prepare</tt>d immediately 
     */
    private boolean prepared = false;
    
    /**
     * Tracks whether {@link #inject} was called yet
     */
    private boolean injected;
    
    public MiximPlatformManager() { //Delegate delegate) {
//        this.delegate = delegate;
    }
    
    /**
     * Initialise the platform manager
     */
    public void init() {
        MiximPlatformManager.logger.debug("Initialising Mixim Platform Manager");

        IContainerHandle primaryContainerHandle = MiximService.getService().getPrimaryContainer();
        this.primaryContainer = this.addContainer(primaryContainerHandle);

        // Do an early scan to ensure preinit mixims are discovered
        this.scanForContainers();
    }
    
    /**
     * Get the phase provider classes from the primary container
     */
    public Collection<String> getPhaseProviderClasses() {
        Collection<String> phaseProviders = this.primaryContainer.getPhaseProviders();
        if (phaseProviders != null) {
            return Collections.<String>unmodifiableCollection(phaseProviders);
        }
        
        return Collections.<String>emptyList();
    }

    /**
     * Add a new container to this platform and return the new container (or an
     * existing container if the handle was previously registered)
     * 
     * @param handle Container handle to add
     * @return container for specified resource handle
     */
    public final MiximContainer addContainer(IContainerHandle handle) {
        MiximContainer existingContainer = this.containers.get(handle);
        if (existingContainer != null) {
            return existingContainer;
        }
        
        MiximContainer container = this.createContainerFor(handle);
        this.containers.put(handle, container);
        this.addNestedContainers(handle);
        return container;
    }

    private MiximContainer createContainerFor(IContainerHandle handle) {
        MiximPlatformManager.logger.debug("Adding mixim platform agents for container {}", handle);
        MiximContainer container = new MiximContainer(this, handle);
        if (this.prepared) {
            container.prepare();
        }
        return container;
    }

    private void addNestedContainers(IContainerHandle handle) {
        for (IContainerHandle nested : handle.getNestedContainers()) {
            if (!this.containers.containsKey(nested)) {
                this.addContainer(nested);
            }
        }
    }

    /**
     * Prepare all containers in this platform
     * 
     * @param args command-line arguments from tweaker
     */
    public final void prepare(CommandLineOptions args) {
        this.prepared = true;
        for (MiximContainer container : this.containers.values()) {
            container.prepare();
        }
        
        for (String config : args.getConfigs()) {
            this.addConfig(config);
        }
    }

    /**
     * Initialise the primary container and dispatch inject to all containers
     */
    public final void inject() {
        if (this.injected) {
            return;
        }
        this.injected = true;
        
        if (this.primaryContainer != null) {
            this.primaryContainer.initPrimaryContainer();
        }
        
        this.scanForContainers();
        MiximPlatformManager.logger.debug("inject() running with {} agents", this.containers.size());
        for (MiximContainer container : this.containers.values()) {
            try {
                container.inject();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        
        this.connectors.inject();
    }

    /**
     * Scan the current environment for mixim containers and add agents for them
     */
    private void scanForContainers() {
        Collection<IContainerHandle> miximContainers = null;
        
        try {
            miximContainers = MiximService.getService().getMiximContainers();
        } catch (AbstractMethodError ame) {
            throw new ServiceVersionError("Mixim service is out of date");
        }
        
        List<IContainerHandle> existingContainers = new ArrayList<IContainerHandle>(this.containers.keySet());
        for (IContainerHandle existingContainer : existingContainers) {
            this.addNestedContainers(existingContainer);
        }
        
        for (IContainerHandle handle : miximContainers) {
            try {
                MiximPlatformManager.logger.debug("Adding agents for Mixim Container {}", handle);
                this.addContainer(handle);
            } catch (Exception ex) {
                ex.printStackTrace();
            } 
        }
    }

    /**
     * Queries all containers for launch target, returns null if no containers
     * specify a launch target
     */
    public String getLaunchTarget() {
        return MiximPlatformManager.DEFAULT_MAIN_CLASS;
    }

    /**
     * Set the desired compatibility level as a string, used by agents to set
     * compatibility level from jar manifest
     * 
     * @param level compatibility level as a string
     */
    @SuppressWarnings("deprecation")
    final void setCompatibilityLevel(String level) {
        try {
            CompatibilityLevel value = CompatibilityLevel.valueOf(level.toUpperCase(Locale.ROOT));
            MiximPlatformManager.logger.debug("Setting mixim compatibility level: {}", value);
            MiximEnvironment.setCompatibilityLevel(value);
        } catch (IllegalArgumentException ex) {
            MiximPlatformManager.logger.warn("Invalid compatibility level specified: {}", level);
        }
    }

    /**
     * Add a config from a jar manifest source or the command line. Supports
     * config declarations in the form <tt>filename.json</tt> or alternatively
     * <tt>filename.json&#064;PHASE</tt> where <tt>PHASE</tt> is a
     * case-sensitive string token representing an environment phase.
     * 
     * @param config config resource name, does not require a leading /
     */
    final void addConfig(String config) {
        if (config.endsWith(".json")) {
            MiximPlatformManager.logger.debug("Registering mixim config: {}", config);
            Mixims.addConfiguration(config);
        } else if (config.contains(".json@")) {
            throw new MiximError("Setting config phase via manifest is no longer supported: " + config + ". Specify target in config instead");
        }
    }
    
    /**
     * Add a token provider class from a jar manifest source. Supports either
     * bare class names in the form <tt>blah.package.ClassName</tt> or
     * alternatively <tt>blah.package.ClassName&#064;PHASE</tt> where
     * <tt>PHASE</tt> is a case-sensitive string token representing an
     * environment phase name.
     * 
     * @param provider provider class name, optionally suffixed with &#064;PHASE
     */
    final void addTokenProvider(String provider) {
        if (provider.contains("@")) {
            String[] parts = provider.split("@", 2);
            Phase phase = Phase.forName(parts[1]);
            if (phase != null) {
                MiximPlatformManager.logger.debug("Registering token provider class: {}", parts[0]);
                MiximEnvironment.getEnvironment(phase).registerTokenProviderClass(parts[0]);
            }
            return;
        }

        MiximEnvironment.getDefaultEnvironment().registerTokenProviderClass(provider);
    }
    
    /**
     * Add a mixim connector class for a jar manifest source. Supports only bare
     * class names.
     * 
     * @param connectorClass Name of the connector class to load
     */
    final void addConnector(String connectorClass) {
        this.connectors.addConnector(connectorClass);
        
    }

}
