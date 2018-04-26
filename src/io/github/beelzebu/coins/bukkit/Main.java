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

import io.github.beelzebu.coins.bukkit.command.CommandManager;
import io.github.beelzebu.coins.bukkit.listener.CommandListener;
import io.github.beelzebu.coins.bukkit.listener.GUIListener;
import io.github.beelzebu.coins.bukkit.listener.InternalListener;
import io.github.beelzebu.coins.bukkit.listener.LoginListener;
import io.github.beelzebu.coins.bukkit.listener.SignListener;
import io.github.beelzebu.coins.bukkit.utils.CoinsEconomy;
import io.github.beelzebu.coins.bukkit.utils.leaderheads.LeaderHeadsHook;
import io.github.beelzebu.coins.bukkit.utils.placeholders.CoinsPlaceholders;
import io.github.beelzebu.coins.bukkit.utils.placeholders.MultipliersPlaceholders;
import io.github.beelzebu.coins.common.CoinsCore;
import io.github.beelzebu.coins.common.executor.Executor;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.messaging.PluginMessageListener;

public class Main extends JavaPlugin {

    private final CoinsCore core = CoinsCore.getInstance();
    private static Main instance;
    private CommandManager commandManager;

    public static Main getInstance() {
        return instance;
    }

    @Override
    public void onLoad() {
        instance = this;
        core.setup(new BukkitMethods());
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
        core.start();
        commandManager = new CommandManager();
        loadManagers();
        startListeners();
        getConfig().getConfigurationSection("Command executor").getKeys(false).forEach((id) -> {
            core.getExecutorManager().addExecutor(new Executor(id, getConfig().getString("Command executor." + id + ".Displayname", id), getConfig().getDouble("Command executor." + id + ".Cost", 0), getConfig().getStringList("Command executor." + id + ".Command")));
        });
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
        if (Bukkit.getPluginManager().getPlugin("LeaderHeads") != null) {
            core.getMethods().log("LeaderHeads found, hooking into it.");
            new LeaderHeadsHook();
        }
    }

    private void startListeners() {
        Bukkit.getPluginManager().registerEvents(new CommandListener(), this);
        Bukkit.getPluginManager().registerEvents(new GUIListener(), this);
        Bukkit.getPluginManager().registerEvents(new InternalListener(), this);
        Bukkit.getPluginManager().registerEvents(new LoginListener(), this);
        Bukkit.getPluginManager().registerEvents(new SignListener(), this);
    }

    private void startTasks() {
        if (core.getConfig().useBungee()) {
            Bukkit.getMessenger().registerOutgoingPluginChannel(this, "Coins");
            Bukkit.getMessenger().registerIncomingPluginChannel(this, "Coins", (PluginMessageListener) core.getMethods().getBungeeMessaging());
            Bukkit.getScheduler().runTaskLaterAsynchronously(this, () -> {
                core.getMessagingService().getMultipliers();
                core.getMessagingService().getExecutors();
            }, 20);
        }
    }
}
