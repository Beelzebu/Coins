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
package io.github.beelzebu.coins.bukkit.command;

import io.github.beelzebu.coins.api.CoinsAPI;
import io.github.beelzebu.coins.api.MultiplierType;
import io.github.beelzebu.coins.api.config.AbstractConfigFile;
import io.github.beelzebu.coins.api.utils.StringUtils;
import io.github.beelzebu.coins.bukkit.menus.CoinsMenu;
import io.github.beelzebu.coins.bukkit.menus.PaginatedMenu;
import io.github.beelzebu.coins.bukkit.utils.CompatUtils;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * @author Beelzebu
 */
public class MultipliersCommand extends AbstractCoinsCommand {

    private final AbstractConfigFile multipliersConfig = plugin.getBootstrap().getFileAsConfig(new File(plugin.getBootstrap().getDataFolder(), "multipliers.yml"));

    MultipliersCommand(String command) {
        super(command);
    }

    /**
     * TODO List:
     * <ul>
     * <li>add a command to get all multipliers for a player</li>
     * <li>add a command to enable any multiplier by the ID</li>
     * <li>add an argument to change multiplier type</li>
     * <li>add a command to edit existing multipliers</li>
     * <li>update messages files to match new menus</li>
     * </ul>
     */
    @Override
    public boolean execute(CommandSender sender, String label, String[] args) {
        String lang = sender instanceof Player ? CompatUtils.getLocale((Player) sender) : "";
        if (!sender.hasPermission(getPermission())) {
            sender.sendMessage(plugin.getString("Errors.No permissions", lang));
            return true;
        }
        if (args.length == 0) {
            if (sender instanceof Player) {
                new CoinsMenu(multipliersConfig.getInt("Menus.Main.Size"), plugin.getString("Menus.Main.Title", lang)) {
                    private void setItems(Player player) {
                        String section = "Menus.Main.Items";
                        multipliersConfig.getConfigurationSection(section).forEach(key -> setItem(multipliersConfig.getInt(section + "." + key + ".Slot"), getItem(multipliersConfig, section + "." + key, player), p -> {
                            List<String> commands = multipliersConfig.getStringList(section + "." + key + ".Commands", new ArrayList<>());
                            p.closeInventory();
                            String menu = multipliersConfig.getString(section + "." + key + ".Menu");
                            if (menu != null && (Objects.equals(menu.toLowerCase(), "local") || Objects.equals(menu.toLowerCase(), "global"))) {
                                PaginatedMenu.createPaginatedGUI(p, menu.toLowerCase().equals("global")).open(p);
                            }
                            commands.forEach(cmd -> {
                                if (cmd.toLowerCase().startsWith("console: ")) {
                                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd.replace(cmd.split(": ")[0], "").substring(2));
                                } else {
                                    Bukkit.dispatchCommand(p, cmd.replace(cmd.split(": ")[0], "").substring(2));
                                }
                            });
                        }));
                    }

                    @Override
                    public void open(Player p) {
                        setItems(p);
                        super.open(p);
                    }
                }.open((Player) sender);
            } else {
                sender.sendMessage(plugin.getString("Errors.Console", lang));
            }
            return true;
        }
        if (sender.hasPermission(getPermission() + ".admin.multiplier")) {
            if (args[0].equalsIgnoreCase("help")) {
                plugin.getMessages(lang).getStringList("Help.Multiplier").forEach(line -> sender.sendMessage(StringUtils.rep(line)));
            }
            if (args[0].equalsIgnoreCase("create")) {
                if (args.length >= 4 && CoinsAPI.isindb(args[1])) {
                    if (!isNumber(args[2]) || !isNumber(args[3])) {
                        sender.sendMessage(plugin.getString("Help.Multiplier Create", lang));
                        return true;
                    }
                    int multiplier = Integer.parseInt(args[2]);
                    int minutes = Integer.parseInt(args[3]);
                    plugin.getStorageProvider().createMultiplier(plugin.getUniqueId(args[1], false), multiplier, minutes, ((args.length == 5 && !args[4].equals("")) ? args[4] : plugin.getConfig().getServerName()), MultiplierType.SERVER);
                    sender.sendMessage(plugin.getString("Multipliers.Created", lang).replaceAll("%player%", args[1]));
                } else {
                    sender.sendMessage(plugin.getString("Help.Multiplier Create", lang));
                }
            }
            if (args[0].equalsIgnoreCase("get")) {
                sender.sendMessage(CoinsAPI.getMultipliers().stream().findFirst().get().getMultiplierTimeFormatted());
            }
        }
        return true;
    }
}
