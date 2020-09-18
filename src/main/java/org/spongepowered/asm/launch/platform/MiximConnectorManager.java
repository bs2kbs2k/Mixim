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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixim.connect.IMiximConnector;
import org.spongepowered.asm.service.IClassProvider;
import org.spongepowered.asm.service.MiximService;

/**
 * Manager for Mixim containers bootstrapping via {@link IMiximConnector}
 */
public class MiximConnectorManager {
    
    /**
     * Logging to the max
     */
    private static final Logger logger = LogManager.getLogger("mixim");
    
    private final Set<String> connectorClasses = new LinkedHashSet<String>();

    private final List<IMiximConnector> connectors = new ArrayList<IMiximConnector>();

    MiximConnectorManager() {
    }

    void addConnector(String connectorClass) {
        this.connectorClasses.add(connectorClass);
    }

    void inject() {
        this.loadConnectors();
        this.initConnectors();
    }

    @SuppressWarnings("unchecked")
    void loadConnectors() {
        IClassProvider classProvider = MiximService.getService().getClassProvider();
        
        for (String connectorClassName : this.connectorClasses) {
            Class<IMiximConnector> connectorClass = null; 
            try {
                Class<?> clazz = classProvider.findClass(connectorClassName);
                if (!IMiximConnector.class.isAssignableFrom(clazz)) {
                    MiximConnectorManager.logger.error("Mixim Connector [" + connectorClassName + "] does not implement IMiximConnector");
                    continue;
                }
                connectorClass = (Class<IMiximConnector>)clazz;
            } catch (ClassNotFoundException ex) {
                MiximConnectorManager.logger.catching(ex);
                continue;
            }
            
            try {
                IMiximConnector connector = connectorClass.newInstance();
                this.connectors.add(connector);
                MiximConnectorManager.logger.info("Successfully loaded Mixim Connector [" + connectorClassName + "]");
            } catch (ReflectiveOperationException ex) {
                MiximConnectorManager.logger.warn("Error loading Mixim Connector [" + connectorClassName + "]", ex);
            }
        }
        this.connectorClasses.clear();
    }
    
    void initConnectors() {
        for (IMiximConnector connector : this.connectors) {
            try {
                connector.connect();
            } catch (Exception ex) {
                MiximConnectorManager.logger.warn("Error initialising Mixim Connector [" + connector.getClass().getName() + "]", ex);
            }
        }
    }

}
