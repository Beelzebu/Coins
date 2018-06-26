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

import io.github.beelzebu.coins.api.config.CoinsConfig;
import io.github.beelzebu.coins.bukkit.CoinsBukkitMain;
import io.github.beelzebu.coins.bukkit.CoinsBukkitPlugin;
import java.io.File;
import java.util.Set;
import org.bukkit.configuration.file.FileConfiguration;

/**
 *
 * @author Beelzebu
 */
public class BukkitConfig extends CoinsConfig {

    private final CoinsBukkitPlugin plugin;
    private final FileConfiguration config;

    public BukkitConfig(File file, CoinsBukkitPlugin plugin) {
        super(file);
        this.plugin = plugin;
        config = ((CoinsBukkitMain) plugin.getBootstrap()).getConfig();
        reload();
    }

    @Override
    public Object get(String path) {
        return config.get(path);
    }

    @Override
    public Set<String> getConfigurationSection(String path) {
        return config.getConfigurationSection(path) != null ? config.getConfigurationSection(path).getKeys(false) : null;
    }

    @Override
    public final void reload() {
        ((CoinsBukkitMain) plugin.getBootstrap()).reloadConfig();
    }
}
