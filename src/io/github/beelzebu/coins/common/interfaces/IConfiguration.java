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

    public Object get(String path);

    public String getString(String path);

    public List<String> getStringList(String path);

    public boolean getBoolean(String path);

    public int getInt(String path);

    public double getDouble(String path);

    public Object get(String path, Object def);

    public String getString(String path, String def);

    public List<String> getStringList(String path, List<String> def);

    public boolean getBoolean(String path, boolean def);

    public int getInt(String path, int def);

    public double getDouble(String path, double def);

    public void set(String path, Object value);

    public Set<String> getConfigurationSection(String path);

    public void reload();
}
