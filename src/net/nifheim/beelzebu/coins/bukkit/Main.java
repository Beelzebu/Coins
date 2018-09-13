/**
 * This file is part of Coins
 *
 * Copyright (C) 2017 Beelzebu
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
package net.nifheim.beelzebu.coins.bukkit;

import java.util.Iterator;
import java.util.UUID;
import net.nifheim.beelzebu.coins.CoinsAPI;
import net.nifheim.beelzebu.coins.bukkit.command.CommandManager;
import net.nifheim.beelzebu.coins.bukkit.listener.CommandListener;
import net.nifheim.beelzebu.coins.bukkit.listener.GUIListener;
import net.nifheim.beelzebu.coins.bukkit.listener.InternalListener;
import net.nifheim.beelzebu.coins.bukkit.listener.PlayerJoinListener;
import net.nifheim.beelzebu.coins.bukkit.listener.SignListener;
import net.nifheim.beelzebu.coins.bukkit.utils.CoinsEconomy;
import net.nifheim.beelzebu.coins.bukkit.utils.Configuration;
import net.nifheim.beelzebu.coins.bukkit.utils.bungee.PluginMessage;
import net.nifheim.beelzebu.coins.bukkit.utils.leaderheads.LeaderHeadsHook;
import net.nifheim.beelzebu.coins.bukkit.utils.placeholders.CoinsPlaceholders;
import net.nifheim.beelzebu.coins.bukkit.utils.placeholders.MultipliersPlaceholders;
import net.nifheim.beelzebu.coins.common.CoinsCore;
import net.nifheim.beelzebu.coins.common.executor.Executor;
import net.nifheim.beelzebu.coins.common.utils.CacheManager;
import net.nifheim.beelzebu.coins.common.utils.CoinsConfig;
import net.nifheim.beelzebu.coins.common.utils.dependencies.DependencyManager;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

public class Main extends JavaPlugin {

    private static Main instance;
    private final CoinsCore core = CoinsCore.getInstance();
    private CommandManager commandManager;
    private Configuration configuration;

    public static Main getInstance() {
        return instance;
    }

    @Override
    public void onLoad() {
        instance = this;
        core.setup(new BukkitMethods());
        DependencyManager.loadAllDependencies();
        if (getConfig().getBoolean("Vault.Use", false)) {
            if (Bukkit.getPluginManager().getPlugin("Vault") != null) {
                new CoinsEconomy(this).setup();
            } else {
                core.log("You enabled Vault in the config, but the plugin Vault can't be found.");
            }
        }
    }

    @Override
    public void onEnable() {
        configuration = new Configuration(this);
        core.start();
        commandManager = new CommandManager();
        loadManagers();
        startListeners();
        getConfig().getConfigurationSection("Command executor").getKeys(false).forEach(id -> core.getExecutorManager().addExecutor(new Executor(id, getConfig().getString("Command executor." + id + ".Displayname", id), getConfig().getDouble("Command executor." + id + ".Cost", 0), getConfig().getStringList("Command executor." + id + ".Command"))));
        startTasks();
    }

    @Override
    public void onDisable() {
        if (getConfig().getBoolean("Vault.Use", false)) {
            new CoinsEconomy(this).shutdown();
        }
        commandManager.unregisterCommand();
        Bukkit.getScheduler().cancelTasks(this);
        core.shutdown();
    }

    private void loadManagers() {
        // Create the command
        commandManager.registerCommand();
        // Hook placeholders
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            core.getMethods().log("PlaceholderAPI found, hooking into it.");
            new CoinsPlaceholders(this).hook();
            new MultipliersPlaceholders(this).hook();
        }
        if (getConfig().getBoolean("Vault.Use", false)) {
            if (Bukkit.getPluginManager().getPlugin("Vault") != null) {
                new CoinsEconomy(this).setup();
            } else {
                core.log("You enabled Vault in the config, but the plugin Vault can't be found.");
            }
        }
        if (Bukkit.getPluginManager().getPlugin("LeaderHeads") != null) {
            core.getMethods().log("LeaderHeads found, hooking into it.");
            new BukkitRunnable() {
                boolean leaderheads = false;

                @Override
                public void run() {
                    if (Bukkit.getPluginManager().getPlugin("LeaderHeads").isEnabled()) {
                        new LeaderHeadsHook();
                        leaderheads = true;
                    }
                    if (leaderheads) {
                        cancel();
                    }
                }
            }.runTaskTimerAsynchronously(this, 20, 20);
        }
    }

    private void startListeners() {
        Bukkit.getPluginManager().registerEvents(new CommandListener(), this);
        Bukkit.getPluginManager().registerEvents(new GUIListener(), this);
        Bukkit.getPluginManager().registerEvents(new InternalListener(), this);
        Bukkit.getPluginManager().registerEvents(new PlayerJoinListener(this), this);
        Bukkit.getPluginManager().registerEvents(new SignListener(), this);
    }

    private void startTasks() {
        if (core.getConfig().useBungee()) {
            PluginMessage pmsg = new PluginMessage();
            Bukkit.getMessenger().registerOutgoingPluginChannel(this, CoinsCore.MESSAGING_CHANNEL);
            Bukkit.getMessenger().registerIncomingPluginChannel(this, CoinsCore.MESSAGING_CHANNEL, pmsg);
            Bukkit.getScheduler().runTaskLaterAsynchronously(this, () -> {
                pmsg.sendToBungeeCord("Multiplier", "getAllMultipliers");
                pmsg.sendToBungeeCord("Coins", "getExecutors");
            }, 20);
        }
        Bukkit.getScheduler().runTaskLaterAsynchronously(this, () -> Bukkit.getOnlinePlayers().forEach(p -> CoinsAPI.createPlayer(p.getName(), p.getUniqueId())), 30);
        core.debug("Starting cache cleanup task");
        Bukkit.getScheduler().runTaskTimerAsynchronously(this, () -> {
            for (Iterator<UUID> it = CacheManager.getPlayersData().keySet().iterator(); it.hasNext(); ) {
                UUID uuid = it.next();
                if (!core.getMethods().isOnline(uuid)) {
                    it.remove();
                    core.debug("Removed '" + uuid + "' from the cache.");
                }
            }
        }, 100, 12000);
    }

    public CoinsConfig getConfiguration() {
        return configuration;
    }
}
