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
package io.github.beelzebu.coins.common.config;

import java.util.List;
import java.util.Set;

/**
 *
 * @author Beelzebu
 */
public interface IConfiguration {

    public Object get(String path);

    public default String getString(String path) {
        return (String) get(path);
    }

    public default List<String> getStringList(String path) {
        return (List<String>) get(path);
    }

    public default boolean getBoolean(String path) {
        return (boolean) get(path);
    }

    public default int getInt(String path) {
        return (int) get(path);
    }

    public default double getDouble(String path) {
        return (double) get(path);
    }

    public default Object get(String path, Object def) {
        return get(path) != null ? get(path) : def;
    }

    public default String getString(String path, String def) {
        return get(path) != null ? (String) get(path) : def;
    }

    public default List<String> getStringList(String path, List<String> def) {
        return getStringList(path) != null ? getStringList(path) : def;
    }

    public default boolean getBoolean(String path, boolean def) {
        return get(path) != null ? (boolean) get(path) : def;
    }

    public default int getInt(String path, int def) {
        return get(path) != null ? (int) get(path) : def;
    }

    public default double getDouble(String path, double def) {
        return get(path) != null ? (double) get(path) : def;
    }

    public Set<String> getConfigurationSection(String path);

    public void reload();
}
