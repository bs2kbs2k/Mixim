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
package org.spongepowered.asm.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.base.Joiner;

/**
 * Provides access to the service layer which connects the mixim transformer to
 * a particular host environment. Host environments are implemented as services
 * implementing {@link IMiximService} in order to decouple them from mixim's
 * core. This allows us to support LegacyLauncher 
 */
public final class MiximService {
    
    /**
     * Log all the things
     */
    private static final Logger logger = LogManager.getLogger("mixim");

    /**
     * Singleton
     */
    private static MiximService instance;
    
    private ServiceLoader<IMiximServiceBootstrap> bootstrapServiceLoader;
    
    private final Set<String> bootedServices = new HashSet<String>(); 

    /**
     * Service loader 
     */
    private ServiceLoader<IMiximService> serviceLoader;

    /**
     * Service
     */
    private IMiximService service = null;

    /**
     * Global Property Service
     */
    private IGlobalPropertyService propertyService;

    /**
     * Singleton pattern
     */
    private MiximService() {
        this.runBootServices();
    }

    private void runBootServices() {
        this.bootstrapServiceLoader = ServiceLoader.<IMiximServiceBootstrap>load(IMiximServiceBootstrap.class, this.getClass().getClassLoader());
        Iterator<IMiximServiceBootstrap> iter = this.bootstrapServiceLoader.iterator();
        while (iter.hasNext()) {
            try {
                IMiximServiceBootstrap bootService = iter.next();
                bootService.bootstrap();
                this.bootedServices.add(bootService.getServiceClassName());
            } catch (ServiceInitialisationException ex) {
                // Expected if service cannot start
                MiximService.logger.debug("Mixim bootstrap service {} is not available: {}", ex.getStackTrace()[0].getClassName(), ex.getMessage());
            } catch (Throwable th) {
                MiximService.logger.debug("Catching {}:{} initialising service", th.getClass().getName(), th.getMessage(), th);
            }
        }
    }

    /**
     * Singleton pattern, get or create the instance
     */
    private static MiximService getInstance() {
        if (MiximService.instance == null) {
            MiximService.instance = new MiximService();
        }
        
        return MiximService.instance;
    }
    
    /**
     * Boot
     */
    public static void boot() {
        MiximService.getInstance();
    }
    
    public static IMiximService getService() {
        return MiximService.getInstance().getServiceInstance();
    }

    private synchronized IMiximService getServiceInstance() {
        if (this.service == null) {
            this.service = this.initService();
        }
        return this.service;
    }

    private IMiximService initService() {
        this.serviceLoader = ServiceLoader.<IMiximService>load(IMiximService.class, this.getClass().getClassLoader());
        Iterator<IMiximService> iter = this.serviceLoader.iterator();
        List<String> badServices = new ArrayList<String>();
        int brokenServiceCount = 0;
        while (iter.hasNext()) {
            try {
                IMiximService service = iter.next();
                if (this.bootedServices.contains(service.getClass().getName())) {
                    MiximService.logger.debug("MiximService [{}] was successfully booted in {}", service.getName(), this.getClass().getClassLoader());
                }
                if (service.isValid()) {
                    return service;
                }
                MiximService.logger.debug("MiximService [{}] is not valid", service.getName());
                badServices.add(String.format("INVALID[%s]", service.getName()));
            } catch (ServiceConfigurationError sce) {
//                sce.printStackTrace();
                brokenServiceCount++;
            } catch (Throwable th) {
                String faultingClassName = th.getStackTrace()[0].getClassName();
                MiximService.logger.debug("MiximService [{}] failed initialisation: {}", faultingClassName, th.getMessage());
                int pos = faultingClassName.lastIndexOf('.');
                badServices.add(String.format("ERROR[%s]", pos < 0 ? faultingClassName : faultingClassName.substring(pos + 1)));
//                th.printStackTrace();
            }
        }
        
        String brokenServiceNote = brokenServiceCount == 0 ? "" : " and " + brokenServiceCount + " other invalid services.";
        throw new ServiceNotAvailableError("No mixim host service is available. Services: " + Joiner.on(", ").join(badServices) + brokenServiceNote);
    }

    /**
     * Blackboard
     */
    public static IGlobalPropertyService getGlobalPropertyService() {
        return MiximService.getInstance().getGlobalPropertyServiceInstance();
    }

    /**
     * Retrieves the GlobalPropertyService Instance... FactoryProviderBean...
     * Observer...InterfaceStream...Function...Broker... help me why won't it
     * stop
     */
    private IGlobalPropertyService getGlobalPropertyServiceInstance() {
        if (this.propertyService == null) {
            this.propertyService = this.initPropertyService();
        }
        return this.propertyService;
    }

    private IGlobalPropertyService initPropertyService() {
        ServiceLoader<IGlobalPropertyService> serviceLoader = ServiceLoader.<IGlobalPropertyService>load(IGlobalPropertyService.class,
                this.getClass().getClassLoader());
        
        Iterator<IGlobalPropertyService> iter = serviceLoader.iterator();
        while (iter.hasNext()) {
            try {
                IGlobalPropertyService service = iter.next();
                return service;
            } catch (ServiceConfigurationError serviceError) {
//                serviceError.printStackTrace();
            } catch (Throwable th) {
//                th.printStackTrace();
            }
        }
        throw new ServiceNotAvailableError("No mixim global property service is available");
    }
}
