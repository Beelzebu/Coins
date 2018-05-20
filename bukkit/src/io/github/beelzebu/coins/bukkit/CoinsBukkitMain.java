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
import io.github.beelzebu.coins.bukkit.command.CommandManager;
import io.github.beelzebu.coins.bukkit.config.BukkitConfig;
import io.github.beelzebu.coins.bukkit.config.BukkitMessages;
import io.github.beelzebu.coins.bukkit.events.CoinsChangeEvent;
import io.github.beelzebu.coins.bukkit.events.MultiplierEnableEvent;
import io.github.beelzebu.coins.bukkit.messaging.BukkitMessaging;
import io.github.beelzebu.coins.bukkit.utils.CoinsEconomy;
import io.github.beelzebu.coins.common.CoinsCore;
import io.github.beelzebu.coins.common.config.CoinsConfig;
import io.github.beelzebu.coins.common.config.MessagesConfig;
import io.github.beelzebu.coins.common.messaging.ProxyMessaging;
import io.github.beelzebu.coins.common.plugin.CoinsBootstrap;
import io.github.beelzebu.coins.common.utils.dependencies.classloader.PluginClassLoader;
import io.github.beelzebu.coins.common.utils.dependencies.classloader.ReflectionClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

public class CoinsBukkitMain extends JavaPlugin implements CoinsBootstrap {

    protected final CoinsCore core = CoinsCore.getInstance();
    @Getter
    private final CommandManager commandManager;
    @Getter
    private final CoinsBukkitPlugin plugin;
    private BukkitConfig config;
    private BukkitMessaging bmessaging;

    public CoinsBukkitMain() {
        plugin = new CoinsBukkitPlugin(this);
        commandManager = new CommandManager();
    }

    @Override
    public void onLoad() {
        core.setup(this);
        if (getConfig().getBoolean("Vault.Use", false)) {
            if (Bukkit.getPluginManager().getPlugin("Vault") != null) {
                log("Vault found, hooking into it.");
                new CoinsEconomy(this).setup();
            } else {
                core.log("You enabled Vault in the config, but the plugin Vault can't be found.");
            }
        }
    }

    @Override
    public void onEnable() {
        config = new BukkitConfig(plugin);
        core.start();
    }

    @Override
    public void onDisable() {
        core.shutdown();
    }

    @Override
    public CoinsConfig getPluginConfig() {
        return config;
    }

    @Override
    public MessagesConfig getMessages(String lang) {
        return new BukkitMessages(lang);
    }

    @Override
    public void runAsync(Runnable rn) {
        Bukkit.getScheduler().runTaskAsynchronously(this, rn);
    }

    @Override
    public void runAsyncTimmer(Runnable rn, long timer) {
        Bukkit.getScheduler().runTaskTimerAsynchronously(this, rn, 0, timer);
    }

    @Override
    public void runTaskLater(Runnable rn, long ticks) {
        Bukkit.getScheduler().runTaskLater(this, rn, ticks);
    }

    @Override
    public void runSync(Runnable rn) {
        Bukkit.getScheduler().runTask(this, rn);
    }

    @Override
    public void executeCommand(String cmd) {
        Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), cmd);
    }

    @Override
    public void log(Object log) {
        Bukkit.getConsoleSender().sendMessage(CoinsCore.getInstance().rep("&8[&cCoins&8] &7" + log));
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
    public String getVersion() {
        return getDescription().getVersion();
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
        Bukkit.getScheduler().runTask(this, () -> Bukkit.getPluginManager().callEvent(new CoinsChangeEvent(uuid, oldCoins, newCoins)));
    }

    @Override
    public void callMultiplierEnableEvent(Multiplier multiplier) {
        Bukkit.getScheduler().runTask(this, () -> Bukkit.getPluginManager().callEvent(new MultiplierEnableEvent(multiplier)));
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
    public ProxyMessaging getBungeeMessaging() {
        return bmessaging == null ? bmessaging = new BukkitMessaging() : bmessaging;
    }

    @Override
    public PluginClassLoader getPluginClassLoader() {
        return new ReflectionClassLoader(plugin);
    }
}
