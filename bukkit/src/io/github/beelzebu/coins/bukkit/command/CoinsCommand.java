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
import io.github.beelzebu.coins.CoinsAPI;
import io.github.beelzebu.coins.MultiplierType;
import io.github.beelzebu.coins.bukkit.CoinsBukkitMain;
import io.github.beelzebu.coins.bukkit.menus.PaginatedMenu;
import io.github.beelzebu.coins.bukkit.utils.CoinsEconomy;
import io.github.beelzebu.coins.common.CacheManager;
import io.github.beelzebu.coins.common.CoinsCore;
import io.github.beelzebu.coins.common.database.StorageType;
import io.github.beelzebu.coins.common.executor.Executor;
import io.github.beelzebu.coins.common.executor.ExecutorManager;
import io.github.beelzebu.coins.common.importer.ImportManager;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 *
 * @author Beelzebu
 */
public class CoinsCommand extends Command {

    private final CoinsCore core = CoinsCore.getInstance();
    private final DecimalFormat df = new DecimalFormat("#.#");

    public CoinsCommand(String command) {
        super(command);
    }

    @Override
    public boolean execute(CommandSender sender, String alias, String[] args) {
        String lang = sender instanceof Player ? ((Player) sender).spigot().getLocale().split("_")[0] : "";
        if (!sender.hasPermission(getPermission())) {
            sender.sendMessage(core.getString("Errors.No permissions", lang));
            return true;
        }
        core.getBootstrap().runAsync(() -> {
            if (args.length == 0) {
                if (sender instanceof Player) {
                    sender.sendMessage(core.getString("Coins.Own coins", lang).replaceAll("%coins%", CoinsAPI.getCoinsString(sender.getName())));
                } else {
                    sender.sendMessage(core.getString("Errors.Console", ""));
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
                sender.sendMessage(core.getString("Errors.Unknown command", lang));
            }
        });
        return true;
    }

    private void help(CommandSender sender, String[] args, String lang) {
        core.getMessages(lang).getStringList("Help.User").forEach(line -> sender.sendMessage(core.rep(line)));
        if (sender.hasPermission("coins.admin")) {
            core.getMessages(lang).getStringList("Help.Admin").forEach(line -> sender.sendMessage(core.rep(line)));
        }
    }

    private void target(CommandSender sender, String[] args, String lang) {
        sender.sendMessage(core.getString("Coins.Get", lang).replaceAll("%coins%", CoinsAPI.getCoinsString(args[0])).replaceAll("%target%", args[0]));
    }

    private void pay(CommandSender sender, String[] args, String lang) {
        if (!sender.hasPermission("coins.user.pay")) {
            sender.sendMessage(core.getString("Errors.No permissions", lang));
            return;
        }
        if (args.length < 3 || args[1].equalsIgnoreCase("?") || args.length == 3 && !isNumber(args[2])) {
            sender.sendMessage(core.getString("Help.Pay Usage", lang));
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
                            if (!core.getString("Coins.Pay", lang).equals("")) {
                                sender.sendMessage(core.getString("Coins.Pay", lang).replaceAll("%coins%", new DecimalFormat("#.#").format(coins)).replaceAll("%target%", target.getName()));
                            }
                            CoinsAPI.addCoins(args[1], coins, false);
                            if (!core.getString("Coins.Pay target", target.spigot().getLocale()).equals("")) {
                                target.sendMessage(core.getString("Coins.Pay target", target.spigot().getLocale()).replaceAll("%coins%", df.format(coins)).replaceAll("%from%", sender.getName()));
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
            return;
        }
        if (!(sender instanceof Player)) {
            sender.sendMessage(core.getString("Errors.Console", lang));
        }
    }

    private void give(CommandSender sender, String[] args, String lang) {
        if (!sender.hasPermission("coins.admin") || !sender.hasPermission("coins.admin.give")) {
            sender.sendMessage(core.getString("Errors.No permissions", lang));
            return;
        }
        if (args.length < 3 || args.length > 4 || args.length >= 3 && !isNumber(args[2])) {
            sender.sendMessage(core.getString("Help.Give Usage", lang));
            return;
        }
        if (!CoinsAPI.isindb(args[1])) {
            sender.sendMessage(core.getString("Errors.Unknown player", lang).replaceAll("%target%", args[1]));
            return;
        }
        String multiplier = "";
        boolean multiply = false;
        if (args.length == 3 || args.length == 4) {
            double coins = Double.parseDouble(args[2]);
            if (core.getBootstrap().isOnline(core.getUUID(args[1], false)) && args.length == 4 && args[3].equalsIgnoreCase("true")) {
                multiply = true;
                Player target = Bukkit.getPlayer(args[1]);
                int amount = CoinsAPI.getMultiplier() != null ? CoinsAPI.getMultiplier().getBaseData().getAmount() : 1;
                if (amount > 1) {
                    multiplier = core.getString("Multipliers.Format", target.spigot().getLocale()).replaceAll("%multiplier%", df.format(amount)).replaceAll("%enabler%", CoinsAPI.getMultiplier().getEnablerName());
                }
            }
            if (core.getBootstrap().isOnline(core.getUUID(args[1], false))) {
                Player target = Bukkit.getPlayer(core.getUUID(args[1], false));
                if (!core.getString("Coins.Give target", target.spigot().getLocale()).equals("")) {
                    target.sendMessage(core.getString("Coins.Give target", target.spigot().getLocale()).replaceAll("%coins%", df.format(coins)).replaceAll("%multiplier_format%", multiplier));
                }
            }
            CoinsAPI.addCoins(args[1], coins, multiply);
            if (!core.getString("Coins.Give", lang).equals("")) {
                sender.sendMessage(core.getString("Coins.Give", lang).replaceAll("%coins%", df.format(coins)).replaceAll("%target%", args[1]));
            }
        } else {
            sender.sendMessage(core.getString("Errors.Unknown command", lang));
        }
    }

    private void take(CommandSender sender, String[] args, String lang) {
        if (!sender.hasPermission("coins.admin") || !sender.hasPermission("coins.admin.take")) {
            sender.sendMessage(core.getString("Errors.No permissions", lang));
            return;
        }
        if (args.length < 3 || args.length == 3 && !isNumber(args[2])) {
            sender.sendMessage(core.getString("Help.Take Usage", lang));
            return;
        }
        if (!CoinsAPI.isindb(args[1])) {
            sender.sendMessage(core.getString("Errors.Unknown player", lang).replaceAll("%target%", args[1]));
            return;
        }
        double currentCoins = CoinsAPI.getCoins(args[1]);
        double coins = Double.parseDouble(args[2]);
        if (currentCoins < coins) {
            sender.sendMessage(core.getString("Errors.No Negative", lang));
            return;
        }
        double finalCoins = currentCoins - coins;
        if (args.length == 3) {
            Player target = Bukkit.getPlayer(args[1]);
            if (!core.getString("Coins.Take", lang).equals("")) {
                sender.sendMessage(core.getString("Coins.Take", lang).replaceAll("%coins%", df.format(coins)).replaceAll("%newcoins%", df.format(finalCoins)).replaceAll("%target%", args[1]));
            }
            if (target != null) {
                if (!core.getString("Coins.Take target", target.spigot().getLocale()).equals("")) {
                    target.sendMessage(core.getString("Coins.Take target", target.spigot().getLocale()).replaceAll("%coins%", df.format(finalCoins)));
                }
            }
        }
    }

    private void reset(CommandSender sender, String[] args, String lang) {
        if (!sender.hasPermission("coins.admin") || !sender.hasPermission("coins.admin.reset")) {
            sender.sendMessage(core.getString("Errors.No permissions", lang));
            return;
        }
        if (args.length < 2) {
            sender.sendMessage(core.getString("Help.Reset Usage", lang));
            return;
        }
        if (CoinsAPI.isindb(args[1])) {
            CoinsAPI.resetCoins(args[1]);
        } else {
            sender.sendMessage(core.getString("Errors.Unknown player", lang).replaceAll("%target%", args[1]));
            return;
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
    }

    private void set(CommandSender sender, String[] args, String lang) {
        if (!sender.hasPermission("coins.admin")) {
            sender.sendMessage(core.getString("Errors.No permissions", lang));
            return;
        }
        if (args.length < 3 || args.length == 3 && !isNumber(args[2])) {
            sender.sendMessage(core.getString("Help.Set Usage", lang));
            return;
        }
        if (args.length == 3) {
            if (args[0].equalsIgnoreCase("set")) {
                double coins = Double.parseDouble(args[2]);
                if (CoinsAPI.isindb(args[1])) {
                    CoinsAPI.setCoins(args[1], coins);
                    if (!core.getString("Coins.Set", lang).equals("")) {
                        sender.sendMessage(core.getString("Coins.Set", lang).replaceAll("%target%", args[1]).replaceAll("%coins%", df.format(coins)));
                    }
                } else {
                    sender.sendMessage(core.getString("Errors.Unknown player", lang).replaceAll("%target%", args[1]));
                    return;
                }
                Player target = Bukkit.getPlayer(args[1]);
                if (target != null && !core.getString("Coins.Set target", target.spigot().getLocale()).equals("")) {
                    target.sendMessage(core.getString("Coins.Set target", target.spigot().getLocale()).replaceAll("%coins%", args[2]));
                }
            }
        }
    }

    private void top(CommandSender sender, String[] args, String lang) {
        int i = 0;
        sender.sendMessage(core.getString("Coins.Top.Header", lang));
        for (Map.Entry<String, Double> ent : CoinsAPI.getTopPlayers(10).entrySet()) {
            i++;
            sender.sendMessage(core.getString("Coins.Top.List", lang).replaceAll("%top%", String.valueOf(i)).replaceAll("%player%", ent.getKey()).replaceAll("%coins%", df.format(ent.getValue())));
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
        if ((sender.hasPermission("coins.admin") || sender.hasPermission("coins.admin.multiplier")) && args.length >= 2) {
            if (args[1].equalsIgnoreCase("help")) {
                core.getMessages(lang).getStringList("Help.Multiplier").forEach(line -> {
                    sender.sendMessage(core.rep(line));
                });
            }
            if (args[1].equalsIgnoreCase("create")) {
                if (args.length >= 5 && CoinsAPI.isindb(args[2])) {
                    if (!isNumber(args[3]) || !isNumber(args[4])) {
                        sender.sendMessage(core.getString("Help.Multiplier Create", lang));
                    }
                    int multiplier = Integer.parseInt(args[3]);
                    int minutes = Integer.parseInt(args[4]);
                    core.getDatabase().createMultiplier(core.getUUID(args[2], false), multiplier, minutes, ((args.length == 6 && !args[5].equals("")) ? args[5] : core.getConfig().getServerName()), MultiplierType.SERVER);
                    sender.sendMessage(core.getString("Multipliers.Created", lang).replaceAll("%player%", args[2]));
                } else {
                    sender.sendMessage(core.getString("Help.Multiplier Create", lang));
                }
            }
            if (args[1].equalsIgnoreCase("get")) {
                sender.sendMessage(CoinsAPI.getMultiplier().getMultiplierTimeFormated());
            }
            return;
        }
        if (args.length == 1) {
            if (sender instanceof Player) {
                PaginatedMenu.createPaginatedGUI((Player) sender, Lists.newLinkedList(CoinsAPI.getAllMultipliersFor(((Player) sender).getUniqueId()))).open((Player) sender);
            } else {
                sender.sendMessage(core.getString("Errors.Console", lang));
            }
        } else {
            sender.sendMessage(core.getString("Help.Multiplier Usage", lang));
        }
    }

    private void execute(CommandSender sender, String[] args, String lang) {
        if (sender instanceof Player) {
            Executor ex = ExecutorManager.getExecutor(args[1]);
            if (ex == null) {
                sender.sendMessage(core.getString("Errors.No Execute", lang));
            } else {
                if (ex.getCost() > 0) {
                    if (CoinsAPI.getCoins(((Player) sender).getUniqueId()) >= ex.getCost()) {
                        CoinsAPI.takeCoins(((Player) sender).getUniqueId(), ex.getCost());
                    } else {
                        sender.sendMessage(core.getString("Errors.No Coins", lang));
                        return;
                    }
                }
                if (!ex.getCommands().isEmpty()) {
                    core.getBootstrap().runSync(() -> { // who knows ¯\_(ツ)_/¯
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
    }

    private void importer(CommandSender sender, String[] args, String lang) {
        if (sender instanceof Player) {
            sender.sendMessage(core.rep("%prefix% &cThis command must be executed from the console."));
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
                sender.sendMessage(core.rep("%prefix% You specified an invalid plugin to import, possible values:"));
                sender.sendMessage(Arrays.toString(ImportManager.PluginToImport.values()));
            }
        } else {
            sender.sendMessage(core.rep("%prefix% Command usage: /coins import <plugin>"));
            sender.sendMessage(core.rep("&cCurrently supported plugins to import: " + Arrays.toString(ImportManager.PluginToImport.values())));
        }
    }

    private void importDB(CommandSender sender, String[] args, String lang) {
        if (sender instanceof Player) {
            sender.sendMessage(core.rep("%prefix% &cThis command must be executed from the console."));
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
                sender.sendMessage(core.rep("%prefix% You specified an invalid storage to import, possible values:"));
                sender.sendMessage(Arrays.toString(StorageType.values()));
            }
        } else {
            sender.sendMessage(core.rep("%prefix% Command usage: /coins importdb <storage>"));
            sender.sendMessage(core.rep("&cCurrently supported storages to import: " + Arrays.toString(StorageType.values())));
        }
    }

    private void reload(CommandSender sender) {
        if (sender.hasPermission("coins.admin.reload")) {
            if (core.getConfig().getBoolean("Vault.Use", false)) {
                new CoinsEconomy((CoinsBukkitMain) core.getBootstrap()).shutdown();
            }
            core.getConfig().reload();
            core.reloadMessages();
            if (core.getConfig().getBoolean("Vault.Use", false)) {
                new CoinsEconomy((CoinsBukkitMain) core.getBootstrap()).setup();
            }
            ExecutorManager.getExecutors().clear();
            core.getBootstrap().getPlugin().loadExecutors();
            if (core.getConfig().useBungee()) {
                core.getMessagingService().getMultipliers();
                core.getMessagingService().getExecutors();
            }
            sender.sendMessage(core.rep("%prefix% Reloaded config and all loaded messages files. If you want reload the command, you need to restart the server."));
        }
    }

    private void about(CommandSender sender) {
        if (sender.hasPermission("coins.admin") || sender.getName().equals("Beelzebu")) {
            sender.sendMessage(core.rep("%prefix% Coins plugin by Beelzebu, plugin info:"));
            sender.sendMessage("");
            sender.sendMessage(core.rep(" &cVersion:&7 " + core.getBootstrap().getVersion()));
            sender.sendMessage(core.rep(" &cExecutors:&7 " + ExecutorManager.getExecutors().size()));
            sender.sendMessage(core.rep(" &cStorage Type:&7 " + core.getStorageType()));
            sender.sendMessage(core.rep(" &cMultipliers in cache:&7 " + CacheManager.getMultipliersData().asMap().keySet()));
            sender.sendMessage(core.rep(" &cPlayers in cache:&7 " + CacheManager.getPlayersData().asMap().size()));
            sender.sendMessage("");
            sender.sendMessage(core.rep(" &cSource Code:&7 https://github.com/Beelzebu/Coins"));
            sender.sendMessage(core.rep(" &cLicense:&7 GNU AGPL v3 (&ahttp://www.gnu.org/licenses/#AGPL&7)"));
            sender.sendMessage("");
        }
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
