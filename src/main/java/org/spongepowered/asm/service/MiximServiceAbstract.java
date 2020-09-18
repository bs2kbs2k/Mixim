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
import java.util.Collection;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.launch.platform.IMiximPlatformAgent;
import org.spongepowered.asm.launch.platform.IMiximPlatformServiceAgent;
import org.spongepowered.asm.launch.platform.container.IContainerHandle;
import org.spongepowered.asm.mixim.MiximEnvironment.CompatibilityLevel;
import org.spongepowered.asm.mixim.MiximEnvironment.Phase;
import org.spongepowered.asm.util.Constants;
import org.spongepowered.asm.util.IConsumer;
import org.spongepowered.asm.util.ReEntranceLock;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;

/**
 * Mixim Service base class
 */
public abstract class MiximServiceAbstract implements IMiximService {
    
    // Consts
    protected static final String LAUNCH_PACKAGE = "org.spongepowered.asm.launch.";
    protected static final String MIXIN_PACKAGE = "org.spongepowered.asm.mixim.";

    /**
     * Logger 
     */
    protected static final Logger logger = LogManager.getLogger("mixim");

    /**
     * Transformer re-entrance lock, shared between the mixim transformer and
     * the metadata service
     */
    protected final ReEntranceLock lock = new ReEntranceLock(1);
    
    /**
     * Service agent instances 
     */
    private List<IMiximPlatformServiceAgent> serviceAgents;

    /**
     * Detected side name
     */
    private String sideName;

    /* (non-Javadoc)
     * @see org.spongepowered.asm.service.IMiximService#prepare()
     */
    @Override
    public void prepare() {
    }

    /* (non-Javadoc)
     * @see org.spongepowered.asm.service.IMiximService#getInitialPhase()
     */
    @Override
    public Phase getInitialPhase() {
        return Phase.PREINIT;
    }
    
    /* (non-Javadoc)
     * @see org.spongepowered.asm.service.IMiximService
     *      #getMinCompatibilityLevel()
     */
    @Override
    public CompatibilityLevel getMinCompatibilityLevel() {
        return null;
    }
    
    /* (non-Javadoc)
     * @see org.spongepowered.asm.service.IMiximService
     *      #getMaxCompatibilityLevel()
     */
    @Override
    public CompatibilityLevel getMaxCompatibilityLevel() {
        return null;
    }

    /* (non-Javadoc)
     * @see org.spongepowered.asm.service.IMiximService#beginPhase()
     */
    @Override
    public void beginPhase() {
    }

    /* (non-Javadoc)
     * @see org.spongepowered.asm.service.IMiximService
     *      #checkEnv(java.lang.Object)
     */
    @Override
    public void checkEnv(Object bootSource) {
    }

    /* (non-Javadoc)
     * @see org.spongepowered.asm.service.IMiximService#init()
     */
    @Override
    public void init() {
        for (IMiximPlatformServiceAgent agent : this.getServiceAgents()) {
            agent.init();
        }
    }

    /* (non-Javadoc)
     * @see org.spongepowered.asm.service.IMiximService#getReEntranceLock()
     */
    @Override
    public ReEntranceLock getReEntranceLock() {
        return this.lock;
    }
    
    /* (non-Javadoc)
     * @see org.spongepowered.asm.service.IMiximService#getMiximContainers()
     */
    @Override
    public Collection<IContainerHandle> getMiximContainers() {
        Builder<IContainerHandle> list = ImmutableList.<IContainerHandle>builder();
        this.getContainersFromAgents(list);
        return list.build();
    }

    /**
     * Collect mixim containers from platform agents
     */
    protected final void getContainersFromAgents(Builder<IContainerHandle> list) {
        for (IMiximPlatformServiceAgent agent : this.getServiceAgents()) {
            Collection<IContainerHandle> containers = agent.getMiximContainers();
            if (containers != null) {
                list.addAll(containers);
            }
        }
    }

    /* (non-Javadoc)
     * @see org.spongepowered.asm.service.IMiximService#getSideName()
     */
    @Override
    public final String getSideName() {
        if (this.sideName != null) {
            return this.sideName;
        }
        
        for (IMiximPlatformServiceAgent agent : this.getServiceAgents()) {
            try {
                String side = agent.getSideName();
                if (side != null) {
                    return this.sideName = side;
                }
            } catch (Exception ex) {
                MiximServiceAbstract.logger.catching(ex);
            }
        }
        
        return Constants.SIDE_UNKNOWN;
    }

    private List<IMiximPlatformServiceAgent> getServiceAgents() {
        if (this.serviceAgents != null) {
            return this.serviceAgents;
        }
        this.serviceAgents = new ArrayList<IMiximPlatformServiceAgent>();
        for (String agentClassName : this.getPlatformAgents()) {
            try {
                @SuppressWarnings("unchecked")
                Class<IMiximPlatformAgent> agentClass = (Class<IMiximPlatformAgent>)this.getClassProvider().findClass(agentClassName, false);
                IMiximPlatformAgent agent = agentClass.newInstance();
                if (agent instanceof IMiximPlatformServiceAgent) {
                    this.serviceAgents.add((IMiximPlatformServiceAgent)agent);
                }
            } catch (Exception ex) {
                // Bad?
                ex.printStackTrace();
            }
        }
        return this.serviceAgents;
    }

    // AMS - TEMP WIRING TO AVOID THE COMPLEXITY OF MERGING MULTIPHASE WITH 0.8
    
    /**
     * Temp wiring. Called when the initial phase is spun up in the environment.
     * 
     * @param phase Initial phase
     * @param phaseConsumer Delegate for the service (or agents) to trigger
     *      later phases
     * @deprecated temporary
     */
    @Deprecated
    public void wire(Phase phase, IConsumer<Phase> phaseConsumer) {
        for (IMiximPlatformServiceAgent agent : this.getServiceAgents()) {
            agent.wire(phase, phaseConsumer);
        }
    }

    /**
     * Temp wiring. Called when the default phase is started in the environment.
     * @deprecated temporary
     */
    @Deprecated
    public void unwire() {
        for (IMiximPlatformServiceAgent agent : this.getServiceAgents()) {
            agent.unwire();
        }
    }

}
