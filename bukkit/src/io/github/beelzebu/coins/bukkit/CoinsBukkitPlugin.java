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

/**
 *
 * @author Beelzebu
 */
public class CoinsBukkitPlugin extends CoinsPlugin {

    public CoinsBukkitPlugin(CoinsBukkitMain bootstrap) {
        super(bootstrap);
    }

    @Override
    public void enable() {
        super.enable();
        // Create the command
        ((CoinsBukkitMain) bootstrap).getCommandManager().registerCommand();
        // Register listeners
        Bukkit.getPluginManager().registerEvents(new CommandListener(), (CoinsBukkitMain) bootstrap);
        Bukkit.getPluginManager().registerEvents(new GUIListener(), (CoinsBukkitMain) bootstrap);
        Bukkit.getPluginManager().registerEvents(new LoginListener(), (CoinsBukkitMain) bootstrap);
        Bukkit.getPluginManager().registerEvents(new SignListener(), (CoinsBukkitMain) bootstrap);
        Bukkit.getScheduler().runTask((CoinsBukkitMain) bootstrap, () -> hookOptionalDependencies());
    }

    @Override
    public void disable() {
        super.disable();
        if (getConfig().getBoolean("Vault.Use", false)) {
            new CoinsEconomy((CoinsBukkitMain) bootstrap).shutdown();
        }
        ((CoinsBukkitMain) bootstrap).getCommandManager().unregisterCommand();
        Bukkit.getScheduler().cancelTasks((CoinsBukkitMain) bootstrap);
    }

    @Override
    public CoinsBootstrap getBootstrap() {
        return bootstrap;
    }

    public FileConfiguration getConfig() {
        return ((CoinsBukkitMain) bootstrap).getConfig();
    }

    private void hookOptionalDependencies() {
        // Hook placeholders
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            bootstrap.log("PlaceholderAPI found, hooking into it.");
            new CoinsPlaceholders().register();
            new MultipliersPlaceholders().register();
        }
        // Hook with LeaderHeads
        if (Bukkit.getPluginManager().getPlugin("LeaderHeads") != null) {
            bootstrap.log("LeaderHeads found, hooking into it.");
            if (Bukkit.getPluginManager().getPlugin("LeaderHeads").isEnabled()) {
                new LeaderHeadsHook();
            }
        }
    }
}
