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
package io.github.beelzebu.coins.bukkit.listener;

import io.github.beelzebu.coins.api.CoinsAPI;
import io.github.beelzebu.coins.api.executor.Executor;
import io.github.beelzebu.coins.api.executor.ExecutorManager;
import io.github.beelzebu.coins.api.plugin.CoinsPlugin;
import io.github.beelzebu.coins.api.utils.StringUtils;
import io.github.beelzebu.coins.bukkit.utils.LocationUtils;
import java.io.File;
import java.io.IOException;
import org.bukkit.Bukkit;
import org.bukkit.block.Sign;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;

/**
 * @author Beelzebu
 */
public class SignListener implements Listener {

    private final CoinsPlugin plugin = CoinsAPI.getPlugin();
    private final File signsFile;
    private final FileConfiguration signs;

    public SignListener() {
        signsFile = new File(plugin.getBootstrap().getDataFolder(), "signs.yml");
        if (!signsFile.exists()) {
            try {
                signsFile.createNewFile();
            } catch (IOException ex) {
                plugin.log("An error has ocurred while creating the signs.yml file.");
                plugin.log(ex.getMessage());
            }
        }
        signs = YamlConfiguration.loadConfiguration(signsFile);
    }

    @EventHandler
    public void onSignPlace(SignChangeEvent e) {
        if (e.getPlayer().hasPermission("coins.admin") && e.getLine(0).equalsIgnoreCase("[coins]") && !e.getLine(1).isEmpty()) {
            e.setLine(0, StringUtils.rep(plugin.getConfig().getString("General.Executor Sign.1")));
            Executor ex = ExecutorManager.getExecutor(e.getLine(1));
            if (ex != null) {
                int id = 0;
                id = signs.getKeys(false).stream().map(i -> 1).reduce(id, Integer::sum);
                signs.set(id + ".Location", LocationUtils.locationToString(e.getBlock().getLocation()));
                signs.set(id + ".Executor", e.getLine(1));
                try {
                    signs.save(signsFile);
                    if (plugin.getConfig().getConfigurationSection("Command executor." + ex.getId() + ".Executor Sign") != null) {
                        e.setLine(1, rep(plugin.getConfig().getString("Command executor." + ex.getId() + ".Executor Sign.2"), ex));
                        e.setLine(2, rep(plugin.getConfig().getString("Command executor." + ex.getId() + ".Executor Sign.3"), ex));
                        e.setLine(3, rep(plugin.getConfig().getString("Command executor." + ex.getId() + ".Executor Sign.4"), ex));
                    } else {
                        e.setLine(1, rep(plugin.getConfig().getString("General.Executor Sign.2"), ex));
                        e.setLine(2, rep(plugin.getConfig().getString("General.Executor Sign.3"), ex));
                        e.setLine(3, rep(plugin.getConfig().getString("General.Executor Sign.4"), ex));
                    }
                } catch (IOException ex1) {
                    plugin.log("An error has ocurred while saving the signs.yml file");
                    plugin.log(ex1.getMessage());
                    e.getPlayer().sendMessage(StringUtils.rep("%prefix% An error has ocurred while saving the signs.yml file, please check the console"));
                }
            } else {
                e.setLine(1, "Unknown Executor");
                e.setLine(2, "");
                e.setLine(3, "");
            }
        }
    }

    @EventHandler
    public void onSignBreak(BlockBreakEvent e) {
        if (e.getBlock().getState() instanceof Sign) {
            Sign sign = (Sign) e.getBlock().getState();
            if ((sign.getLine(0).equals(StringUtils.rep(plugin.getConfig().getString("General.Executor Sign.1"))) || sign.getLine(0).equals(StringUtils.rep(plugin.getConfig().getString("General.Executor Sign.1")))) && !e.getPlayer().hasPermission("coins.admin")) {
                e.setCancelled(true);
            } else {
                for (String id : signs.getKeys(false)) {
                    if (signs.getString(id + ".Location").equals(LocationUtils.locationToString(e.getBlock().getLocation()))) {
                        signs.set(id, null);
                        try {
                            signs.save(signsFile);
                            e.getPlayer().sendMessage(StringUtils.rep("%prefix% &7You removed a executor sign."));
                        } catch (IOException ex) {
                            e.setCancelled(true);
                            plugin.log("An error has ocurred while saving the signs.yml file");
                            plugin.log(ex.getMessage());
                            e.getPlayer().sendMessage(StringUtils.rep("%prefix% An error has ocurred while saving the signs.yml file, please check the console"));
                        }
                        break;
                    }
                }
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onSignUse(PlayerInteractEvent e) {
        if (e.getAction().equals(Action.RIGHT_CLICK_BLOCK) && e.getClickedBlock().getState() instanceof Sign) {
            Player p = e.getPlayer();
            for (String id : signs.getKeys(false)) {
                if (signs.getString(id + ".Location").equals(LocationUtils.locationToString(e.getClickedBlock().getLocation()))) {
                    Executor ex = ExecutorManager.getExecutor(signs.getString(id + ".Executor"));
                    if (ex == null) {
                        p.sendMessage(plugin.getString("Errors.No Execute", p.spigot().getLocale()));
                    } else {
                        if (ex.getCost() > 0) {
                            if (CoinsAPI.getCoins(p.getUniqueId()) >= ex.getCost()) {
                                CoinsAPI.takeCoins(p.getName(), ex.getCost());
                            } else {
                                p.sendMessage(plugin.getString("Errors.No Coins", p.spigot().getLocale()));
                                return;
                            }
                        }
                        if (!ex.getCommands().isEmpty()) {
                            plugin.getBootstrap().runSync(() -> {
                                String command;
                                for (String str : ex.getCommands()) {
                                    command = StringUtils.rep(str).replaceAll("%player%", p.getName());
                                    if (command.startsWith("message:")) {
                                        p.sendMessage(StringUtils.rep(command.replaceFirst("message:", "")));
                                    } else if (command.startsWith("broadcast:")) {
                                        Bukkit.getServer().broadcastMessage(StringUtils.rep(command.replaceFirst("broadcast:", "")));
                                    } else {
                                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
                                    }
                                }
                            });
                        }
                    }
                    break;
                }
            }
        }
    }

    private String rep(String str, Executor ex) {
        return StringUtils.rep(str).replaceAll("%executor_displayname%", ex.getDisplayname()).replaceAll("%executor_cost%", String.valueOf(ex.getCost()));
    }
}
