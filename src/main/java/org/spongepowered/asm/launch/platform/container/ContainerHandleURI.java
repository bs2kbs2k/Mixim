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
package org.spongepowered.asm.launch.platform.container;

import java.io.File;
import java.net.URI;
import java.util.Collection;
import java.util.Collections;

import org.spongepowered.asm.launch.platform.MainAttributes;

/**
 * Container handle which directly replaces the use of classpath location URIs
 * from previous mixim versions
 */
public class ContainerHandleURI implements IContainerHandle {
    
    /**
     * Target URI
     */
    private final URI uri;

    /**
     * File containing this tweaker
     */
    private final File file;
    
    /**
     * "Main" manifest attributes from the container
     */
    private final MainAttributes attributes;

    public ContainerHandleURI(URI uri) {
        this.uri = uri;
        this.file = this.uri != null ? new File(this.uri) : null;
        this.attributes = MainAttributes.of(uri);
    }
    
    /**
     * Get the URI for this handle
     */
    public URI getURI() {
        return this.uri;
    }
    
    /**
     * Get the container file
     */
    public File getFile() {
        return this.file;
    }

    /* (non-Javadoc)
     * @see org.spongepowered.asm.launch.platform.container.IContainerHandle
     *      #getAttribute(java.lang.String)
     */
    @Override
    public String getAttribute(String name) {
        return this.attributes.get(name);
    }
    
    /* (non-Javadoc)
     * @see org.spongepowered.asm.launch.platform.container.IContainerHandle
     *      #getNestedContainers()
     */
    @Override
    public Collection<IContainerHandle> getNestedContainers() {
        return Collections.<IContainerHandle>emptyList();
    }
    
    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object other) {
        if (!(other instanceof ContainerHandleURI)) {
            return false;
        }
        return this.uri.equals(((ContainerHandleURI)other).uri);
    }
    
    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return this.uri.hashCode();
    }
    
    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return String.format("ContainerHandleURI(%s)", this.uri);
    }

}
