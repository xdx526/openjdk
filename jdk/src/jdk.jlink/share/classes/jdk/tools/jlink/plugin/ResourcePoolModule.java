/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package jdk.tools.jlink.plugin;

import java.lang.module.ModuleDescriptor;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

/**
  * Link-time representation of a module.
  */
public interface ResourcePoolModule {
    /**
     * The module name.
     *
     * @return The module name.
     */
    public String name();

    /**
     * Retrieves a ResourcePoolEntry from the given path (e.g:
     * /mymodule/com.foo.bar/MyClass.class)
     *
     * @param path The piece of data path.
     * @return A ResourcePoolEntry of the given path, if found.
     */
    public Optional<ResourcePoolEntry> findEntry(String path);

    /**
     * The module descriptor of this module.
     *
     * @return The module descriptor.
     */
    public ModuleDescriptor descriptor();

    /**
     * Retrieves all the packages located in this module.
     *
     * @return The set of packages.
     */
    public Set<String> packages();

    /**
     * Retrieves the stream of ResourcePoolEntry.
     *
     * @return The ResourcePoolEntry stream.
     */
    public Stream<ResourcePoolEntry> entries();

    /**
     * Return the number of ResourcePoolEntry count in this ResourcePoolModule.
     *
     * @return the entry count.
     */
    public int entryCount();
}
