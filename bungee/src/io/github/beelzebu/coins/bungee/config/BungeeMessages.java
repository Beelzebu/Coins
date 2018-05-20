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

import io.github.beelzebu.coins.common.CoinsCore;
import io.github.beelzebu.coins.common.config.MessagesConfig;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;

/**
 *
 * @author Beelzebu
 */
public class BungeeMessages extends MessagesConfig {

    private File langFile;
    private net.md_5.bungee.config.Configuration messages;

    public BungeeMessages(String lang) {
        super(lang);
        load(new File(CoinsCore.getInstance().getBootstrap().getDataFolder() + "/messages", "messages" + lang + ".yml"));
    }

    @Override
    public Object get(String path) {
        return messages.get(path);
    }

    @Override
    public String getString(String path) {
        return messages.getString(path);
    }

    @Override
    public List<String> getStringList(String path) {
        return messages.getStringList(path);
    }

    @Override
    public boolean getBoolean(String path) {
        return messages.getBoolean(path);
    }

    @Override
    public int getInt(String path) {
        return messages.getInt(path);
    }

    @Override
    public double getDouble(String path) {
        return messages.getDouble(path);
    }

    @Override
    public Object get(String path, Object def) {
        return (messages.get(path) == null ? def : messages.get(path));
    }

    @Override
    public String getString(String path, String def) {
        return (messages.get(path) == null ? def : messages.getString(path));
    }

    @Override
    public List<String> getStringList(String path, List<String> def) {
        return (messages.get(path) == null ? def : messages.getStringList(path));
    }

    @Override
    public boolean getBoolean(String path, boolean def) {
        return (messages.get(path) == null ? def : messages.getBoolean(path));
    }

    @Override
    public int getInt(String path, int def) {
        return (messages.get(path) == null ? def : messages.getInt(path));
    }

    @Override
    public double getDouble(String path, double def) {
        return (messages.get(path) == null ? def : messages.getDouble(path));
    }

    @Override
    public void set(String path, Object value) {
        messages.set(path, value);
    }

    @Override
    public Set<String> getConfigurationSection(String path) {
        return (Set<String>) messages.getSection(path).getKeys();
    }

    @Override
    public final void reload() {
        load(langFile);
    }

    private net.md_5.bungee.config.Configuration load(File file) {
        langFile = file;
        try {
            messages = ConfigurationProvider.getProvider(YamlConfiguration.class).load(file);
        } catch (IOException ex) {
            Logger.getLogger(BungeeConfig.class.getName()).log(Level.WARNING, "An unexpected error has ocurred reloading the messages file. {0}", ex.getMessage());
        }
        return messages;
    }
}
