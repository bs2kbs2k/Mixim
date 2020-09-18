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
package org.spongepowered.asm.launch;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.launch.platform.CommandLineOptions;
import org.spongepowered.asm.launch.platform.MiximPlatformManager;
import org.spongepowered.asm.mixim.MiximEnvironment;
import org.spongepowered.asm.mixim.MiximEnvironment.Phase;
import org.spongepowered.asm.service.MiximService;

/**
 * Bootstraps the mixim subsystem. This class acts as a bridge between the mixim
 * subsystem and the tweaker or coremod which is boostrapping it. Without this
 * class, a coremod may cause classload of MiximEnvironment in the
 * LaunchClassLoader before we have a chance to exclude it. By placing the main
 * bootstrap logic here we avoid the need for consumers to add the classloader
 * exclusion themselves.
 * 
 * <p>In development, where (because of the classloader environment at dev time)
 * it is safe to let a coremod initialise the mixim subsystem, we can perform
 * initialisation all in one go using the {@link #init} method and everything is
 * fine. However in production the tweaker must be used and the situation is a
 * little more delicate.</p>
 * 
 * <p>In an ideal world, the mixim tweaker would initialise the environment in
 * its constructor and that would be the end of the story. However we also need
 * to register the additional tweaker for environment to detect the transition
 * from pre-init to default and we cannot do this within the tweaker constructor
 * without triggering a ConcurrentModificationException in the tweaker list. To
 * work around this we register the secondary tweaker from within the mixim 
 * tweaker's acceptOptions method instead.</p>
 */
public abstract class MiximBootstrap {

    /**
     * Subsystem version
     */
    public static final String VERSION = "0.8.1";
    
    /**
     * Log all the things
     */
    private static final Logger logger = LogManager.getLogger("mixim");
    
    // These are Klass local, with luck this shouldn't be a problem
    private static boolean initialised = false;
    private static boolean initState = true;
    
    // Static initialiser, run boot services as early as possible
    static {
        MiximService.boot();
        MiximService.getService().prepare();
    }
    
    /**
     * Platform manager instance
     */
    private static MiximPlatformManager platform;

    private MiximBootstrap() {}
    
    /**
     * @deprecated use <tt>MiximService.getService().beginPhase()</tt> instead
     */
    @Deprecated
    public static void addProxy() {
        MiximService.getService().beginPhase();
    }

    /**
     * Get the platform manager
     */
    public static MiximPlatformManager getPlatform() {
        if (MiximBootstrap.platform == null) {
            Object globalPlatformManager = GlobalProperties.<Object>get(GlobalProperties.Keys.PLATFORM_MANAGER);
            if (globalPlatformManager instanceof MiximPlatformManager) {
                MiximBootstrap.platform = (MiximPlatformManager)globalPlatformManager;
            } else {
                MiximBootstrap.platform = new MiximPlatformManager();
                GlobalProperties.put(GlobalProperties.Keys.PLATFORM_MANAGER, MiximBootstrap.platform);
                MiximBootstrap.platform.init();
            }
        }
        return MiximBootstrap.platform;
    }

    /**
     * Initialise the mixim subsystem
     */
    public static void init() {
        if (!MiximBootstrap.start()) {
            return;
        }

        MiximBootstrap.doInit(CommandLineOptions.defaultArgs());
    }

    /**
     * Phase 1 of mixim initialisation
     */
    static boolean start() {
        if (MiximBootstrap.isSubsystemRegistered()) {
            if (!MiximBootstrap.checkSubsystemVersion()) {
                throw new MiximInitialisationError("Mixim subsystem version " + MiximBootstrap.getActiveSubsystemVersion()
                        + " was already initialised. Cannot bootstrap version " + MiximBootstrap.VERSION);
            }
            return false;
        }
            
        MiximBootstrap.registerSubsystem(MiximBootstrap.VERSION);
        
        if (!MiximBootstrap.initialised) {
            MiximBootstrap.initialised = true;
            
            Phase initialPhase = MiximService.getService().getInitialPhase();
            if (initialPhase == Phase.DEFAULT) {
                MiximBootstrap.logger.error("Initialising mixim subsystem after game pre-init phase! Some mixims may be skipped.");
                MiximEnvironment.init(initialPhase);
                MiximBootstrap.getPlatform().prepare(CommandLineOptions.defaultArgs());
                MiximBootstrap.initState = false;
            } else {
                MiximEnvironment.init(initialPhase);
            }
            
            MiximService.getService().beginPhase();
        }
        
        MiximBootstrap.getPlatform();
        
        return true;
    }

    @Deprecated
    static void doInit(List<String> args) {
        MiximBootstrap.doInit(CommandLineOptions.ofArgs(args));
    }
    
    /**
     * Phase 2 of mixim initialisation, initialise the phases
     */
    static void doInit(CommandLineOptions args) {
        if (!MiximBootstrap.initialised) {
            if (MiximBootstrap.isSubsystemRegistered()) {
                MiximBootstrap.logger.warn("Multiple Mixim containers present, init suppressed for " + MiximBootstrap.VERSION);
                return;
            }
            
            throw new IllegalStateException("MiximBootstrap.doInit() called before MiximBootstrap.start()");
        }

        MiximBootstrap.getPlatform().getPhaseProviderClasses();
//        for (String platformProviderClass : MiximBootstrap.getPlatform().getPhaseProviderClasses()) {
//            System.err.printf("Registering %s\n", platformProviderClass);
//            MiximEnvironment.registerPhaseProvider(platformProviderClass);
//        }

        if (MiximBootstrap.initState) {
            MiximBootstrap.getPlatform().prepare(args);
            MiximService.getService().init();
        }
    }

    static void inject() {
        MiximBootstrap.getPlatform().inject();
    }

    private static boolean isSubsystemRegistered() {
        return GlobalProperties.<Object>get(GlobalProperties.Keys.INIT) != null;
    }

    private static boolean checkSubsystemVersion() {
        return MiximBootstrap.VERSION.equals(MiximBootstrap.getActiveSubsystemVersion());
    }

    private static Object getActiveSubsystemVersion() {
        Object version = GlobalProperties.get(GlobalProperties.Keys.INIT);
        return version != null ? version : "";
    }

    private static void registerSubsystem(String version) {
        GlobalProperties.put(GlobalProperties.Keys.INIT, version);
    }

}
