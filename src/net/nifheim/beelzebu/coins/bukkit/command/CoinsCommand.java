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
package net.nifheim.beelzebu.coins.bukkit.command;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import net.nifheim.beelzebu.coins.CoinsAPI;
import net.nifheim.beelzebu.coins.bukkit.Main;
import net.nifheim.beelzebu.coins.bukkit.utils.CoinsEconomy;
import net.nifheim.beelzebu.coins.bukkit.utils.bungee.PluginMessage;
import net.nifheim.beelzebu.coins.bukkit.utils.gui.MultipliersGUI;
import net.nifheim.beelzebu.coins.core.CacheManager;
import net.nifheim.beelzebu.coins.core.Core;
import net.nifheim.beelzebu.coins.core.database.StorageType;
import net.nifheim.beelzebu.coins.core.executor.Executor;
import net.nifheim.beelzebu.coins.core.importer.ImportManager;
import net.nifheim.beelzebu.coins.core.multiplier.MultiplierType;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 *
 * @author Beelzebu
 */
public class CoinsCommand extends Command {

    private final Core core = Core.getInstance();
    private final Main plugin = Main.getInstance();
    private String lang = "";
    private final String perm;

    public CoinsCommand(String command, String desc, String usage, String permission, List<String> aliases) {
        super(command);
        description = desc;
        usageMessage = usage;
        perm = permission;
        setAliases(aliases);
    }

    @Override
    public boolean execute(CommandSender sender, String alias, String[] args) {
        if (!sender.hasPermission(perm)) {
            sender.sendMessage(core.getString("Errors.No permissions", lang));
            return true;
        }
        core.getMethods().runAsync(() -> {
            if (sender instanceof Player) {
                lang = ((Player) sender).spigot().getLocale().split("_")[0];
            } else {
                lang = "";
            }
            if (args.length == 0) {
                if (sender instanceof Player) {
                    sender.sendMessage(core.getString("Coins.Own coins", lang).replaceAll("%coins%", CoinsAPI.getCoinsString(sender.getName())));
                } else {
                    sender.sendMessage(core.getString("Errors.Console", ""));
                }
            } else if (args[0].equalsIgnoreCase("execute")) {
                execute(sender, args);
            } else if ((args[0].equalsIgnoreCase("help") || args[0].equalsIgnoreCase("ayuda")) && args.length == 1) {
                help(sender, args);
            } else if ((args[0].equalsIgnoreCase("pay") || args[0].equalsIgnoreCase("p") || args[0].equalsIgnoreCase("pagar"))) {
                pay(sender, args);
            } else if ((args[0].equalsIgnoreCase("give") || args[0].equalsIgnoreCase("dar"))) {
                give(sender, args);
            } else if ((args[0].equalsIgnoreCase("take") || args[0].equalsIgnoreCase("quitar"))) {
                take(sender, args);
            } else if ((args[0].equalsIgnoreCase("reset"))) {
                reset(sender, args);
            } else if ((args[0].equalsIgnoreCase("set"))) {
                set(sender, args);
            } else if (args[0].equalsIgnoreCase("multiplier") || args[0].equalsIgnoreCase("multipliers")) {
                multiplier(sender, args);
            } else if (args[0].equalsIgnoreCase("top") && args.length == 1) {
                top(sender, args);
            } else if (args[0].equalsIgnoreCase("import")) {
                imporT(sender, args);
            } else if (args[0].equalsIgnoreCase("importdb")) {
                importDB(sender, args);
            } else if (args[0].equalsIgnoreCase("reload")) {
                reload(sender);
            } else if (args[0].equalsIgnoreCase("about")) {
                about(sender);
            } else if (args.length == 1 && CoinsAPI.isindb(args[0])) {
                target(sender, args);
            } else {
                sender.sendMessage(core.getString("Errors.Unknown command", lang));
            }
        });
        return true;
    }

    public boolean help(CommandSender sender, String[] args) {
        core.getMessages(lang).getStringList("Help.User").forEach((str) -> {
            sender.sendMessage(core.rep(str));
        });
        if (sender.hasPermission("coins.admin")) {
            core.getMessages(lang).getStringList("Help.Admin").forEach((str) -> {
                sender.sendMessage(core.rep(str));
            });
        }
        return true;
    }

