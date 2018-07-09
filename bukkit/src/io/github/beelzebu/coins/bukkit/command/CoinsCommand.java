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
package io.github.beelzebu.coins.bukkit.command;

import com.google.common.collect.Lists;
import io.github.beelzebu.coins.api.CoinsAPI;
import io.github.beelzebu.coins.api.MultiplierType;
import io.github.beelzebu.coins.api.executor.Executor;
import io.github.beelzebu.coins.api.executor.ExecutorManager;
import io.github.beelzebu.coins.api.plugin.CoinsPlugin;
import io.github.beelzebu.coins.api.storage.StorageType;
import io.github.beelzebu.coins.api.utils.StringUtils;
import io.github.beelzebu.coins.bukkit.CoinsBukkitMain;
import io.github.beelzebu.coins.bukkit.menus.PaginatedMenu;
import io.github.beelzebu.coins.bukkit.utils.CoinsEconomy;
import io.github.beelzebu.coins.common.importer.ImportManager;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * @author Beelzebu
 */
public class CoinsCommand extends Command {

    private final CoinsPlugin plugin;
    private final DecimalFormat df = new DecimalFormat("#.#");

    CoinsCommand(String command, CoinsPlugin plugin) {
        super(command);
        this.plugin = plugin;
    }

    @Override
    public boolean execute(CommandSender sender, String alias, String[] args) {
        String lang = sender instanceof Player ? ((Player) sender).spigot().getLocale().split("_")[0] : "";
        if (!sender.hasPermission(getPermission())) {
            sender.sendMessage(plugin.getString("Errors.No permissions", lang));
            return true;
        }
        plugin.getBootstrap().runAsync(() -> {
            if (args.length == 0) {
                if (sender instanceof Player) {
                    sender.sendMessage(plugin.getString("Coins.Own coins", lang).replaceAll("%coins%", CoinsAPI.getCoinsString(sender.getName())));
                } else {
                    sender.sendMessage(plugin.getString("Errors.Console", ""));
                }
            } else if (args[0].equalsIgnoreCase("execute")) {
                execute(sender, args, lang);
            } else if ((args[0].equalsIgnoreCase("help") || args[0].equalsIgnoreCase("ayuda")) && args.length == 1) {
                help(sender, args, lang);
            } else if ((args[0].equalsIgnoreCase("pay") || args[0].equalsIgnoreCase("p") || args[0].equalsIgnoreCase("pagar"))) {
                pay(sender, args, lang);
            } else if ((args[0].equalsIgnoreCase("give") || args[0].equalsIgnoreCase("dar"))) {
                give(sender, args, lang);
            } else if ((args[0].equalsIgnoreCase("take") || args[0].equalsIgnoreCase("quitar"))) {
                take(sender, args, lang);
            } else if ((args[0].equalsIgnoreCase("reset"))) {
                reset(sender, args, lang);
            } else if ((args[0].equalsIgnoreCase("set"))) {
                set(sender, args, lang);
            } else if (args[0].equalsIgnoreCase("multiplier") || args[0].equalsIgnoreCase("multipliers")) {
                multiplier(sender, args, lang);
            } else if (args[0].equalsIgnoreCase("top") && args.length == 1) {
                top(sender, args, lang);
            } else if (args[0].equalsIgnoreCase("import")) {
                importer(sender, args, lang);
            } else if (args[0].equalsIgnoreCase("importdb")) {
                importDB(sender, args, lang);
            } else if (args[0].equalsIgnoreCase("reload")) {
                reload(sender);
            } else if (args[0].equalsIgnoreCase("about")) {
                about(sender);
            } else if (args.length == 1 && CoinsAPI.isindb(args[0])) {
                target(sender, args, lang);
            } else {
                sender.sendMessage(plugin.getString("Errors.Unknown command", lang));
            }
        });
        return true;
    }

    private void help(CommandSender sender, String[] args, String lang) {
        plugin.getMessages(lang).getStringList("Help.User").forEach(line -> sender.sendMessage(StringUtils.rep(line)));
        if (sender.hasPermission(getPermission() + ".admin.help")) {
            plugin.getMessages(lang).getStringList("Help.Admin").forEach(line -> sender.sendMessage(StringUtils.rep(line)));
        }
    }

