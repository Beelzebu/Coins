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
package io.github.beelzebu.coins.bukkit.config;

import io.github.beelzebu.coins.bukkit.Main;
import io.github.beelzebu.coins.common.config.CoinsConfig;
import java.util.List;
import java.util.Set;
import org.bukkit.configuration.file.FileConfiguration;

/**
 *
 * @author Beelzebu
 */
public class BukkitConfig extends CoinsConfig {

    private final FileConfiguration config = Main.getInstance().getConfig();

    public BukkitConfig() {
        reload();
    }

    @Override
    public Object get(String path) {
        return config.get(path);
    }

    @Override
    public String getString(String path) {
        return config.getString(path);
    }

    @Override
    public List<String> getStringList(String path) {
        return config.getStringList(path);
    }

    @Override
    public boolean getBoolean(String path) {
        return config.getBoolean(path);
    }

    @Override
    public int getInt(String path) {
        return config.getInt(path);
    }

    @Override
    public double getDouble(String path) {
        return config.getDouble(path);
    }

    @Override
    public Object get(String path, Object def) {
        return (config.get(path) == null ? def : config.get(path));
    }

    @Override
    public String getString(String path, String def) {
        return (config.get(path) == null ? def : config.getString(path));
    }

    @Override
    public List<String> getStringList(String path, List<String> def) {
        return (config.get(path) == null ? def : config.getStringList(path));
    }

    @Override
    public boolean getBoolean(String path, boolean def) {
        return (config.get(path) == null ? def : config.getBoolean(path));
    }

    @Override
    public int getInt(String path, int def) {
        return (config.get(path) == null ? def : config.getInt(path));
    }

    @Override
    public double getDouble(String path, double def) {
        return (config.get(path) == null ? def : config.getDouble(path));
    }

    @Override
    public void set(String path, Object value) {
        config.set(path, value);
    }

    @Override
    public Set<String> getConfigurationSection(String path) {
        return config.getConfigurationSection(path) != null ? config.getConfigurationSection(path).getKeys(false) : null;
    }

    @Override
    public final void reload() {
        Main.getInstance().reloadConfig();
    }
}