    public boolean target(CommandSender sender, String[] args) {
        sender.sendMessage(core.getString("Coins.Get", lang).replaceAll("%coins%", CoinsAPI.getCoinsString(args[0])).replaceAll("%target%", args[0]));
        return true;
    }

    public boolean pay(CommandSender sender, String[] args) {
        if (!sender.hasPermission("coins.user.pay")) {
            sender.sendMessage(core.getString("Errors.No permissions", lang));
            return true;
        }
        if (args.length < 3 || args[1].equalsIgnoreCase("?")) {
            sender.sendMessage(core.getString("Help.Pay Usage", lang));
            return true;
        }
        if (sender instanceof Player && args.length == 3 && !args[1].equalsIgnoreCase(sender.getName())) {
            Player target = Bukkit.getPlayer(args[1]);
            if (CoinsAPI.isindb(args[1])) {
                double coins = Double.parseDouble(args[2]);
                if (coins > 0) {
                    if (CoinsAPI.getCoins(((Player) sender).getUniqueId()) >= coins) {
                        if (target != null) {
                            CoinsAPI.takeCoins(sender.getName(), coins);
                            if (!core.getString("Coins.Pay", lang).equals("")) {
                                sender.sendMessage(core.getString("Coins.Pay", lang).replaceAll("%coins%", String.valueOf(coins)).replaceAll("%target%", target.getName()));
                            }
                            CoinsAPI.addCoins(args[1], coins, false);
                            if (!core.getString("Coins.Pay target", target.spigot().getLocale()).equals("")) {
                                target.sendMessage(core.getString("Coins.Pay target", target.spigot().getLocale()).replaceAll("%coins%", String.valueOf(coins)).replaceAll("%from%", sender.getName()));
                            }
                        } else {
                            sender.sendMessage(core.getString("Errors.Unknown player", lang));
                        }
                    } else {
                        sender.sendMessage(core.getString("Errors.No Coins", lang));
                    }
                } else {
                    sender.sendMessage(core.getString("Errors.No Zero", lang));
                }
            }
            return true;
        }
        if (!(sender instanceof Player)) {
            sender.sendMessage(core.getString("Errors.Console", lang));
        }
        return true;
    }

    public boolean give(CommandSender sender, String[] args) {
        if (!sender.hasPermission("coins.admin") || !sender.hasPermission("coins.admin.give")) {
            sender.sendMessage(core.getString("Errors.No permissions", lang));
            return true;
        }
        if (args.length < 3 || args.length > 4) {
            sender.sendMessage(core.getString("Help.Give Usage", lang));
        }
        if (!CoinsAPI.isindb(args[1])) {
            sender.sendMessage(core.getString("Errors.Unknown player", lang).replaceAll("%target%", args[1]));
            return true;
        }
        String multiplier = "";
        boolean multiply = false;
        if (args.length == 3 || args.length == 4) {
            double coins = Double.parseDouble(args[2]);
            if (core.isOnline(core.getUUID(args[1], false)) && args.length == 4 && args[3].equalsIgnoreCase("true")) {
                multiply = true;
                Player target = Bukkit.getPlayer(args[1]);
                int amount = CoinsAPI.getMultiplier() != null ? CoinsAPI.getMultiplier().getBaseData().getAmount() : 1;
                if (amount > 1) {
                    multiplier = core.getString("Multipliers.Format", target.spigot().getLocale()).replaceAll("%multiplier%", String.valueOf(amount)).replaceAll("%enabler%", CoinsAPI.getMultiplier().getEnablerName());
                }
            }
            if (core.isOnline(core.getUUID(args[1], false))) {
                Player target = Bukkit.getPlayer(core.getUUID(args[1], false));
                if (!core.getString("Coins.Give target", target.spigot().getLocale()).equals("")) {
                    target.sendMessage(core.getString("Coins.Give target", target.spigot().getLocale()).replaceAll("%coins%", String.valueOf(coins)).replaceAll("%multiplier_format%", multiplier));
                }
            }
            CoinsAPI.addCoins(args[1], coins, multiply);
            if (!core.getString("Coins.Give", lang).equals("")) {
                sender.sendMessage(core.getString("Coins.Give", lang).replaceAll("%coins%", String.valueOf(coins)).replaceAll("%target%", args[1]));
            }
        } else {
            sender.sendMessage(core.getString("Errors.Unknown command", lang));
        }
        return true;
    }

