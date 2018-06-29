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
package io.github.beelzebu.coins.bungee.config;

import io.github.beelzebu.coins.api.config.CoinsConfig;
import io.github.beelzebu.coins.api.plugin.CoinsPlugin;
import java.io.File;
import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.logging.Level;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;

/**
 * @author Beelzebu
 */
public class BungeeConfig extends CoinsConfig {

    private final File configFile;
    private net.md_5.bungee.config.Configuration config;

    public BungeeConfig(File file, CoinsPlugin plugin) {
        super(file, plugin);
        configFile = file;
        reload();
    }

    @Override
    public Object get(String path) {
        return config.get(path);
    }

    @Override
    public Set<String> getConfigurationSection(String path) {
        return new LinkedHashSet<>(config.getSection(path).getKeys());
    }

    @Override
    public final void reload() {
        try {
            config = ConfigurationProvider.getProvider(YamlConfiguration.class).load(configFile);
        } catch (IOException ex) {
            plugin.getBootstrap().getLogger().log(Level.SEVERE, "An unexpected error has occurred reloading the config. {0}", ex.getMessage());
        }
    }
}