    private void target(CommandSender sender, String[] args, String lang) {
        if (!sender.hasPermission(getPermission() + ".target")) {
            sender.sendMessage(plugin.getString("Errors.No permissions", lang));
            return;
        }
        sender.sendMessage(plugin.getString("Coins.Get", lang).replaceAll("%coins%", CoinsAPI.getCoinsString(args[0])).replaceAll("%target%", args[0]));
    }

    private void pay(CommandSender sender, String[] args, String lang) {
        if (!sender.hasPermission(getPermission() + ".pay")) {
            sender.sendMessage(plugin.getString("Errors.No permissions", lang));
            return;
        }
        if (args.length < 3 || args[1].equalsIgnoreCase("?") || args.length == 3 && !isNumber(args[2])) {
            sender.sendMessage(plugin.getString("Help.Pay Usage", lang));
            return;
        }
        if (sender instanceof Player && args.length == 3 && !args[1].equalsIgnoreCase(sender.getName())) {
            Player target = Bukkit.getPlayer(args[1]);
            if (CoinsAPI.isindb(args[1])) {
                double coins = Double.parseDouble(args[2]);
                if (coins > 0) {
                    if (CoinsAPI.getCoins(((Player) sender).getUniqueId()) >= coins) {
                        if (target != null) {
                            CoinsAPI.takeCoins(sender.getName(), coins);
                            if (!plugin.getString("Coins.Pay", lang).equals("")) {
                                sender.sendMessage(plugin.getString("Coins.Pay", lang).replaceAll("%coins%", new DecimalFormat("#.#").format(coins)).replaceAll("%target%", target.getName()));
                            }
                            CoinsAPI.addCoins(args[1], coins, false);
                            if (!plugin.getString("Coins.Pay target", target.spigot().getLocale()).equals("")) {
                                target.sendMessage(plugin.getString("Coins.Pay target", target.spigot().getLocale()).replaceAll("%coins%", df.format(coins)).replaceAll("%from%", sender.getName()));
                            }
                        } else {
                            sender.sendMessage(plugin.getString("Errors.Unknown player", lang));
                        }
                    } else {
                        sender.sendMessage(plugin.getString("Errors.No Coins", lang));
                    }
                } else {
                    sender.sendMessage(plugin.getString("Errors.No Zero", lang));
                }
            }
            return;
        }
        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.getString("Errors.Console", lang));
        }
    }

    private void give(CommandSender sender, String[] args, String lang) {
        if (!sender.hasPermission(getPermission() + ".admin.give")) {
            sender.sendMessage(plugin.getString("Errors.No permissions", lang));
            return;
        }
        if (args.length < 3 || args.length >= 4 || (args.length >= 3 && !isNumber(args[2]))) {
            sender.sendMessage(plugin.getString("Help.Give Usage", lang));
            return;
        }
        if (!CoinsAPI.isindb(args[1])) {
            sender.sendMessage(plugin.getString("Errors.Unknown player", lang).replaceAll("%target%", args[1]));
            return;
        }
        String multiplier = "";
        boolean multiply = false;
        if (args.length == 3 || args.length == 4) {
            double coins = Double.parseDouble(args[2]);
            if (plugin.getBootstrap().isOnline(plugin.getUniqueId(args[1], false)) && args.length == 4 && args[3].equalsIgnoreCase("true")) {
                multiply = true;
                Player target = Bukkit.getPlayer(args[1]);
                int amount = !CoinsAPI.getMultipliers().isEmpty() ? CoinsAPI.getMultipliers().stream().mapToInt(m -> m.getData().getAmount()).sum() : 1;
                if (amount > 1) {
                    multiplier = plugin.getString("Multipliers.Format", target.spigot().getLocale()).replaceAll("%multiplier%", df.format(amount)).replaceAll("%enabler%", CoinsAPI.getMultipliers().stream().findFirst().get().getData().getEnablerName());
                }
            }
            if (plugin.getBootstrap().isOnline(plugin.getUniqueId(args[1], false))) {
                Player target = Bukkit.getPlayer(plugin.getUniqueId(args[1], false));
                if (!plugin.getString("Coins.Give target", target.spigot().getLocale()).equals("")) {
                    target.sendMessage(plugin.getString("Coins.Give target", target.spigot().getLocale()).replaceAll("%coins%", df.format(coins)).replaceAll("%multiplier_format%", multiplier));
                }
            }
            CoinsAPI.addCoins(args[1], coins, multiply);
            if (!plugin.getString("Coins.Give", lang).equals("")) {
                sender.sendMessage(plugin.getString("Coins.Give", lang).replaceAll("%coins%", df.format(coins)).replaceAll("%target%", args[1]));
            }
        } else {
            sender.sendMessage(plugin.getString("Errors.Unknown command", lang));
        }
    }

    private void take(CommandSender sender, String[] args, String lang) {
        if (!sender.hasPermission(getPermission() + ".admin.take")) {
            sender.sendMessage(plugin.getString("Errors.No permissions", lang));
            return;
        }
        if (args.length < 3 || args.length == 3 && !isNumber(args[2])) {
            sender.sendMessage(plugin.getString("Help.Take Usage", lang));
            return;
        }
        if (!CoinsAPI.isindb(args[1])) {
            sender.sendMessage(plugin.getString("Errors.Unknown player", lang).replaceAll("%target%", args[1]));
            return;
        }
        double currentCoins = CoinsAPI.getCoins(args[1]);
        double coins = Double.parseDouble(args[2]);
        if (currentCoins < coins) {
            sender.sendMessage(plugin.getString("Errors.No Negative", lang));
            return;
        }
        double finalCoins = currentCoins - coins;
        if (args.length == 3) {
            Player target = Bukkit.getPlayer(args[1]);
            if (!plugin.getString("Coins.Take", lang).equals("")) {
                sender.sendMessage(plugin.getString("Coins.Take", lang).replaceAll("%coins%", df.format(coins)).replaceAll("%newcoins%", df.format(finalCoins)).replaceAll("%target%", args[1]));
            }
            if (target != null) {
                if (!plugin.getString("Coins.Take target", target.spigot().getLocale()).equals("")) {
                    target.sendMessage(plugin.getString("Coins.Take target", target.spigot().getLocale()).replaceAll("%coins%", df.format(finalCoins)));
                }
            }
        }
    }

    private void reset(CommandSender sender, String[] args, String lang) {
        if (!sender.hasPermission(getPermission() + ".admin.reset")) {
            sender.sendMessage(plugin.getString("Errors.No permissions", lang));
            return;
        }
        if (args.length < 2) {
            sender.sendMessage(plugin.getString("Help.Reset Usage", lang));
            return;
        }
        if (CoinsAPI.isindb(args[1])) {
            CoinsAPI.resetCoins(args[1]);
        } else {
            sender.sendMessage(plugin.getString("Errors.Unknown player", lang).replaceAll("%target%", args[1]));
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (!plugin.getString("Coins.Reset", lang).equals("")) {
            if (!plugin.getString("Coins.Reset", lang).equals("")) {
                sender.sendMessage(plugin.getString("Coins.Reset", lang).replaceAll("%target%", args[1]));
            }
        }
        if (target != null && !plugin.getString("Coins.Reset target", target.spigot().getLocale()).equals("")) {
            target.sendMessage(plugin.getString("Coins.Reset target", target.spigot().getLocale()));
        }
    }

    private void set(CommandSender sender, String[] args, String lang) {
        if (!sender.hasPermission(getPermission() + ".admin.set")) {
            sender.sendMessage(plugin.getString("Errors.No permissions", lang));
            return;
        }
        if (args.length < 3 || args.length == 3 && !isNumber(args[2])) {
            sender.sendMessage(plugin.getString("Help.Set Usage", lang));
            return;
        }
        if (args.length == 3) {
            if (args[0].equalsIgnoreCase("set")) {
                double coins = Double.parseDouble(args[2]);
                if (CoinsAPI.isindb(args[1])) {
                    CoinsAPI.setCoins(args[1], coins);
                    if (!plugin.getString("Coins.Set", lang).equals("")) {
                        sender.sendMessage(plugin.getString("Coins.Set", lang).replaceAll("%target%", args[1]).replaceAll("%coins%", df.format(coins)));
                    }
                } else {
                    sender.sendMessage(plugin.getString("Errors.Unknown player", lang).replaceAll("%target%", args[1]));
                    return;
                }
                Player target = Bukkit.getPlayer(args[1]);
                if (target != null && !plugin.getString("Coins.Set target", target.spigot().getLocale()).equals("")) {
                    target.sendMessage(plugin.getString("Coins.Set target", target.spigot().getLocale()).replaceAll("%coins%", args[2]));
                }
            }
        }
    }

    private void top(CommandSender sender, String[] args, String lang) {
        if (!sender.hasPermission(getPermission() + ".top")) {
            sender.sendMessage(plugin.getString("Errors.No permissions", lang));
            return;
        }
        int i = 0;
        sender.sendMessage(plugin.getString("Coins.Top.Header", lang));
        for (Map.Entry<String, Double> ent : CoinsAPI.getTopPlayers(10).entrySet()) {
            i++;
            sender.sendMessage(plugin.getString("Coins.Top.List", lang).replaceAll("%top%", String.valueOf(i)).replaceAll("%player%", ent.getKey()).replaceAll("%coins%", df.format(ent.getValue())));
        }
    }

    /**
     * TODO List:
     * <ul>
     * <li>add a command to get all multipliers for a player</li>
     * <li>add a command to enable any multiplier by the ID</li>
     * <li>add an argument to change multiplier type</li>
     * <li>add a command to edit existing multipliers</li>
     * <li>update messages files to match new command</li>
     * </ul>
     */
    private void multiplier(CommandSender sender, String[] args, String lang) {
        if (sender.hasPermission(getPermission() + ".admin.multiplier") && args.length >= 2) {
            if (args[1].equalsIgnoreCase("help")) {
                plugin.getMessages(lang).getStringList("Help.Multiplier").forEach(line -> {
                    sender.sendMessage(StringUtils.rep(line));
                });
            }
            if (args[1].equalsIgnoreCase("create")) {
                if (args.length >= 5 && CoinsAPI.isindb(args[2])) {
                    if (!isNumber(args[3]) || !isNumber(args[4])) {
                        sender.sendMessage(plugin.getString("Help.Multiplier Create", lang));
                    }
                    int multiplier = Integer.parseInt(args[3]);
                    int minutes = Integer.parseInt(args[4]);
                    plugin.getDatabase().createMultiplier(plugin.getUniqueId(args[2], false), multiplier, minutes, ((args.length == 6 && !args[5].equals("")) ? args[5] : plugin.getConfig().getServerName()), MultiplierType.SERVER);
                    sender.sendMessage(plugin.getString("Multipliers.Created", lang).replaceAll("%player%", args[2]));
                } else {
                    sender.sendMessage(plugin.getString("Help.Multiplier Create", lang));
                }
            }
            if (args[1].equalsIgnoreCase("get")) {
                sender.sendMessage(CoinsAPI.getMultipliers().stream().findFirst().get().getMultiplierTimeFormatted());
            }
            return;
        }
        if (args.length == 1) {
            if (sender instanceof Player) {
                PaginatedMenu.createPaginatedGUI((Player) sender, Lists.newLinkedList(CoinsAPI.getAllMultipliersFor(((Player) sender).getUniqueId()))).open((Player) sender);
            } else {
                sender.sendMessage(plugin.getString("Errors.Console", lang));
            }
        } else {
            sender.sendMessage(plugin.getString("Help.Multiplier Usage", lang));
        }
    }

    private void execute(CommandSender sender, String[] args, String lang) {
        if (sender instanceof Player) {
            Executor ex = ExecutorManager.getExecutor(args[1]);
            if (ex == null) {
                sender.sendMessage(plugin.getString("Errors.No Execute", lang));
            } else {
                if (ex.getCost() > 0) {
                    if (CoinsAPI.getCoins(((Player) sender).getUniqueId()) >= ex.getCost()) {
                        CoinsAPI.takeCoins(((Player) sender).getUniqueId(), ex.getCost());
                    } else {
                        sender.sendMessage(plugin.getString("Errors.No Coins", lang));
                        return;
                    }
                }
                if (!ex.getCommands().isEmpty()) {
                    plugin.getBootstrap().runSync(() -> { // who knows ¯\_(ツ)_/¯
                        String command;
                        for (String str : ex.getCommands()) {
                            command = StringUtils.rep(str).replaceAll("%player%", sender.getName());
                            if (command.startsWith("message:")) {
                                sender.sendMessage(StringUtils.rep(command.replaceFirst("message:", "")));
                            } else if (command.startsWith("broadcast:")) {
                                Bukkit.getServer().broadcastMessage(StringUtils.rep(command.replaceFirst("broadcast:", "")));
                            } else {
                                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
                            }
                        }
                    });
                }
            }
        } else {
            sender.sendMessage(plugin.getString("Errors.Console", lang));
        }
    }

    private void importer(CommandSender sender, String[] args, String lang) {
        if (sender instanceof Player) {
            sender.sendMessage(StringUtils.rep("%prefix% &cThis command must be executed from the console."));
            return;
        }
        if (args.length == 2) {
            boolean worked = false;
            ImportManager importManager = new ImportManager();
            for (ImportManager.PluginToImport pluginToImport : ImportManager.PluginToImport.values()) {
                if (pluginToImport.toString().equals(args[1].toUpperCase())) {
                    worked = true;
                    importManager.importFrom(pluginToImport);
                    break;
                }
            }
            if (!worked) {
                sender.sendMessage(StringUtils.rep("%prefix% You specified an invalid plugin to import, possible values:"));
                sender.sendMessage(Arrays.toString(ImportManager.PluginToImport.values()));
            }
        } else {
            sender.sendMessage(StringUtils.rep("%prefix% Command usage: /coins import <plugin>"));
            sender.sendMessage(StringUtils.rep("&cCurrently supported plugins to import: " + Arrays.toString(ImportManager.PluginToImport.values())));
        }
    }

    private void importDB(CommandSender sender, String[] args, String lang) {
        if (sender instanceof Player) {
            sender.sendMessage(StringUtils.rep("%prefix% &cThis command must be executed from the console."));
            return;
        }
        if (args.length == 2) {
            boolean worked = false;
            ImportManager importManager = new ImportManager();
            for (StorageType storage : StorageType.values()) {
                if (storage.toString().equals(args[1].toUpperCase())) {
                    worked = true;
                    importManager.importFromStorage(storage);
                    break;
                }
            }
            if (!worked) {
                sender.sendMessage(StringUtils.rep("%prefix% You specified an invalid storage to import, possible values:"));
                sender.sendMessage(Arrays.toString(StorageType.values()));
            }
        } else {
            sender.sendMessage(StringUtils.rep("%prefix% Command usage: /coins importdb <storage>"));
            sender.sendMessage(StringUtils.rep("&cCurrently supported storages to import: " + Arrays.toString(StorageType.values())));
        }
    }

    private void reload(CommandSender sender) {
        if (sender.hasPermission(getPermission() + ".admin.reload")) {
            if (plugin.getConfig().getBoolean("Vault.Use", false)) {
                new CoinsEconomy((CoinsBukkitMain) plugin.getBootstrap()).shutdown();
            }
            plugin.getConfig().reload();
            plugin.reloadMessages();
            if (plugin.getConfig().getBoolean("Vault.Use", false)) {
                new CoinsEconomy((CoinsBukkitMain) plugin.getBootstrap()).setup();
            }
            ExecutorManager.getExecutors().clear();
            plugin.getBootstrap().getPlugin().loadExecutors();
            if (plugin.getConfig().useBungee()) {
                plugin.getMessagingService().getMultipliers();
                plugin.getMessagingService().getExecutors();
            }
            sender.sendMessage(StringUtils.rep("%prefix% Reloaded config and all loaded messages files. If you want reload the command, you need to restart the server."));
        }
    }

    private void about(CommandSender sender) {
        sender.sendMessage(StringUtils.rep("%prefix% Coins plugin by Beelzebu, plugin info:"));
        sender.sendMessage("");
        sender.sendMessage(StringUtils.rep(" &cVersion:&7 " + plugin.getBootstrap().getVersion()));
        sender.sendMessage(StringUtils.rep(" &cExecutors:&7 " + ExecutorManager.getExecutors().size()));
        sender.sendMessage(StringUtils.rep(" &cStorage Type:&7 " + plugin.getStorageType()));
        sender.sendMessage(StringUtils.rep(" &cMultipliers in cache:&7 " + plugin.getCache().getMultipliers()));
        sender.sendMessage(StringUtils.rep(" &cPlayers in cache:&7 " + plugin.getCache().getPlayers().size()));
        sender.sendMessage("");
        sender.sendMessage(StringUtils.rep(" &cSource Code:&7 https://github.com/Beelzebu/Coins"));
        sender.sendMessage(StringUtils.rep(" &cLicense:&7 GNU AGPL v3 (&ahttp://www.gnu.org/licenses/#AGPL&7)"));
        sender.sendMessage("");
    }

    private boolean isNumber(String number) {
        if (number == null) {
            return false;
        }
        try {
            Double.parseDouble(number);
            return true;
        } catch (NumberFormatException ex) {
            return false;
        }
    }
}
