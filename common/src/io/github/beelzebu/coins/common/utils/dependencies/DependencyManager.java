/*
 * This file is part of LuckPerms, licensed under the MIT License.
 *
 *  Copyright (c) lucko (Luck) <luck@lucko.me>
 *  Copyright (c) contributors
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in all
 *  copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *  SOFTWARE.
 */
package io.github.beelzebu.coins.common.utils.dependencies;

import com.google.common.io.ByteStreams;
import io.github.beelzebu.coins.common.CoinsCore;
import io.github.beelzebu.coins.common.database.StorageType;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Set;

/**
 * Responsible for loading runtime dependencies.
 */
public class DependencyManager {

    private static final CoinsCore CORE = CoinsCore.getInstance();
    private final DependencyRegistry registry = new DependencyRegistry();
    private final EnumMap<Dependency, Path> loaded = new EnumMap<>(Dependency.class);

    private Path getSaveDirectory() {
        Path saveDirectory = new File(CORE.getBootstrap().getDataFolder(), "lib").toPath();
        try {
            Files.createDirectories(saveDirectory);
        } catch (IOException e) {
            throw new RuntimeException("Unable to create lib directory", e);
        }
        return saveDirectory;
    }

    public void loadStorageDependencies(StorageType storageType) {
        Set<Dependency> dependencies = registry.resolveStorageDependencies(storageType);
        CORE.log("Identified following storage dependencies: " + dependencies);
        loadDependencies(dependencies);
    }

    public void loadDependencies(Set<Dependency> dependencies) {
        Path saveDirectory = getSaveDirectory();

        // create a list of file sources
        List<Source> sources = new ArrayList<>();

        // obtain a file for each of the dependencies
        for (Dependency dependency : dependencies) {
            if (loaded.containsKey(dependency)) {
                continue;
            }

            try {
                Path file = downloadDependency(saveDirectory, dependency);
                sources.add(new Source(dependency, file));
            } catch (Throwable e) {
                CORE.log("Exception whilst downloading dependency " + dependency.name());
                e.printStackTrace();
            }
        }

        // load each of the jars
        for (Source source : sources) {
            try {
                CORE.getBootstrap().getPluginClassLoader().loadJar(source.file);
                loaded.put(source.dependency, source.file);
            } catch (Throwable e) {
                CORE.log("Failed to load dependency jar '" + source.file.getFileName().toString() + "'.");
                e.printStackTrace();
            }
        }
    }

    private Path downloadDependency(Path saveDirectory, Dependency dependency) throws Exception {
        String fileName = dependency.name().toLowerCase() + "-" + dependency.getVersion() + ".jar";
        Path file = saveDirectory.resolve(fileName);

        // if the file already exists, don't attempt to re-download it.
        if (Files.exists(file)) {
            return file;
        }

        URL url = new URL(dependency.getUrl());
        try (InputStream in = url.openStream()) {

            // download the jar content
            byte[] bytes = ByteStreams.toByteArray(in);
            if (bytes.length == 0) {
                throw new RuntimeException("Empty stream");
            }

            CORE.log("Successfully downloaded '" + fileName + "'");

            // if the checksum matches, save the content to disk
            Files.write(file, bytes);
        }

        // ensure the file saved correctly
        if (!Files.exists(file)) {
            throw new IllegalStateException("File not present. - " + file.toString());
        } else {
            return file;
        }
    }

    private static final class Source {

        private final Dependency dependency;
        private final Path file;

        private Source(Dependency dependency, Path file) {
            this.dependency = dependency;
            this.file = file;
        }
    }
}