    public boolean take(CommandSender sender, String[] args) {
        if (!sender.hasPermission("coins.admin") || !sender.hasPermission("coins.admin.take")) {
            sender.sendMessage(core.getString("Errors.No permissions", lang));
            return true;
        }
        if (args.length < 3) {
            sender.sendMessage(core.getString("Help.Take Usage", lang));
            return true;
        }
        if (args.length == 3) {
            Player target = Bukkit.getPlayer(args[1]);
            if (args[0].equalsIgnoreCase("take")) {
                double coins = Double.parseDouble(args[2]);
                double finalcoins = CoinsAPI.getCoins(args[1]) - coins;
                if (target == null) {
                    if (CoinsAPI.getCoins(args[1]) < coins) {
                        sender.sendMessage(core.getString("Errors.No Negative", lang));
                    } else if (CoinsAPI.getCoins(args[1]) >= coins) {
                        if (CoinsAPI.isindb(args[1])) {
                            CoinsAPI.takeCoins(args[1], coins);
                            if (!core.getString("Coins.Take", lang).equals("")) {
                                sender.sendMessage(core.getString("Coins.Take", lang).replaceAll("%coins%", String.valueOf(coins)).replaceAll("%newcoins%", String.valueOf(finalcoins)).replaceAll("%target%", args[1]));
                            }
                        } else {
                            sender.sendMessage(core.getString("Errors.Unknown player", lang).replaceAll("%target%", args[1]));
                        }
                    }
                } else {
                    if (CoinsAPI.getCoins(target.getUniqueId()) < coins) {
                        sender.sendMessage(core.getString("Errors.No Negative", lang));
                    }
                    if (CoinsAPI.getCoins(target.getUniqueId()) >= coins) {
                        CoinsAPI.takeCoins(args[1], coins);
                        if (!core.getString("Coins.Take", lang).equals("")) {
                            sender.sendMessage(core.getString("Coins.Take", lang).replaceAll("%target%", target.getName()).replaceAll("%coins%", String.valueOf(coins)).replaceAll("%newcoins%", String.valueOf(finalcoins)));
                        }
                        if (!core.getString("Coins.Take target", target.spigot().getLocale()).equals("")) {
                            target.sendMessage(core.getString("Coins.Take target", target.spigot().getLocale()).replaceAll("%coins%", String.valueOf(finalcoins)));
                        }
                    }
                }
            }
        }
        return true;
    }

