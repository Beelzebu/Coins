/*
 * This file is part of Coins
 *
 * Copyright © 2018 Beelzebu
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU Affero General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option) any
 * later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package io.github.beelzebu.coins.api.dependency.relocation;

import io.github.beelzebu.coins.api.dependency.Dependency;
import io.github.beelzebu.coins.api.dependency.DependencyManager;
import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Beelzebu
 */
public class RelocationHandler {

    private final Set<Dependency> dependencies = EnumSet.of(Dependency.ASM, Dependency.ASM_COMMONS, Dependency.JAR_RELOCATOR);
    private final Constructor<?> relocatorConstructor;
    private final Method relocatorMethod;

    public RelocationHandler(DependencyManager dependencyManager) {
        try {
            dependencyManager.loadDependencies(dependencies);
            Class<?> relocator = dependencyManager.obtainClassLoaderWith(dependencies).loadClass("me.lucko.jarrelocator.JarRelocator");
            relocatorConstructor = relocator.getDeclaredConstructor(File.class, File.class, Map.class);
            relocatorConstructor.setAccessible(true);
            relocatorMethod = relocator.getDeclaredMethod("run");
            relocatorMethod.setAccessible(true);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void remap(Path input, Path output, List<Relocation> relocations) throws Exception {
        Map<String, String> mappings = new HashMap<>();
        for (Relocation relocation : relocations) {
            mappings.put(relocation.getPattern(), relocation.getRelocatedPattern());
        }
        relocatorMethod.invoke(relocatorConstructor.newInstance(input.toFile(), output.toFile(), mappings));
    }
}
