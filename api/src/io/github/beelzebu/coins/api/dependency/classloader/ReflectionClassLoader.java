/**
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
package io.github.beelzebu.coins.api.dependency.classloader;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;

public class ReflectionClassLoader {

    private static final Method ADD_URL_METHOD;

    static {
        try {
            ADD_URL_METHOD = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
            ADD_URL_METHOD.setAccessible(true);
        } catch (NoSuchMethodException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private final URLClassLoader classLoader;

    public ReflectionClassLoader(Object plugin) throws IllegalStateException {
        ClassLoader clazzLoader = plugin.getClass().getClassLoader();
        if (clazzLoader instanceof URLClassLoader) {
            classLoader = (URLClassLoader) clazzLoader;
        } else {
            throw new IllegalStateException("ClassLoader is not instance of URLClassLoader");
        }
    }

    public void loadJar(URL url) {
        try {
            ADD_URL_METHOD.invoke(classLoader, url);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    public void loadJar(Path file) {
        try {
            loadJar(file.toUri().toURL());
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }
}