    public boolean reset(CommandSender sender, String[] args) {
        if (!sender.hasPermission("coins.admin") || !sender.hasPermission("coins.admin.reset")) {
            sender.sendMessage(core.getString("Errors.No permissions", lang));
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage(core.getString("Help.Reset Usage", lang));
            return true;
        }
        if (CoinsAPI.isindb(args[1])) {
            CoinsAPI.resetCoins(args[1]);
        } else {
            sender.sendMessage(core.getString("Errors.Unknown player", lang).replaceAll("%target%", args[1]));
            return true;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (!core.getString("Coins.Reset", lang).equals("")) {
            if (!core.getString("Coins.Reset", lang).equals("")) {
                sender.sendMessage(core.getString("Coins.Reset", lang).replaceAll("%target%", args[1]));
            }
        }
        if (target != null && !core.getString("Coins.Reset target", target.spigot().getLocale()).equals("")) {
            target.sendMessage(core.getString("Coins.Reset target", target.spigot().getLocale()));
        }
        return true;
    }

    public boolean set(CommandSender sender, String[] args) {
        if (!sender.hasPermission("coins.admin")) {
            sender.sendMessage(core.getString("Errors.No permissions", lang));
            return true;
        }
        if (args.length < 3) {
            sender.sendMessage(core.getString("Help.Set Usage", lang));
            return true;
        }
        if (args.length == 3) {
            if (args[0].equalsIgnoreCase("set")) {
                double coins = Double.parseDouble(args[2]);
                if (CoinsAPI.isindb(args[1])) {
                    CoinsAPI.setCoins(args[1], coins);
                    if (!core.getString("Coins.Set", lang).equals("")) {
                        sender.sendMessage(core.getString("Coins.Set", lang).replaceAll("%target%", args[1]).replaceAll("%coins%", String.valueOf(coins)));
                    }
                } else {
                    sender.sendMessage(core.getString("Errors.Unknown player", lang).replaceAll("%target%", args[1]));
                    return true;
                }
                Player target = Bukkit.getPlayer(args[1]);
                if (target != null && !core.getString("Coins.Set target", target.spigot().getLocale()).equals("")) {
                    target.sendMessage(core.getString("Coins.Set target", target.spigot().getLocale()).replaceAll("%coins%", args[2]));
                }
            }
        }
        return true;
    }

    public boolean top(CommandSender sender, String[] args) {
        sender.sendMessage(core.getString("Coins.Top.Header", lang));
        Map<String, Double> topMap = CoinsAPI.getTopPlayers(10);
        int i = 0;
        for (String player : topMap.keySet()) {
            i++;
            sender.sendMessage(core.getString("Coins.Top.List", lang).replaceAll("%top%", String.valueOf(i)).replaceAll("%player%", player).replaceAll("%coins%", String.valueOf((int) Math.round(topMap.get(player)))));
        }
        return true;
    }

    private boolean multiplier(CommandSender sender, String[] args) {
        // TODO: add a command to get all multipliers for a player and a command to enable any multiplier by the ID.
        if ((sender.hasPermission("coins.admin") || sender.hasPermission("coins.admin.multiplier")) && args.length >= 2) {
            if (args[1].equalsIgnoreCase("help")) {
                core.getMessages(lang).getStringList("Help.Multiplier").forEach(line -> {
                    sender.sendMessage(core.rep(line));
                });
            }
            if (args[1].equalsIgnoreCase("create")) {
                if (args.length >= 5 && CoinsAPI.isindb(args[2])) {
                    try {
                        int multiplier = Integer.parseInt(args[3]);
                        int minutes = Integer.parseInt(args[4]);
                        core.getDatabase().createMultiplier(core.getUUID(args[2], false), multiplier, minutes, ((args.length == 6 && !args[5].equals("")) ? args[5] : null), MultiplierType.SERVER);
                        sender.sendMessage(core.getString("Multipliers.Created", lang).replaceAll("%player%", args[2]));
                    } catch (NumberFormatException e) {
                        sender.sendMessage(core.getString("Help.Multiplier Create", lang));
                    }
                } else {
                    sender.sendMessage(core.getString("Help.Multiplier Create", lang));
                }
            }
            if (args[1].equalsIgnoreCase("get")) {
                sender.sendMessage(CoinsAPI.getMultiplier().getMultiplierTimeFormated());
            }
            return true;
        }
        if (args.length == 1) {
            if (sender instanceof Player) {
                new MultipliersGUI((Player) sender, core.getString("Multipliers.Menu.Title", lang)).open((Player) sender);
            } else {
                sender.sendMessage(core.getString("Errors.Console", lang));
            }
        } else {
            sender.sendMessage(core.getString("Help.Multiplier Usage", lang));
        }
        return true;
    }

    private boolean execute(CommandSender sender, String[] args) {
        if (sender instanceof Player) {
            Executor ex = core.getExecutorManager().getExecutor(args[1]);
            if (ex == null) {
                sender.sendMessage(core.getString("Errors.No Execute", lang));
            } else {
                if (ex.getCost() > 0) {
                    if (CoinsAPI.getCoins(((Player) sender).getUniqueId()) >= ex.getCost()) {
                        CoinsAPI.takeCoins(((Player) sender).getUniqueId(), ex.getCost());
                    } else {
                        sender.sendMessage(core.getString("Errors.No Coins", lang));
                        return true;
                    }
                }
                if (!ex.getCommands().isEmpty()) {
                    core.getMethods().runSync(() -> {
                        String command;
                        for (String str : ex.getCommands()) {
                            command = core.rep(str).replaceAll("%player%", sender.getName());
                            if (command.startsWith("message:")) {
                                sender.sendMessage(core.rep(command.replaceFirst("message:", "")));
                            } else if (command.startsWith("broadcast:")) {
                                Bukkit.getServer().broadcastMessage(core.rep(command.replaceFirst("broadcast:", "")));
                            } else {
                                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
                            }
                        }
                    });
                }
            }
        } else {
            sender.sendMessage(core.getString("Errors.Console", lang));
        }
        return true;
    }

    private boolean imporT(CommandSender sender, String[] args) {
        if (sender instanceof Player) {
            sender.sendMessage(core.rep("%prefix% &cThis command must be executed from the console."));
            return true;
        }
        if (args.length == 2) {
            boolean worked = false;
            ImportManager importManager = new ImportManager(core);
            for (ImportManager.PluginToImport pluginToImport : ImportManager.PluginToImport.values()) {
                if (pluginToImport.toString().equals(args[1].toUpperCase())) {
                    worked = true;
                    importManager.importFrom(pluginToImport);
                    break;
                }
            }
            if (!worked) {
                sender.sendMessage(core.rep("%prefix% You specified an invalid plugin to import, possible values:"));
                sender.sendMessage(Arrays.toString(ImportManager.PluginToImport.values()));
            }
            return true;
        } else {
            sender.sendMessage(core.rep("%prefix% Command usage: /coins import <plugin>"));
            sender.sendMessage(core.rep("&cCurrently supported plugins to import: " + Arrays.toString(ImportManager.PluginToImport.values())));
        }
        return true;
    }

    private boolean importDB(CommandSender sender, String[] args) {
        if (sender instanceof Player) {
            sender.sendMessage(core.rep("%prefix% &cThis command must be executed from the console."));
            return true;
        }
        if (args.length == 2) {
            boolean worked = false;
            ImportManager importManager = new ImportManager(core);
            for (StorageType storage : StorageType.values()) {
                if (storage.toString().equals(args[1].toUpperCase())) {
                    worked = true;
                    importManager.importFromStorage(storage);
                    break;
                }
            }
            if (!worked) {
                sender.sendMessage(core.rep("%prefix% You specified an invalid storage to import, possible values:"));
                sender.sendMessage(Arrays.toString(StorageType.values()));
            }
            return true;
        } else {
            sender.sendMessage(core.rep("%prefix% Command usage: /coins importdb <storage>"));
            sender.sendMessage(core.rep("&cCurrently supported storages to import: " + Arrays.toString(StorageType.values())));
        }
        return true;
    }

    private boolean reload(CommandSender sender) {
        if (sender.hasPermission("coins.admin.reload")) {
            if (plugin.getConfig().getBoolean("Vault.Use", false)) {
                new CoinsEconomy(plugin).shutdown();
            }
            core.getConfig().reload();
            core.reloadMessages();
            if (plugin.getConfig().getBoolean("Vault.Use", false)) {
                new CoinsEconomy(plugin).setup();
            }
            core.getExecutorManager().getExecutors().clear();
            plugin.getConfig().getConfigurationSection("Command executor").getKeys(false).forEach((id) -> {
                core.getExecutorManager().addExecutor(new Executor(id, plugin.getConfig().getString("Command executor." + id + ".Displayname", id), plugin.getConfig().getDouble("Command executor." + id + ".Cost", 0), plugin.getConfig().getStringList("Command executor." + id + ".Command")));
            });
            if (core.getConfig().useBungee()) {
                PluginMessage pm = new PluginMessage();
                pm.sendToBungeeCord("Multiplier", "getAllMultipliers");
                pm.sendToBungeeCord("Coins", "getExecutors");
            }
            sender.sendMessage(core.rep("%prefix% Reloaded config and all loaded messages files. If you want reload the command, you need to restart the server."));
        }
        return true;
    }

    private boolean about(CommandSender sender) {
        if (sender.hasPermission("coins.admin") || sender.getName().equals("Beelzebu")) {
            sender.sendMessage(core.rep("%prefix% Plugin info:"));
            sender.sendMessage("");
            sender.sendMessage(core.rep(" &cVersion:&7 " + core.getMethods().getVersion()));
            sender.sendMessage(core.rep(" &cExecutors:&7 " + core.getExecutorManager().getExecutors().size()));
            sender.sendMessage(core.rep(" &cStorage Type:&7 " + core.getStorageType()));
            sender.sendMessage(core.rep(" &cMultipliers in cache:&7 " + CacheManager.getMultipliersData().asMap().keySet()));
            sender.sendMessage(core.rep(" &cPlayers in cache:&7 " + CacheManager.getPlayersData().size()));
            sender.sendMessage("");
        }
        return true;
    }
}
