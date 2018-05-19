/**
 * bootstrap file is part of Coins
 *
 * Copyright (C) 2018 Beelzebu
 *
 * bootstrap program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU Affero General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option) any
 * later version.
 *
 * bootstrap program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with bootstrap program. If not, see <http://www.gnu.org/licenses/>.
 */
package io.github.beelzebu.coins.bukkit;

import io.github.beelzebu.coins.bukkit.listener.CommandListener;
import io.github.beelzebu.coins.bukkit.listener.GUIListener;
import io.github.beelzebu.coins.bukkit.listener.InternalListener;
import io.github.beelzebu.coins.bukkit.listener.LoginListener;
import io.github.beelzebu.coins.bukkit.listener.SignListener;
import io.github.beelzebu.coins.bukkit.utils.CoinsEconomy;
import io.github.beelzebu.coins.bukkit.utils.leaderheads.LeaderHeadsHook;
import io.github.beelzebu.coins.bukkit.utils.placeholders.CoinsPlaceholders;
import io.github.beelzebu.coins.bukkit.utils.placeholders.MultipliersPlaceholders;
import io.github.beelzebu.coins.common.plugin.CoinsBootstrap;
import io.github.beelzebu.coins.common.plugin.CoinsPlugin;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.scheduler.BukkitRunnable;

/**
 *
 * @author Beelzebu
 */
public class CoinsBukkitPlugin extends CoinsPlugin {

    private final CoinsBukkitMain bootstrap;

    public CoinsBukkitPlugin(CoinsBukkitMain bootstrap) {
        this.bootstrap = bootstrap;
    }

    @Override
    public void enable() {
        super.enable();
        // Create the command
        bootstrap.getCommandManager().registerCommand();
        // Hook placeholders
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            bootstrap.log("PlaceholderAPI found, hooking into it.");
            new CoinsPlaceholders().register();
            new MultipliersPlaceholders().register();
        }
        // Hook with LeaderHeads
        if (Bukkit.getPluginManager().getPlugin("LeaderHeads") != null) {
            bootstrap.log("LeaderHeads found, hooking into it.");
            new BukkitRunnable() {
                private boolean leaderheads = false;

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
            }.runTaskTimerAsynchronously(bootstrap, 20, 20);
        }
        // Register listeners
        Bukkit.getPluginManager().registerEvents(new CommandListener(), bootstrap);
        Bukkit.getPluginManager().registerEvents(new GUIListener(), bootstrap);
        Bukkit.getPluginManager().registerEvents(new InternalListener(), bootstrap);
        Bukkit.getPluginManager().registerEvents(new LoginListener(), bootstrap);
        Bukkit.getPluginManager().registerEvents(new SignListener(), bootstrap);
    }

    @Override
    public void disable() {
        super.disable();
        if (getConfig().getBoolean("Vault.Use", false)) {
            new CoinsEconomy(bootstrap).shutdown();
        }
        bootstrap.getCommandManager().unregisterCommand();
        Bukkit.getScheduler().cancelTasks(bootstrap);
    }

    @Override
    public CoinsBootstrap getBootstrap() {
        return bootstrap;
    }

    public FileConfiguration getConfig() {
        return bootstrap.getConfig();
    }
}
