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

import io.github.beelzebu.coins.api.config.AbstractConfigFile;
import java.io.File;
import java.util.Set;
import org.bukkit.configuration.file.YamlConfiguration;

/**
 *
 * @author Beelzebu
 */
public class BukkitMessages extends AbstractConfigFile {

    private final File langFile;
    private YamlConfiguration messages;

    public BukkitMessages(File file) {
        super(file);
        messages = YamlConfiguration.loadConfiguration(langFile = file);
    }

    @Override
    public Object get(String path) {
        return messages.get(path);
    }

    @Override
    public Set<String> getConfigurationSection(String path) {
        return messages.getConfigurationSection(path).getKeys(false);
    }

    @Override
    public void reload() {
        messages = YamlConfiguration.loadConfiguration(langFile);
    }
}
