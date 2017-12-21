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
package net.nifheim.beelzebu.coins.bukkit.utils;

import java.util.List;
import java.util.Set;
import net.nifheim.beelzebu.coins.bukkit.Main;
import net.nifheim.beelzebu.coins.core.utils.CoinsConfig;
import org.bukkit.configuration.file.FileConfiguration;

/**
 *
 * @author Beelzebu
 */
public class Configuration extends CoinsConfig {

    private final Main plugin;
    private final FileConfiguration config;

    public Configuration(Main main) {
        plugin = main;
        config = main.getConfig();
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
        return config.getConfigurationSection(path).getKeys(false);
    }

    @Override
    public void reload() {
        plugin.reloadConfig();
    }
}
