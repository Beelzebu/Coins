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
package io.github.beelzebu.coins.bukkit;

import io.github.beelzebu.coins.Multiplier;
import io.github.beelzebu.coins.bukkit.config.BukkitConfig;
import io.github.beelzebu.coins.bukkit.config.BukkitMessages;
import io.github.beelzebu.coins.bukkit.events.CoinsChangeEvent;
import io.github.beelzebu.coins.bukkit.events.MultiplierEnableEvent;
import io.github.beelzebu.coins.bukkit.messaging.BukkitBungeeMessaging;
import io.github.beelzebu.coins.common.CoinsCore;
import io.github.beelzebu.coins.common.config.CoinsConfig;
import io.github.beelzebu.coins.common.config.MessagesConfig;
import io.github.beelzebu.coins.common.interfaces.IMethods;
import io.github.beelzebu.coins.common.messaging.BungeeMessaging;
import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;

/**
 *
 * @author Beelzebu
 */
public class BukkitMethods implements IMethods {

    private final Main plugin = Main.getInstance();
    private final CommandSender console = Bukkit.getConsoleSender();
    private BukkitBungeeMessaging bbmessaging;
    private BukkitConfig config;

    @Override
    public Object getPlugin() {
        return plugin;
    }

    @Override
    public void loadConfig() {
        config = new BukkitConfig();
    }

    @Override
    public CoinsConfig getConfig() {
        return config == null ? config = new BukkitConfig() : config;
    }

    @Override
    public MessagesConfig getMessages(String lang) {
        return new BukkitMessages(lang);
    }

    @Override
    public void runAsync(Runnable rn) {
        Bukkit.getScheduler().runTaskAsynchronously((Plugin) getPlugin(), rn);
    }

    @Override
    public void runAsync(Runnable rn, Long timer) {
        Bukkit.getScheduler().runTaskTimerAsynchronously((Plugin) getPlugin(), rn, 0, timer);
    }

    @Override
    public void runSync(Runnable rn) {
        Bukkit.getScheduler().runTask((Plugin) getPlugin(), rn);
    }

    @Override
    public void executeCommand(String cmd) {
        Bukkit.getServer().dispatchCommand(console, cmd);
    }

    @Override
    public void log(Object log) {
        console.sendMessage(CoinsCore.getInstance().rep("&8[&cCoins&8] &7" + log));
    }

    @Override
    public Object getConsole() {
        return Bukkit.getConsoleSender();
    }

    @Override
    public void sendMessage(Object commandsender, String msg) {
        ((CommandSender) commandsender).sendMessage(msg);
    }

    @Override
    public File getDataFolder() {
        return plugin.getDataFolder();
    }

    @Override
    public InputStream getResource(String file) {
        return plugin.getResource(file);
    }

    @Override
    public String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean isOnline(UUID uuid) {
        return Bukkit.getPlayer(uuid) != null;
    }

    @Override
    public boolean isOnline(String name) {
        return Bukkit.getPlayer(name) != null;
    }

    @Override
    public UUID getUUID(String name) {
        return Bukkit.getPlayer(name) != null ? Bukkit.getPlayer(name).getUniqueId() : null;
    }

    @Override
    public String getName(UUID uuid) {
        return Bukkit.getPlayer(uuid) != null ? Bukkit.getPlayer(uuid).getName() : null;
    }

    @Override
    public void callCoinsChangeEvent(UUID uuid, double oldCoins, double newCoins) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            Bukkit.getPluginManager().callEvent(new CoinsChangeEvent(uuid, oldCoins, newCoins));
        });
    }

    @Override
    public void callMultiplierEnableEvent(Multiplier multiplier) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            Bukkit.getPluginManager().callEvent(new MultiplierEnableEvent(multiplier));
        });
    }

    @Override
    public List<String> getPermissions(UUID uuid) {
        List<String> permissions = new ArrayList<>();
        if (isOnline(uuid)) {
            Bukkit.getPlayer(uuid).getEffectivePermissions().forEach(perm -> {
                permissions.add(perm.getPermission());
            });
        }
        return permissions;
    }

    @Override
    public Logger getLogger() {
        return plugin.getLogger();
    }

    @Override
    public BungeeMessaging getBungeeMessaging() {
        return bbmessaging == null ? bbmessaging = new BukkitBungeeMessaging() : bbmessaging;
    }
}
