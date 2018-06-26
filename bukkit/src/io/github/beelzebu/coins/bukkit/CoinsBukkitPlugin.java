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

import io.github.beelzebu.coins.api.cache.CacheProvider;
import io.github.beelzebu.coins.bukkit.listener.CommandListener;
import io.github.beelzebu.coins.bukkit.listener.GUIListener;
import io.github.beelzebu.coins.bukkit.listener.LoginListener;
import io.github.beelzebu.coins.bukkit.listener.SignListener;
import io.github.beelzebu.coins.bukkit.utils.CoinsEconomy;
import io.github.beelzebu.coins.bukkit.utils.leaderheads.LeaderHeadsHook;
import io.github.beelzebu.coins.bukkit.utils.placeholders.CoinsPlaceholders;
import io.github.beelzebu.coins.bukkit.utils.placeholders.MultipliersPlaceholders;
import io.github.beelzebu.coins.common.cache.CacheManager;
import io.github.beelzebu.coins.common.plugin.CommonCoinsPlugin;
import org.bukkit.Bukkit;

/**
 *
 * @author Beelzebu
 */
public class CoinsBukkitPlugin extends CommonCoinsPlugin {

    public CoinsBukkitPlugin(CoinsBukkitMain bootstrap) {
        super(bootstrap);
    }

    @Override
    public void enable() {
        super.enable();
        // Create the command
        ((CoinsBukkitMain) getBootstrap()).getCommandManager().registerCommand();
        // Register listeners
        Bukkit.getPluginManager().registerEvents(new CommandListener(), (CoinsBukkitMain) getBootstrap());
        Bukkit.getPluginManager().registerEvents(new GUIListener(), (CoinsBukkitMain) getBootstrap());
        Bukkit.getPluginManager().registerEvents(new LoginListener(), (CoinsBukkitMain) getBootstrap());
        Bukkit.getPluginManager().registerEvents(new SignListener(), (CoinsBukkitMain) getBootstrap());
        Bukkit.getScheduler().runTask((CoinsBukkitMain) getBootstrap(), () -> hookOptionalDependencies());
    }

    @Override
    public void disable() {
        super.disable();
        if (getConfig().getBoolean("Vault.Use", false)) {
            new CoinsEconomy((CoinsBukkitMain) getBootstrap()).shutdown();
        }
        ((CoinsBukkitMain) getBootstrap()).getCommandManager().unregisterCommand();
        Bukkit.getScheduler().cancelTasks((CoinsBukkitMain) getBootstrap());
    }

    private void hookOptionalDependencies() {
        // Hook placeholders
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            getBootstrap().log("PlaceholderAPI found, hooking into it.");
            new CoinsPlaceholders().register();
            new MultipliersPlaceholders().register();
        }
        // Hook with LeaderHeads
        if (Bukkit.getPluginManager().getPlugin("LeaderHeads") != null) {
            getBootstrap().log("LeaderHeads found, hooking into it.");
            if (Bukkit.getPluginManager().getPlugin("LeaderHeads").isEnabled()) {
                new LeaderHeadsHook();
            }
        }
    }

    @Override
    public CacheProvider getCache() {
        return new CacheManager();
    }
}
