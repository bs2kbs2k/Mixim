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

import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixim.MiximEnvironment;
import org.spongepowered.asm.util.Constants;

/**
 * Class loader that is used to load fake mixim classes so that they can be
 * re-defined.
 */
class MiximAgentClassLoader extends ClassLoader {

    private static final Logger logger = LogManager.getLogger("mixim.agent");

    /**
     * Mapping of mixim mixim classes to their fake classes
     */
    private Map<Class<?>, byte[]> mixims = new HashMap<Class<?>, byte[]>();

    /**
     * Mapping that keep track of bytecode for classes that are targeted by
     * mixims
     */
    private Map<String, byte[]> targets = new HashMap<String, byte[]>();

    /**
     * Add a fake mixim class
     *
     * @param name Name of the fake class
     */
    void addMiximClass(String name) {
        MiximAgentClassLoader.logger.debug("Mixim class {} added to class loader", name);
        try {
            byte[] bytes = this.materialise(name);
            Class<?> clazz = this.defineClass(name, bytes, 0, bytes.length);
            // apparently the class needs to be instantiated at least once
            // to be including in list returned by allClasses() method in jdi api
            clazz.newInstance();
            this.mixims.put(clazz, bytes);
        } catch (Throwable e) {
            MiximAgentClassLoader.logger.catching(e);
        }
    }

    /**
     * Registers the bytecode for a class targeted by a mixim
     *
     * @param name Name of the target clas
     * @param classNode ASM tree node of the target class
     */
    void addTargetClass(String name, ClassNode classNode) {
        synchronized (this.targets) {
            if (this.targets.containsKey(name)) {
                return;
            }
            try {
                ClassWriter cw = new ClassWriter(0);
                classNode.accept(cw);
                this.targets.put(name, cw.toByteArray());
            } catch (Exception ex) {
                MiximAgentClassLoader.logger.error("Error storing original class bytecode for {} in mixim hotswap agent. {}: {}",
                        name, ex.getClass().getName(), ex.getMessage());
                MiximAgentClassLoader.logger.debug(ex);
            }
        }
    }

    /**
     * Gets the bytecode for a fake mixim class
     *
     * @param clazz Mixim class
     * @return Bytecode of the fake mixim class
     */
    byte[] getFakeMiximBytecode(Class<?> clazz) {
        return this.mixims.get(clazz);
    }

    /**
     * Gets the original bytecode for a target class
     *
     * @param name Name of the target class
     * @return Original bytecode
     */
    byte[] getOriginalTargetBytecode(String name) {
        synchronized (this.targets) {
            return this.targets.get(name);
        }
    }

    /**
     * Generates the simplest possible class that is instantiable
     *
     * @param name Name of the generated class
     * @return Bytecode of the generated class
     */
    private byte[] materialise(String name) {
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        cw.visit(MiximEnvironment.getCompatibilityLevel().classVersion(), Opcodes.ACC_PUBLIC, name.replace('.', '/'), null,
                Type.getInternalName(Object.class), null);

        // create init method
        MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC, Constants.CTOR, "()V", null, null);
        mv.visitCode();
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, Type.getInternalName(Object.class), Constants.CTOR, "()V", false);
        mv.visitInsn(Opcodes.RETURN);
        mv.visitMaxs(1, 1);
        mv.visitEnd();

        cw.visitEnd();
        return cw.toByteArray();
    }

}
