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
package org.spongepowered.tools.agent;

import java.lang.instrument.ClassDefinition;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixim.transformer.IMiximTransformer;
import org.spongepowered.asm.mixim.transformer.ext.IHotSwap;
import org.spongepowered.asm.mixim.transformer.throwables.MiximReloadException;
import org.spongepowered.asm.service.IMiximService;
import org.spongepowered.asm.service.MiximService;
import org.spongepowered.asm.util.asm.ASM;

/**
 * An agent that re-transforms a mixim's target classes if the mixim has been
 * redefined. Basically this agent enables hot-swapping of mixims.
 */
public class MiximAgent implements IHotSwap {

    /**
     * Class file transformer that re-transforms mixim's target classes if the
     * mixim has been redefined.
     */
    class Transformer implements ClassFileTransformer {

        @Override
        public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain domain, byte[] classfileBuffer)
                throws IllegalClassFormatException {
            if (classBeingRedefined == null) {
                return null;
            }
            
            byte[] miximBytecode = MiximAgent.classLoader.getFakeMiximBytecode(classBeingRedefined);
            if (miximBytecode != null) {
                ClassNode classNode = new ClassNode(ASM.API_VERSION);
                ClassReader cr = new ClassReader(classfileBuffer);
                cr.accept(classNode, ClassReader.EXPAND_FRAMES);
                
                List<String> targets = this.reloadMixim(className, classNode);
                if (targets == null || !this.reApplyMixims(targets)) {
                    return MiximAgent.ERROR_BYTECODE;
                }
                return miximBytecode;
            }
            
            try {
                MiximAgent.logger.info("Redefining class " + className);
                return MiximAgent.this.classTransformer.transformClassBytes(null, className, classfileBuffer);
            } catch (Throwable th) {
                MiximAgent.logger.error("Error while re-transforming class " + className, th);
                return MiximAgent.ERROR_BYTECODE;
            }
        }

        private List<String> reloadMixim(String className, ClassNode classNode) {
            MiximAgent.logger.info("Redefining mixim {}", className);
            try {
                return MiximAgent.this.classTransformer.reload(className.replace('/', '.'), classNode);
            } catch (MiximReloadException e) {
                MiximAgent.logger.error("Mixim {} cannot be reloaded, needs a restart to be applied: {} ", e.getMiximInfo(), e.getMessage());
            } catch (Throwable th) {
                // catch everything as otherwise it is ignored
                MiximAgent.logger.error("Error while finding targets for mixim " + className, th);
            }
            return null;
        }

        /**
         * Re-apply all mixims to the supplied list of target classes.
         * 
         * @param targets Target classes to re-transform
         * @return true if all targets were transformed, false if transformation
         *          failed
         */
        private boolean reApplyMixims(List<String> targets) {
            IMiximService service = MiximService.getService();
            
            for (String target : targets) {
                String targetName = target.replace('/', '.');
                MiximAgent.logger.debug("Re-transforming target class {}", target);
                try {
                    Class<?> targetClass = service.getClassProvider().findClass(targetName);
                    byte[] targetBytecode = MiximAgent.classLoader.getOriginalTargetBytecode(targetName);
                    if (targetBytecode == null) {
                        MiximAgent.logger.error("Target class {} bytecode is not registered", targetName);
                        return false;
                    }
                    targetBytecode = MiximAgent.this.classTransformer.transformClassBytes(null, targetName, targetBytecode);
                    MiximAgent.instrumentation.redefineClasses(new ClassDefinition(targetClass, targetBytecode));
                } catch (Throwable th) {
                    MiximAgent.logger.error("Error while re-transforming target class " + target, th);
                    return false;
                }
            }
            return true;
        }
    }

    /**
     * Bytecode that signals an error. When returned from the class file
     * transformer this causes a class file format exception and indicates in
     * the ide that somethings went wrong.
     */
    public static final byte[] ERROR_BYTECODE = new byte[]{1};

    /**
     * Class loader used to load mixim classes
     */
    static final MiximAgentClassLoader classLoader = new MiximAgentClassLoader();

    static final Logger logger = LogManager.getLogger("mixim.agent");

    /**
     * Instance used to register the transformer
     */
    static Instrumentation instrumentation = null;

    /**
     * Instances of all agents
     */
    private static List<MiximAgent> agents = new ArrayList<MiximAgent>();

    /**
     * MiximTransformer instance to use to transform the mixim's target classes
     */
    final IMiximTransformer classTransformer;

    /**
     * Constructs an agent from a class transformer in which it will use to
     * transform mixim's target class.
     *
     * @param classTransformer Class transformer that will transform a mixim's
     *                         target class
     */
    public MiximAgent(IMiximTransformer classTransformer) {
        this.classTransformer = classTransformer;
        MiximAgent.agents.add(this);
        if (MiximAgent.instrumentation != null) {
            this.initTransformer();
        }
    }

    private void initTransformer() {
        MiximAgent.instrumentation.addTransformer(new Transformer(), true);
    }

    @Override
    public void registerMiximClass(String name) {
        MiximAgent.classLoader.addMiximClass(name);
    }

    @Override
    public void registerTargetClass(String name, ClassNode classNode) {
        MiximAgent.classLoader.addTargetClass(name, classNode);
    }

    /**
     * Sets the instrumentation instance so that the mixim agents can redefine
     * mixims.
     *
     * @param instrumentation Instance to use to redefine the mixims
     */
    public static void init(Instrumentation instrumentation) {
        MiximAgent.instrumentation = instrumentation;
        if (!MiximAgent.instrumentation.isRedefineClassesSupported()) {
            MiximAgent.logger.error("The instrumentation doesn't support re-definition of classes");
        }
        for (MiximAgent agent : MiximAgent.agents) {
            agent.initTransformer();
        }
    }

    /**
     * Initialize the java agent
     *
     * <p>This will be called automatically if the jar is in a -javaagent java
     * command line argument</p>
     *
     * @param arg Ignored
     * @param instrumentation Instance to use to transform the mixims
     */
    public static void premain(String arg, Instrumentation instrumentation) {
        System.setProperty("mixim.hotSwap", "true");
        MiximAgent.init(instrumentation);
    }

    /**
     * Initialize the java agent
     *
     * <p>This will be called automatically if the java agent is loaded after
     * JVVM startup</p>
     *
     * @param arg Ignored
     * @param instrumentation Instance to use to re-define the mixims
     */
    public static void agentmain(String arg, Instrumentation instrumentation) {
        MiximAgent.init(instrumentation);
    }

}
