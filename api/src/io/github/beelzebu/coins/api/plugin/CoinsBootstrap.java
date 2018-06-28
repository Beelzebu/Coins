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

    public CoinsPlugin getPlugin();

    public CoinsConfig getPluginConfig();

    public AbstractConfigFile getFileAsConfig(File file);

    public void runAsync(Runnable rn);

    public void runAsyncTimmer(Runnable rn, long timmer);

    public void runTaskLater(Runnable rn, long ticks);

    public void runSync(Runnable rn);

    public void executeCommand(String cmd);

    public void log(String msg);

    public Object getConsole();

    public void sendMessage(Object CommandSender, String msg);

    public File getDataFolder();

    public InputStream getResource(String filename);

    public String getVersion();

    public boolean isOnline(UUID uuid);

    public boolean isOnline(String name);

    /**
     * Get the UUID of a online player by his name.
     *
     * @param name The name of the online player to get the uuid.
     * @return The uuid of the online player.
     * @throws NullPointerException if the player with that name is offline.
     */
    public UUID getUUID(String name) throws NullPointerException;

    /**
     * Get the name of a online player by his UUID.
     *
     * @param uuid The UUID of the online player to get the name.
     * @return The uuid of the online player.
     * @throws NullPointerException if the player with that uuid is offline.
     */
    public String getName(UUID uuid) throws NullPointerException;

    public void callCoinsChangeEvent(UUID uuid, double oldCoins, double newCoins);

    public void callMultiplierEnableEvent(Multiplier multiplier);

    public List<String> getPermissions(UUID uuid);

    public Logger getLogger();

    public ProxyMessaging getBungeeMessaging();

    default ReflectionClassLoader getReflectionClassLoader() {
        return new ReflectionClassLoader(getPlugin());
    }
}
