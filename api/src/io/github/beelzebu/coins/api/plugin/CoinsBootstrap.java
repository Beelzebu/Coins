/**
 * This file is part of Coins
 *
 * Copyright (C) 2018 Beelzebu
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
package io.github.beelzebu.coins.api.plugin;

import io.github.beelzebu.coins.api.Multiplier;
import io.github.beelzebu.coins.api.config.AbstractConfigFile;
import io.github.beelzebu.coins.api.config.CoinsConfig;
import io.github.beelzebu.coins.api.dependency.classloader.ReflectionClassLoader;
import io.github.beelzebu.coins.api.messaging.ProxyMessaging;
import java.io.File;
import java.io.InputStream;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * @author Beelzebu
 */
public interface CoinsBootstrap {

    CoinsPlugin getPlugin();

    CoinsConfig getPluginConfig();

    AbstractConfigFile getFileAsConfig(File file);

    void runAsync(Runnable rn);

    void runSync(Runnable rn);

    void executeCommand(String cmd);

    void log(String msg);

    Object getConsole();

    void sendMessage(Object CommandSender, String msg);

    File getDataFolder();

    InputStream getResource(String filename);

    String getVersion();

    boolean isOnline(UUID uuid);

    boolean isOnline(String name);

    /**
     * Get the UUID of a online player by his name.
     *
     * @param name The name of the online player to get the uuid.
     * @return The uuid of the online player.
     */
    UUID getUUID(String name);

    /**
     * Get the name of a online player by his UUID.
     *
     * @param uuid The UUID of the online player to get the name.
     * @return The uuid of the online player.
     */
    String getName(UUID uuid);

    void callCoinsChangeEvent(UUID uuid, double oldCoins, double newCoins);

    void callMultiplierEnableEvent(Multiplier multiplier);

    List<String> getPermissions(UUID uuid);

    Logger getLogger();

    ProxyMessaging getBungeeMessaging();

    default ReflectionClassLoader getReflectionClassLoader() {
        return new ReflectionClassLoader(getPlugin());
    }
}
