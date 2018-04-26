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

import io.github.beelzebu.coins.common.CoinsCore;
import io.github.beelzebu.coins.common.database.StorageType;
import io.github.beelzebu.coins.common.messaging.MessagingServiceType;
import java.io.File;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import lombok.experimental.UtilityClass;

/**
 * Responsible for loading runtime dependencies.
 */
@UtilityClass
public class DependencyManager {

    private static final CoinsCore core = CoinsCore.getInstance();
    private static final Method ADD_URL_METHOD;

    static {
        Method addUrlMethod = null;
        try {
            addUrlMethod = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
            addUrlMethod.setAccessible(true);
        } catch (NoSuchMethodException e) {
        }
        ADD_URL_METHOD = addUrlMethod;
    }

    public static void loadAllDependencies() {
        Set<Dependency> dependencies = new LinkedHashSet<>();
        dependencies.addAll(Arrays.asList(Dependency.values()));
        if (classExists("org.sqlite.JDBC") || core.isBungee()) {
            dependencies.remove(Dependency.SQLITE_DRIVER);
        }
        if (classExists("com.mysql.jdbc.Driver")) {
            dependencies.remove(Dependency.MYSQL_DRIVER);
        }
        if (classExists("org.slf4j.Logger") && classExists("org.slf4j.LoggerFactory")) {
            dependencies.remove(Dependency.SLF4J_API);
            dependencies.remove(Dependency.SLF4J_SIMPLE);
        }
        if (classExists("org.apache.commons.io.FileUtils")) {
            dependencies.remove(Dependency.COMMONS_IO);
        }
        if (!core.getStorageType().equals(StorageType.MYSQL) && !core.getStorageType().equals(StorageType.SQLITE)) {
            dependencies.remove(Dependency.HIKARI);
        }
        if (core.getMessagingService() == null || !core.getMessagingService().getType().equals(MessagingServiceType.REDIS)) {
            dependencies.remove(Dependency.COMMONS_POOL);
            dependencies.remove(Dependency.JEDIS);
        }
        if (!dependencies.isEmpty()) {
            loadDependencies(dependencies);
        }
    }

    public static void loadDependencies(Set<Dependency> dependencies) throws RuntimeException {
        core.getMethods().log("Identified the following dependencies: " + dependencies.toString());

        File libDir = new File(core.getMethods().getDataFolder(), "lib");
        if (!(libDir.exists() || libDir.mkdirs())) {
            throw new RuntimeException("Unable to create lib dir - " + libDir.getPath());
        }

        // Download files.
        List<File> filesToLoad = new ArrayList<>();
        dependencies.forEach(dependency -> {
            try {
                filesToLoad.add(downloadDependency(libDir, dependency));
            } catch (Exception e) {
                core.getMethods().log("Exception whilst downloading dependency " + dependency.name());
            }
        });

        // Load classes.
        filesToLoad.forEach(file -> {
            try {
                loadJar(file);
            } catch (Throwable t) {
                core.getMethods().log("Failed to load dependency jar " + file.getName());
            }
        });
    }

    private static File downloadDependency(File libDir, Dependency dependency) throws Exception {
        String fileName = dependency.name().toLowerCase() + "-" + dependency.getVersion() + ".jar";

        File file = new File(libDir, fileName);
        if (file.exists()) {
            return file;
        }

        URL url = new URL(dependency.getUrl());

        core.getMethods().log("Dependency '" + fileName + "' could not be found. Attempting to download.");
        try (InputStream in = url.openStream()) {
            Files.copy(in, file.toPath());
        }

        if (!file.exists()) {
            throw new IllegalStateException("File not present. - " + file.toString());
        } else {
            core.getMethods().log("Dependency '" + fileName + "' successfully downloaded.");
            return file;
        }
    }

    private static void loadJar(File file) throws RuntimeException {
        // get the classloader to load into
        ClassLoader classLoader = core.getMethods().getPlugin().getClass().getClassLoader();

        if (classLoader instanceof URLClassLoader) {
            try {
                ADD_URL_METHOD.invoke(classLoader, file.toURI().toURL());
            } catch (IllegalAccessException | InvocationTargetException | MalformedURLException e) {
                throw new RuntimeException("Unable to invoke URLClassLoader#addURL", e);
            }
        } else {
            throw new RuntimeException("Unknown classloader type: " + classLoader.getClass());
        }
    }

    private static boolean classExists(String className) {
        try {
            Class.forName(className);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}
