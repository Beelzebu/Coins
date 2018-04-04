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
package io.github.beelzebu.coins.common.interfaces;

import java.util.List;
import java.util.Set;

/**
 *
 * @author Beelzebu
 */
public interface IConfiguration {

    Object get(String path);

    String getString(String path);

    List<String> getStringList(String path);

    boolean getBoolean(String path);

    int getInt(String path);

    double getDouble(String path);

    Object get(String path, Object def);

    String getString(String path, String def);

    List<String> getStringList(String path, List<String> def);

    boolean getBoolean(String path, boolean def);

    int getInt(String path, int def);

    double getDouble(String path, double def);

    void set(String path, Object value);

    Set<String> getConfigurationSection(String path);

    void reload();
}
