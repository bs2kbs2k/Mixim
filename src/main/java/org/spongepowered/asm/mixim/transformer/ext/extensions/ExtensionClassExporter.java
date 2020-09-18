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
package org.spongepowered.asm.mixim.transformer.ext.extensions;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixim.MiximEnvironment;
import org.spongepowered.asm.mixim.MiximEnvironment.Option;
import org.spongepowered.asm.mixim.transformer.ext.IDecompiler;
import org.spongepowered.asm.mixim.transformer.ext.IExtension;
import org.spongepowered.asm.mixim.transformer.ext.ITargetClassContext;
import org.spongepowered.asm.transformers.MiximClassWriter;
import org.spongepowered.asm.util.Constants;
import org.spongepowered.asm.util.perf.Profiler.Section;

import com.google.common.io.Files;
import com.google.common.io.MoreFiles;
import com.google.common.io.RecursiveDeleteOption;

/**
 * Debug exporter
 */
public class ExtensionClassExporter implements IExtension {
    
    private static final String DECOMPILER_CLASS = "org.spongepowered.asm.mixim.transformer.debug.RuntimeDecompiler";
    
    private static final String EXPORT_CLASS_DIR = "class";
    private static final String EXPORT_JAVA_DIR = "java";

    /**
     * Logger
     */
    private static final Logger logger = LogManager.getLogger("mixim");

    /**
     * Directory to export classes to when debug.export is enabled
     */
    private final File classExportDir = new File(Constants.DEBUG_OUTPUT_DIR, ExtensionClassExporter.EXPORT_CLASS_DIR);
    
    /**
     * Runtime decompiler for exported classes 
     */
    private final IDecompiler decompiler;
    
    public ExtensionClassExporter(MiximEnvironment env) {
        this.decompiler = this.initDecompiler(env, new File(Constants.DEBUG_OUTPUT_DIR, ExtensionClassExporter.EXPORT_JAVA_DIR));

        try {
            MoreFiles.deleteRecursively(this.classExportDir.toPath(), RecursiveDeleteOption.ALLOW_INSECURE);
        } catch (IOException ex) {
            ExtensionClassExporter.logger.debug("Error cleaning class output directory: {}", ex.getMessage());
        }
    }
    
    public boolean isDecompilerActive() {
        return this.decompiler != null;
    }
    
    private IDecompiler initDecompiler(MiximEnvironment env, File outputPath) {
        if (!env.getOption(Option.DEBUG_EXPORT_DECOMPILE)) {
            return null;
        }
        
        try {
            boolean as = env.getOption(Option.DEBUG_EXPORT_DECOMPILE_THREADED);
            ExtensionClassExporter.logger.info("Attempting to load Fernflower decompiler{}", as ? " (Threaded mode)" : "");
            String className = ExtensionClassExporter.DECOMPILER_CLASS + (as ? "Async" : "");
            @SuppressWarnings("unchecked")
            Class<? extends IDecompiler> clazz = (Class<? extends IDecompiler>)Class.forName(className);
            Constructor<? extends IDecompiler> ctor = clazz.getDeclaredConstructor(File.class);
            IDecompiler decompiler = ctor.newInstance(outputPath);
            ExtensionClassExporter.logger.info("Fernflower decompiler was successfully initialised, exported classes will be decompiled{}",
                    as ? " in a separate thread" : "");
            return decompiler;
        } catch (Throwable th) {
            ExtensionClassExporter.logger.info("Fernflower could not be loaded, exported classes will not be decompiled. {}: {}",
                    th.getClass().getSimpleName(), th.getMessage());
        }
        return null;
    }

    private String prepareFilter(String filter) {
        filter = "^\\Q" + filter.replace("**", "\201").replace("*", "\202").replace("?", "\203") + "\\E$";
        return filter.replace("\201", "\\E.*\\Q").replace("\202", "\\E[^\\.]+\\Q").replace("\203", "\\E.\\Q").replace("\\Q\\E", "");
    }

    private boolean applyFilter(String filter, String subject) {
        return Pattern.compile(this.prepareFilter(filter), Pattern.CASE_INSENSITIVE).matcher(subject).matches();
    }
    
    @Override
    public boolean checkActive(MiximEnvironment environment) {
        return true;
    }

    @Override
    public void preApply(ITargetClassContext context) {
    }

    @Override
    public void postApply(ITargetClassContext context) {
    }

    @Override
    public void export(MiximEnvironment env, String name, boolean force, ClassNode classNode) {
        // Export transformed class for debugging purposes
        if (force || env.getOption(Option.DEBUG_EXPORT)) {
            String filter = env.getOptionValue(Option.DEBUG_EXPORT_FILTER);
            if (force || filter == null || this.applyFilter(filter, name)) {
                Section exportTimer = MiximEnvironment.getProfiler().begin("debug.export");
                
                File outputFile = this.dumpClass(name.replace('.', '/'), classNode);
                if (this.decompiler != null) {
                    this.decompiler.decompile(outputFile);
                }
                exportTimer.end();
            }
        }
    }

    /**
     * Write class bytecode to disk for debug purposes
     * 
     * @param fileName filename to write (.class will be automatically appended)
     * @param classNode class to dump
     * @return written file
     */
    public File dumpClass(String fileName, ClassNode classNode) {
        File outputFile = new File(this.classExportDir, fileName + ".class");
        outputFile.getParentFile().mkdirs();
        try {
            byte[] bytecode = ExtensionClassExporter.getClassBytes(classNode, true);
            if (bytecode != null) {
                Files.write(bytecode, outputFile);
            }
        } catch (IOException ex) {
            // don't care
        }
        return outputFile;
    }

    private static byte[] getClassBytes(ClassNode classNode, boolean computeFrames) {
        byte[] bytes = null;
        try {
            MiximClassWriter cw = new MiximClassWriter(computeFrames ? ClassWriter.COMPUTE_FRAMES : 0);
            classNode.accept(cw);
            bytes = cw.toByteArray();
        } catch (NegativeArraySizeException ex) {
            // Try again with compute frames turned off, this gives us a better chance
            // of successful export when the class is corrupt which - given we are
            // exporting for debugging purposes - is worthwhile so that we have a
            // the bytecode to inspect!
            if (computeFrames) {
                ExtensionClassExporter.logger.warn("Exporting class {} with COMPUTE_FRAMES failed! Trying a raw export.", classNode.name);
                return ExtensionClassExporter.getClassBytes(classNode, false);
            }
            ex.printStackTrace();
        } catch (Exception ex) {
            // well, damn
            ex.printStackTrace();
        }
        return bytes;
    }

}
