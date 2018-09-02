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
package io.github.beelzebu.coins.api.dependency;

import com.google.common.collect.ImmutableSet;
import com.google.common.io.ByteStreams;
import io.github.beelzebu.coins.api.dependency.classloader.IsolatedClassLoader;
import io.github.beelzebu.coins.api.dependency.relocation.Relocation;
import io.github.beelzebu.coins.api.dependency.relocation.RelocationHandler;
import io.github.beelzebu.coins.api.plugin.CoinsPlugin;
import io.github.beelzebu.coins.api.storage.StorageType;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public final class DependencyManager {

    private final CoinsPlugin plugin;
    private final DependencyRegistry registry;
    private final Map<Dependency, Path> loaded = new EnumMap<>(Dependency.class);
    private final Map<ImmutableSet<Dependency>, IsolatedClassLoader> loaders = new HashMap<>();
    private RelocationHandler relocationHandler;

    private Path getSaveDirectory() {
        Path saveDirectory = new File(plugin.getBootstrap().getDataFolder(), "lib").toPath();
        try {
            Files.createDirectories(saveDirectory);
        } catch (IOException ex) {
            throw new RuntimeException("Unable to create lib directory", ex);
        }
        return saveDirectory;
    }

    public void loadStorageDependencies(StorageType storageType) {
        Set<Dependency> dependencies = registry.resolveStorageDependencies(storageType);
        plugin.log("Identified following storage dependencies: " + dependencies);
        loadDependencies(dependencies);
    }

    public void loadDependencies(Set<Dependency> dependencies) {
        Path saveDirectory = getSaveDirectory();
        List<Source> sources = new ArrayList<>();
        if (dependencies.contains(Dependency.JEDIS)) {
            dependencies.add(Dependency.COMMONS_POOL_2);
        }
        dependencies.stream().filter(dependency -> !loaded.containsKey(dependency)).forEachOrdered(dependency -> {
            try {
                Path file = downloadDependency(saveDirectory, dependency);
                sources.add(new Source(dependency, file));
            } catch (Exception ex) {
                plugin.log("Exception whilst downloading dependency " + dependency.name());
                plugin.debug(ex);
            }
        });
        List<Source> remappedJars = new ArrayList<>(sources.size());
        for (Source source : sources) {
            try {
                List<Relocation> relocations = source.getDependency().getRelocations();
                if (relocations.isEmpty()) {
                    remappedJars.add(source);
                    continue;
                }
                Path input = source.getFile();
                Path output = input.getParent().resolve("remapped-" + input.getFileName().toString());
                if (Files.exists(output)) {
                    remappedJars.add(new Source(source.getDependency(), output));
                    continue;
                }
                plugin.log("Attempting to apply relocations to " + input.getFileName().toString() + "...");
                relocationHandler = new RelocationHandler(this);
                relocationHandler.remap(input, output, relocations);
                remappedJars.add(new Source(source.getDependency(), output));
            } catch (Exception ex) {
                plugin.log("Unable to remap the source file '" + source.getDependency().name() + "'.");
                plugin.debug(ex);
            }
        }
        remappedJars.forEach(jar -> {
            if (!registry.shouldAutoLoad(jar.getDependency())) {
                loaded.put(jar.getDependency(), jar.getFile());
            } else {
                try {
                    plugin.getBootstrap().getReflectionClassLoader().loadJar(jar.getFile());
                    loaded.put(jar.getDependency(), jar.getFile());
                } catch (Throwable ex) {
                    plugin.log("Failed to load dependency jar '" + jar.getFile().getFileName().toString() + "'.");
                    plugin.debug(ex.getMessage());
                }
            }
        });
    }

    private Path downloadDependency(Path saveDirectory, Dependency dependency) throws Exception {
        String fileName = dependency.name().toLowerCase() + "-" + dependency.getVersion() + ".jar";
        Path file = saveDirectory.resolve(fileName);
        if (Files.exists(file)) {
            return file;
        }
        URL url = new URL(dependency.getUrl());
        try (InputStream in = url.openStream()) {
            byte[] bytes = ByteStreams.toByteArray(in);
            if (bytes.length == 0) {
                throw new RuntimeException("Empty stream");
            }
            plugin.log("Successfully downloaded '" + fileName + "'");
            Files.write(file, bytes);
        }
        if (!Files.exists(file)) {
            throw new IllegalStateException("File not present. - " + file.toString());
        } else {
            return file;
        }
    }

    public IsolatedClassLoader obtainClassLoaderWith(Set<Dependency> dependencies) {
        ImmutableSet<Dependency> set = ImmutableSet.copyOf(dependencies);
        dependencies.stream().filter(dependency -> !loaded.containsKey(dependency)).forEachOrdered(dependency -> {
            throw new IllegalStateException("Dependency " + dependency + " is not loaded.");
        });
        synchronized (loaders) {
            IsolatedClassLoader classLoader = loaders.get(set);
            if (classLoader != null) {
                return classLoader;
            }
            URL[] urls = set.stream().map(loaded::get).map(file -> {
                try {
                    return file.toUri().toURL();
                } catch (MalformedURLException ex) {
                    throw new RuntimeException(ex);
                }
            }).toArray(URL[]::new);
            classLoader = new IsolatedClassLoader(urls);
            loaders.put(set, classLoader);
            return classLoader;
        }
    }

    @Getter
    private static final class Source {

        private final Dependency dependency;
        private final Path file;

        private Source(Dependency dependency, Path file) {
            this.dependency = dependency;
            this.file = file;
        }
    }
}
