/*
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

import io.github.beelzebu.coins.bukkit.listener.CommandListener;
import io.github.beelzebu.coins.bukkit.listener.GUIListener;
import io.github.beelzebu.coins.bukkit.listener.LoginListener;
import io.github.beelzebu.coins.bukkit.listener.SignListener;
import io.github.beelzebu.coins.bukkit.menus.CoinsMenu;
import io.github.beelzebu.coins.bukkit.utils.CoinsEconomy;
import io.github.beelzebu.coins.bukkit.utils.CompatUtils;
import io.github.beelzebu.coins.bukkit.utils.leaderheads.LeaderHeadsHook;
import io.github.beelzebu.coins.bukkit.utils.placeholders.CoinsPlaceholders;
import io.github.beelzebu.coins.bukkit.utils.placeholders.MultipliersPlaceholders;
import io.github.beelzebu.coins.common.plugin.CommonCoinsPlugin;
import java.util.stream.Stream;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginEnableEvent;

/**
 * @author Beelzebu
 */
public class CoinsBukkitPlugin extends CommonCoinsPlugin {

    private boolean vault = false, placeholderapi = false, leaderheads = false;

    CoinsBukkitPlugin(CoinsBukkitMain bootstrap) {
        super(bootstrap);
    }

    @Override
    public void load() {
        super.load();
        hookOptionalDependencies();
    }

    @Override
    public void enable() {
        CompatUtils.setup();
        super.enable();
        hookOptionalDependencies();
        // Create the command
        ((CoinsBukkitMain) getBootstrap()).getCommandManager().registerCommand();
        // Register listeners
        Bukkit.getPluginManager().registerEvents(new CommandListener(this), (CoinsBukkitMain) getBootstrap());
        Bukkit.getPluginManager().registerEvents(new GUIListener(), (CoinsBukkitMain) getBootstrap());
        Bukkit.getPluginManager().registerEvents(new LoginListener(this), (CoinsBukkitMain) getBootstrap());
        Bukkit.getPluginManager().registerEvents(new SignListener(this), (CoinsBukkitMain) getBootstrap());
        Bukkit.getPluginManager().registerEvents(new Listener() {
            @EventHandler
            public void onPluginEnable(PluginEnableEvent e) { // keep trying to hook with optional dependencies
                if (Stream.of("Vault", "PlaceholderAPI", "LeaderHeads").anyMatch(hook -> e.getPlugin().getName().equalsIgnoreCase(hook))) {
                    hookOptionalDependencies();
                }
            }
        }, (CoinsBukkitMain) getBootstrap());
    }

    @Override
    public void disable() {
        super.disable();
        if (getConfig().getBoolean("Vault.Use", false) && vault) {
            new CoinsEconomy((CoinsBukkitMain) getBootstrap()).shutdown();
        }
        CoinsMenu.getInventoriesByUUID().values().forEach(CoinsMenu::delete);
        ((CoinsBukkitMain) getBootstrap()).getCommandManager().unregisterCommand();
        Bukkit.getScheduler().cancelTasks((CoinsBukkitMain) getBootstrap());
    }

    private void hookOptionalDependencies() {
        // Hook vault
        if (getConfig().getBoolean("Vault.Use", false)) {
            if (Bukkit.getPluginManager().getPlugin("Vault") != null) {
                if (!vault) {
                    log("Vault found, hooking into it.");
                    new CoinsEconomy((CoinsBukkitMain) getBootstrap()).setup();
                    vault = true;
                }
            } else {
                log("You enabled Vault in the config, but the plugin Vault can't be found.");
            }
        }
        // Hook placeholders
        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI") && !placeholderapi) {
            getBootstrap().log("PlaceholderAPI found, hooking into it.");
            new CoinsPlaceholders().register();
            new MultipliersPlaceholders().register();
            placeholderapi = true;
        }
        // Hook with LeaderHeads
        if (Bukkit.getPluginManager().isPluginEnabled("LeaderHeads") && !leaderheads) {
            getBootstrap().log("LeaderHeads found, hooking into it.");
            new LeaderHeadsHook();
            leaderheads = true;
        }
    }
}
